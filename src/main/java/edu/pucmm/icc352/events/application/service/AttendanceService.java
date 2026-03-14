package edu.pucmm.icc352.events.application.service;

import edu.pucmm.icc352.events.application.security.AuthenticatedUser;
import edu.pucmm.icc352.events.domain.model.Event;
import edu.pucmm.icc352.events.domain.model.EventStatus;
import edu.pucmm.icc352.events.domain.model.Registration;
import edu.pucmm.icc352.events.infrastructure.persistence.RegistrationRepository;
import edu.pucmm.icc352.events.infrastructure.persistence.TransactionManager;
import edu.pucmm.icc352.events.infrastructure.qr.QrImageDecoder;
import edu.pucmm.icc352.events.infrastructure.qr.QrPayload;
import edu.pucmm.icc352.events.infrastructure.qr.QrPayloadCodec;
import edu.pucmm.icc352.events.shared.error.AppException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class AttendanceService {
    private final TransactionManager transactionManager;
    private final RegistrationRepository registrationRepository;
    private final EventService eventService;
    private final QrImageDecoder qrImageDecoder;
    private final QrPayloadCodec qrPayloadCodec;
    private final Clock clock;

    public AttendanceService(
            TransactionManager transactionManager,
            RegistrationRepository registrationRepository,
            EventService eventService,
            QrImageDecoder qrImageDecoder,
            QrPayloadCodec qrPayloadCodec,
            Clock clock
    ) {
        this.transactionManager = transactionManager;
        this.registrationRepository = registrationRepository;
        this.eventService = eventService;
        this.qrImageDecoder = qrImageDecoder;
        this.qrPayloadCodec = qrPayloadCodec;
        this.clock = clock;
    }

    public Registration markAttendance(AuthenticatedUser actor, Long eventId, String qrContent) {
        return markAttendance(actor, eventId, decodePayload(qrContent));
    }

    public Registration markAttendanceFromImage(AuthenticatedUser actor, Long eventId, byte[] qrImage) {
        try {
            String qrContent = qrImageDecoder.decode(qrImage);
            return markAttendance(actor, eventId, decodePayload(qrContent));
        } catch (IllegalArgumentException exception) {
            throw AppException.badRequest(exception.getMessage());
        }
    }

    private Registration markAttendance(AuthenticatedUser actor, Long eventId, QrPayload payload) {
        return transactionManager.write(session -> {
            Event event = eventService.getManagedEvent(session, actor, eventId);
            if (event.getStatus() == EventStatus.CANCELLED) {
                throw AppException.conflict("No se puede registrar asistencia en un evento cancelado.");
            }
            if (!event.startsOn(LocalDate.now(clock))) {
                throw AppException.conflict("La asistencia solo puede registrarse el día del evento.");
            }

            if (payload.eventId() == null || payload.userId() == null || payload.token() == null || payload.token().isBlank()) {
                throw AppException.badRequest("El contenido del QR está incompleto.");
            }
            if (!eventId.equals(payload.eventId())) {
                throw AppException.badRequest("El QR no pertenece al evento indicado.");
            }

            Registration registration = registrationRepository.findByValidationToken(session, payload.token())
                    .orElseThrow(() -> AppException.notFound("No existe una inscripción asociada al QR."));

            if (!registration.getEvent().getId().equals(payload.eventId())
                    || !registration.getUser().getId().equals(payload.userId())) {
                throw AppException.badRequest("El contenido del QR fue alterado o no es válido.");
            }

            registration.markAttendance(LocalDateTime.now(clock));
            return registration;
        });
    }

    private QrPayload decodePayload(String qrContent) {
        if (qrContent == null || qrContent.isBlank()) {
            throw AppException.badRequest("El contenido del QR es obligatorio.");
        }
        try {
            return qrPayloadCodec.decode(qrContent);
        } catch (IllegalArgumentException exception) {
            throw AppException.badRequest("El contenido del QR no es válido.");
        }
    }
}
