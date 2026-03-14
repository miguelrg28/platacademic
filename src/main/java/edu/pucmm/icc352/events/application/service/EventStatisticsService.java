package edu.pucmm.icc352.events.application.service;

import edu.pucmm.icc352.events.application.security.AuthenticatedUser;
import edu.pucmm.icc352.events.domain.model.Registration;
import edu.pucmm.icc352.events.infrastructure.persistence.RegistrationRepository;
import edu.pucmm.icc352.events.infrastructure.persistence.TransactionManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class EventStatisticsService {
    private final TransactionManager transactionManager;
    private final RegistrationRepository registrationRepository;
    private final EventService eventService;

    public EventStatisticsService(
            TransactionManager transactionManager,
            RegistrationRepository registrationRepository,
            EventService eventService
    ) {
        this.transactionManager = transactionManager;
        this.registrationRepository = registrationRepository;
        this.eventService = eventService;
    }

    public EventSummary getSummary(AuthenticatedUser actor, Long eventId) {
        return transactionManager.read(session -> {
            eventService.getManagedEvent(session, actor, eventId);
            List<Registration> registrations = registrationRepository.findByEventId(session, eventId);

            long totalRegistered = registrations.stream()
                    .filter(Registration::isActive)
                    .count();
            long totalAttendees = registrations.stream()
                    .filter(Registration::attendanceMarked)
                    .count();
            double attendanceRate = totalRegistered == 0
                    ? 0
                    : (double) totalAttendees * 100.0 / totalRegistered;

            Map<LocalDate, Long> registrationsByDay = registrations.stream()
                    .collect(Collectors.groupingBy(
                            registration -> registration.getCreatedAt().toLocalDate(),
                            TreeMap::new,
                            Collectors.counting()
                    ));

            Map<Integer, Long> attendanceByHour = registrations.stream()
                    .filter(Registration::attendanceMarked)
                    .collect(Collectors.groupingBy(
                            registration -> registration.getAttendanceMarkedAt().getHour(),
                            TreeMap::new,
                            Collectors.counting()
                    ));

            return new EventSummary(totalRegistered, totalAttendees, attendanceRate, registrationsByDay, attendanceByHour);
        });
    }

    public record EventSummary(
            long totalRegistered,
            long totalAttendees,
            double attendancePercentage,
            Map<LocalDate, Long> registrationsByDay,
            Map<Integer, Long> attendanceByHour
    ) {
    }
}
