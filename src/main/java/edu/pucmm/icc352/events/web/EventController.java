package edu.pucmm.icc352.events.web;

import edu.pucmm.icc352.events.application.security.AuthGuard;
import edu.pucmm.icc352.events.application.service.AttendanceService;
import edu.pucmm.icc352.events.application.service.EventService;
import edu.pucmm.icc352.events.application.service.EventStatisticsService;
import edu.pucmm.icc352.events.application.service.RegistrationService;
import edu.pucmm.icc352.events.infrastructure.qr.QrCodeGenerator;
import edu.pucmm.icc352.events.shared.error.AppException;
import io.javalin.Javalin;
import io.javalin.http.UploadedFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;
import static io.javalin.apibuilder.ApiBuilder.put;

public final class EventController {
    private final AuthGuard authGuard;
    private final EventService eventService;
    private final RegistrationService registrationService;
    private final AttendanceService attendanceService;
    private final EventStatisticsService eventStatisticsService;
    private final QrCodeGenerator qrCodeGenerator;

    public EventController(
            AuthGuard authGuard,
            EventService eventService,
            RegistrationService registrationService,
            AttendanceService attendanceService,
            EventStatisticsService eventStatisticsService,
            QrCodeGenerator qrCodeGenerator
    ) {
        this.authGuard = authGuard;
        this.eventService = eventService;
        this.registrationService = registrationService;
        this.attendanceService = attendanceService;
        this.eventStatisticsService = eventStatisticsService;
        this.qrCodeGenerator = qrCodeGenerator;
    }

    public void register(Javalin app) {
        app.unsafe.routes.apiBuilder(() -> {
            get("/api/health", ctx -> ctx.json(Map.of("status", "ok")));

            get("/api/events", ctx -> ctx.json(eventService.listAvailable().stream()
                    .map(ApiDtos::toEventResponse)
                    .toList()));

            get("/api/events/managed", ctx -> {
                var actor = authGuard.requireOrganizerOrAdmin(ctx);
                ctx.json(eventService.listManagedEvents(actor).stream()
                        .map(ApiDtos::toEventResponse)
                        .toList());
            });

            get("/api/my/registrations", ctx -> {
                var actor = authGuard.requireAuthenticated(ctx);
                ctx.json(registrationService.listOwnRegistrations(actor).stream()
                        .map(ApiDtos::toRegistrationResponse)
                        .toList());
            });

            post("/api/events", ctx -> {
                var actor = authGuard.requireOrganizerOrAdmin(ctx);
                ApiDtos.EventRequest request = ctx.bodyAsClass(ApiDtos.EventRequest.class);
                var event = eventService.create(actor, toCommand(request));
                ctx.status(201).json(ApiDtos.toEventResponse(event));
            });

            put("/api/events/{eventId}", ctx -> {
                var actor = authGuard.requireOrganizerOrAdmin(ctx);
                ApiDtos.EventRequest request = ctx.bodyAsClass(ApiDtos.EventRequest.class);
                var event = eventService.update(actor, eventId(ctx), toCommand(request));
                ctx.json(ApiDtos.toEventResponse(event));
            });

            post("/api/events/{eventId}/publish", ctx -> {
                var actor = authGuard.requireOrganizerOrAdmin(ctx);
                ctx.json(ApiDtos.toEventResponse(eventService.publish(actor, eventId(ctx))));
            });

            post("/api/events/{eventId}/unpublish", ctx -> {
                var actor = authGuard.requireOrganizerOrAdmin(ctx);
                ctx.json(ApiDtos.toEventResponse(eventService.unpublish(actor, eventId(ctx))));
            });

            post("/api/events/{eventId}/cancel", ctx -> {
                var actor = authGuard.requireOrganizerOrAdmin(ctx);
                ctx.json(ApiDtos.toEventResponse(eventService.cancel(actor, eventId(ctx))));
            });

            get("/api/events/{eventId}", ctx -> ctx.json(ApiDtos.toEventResponse(
                    eventService.findVisibleById(authGuard.optional(ctx), eventId(ctx))
            )));

            post("/api/events/{eventId}/registrations", ctx -> {
                var actor = authGuard.requireAuthenticated(ctx);
                var registration = registrationService.register(actor, eventId(ctx));
                ctx.status(201).json(ApiDtos.toRegistrationResponse(registration));
            });

            delete("/api/events/{eventId}/registrations/me", ctx -> {
                var actor = authGuard.requireAuthenticated(ctx);
                registrationService.cancelOwnRegistration(actor, eventId(ctx));
                ctx.status(204);
            });

            get("/api/events/{eventId}/registrations/me/qr", ctx -> {
                var actor = authGuard.requireAuthenticated(ctx);
                var ticket = registrationService.getOwnQrTicket(actor, eventId(ctx));
                ctx.json(new ApiDtos.QrResponse(
                        ApiDtos.toRegistrationResponse(ticket.registration()),
                        ticket.qrContent(),
                        "/api/events/%d/registrations/me/qr/image".formatted(eventId(ctx))
                ));
            });

            get("/api/events/{eventId}/registrations/me/qr/image", ctx -> {
                var actor = authGuard.requireAuthenticated(ctx);
                var ticket = registrationService.getOwnQrTicket(actor, eventId(ctx));
                byte[] image = qrCodeGenerator.generatePng(ticket.qrContent());
                ctx.contentType("image/png");
                ctx.result(new ByteArrayInputStream(image));
            });

            get("/api/events/{eventId}/registrations", ctx -> {
                var actor = authGuard.requireOrganizerOrAdmin(ctx);
                ctx.json(registrationService.listEventRegistrations(actor, eventId(ctx)).stream()
                        .map(ApiDtos::toRegistrationResponse)
                        .toList());
            });

            post("/api/events/{eventId}/attendance/scan", ctx -> {
                var actor = authGuard.requireOrganizerOrAdmin(ctx);
                ApiDtos.ScanQrRequest request = ctx.bodyAsClass(ApiDtos.ScanQrRequest.class);
                var registration = attendanceService.markAttendance(actor, eventId(ctx), request.qrContent());
                ctx.json(ApiDtos.toRegistrationResponse(registration));
            });

            post("/api/events/{eventId}/attendance/scan-image", ctx -> {
                var actor = authGuard.requireOrganizerOrAdmin(ctx);
                UploadedFile uploadedFile = ctx.uploadedFile("qrImage");
                if (uploadedFile == null) {
                    throw AppException.badRequest("Debes subir una imagen con el QR.");
                }

                var registration = attendanceService.markAttendanceFromImage(
                        actor,
                        eventId(ctx),
                        readBytes(uploadedFile)
                );
                ctx.json(ApiDtos.toRegistrationResponse(registration));
            });

            get("/api/events/{eventId}/summary", ctx -> {
                var actor = authGuard.requireOrganizerOrAdmin(ctx);
                ctx.json(ApiDtos.toSummaryResponse(eventStatisticsService.getSummary(actor, eventId(ctx))));
            });
        });
    }

    private EventService.UpsertEventCommand toCommand(ApiDtos.EventRequest request) {
        return new EventService.UpsertEventCommand(
                request.title(),
                request.description(),
                request.startsAt(),
                request.location(),
                request.maxCapacity()
        );
    }

    private Long eventId(io.javalin.http.Context ctx) {
        return ctx.pathParamAsClass("eventId", Long.class).get();
    }

    private byte[] readBytes(UploadedFile uploadedFile) {
        try (var content = uploadedFile.content()) {
            return content.readAllBytes();
        } catch (IOException exception) {
            throw AppException.badRequest("No se pudo leer la imagen QR enviada.");
        }
    }
}
