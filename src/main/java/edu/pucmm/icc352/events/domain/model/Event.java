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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Lob
    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "location", nullable = false, length = 150)
    private String location;

    @Column(name = "max_capacity", nullable = false)
    private int maxCapacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private EventStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    protected Event() {
    }

    public static Event create(
            String title,
            String description,
            LocalDateTime startsAt,
            String location,
            int maxCapacity,
            User createdBy,
            LocalDateTime now
    ) {
        Event event = new Event();
        event.title = title;
        event.description = description;
        event.startsAt = startsAt;
        event.location = location;
        event.maxCapacity = maxCapacity;
        event.status = EventStatus.DRAFT;
        event.createdBy = createdBy;
        event.createdAt = now;
        event.updatedAt = now;
        return event;
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

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }

    public String getLocation() {
        return location;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public EventStatus getStatus() {
        return status;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public boolean isOwnedBy(Long userId) {
        return createdBy != null && createdBy.getId() != null && createdBy.getId().equals(userId);
    }

    public boolean isVisibleToParticipants() {
        return status == EventStatus.PUBLISHED;
    }

    public boolean startsOn(LocalDate date) {
        return startsAt.toLocalDate().equals(date);
    }

    public boolean hasStarted(LocalDateTime now) {
        return !startsAt.isAfter(now);
    }

    public void updateDetails(
            String title,
            String description,
            LocalDateTime startsAt,
            String location,
            int maxCapacity
    ) {
        ensureNotCancelled();
        this.title = title;
        this.description = description;
        this.startsAt = startsAt;
        this.location = location;
        this.maxCapacity = maxCapacity;
        this.updatedAt = LocalDateTime.now();
    }

    public void publish() {
        ensureNotCancelled();
        status = EventStatus.PUBLISHED;
        updatedAt = LocalDateTime.now();
    }

    public void unpublish() {
        ensureNotCancelled();
        status = EventStatus.DRAFT;
        updatedAt = LocalDateTime.now();
    }

    public void cancel(LocalDateTime now) {
        ensureNotCancelled();
        status = EventStatus.CANCELLED;
        cancelledAt = now;
        updatedAt = now;
    }

    public void ensureNotCancelled() {
        if (status == EventStatus.CANCELLED) {
            throw AppException.conflict("El evento ya fue cancelado.");
        }
    }
}
