package edu.pucmm.icc352.events.application.service;

import edu.pucmm.icc352.events.application.security.AuthenticatedUser;
import edu.pucmm.icc352.events.domain.model.Event;
import edu.pucmm.icc352.events.domain.model.User;
import edu.pucmm.icc352.events.infrastructure.persistence.EventRepository;
import edu.pucmm.icc352.events.infrastructure.persistence.RegistrationRepository;
import edu.pucmm.icc352.events.infrastructure.persistence.TransactionManager;
import edu.pucmm.icc352.events.infrastructure.persistence.UserRepository;
import edu.pucmm.icc352.events.shared.error.AppException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public final class EventService {
    private final TransactionManager transactionManager;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository;
    private final Clock clock;

    public EventService(
            TransactionManager transactionManager,
            EventRepository eventRepository,
            UserRepository userRepository,
            RegistrationRepository registrationRepository,
            Clock clock
    ) {
        this.transactionManager = transactionManager;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.registrationRepository = registrationRepository;
        this.clock = clock;
    }

    public Event create(AuthenticatedUser actor, UpsertEventCommand command) {
        requireEventManager(actor);
        validate(command);

        return transactionManager.write(session -> {
            User creator = userRepository.findById(session, actor.id())
                    .orElseThrow(() -> AppException.notFound("Usuario no encontrado."));

            Event event = Event.create(
                    command.title().trim(),
                    command.description().trim(),
                    command.startsAt(),
                    command.location().trim(),
                    command.maxCapacity(),
                    creator,
                    LocalDateTime.now(clock)
            );
            eventRepository.save(session, event);
            return event;
        });
    }

    public Event update(AuthenticatedUser actor, Long eventId, UpsertEventCommand command) {
        requireEventManager(actor);
        validate(command);

        return transactionManager.write(session -> {
            Event event = getManagedEvent(session, actor, eventId);
            long activeRegistrations = registrationRepository.countActiveByEventId(session, eventId);
            if (command.maxCapacity() < activeRegistrations) {
                throw AppException.conflict("El nuevo cupo no puede ser menor a los inscritos actuales.");
            }

            event.updateDetails(
                    command.title().trim(),
                    command.description().trim(),
                    command.startsAt(),
                    command.location().trim(),
                    command.maxCapacity()
            );
            return event;
        });
    }

    public Event publish(AuthenticatedUser actor, Long eventId) {
        requireEventManager(actor);
        return transactionManager.write(session -> {
            Event event = getManagedEvent(session, actor, eventId);
            event.publish();
            return event;
        });
    }

    public Event unpublish(AuthenticatedUser actor, Long eventId) {
        requireEventManager(actor);
        return transactionManager.write(session -> {
            Event event = getManagedEvent(session, actor, eventId);
            event.unpublish();
            return event;
        });
    }

    public Event cancel(AuthenticatedUser actor, Long eventId) {
        requireEventManager(actor);
        return transactionManager.write(session -> {
            Event event = getManagedEvent(session, actor, eventId);
            event.cancel(LocalDateTime.now(clock));
            return event;
        });
    }

    public void delete(AuthenticatedUser actor, Long eventId) {
        if (!actor.isAdmin()) {
            throw AppException.forbidden("Solo el administrador puede eliminar eventos.");
        }

        transactionManager.write(session -> {
            Event event = eventRepository.findById(session, eventId)
                    .orElseThrow(() -> AppException.notFound("Evento no encontrado."));
            eventRepository.delete(session, event);
            return null;
        });
    }

    public List<Event> listAvailable() {
        return transactionManager.read(session -> eventRepository.findAvailable(session, LocalDateTime.now(clock)));
    }

    public List<Event> listManagedEvents(AuthenticatedUser actor) {
        requireEventManager(actor);
        return transactionManager.read(session -> eventRepository.findAll(session).stream()
                .filter(event -> actor.isAdmin() || event.isOwnedBy(actor.id()))
                .toList());
    }

    public List<Event> listAll(AuthenticatedUser actor) {
        if (!actor.isAdmin()) {
            throw AppException.forbidden("Solo el administrador puede ver todos los eventos.");
        }
        return transactionManager.read(eventRepository::findAll);
    }

    public Event findVisibleById(Optional<AuthenticatedUser> actor, Long eventId) {
        return transactionManager.read(session -> {
            Event event = eventRepository.findById(session, eventId)
                    .orElseThrow(() -> AppException.notFound("Evento no encontrado."));

            if (event.isVisibleToParticipants()) {
                return event;
            }

            if (actor.isPresent() && canManage(actor.get(), event)) {
                return event;
            }

            throw AppException.notFound("Evento no encontrado.");
        });
    }

    Event getManagedEvent(org.hibernate.Session session, AuthenticatedUser actor, Long eventId) {
        Event event = eventRepository.findById(session, eventId)
                .orElseThrow(() -> AppException.notFound("Evento no encontrado."));
        if (!canManage(actor, event)) {
            throw AppException.forbidden("No tienes permiso para gestionar este evento.");
        }
        return event;
    }

    private boolean canManage(AuthenticatedUser actor, Event event) {
        return actor.isAdmin() || event.isOwnedBy(actor.id());
    }

    private void requireEventManager(AuthenticatedUser actor) {
        if (!actor.canManageEvents()) {
            throw AppException.forbidden("Esta operación requiere rol de organizador o administrador.");
        }
    }

    private void validate(UpsertEventCommand command) {
        if (command.title() == null || command.title().isBlank()) {
            throw AppException.badRequest("El título es obligatorio.");
        }
        if (command.description() == null || command.description().isBlank()) {
            throw AppException.badRequest("La descripción es obligatoria.");
        }
        if (command.location() == null || command.location().isBlank()) {
            throw AppException.badRequest("El lugar es obligatorio.");
        }
        if (command.startsAt() == null) {
            throw AppException.badRequest("La fecha del evento es obligatoria.");
        }
        if (!command.startsAt().isAfter(LocalDateTime.now())) {
            throw AppException.badRequest("La fecha del evento debe ser futura.");
        }
        if (command.maxCapacity() == null || command.maxCapacity() <= 0) {
            throw AppException.badRequest("El cupo máximo debe ser mayor que cero.");
        }
    }

    public record UpsertEventCommand(
            String title,
            String description,
            LocalDateTime startsAt,
            String location,
            Integer maxCapacity
    ) {
    }
}
