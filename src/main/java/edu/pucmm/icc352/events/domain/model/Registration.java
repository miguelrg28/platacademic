package edu.pucmm.icc352.events.domain.model;

import edu.pucmm.icc352.events.shared.error.AppException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "registrations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_registrations_event_user", columnNames = {"event_id", "user_id"}),
                @UniqueConstraint(name = "uk_registrations_validation_token", columnNames = "validation_token")
        }
)
public class Registration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "validation_token", nullable = false, length = 120)
    private String validationToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RegistrationStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "attendance_marked_at")
    private LocalDateTime attendanceMarkedAt;

    protected Registration() {
    }

    public static Registration create(Event event, User user, String validationToken, LocalDateTime now) {
        Registration registration = new Registration();
        registration.event = event;
        registration.user = user;
        registration.validationToken = validationToken;
        registration.status = RegistrationStatus.ACTIVE;
        registration.createdAt = now;
        registration.updatedAt = now;
        return registration;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Event getEvent() {
        return event;
    }

    public User getUser() {
        return user;
    }

    public String getValidationToken() {
        return validationToken;
    }

    public RegistrationStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getAttendanceMarkedAt() {
        return attendanceMarkedAt;
    }

    public boolean isActive() {
        return status == RegistrationStatus.ACTIVE;
    }

    public boolean attendanceMarked() {
        return attendanceMarkedAt != null;
    }

    public void reactivate(String newToken, LocalDateTime now) {
        validationToken = newToken;
        status = RegistrationStatus.ACTIVE;
        cancelledAt = null;
        attendanceMarkedAt = null;
        updatedAt = now;
    }

    public void cancel(LocalDateTime now) {
        if (status == RegistrationStatus.CANCELLED) {
            throw AppException.conflict("La inscripción ya fue cancelada.");
        }
        status = RegistrationStatus.CANCELLED;
        cancelledAt = now;
        updatedAt = now;
    }

    public void markAttendance(LocalDateTime now) {
        if (status != RegistrationStatus.ACTIVE) {
            throw AppException.conflict("Solo las inscripciones activas pueden marcar asistencia.");
        }
        if (attendanceMarked()) {
            throw AppException.conflict("La asistencia ya fue registrada.");
        }
        attendanceMarkedAt = now;
        updatedAt = now;
    }
}
