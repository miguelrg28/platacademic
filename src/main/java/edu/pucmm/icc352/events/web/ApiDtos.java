package edu.pucmm.icc352.events.web;

import edu.pucmm.icc352.events.application.service.EventStatisticsService;
import edu.pucmm.icc352.events.domain.model.Event;
import edu.pucmm.icc352.events.domain.model.EventStatus;
import edu.pucmm.icc352.events.domain.model.Registration;
import edu.pucmm.icc352.events.domain.model.RegistrationStatus;
import edu.pucmm.icc352.events.domain.model.User;
import edu.pucmm.icc352.events.domain.model.UserRole;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ApiDtos {
    private ApiDtos() {
    }

    public record RegisterRequest(
            String username,
            String fullName,
            String email,
            String password
    ) {
    }

    public record LoginRequest(
            String username,
            String password
    ) {
    }

    public record EventRequest(
            String title,
            String description,
            LocalDateTime startsAt,
            String location,
            Integer maxCapacity
    ) {
    }

    public record BlockedStateRequest(Boolean blocked) {
    }

    public record OrganizerRoleRequest(Boolean enabled) {
    }

    public record ScanQrRequest(String qrContent) {
    }

    public record ErrorResponse(
            String code,
            String message
    ) {
    }

    public record UserResponse(
            Long id,
            String username,
            String fullName,
            String email,
            boolean active,
            boolean immutableAdmin,
            Set<UserRole> roles,
            LocalDateTime createdAt
    ) {
    }

    public record EventResponse(
            Long id,
            String title,
            String description,
            LocalDateTime startsAt,
            String location,
            int maxCapacity,
            EventStatus status,
            Long createdByUserId,
            LocalDateTime createdAt,
            LocalDateTime cancelledAt
    ) {
    }

    public record RegistrationResponse(
            Long id,
            Long eventId,
            String eventTitle,
            LocalDateTime eventStartsAt,
            String eventLocation,
            Long userId,
            String username,
            RegistrationStatus status,
            LocalDateTime createdAt,
            LocalDateTime attendanceMarkedAt
    ) {
    }

    public record QrResponse(
            RegistrationResponse registration,
            String qrContent,
            String qrImageUrl
    ) {
    }

    public record DailyMetric(
            LocalDate day,
            Long count
    ) {
    }

    public record HourlyMetric(
            Integer hour,
            Long count
    ) {
    }

    public record EventSummaryResponse(
            long totalRegistered,
            long totalAttendees,
            double attendancePercentage,
            List<DailyMetric> registrationsByDay,
            List<HourlyMetric> attendanceByHour
    ) {
    }

    public static UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.isActive(),
                user.isImmutableAdmin(),
                user.getRoles(),
                user.getCreatedAt()
        );
    }

    public static EventResponse toEventResponse(Event event) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getStartsAt(),
                event.getLocation(),
                event.getMaxCapacity(),
                event.getStatus(),
                event.getCreatedBy().getId(),
                event.getCreatedAt(),
                event.getCancelledAt()
        );
    }

    public static RegistrationResponse toRegistrationResponse(Registration registration) {
        return new RegistrationResponse(
                registration.getId(),
                registration.getEvent().getId(),
                registration.getEvent().getTitle(),
                registration.getEvent().getStartsAt(),
                registration.getEvent().getLocation(),
                registration.getUser().getId(),
                registration.getUser().getUsername(),
                registration.getStatus(),
                registration.getCreatedAt(),
                registration.getAttendanceMarkedAt()
        );
    }

    public static EventSummaryResponse toSummaryResponse(EventStatisticsService.EventSummary summary) {
        return new EventSummaryResponse(
                summary.totalRegistered(),
                summary.totalAttendees(),
                summary.attendancePercentage(),
                toDailyMetrics(summary.registrationsByDay()),
                toHourlyMetrics(summary.attendanceByHour())
        );
    }

    private static List<DailyMetric> toDailyMetrics(Map<LocalDate, Long> metrics) {
        return metrics.entrySet().stream()
                .map(entry -> new DailyMetric(entry.getKey(), entry.getValue()))
                .toList();
    }

    private static List<HourlyMetric> toHourlyMetrics(Map<Integer, Long> metrics) {
        return metrics.entrySet().stream()
                .map(entry -> new HourlyMetric(entry.getKey(), entry.getValue()))
                .toList();
    }
}
