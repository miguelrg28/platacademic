package edu.pucmm.icc352.events.web;

import edu.pucmm.icc352.events.application.security.AuthGuard;
import edu.pucmm.icc352.events.application.service.EventService;
import edu.pucmm.icc352.events.application.service.UserAdministrationService;
import io.javalin.Javalin;

import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.put;

public final class AdminController {
    private final AuthGuard authGuard;
    private final UserAdministrationService userAdministrationService;
    private final EventService eventService;

    public AdminController(
            AuthGuard authGuard,
            UserAdministrationService userAdministrationService,
            EventService eventService
    ) {
        this.authGuard = authGuard;
        this.userAdministrationService = userAdministrationService;
        this.eventService = eventService;
    }

    public void register(Javalin app) {
        app.unsafe.routes.apiBuilder(() -> {
            get("/api/admin/users", ctx -> {
                var actor = authGuard.requireAdmin(ctx);
                ctx.json(userAdministrationService.listUsers(actor).stream()
                        .map(ApiDtos::toUserResponse)
                        .toList());
            });

            put("/api/admin/users/{userId}/blocked", ctx -> {
                var actor = authGuard.requireAdmin(ctx);
                ApiDtos.BlockedStateRequest request = ctx.bodyAsClass(ApiDtos.BlockedStateRequest.class);
                var user = userAdministrationService.updateBlockedState(actor, userId(ctx), Boolean.TRUE.equals(request.blocked()));
                ctx.json(ApiDtos.toUserResponse(user));
            });

            put("/api/admin/users/{userId}/organizer-role", ctx -> {
                var actor = authGuard.requireAdmin(ctx);
                ApiDtos.OrganizerRoleRequest request = ctx.bodyAsClass(ApiDtos.OrganizerRoleRequest.class);
                var user = userAdministrationService.updateOrganizerRole(actor, userId(ctx), Boolean.TRUE.equals(request.enabled()));
                ctx.json(ApiDtos.toUserResponse(user));
            });

            get("/api/admin/events", ctx -> {
                var actor = authGuard.requireAdmin(ctx);
                ctx.json(eventService.listAll(actor).stream()
                        .map(ApiDtos::toEventResponse)
                        .toList());
            });

            delete("/api/admin/events/{eventId}", ctx -> {
                var actor = authGuard.requireAdmin(ctx);
                eventService.delete(actor, eventId(ctx));
                ctx.status(204);
            });
        });
    }

    private Long userId(io.javalin.http.Context ctx) {
        return ctx.pathParamAsClass("userId", Long.class).get();
    }

    private Long eventId(io.javalin.http.Context ctx) {
        return ctx.pathParamAsClass("eventId", Long.class).get();
    }
}
