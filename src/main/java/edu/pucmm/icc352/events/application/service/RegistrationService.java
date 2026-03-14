package edu.pucmm.icc352.events.application.service;

import edu.pucmm.icc352.events.application.security.AuthenticatedUser;
import edu.pucmm.icc352.events.domain.model.Event;
import edu.pucmm.icc352.events.domain.model.Registration;
import edu.pucmm.icc352.events.domain.model.User;
import edu.pucmm.icc352.events.infrastructure.persistence.EventRepository;
import edu.pucmm.icc352.events.infrastructure.persistence.RegistrationRepository;
import edu.pucmm.icc352.events.infrastructure.persistence.TransactionManager;
import edu.pucmm.icc352.events.infrastructure.persistence.UserRepository;
import edu.pucmm.icc352.events.infrastructure.qr.QrPayload;
import edu.pucmm.icc352.events.infrastructure.qr.QrPayloadCodec;
import edu.pucmm.icc352.events.shared.error.AppException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class RegistrationService {
    private final TransactionManager transactionManager;
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final EventService eventService;
    private final QrPayloadCodec qrPayloadCodec;
    private final Clock clock;

    public RegistrationService(
            TransactionManager transactionManager,
            EventRepository eventRepository,
            RegistrationRepository registrationRepository,
            UserRepository userRepository,
            EventService eventService,
            QrPayloadCodec qrPayloadCodec,
            Clock clock
    ) {
        this.transactionManager = transactionManager;
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.eventService = eventService;
        this.qrPayloadCodec = qrPayloadCodec;
        this.clock = clock;
    }

    public Registration register(AuthenticatedUser actor, Long eventId) {
        return transactionManager.write(session -> {
            Event event = eventRepository.findById(session, eventId)
                    .orElseThrow(() -> AppException.notFound("Evento no encontrado."));

            validateRegistrationWindow(event);
            if (!event.isVisibleToParticipants()) {
                throw AppException.conflict("Solo los eventos publicados aceptan inscripciones.");
            }

            long activeRegistrations = registrationRepository.countActiveByEventId(session, eventId);
            if (activeRegistrations >= event.getMaxCapacity()) {
                throw AppException.conflict("El evento ya alcanzó su cupo máximo.");
            }

            User user = userRepository.findById(session, actor.id())
                    .orElseThrow(() -> AppException.notFound("Usuario no encontrado."));

            Registration existing = registrationRepository.findByEventIdAndUserId(session, eventId, actor.id()).orElse(null);
            if (existing != null) {
                if (existing.isActive()) {
                    throw AppException.conflict("Ya estás inscrito en este evento.");
                }
                existing.reactivate(newToken(), LocalDateTime.now(clock));
                return existing;
            }

            Registration registration = Registration.create(event, user, newToken(), LocalDateTime.now(clock));
            registrationRepository.save(session, registration);
            return registration;
        });
    }

    public void cancelOwnRegistration(AuthenticatedUser actor, Long eventId) {
        transactionManager.write(session -> {
            Registration registration = registrationRepository.findByEventIdAndUserId(session, eventId, actor.id())
                    .orElseThrow(() -> AppException.notFound("No existe una inscripción para este usuario."));

            if (!registration.getEvent().getStartsAt().isAfter(LocalDateTime.now(clock))) {
                throw AppException.conflict("La inscripción solo puede cancelarse antes de la fecha del evento.");
            }

            registration.cancel(LocalDateTime.now(clock));
            return null;
        });
    }

    public QrTicket getOwnQrTicket(AuthenticatedUser actor, Long eventId) {
        return transactionManager.read(session -> {
            Registration registration = registrationRepository.findByEventIdAndUserId(session, eventId, actor.id())
                    .orElseThrow(() -> AppException.notFound("No existe una inscripción activa para este usuario."));

            if (!registration.isActive()) {
                throw AppException.conflict("La inscripción no está activa.");
            }

            QrPayload payload = new QrPayload(eventId, actor.id(), registration.getValidationToken());
            return new QrTicket(registration, qrPayloadCodec.encode(payload));
        });
    }

    public List<Registration> listEventRegistrations(AuthenticatedUser actor, Long eventId) {
        return transactionManager.read(session -> {
            eventService.getManagedEvent(session, actor, eventId);
            return registrationRepository.findByEventId(session, eventId);
        });
    }

    public List<Registration> listOwnRegistrations(AuthenticatedUser actor) {
        return transactionManager.read(session -> registrationRepository.findActiveByUserId(session, actor.id()));
    }

    private void validateRegistrationWindow(Event event) {
        event.ensureNotCancelled();
        if (event.hasStarted(LocalDateTime.now(clock))) {
            throw AppException.conflict("No es posible inscribirse en un evento ya iniciado.");
        }
    }

    private String newToken() {
        return UUID.randomUUID().toString();
    }

    public record QrTicket(
            Registration registration,
            String qrContent
    ) {
    }
}
