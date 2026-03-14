package edu.pucmm.icc352.events.domain.model;

import edu.pucmm.icc352.events.shared.error.AppException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        }
)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "email", nullable = false, length = 120)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "immutable_admin", nullable = false)
    private boolean immutableAdmin;

    @ElementCollection(fetch = FetchType.EAGER, targetClass = UserRole.class)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private Set<UserRole> roles = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected User() {
    }

    public static User participant(
            String username,
            String fullName,
            String email,
            String passwordHash,
            LocalDateTime now
    ) {
        User user = new User();
        user.username = username;
        user.fullName = fullName;
        user.email = email;
        user.passwordHash = passwordHash;
        user.active = true;
        user.immutableAdmin = false;
        user.roles.add(UserRole.PARTICIPANT);
        user.createdAt = now;
        user.updatedAt = now;
        return user;
    }

    public static User bootstrapAdmin(
            String username,
            String fullName,
            String email,
            String passwordHash,
            LocalDateTime now
    ) {
        User user = participant(username, fullName, email, passwordHash, now);
        user.roles.add(UserRole.ADMIN);
        user.immutableAdmin = true;
        return user;
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

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isImmutableAdmin() {
        return immutableAdmin;
    }

    public Set<UserRole> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean hasRole(UserRole role) {
        return roles.contains(role);
    }

    public boolean canManageEvents() {
        return hasRole(UserRole.ADMIN) || hasRole(UserRole.ORGANIZER);
    }

    public void ensureCanAuthenticate() {
        if (!active) {
            throw AppException.unauthorized("El usuario está bloqueado.");
        }
    }

    public void grantOrganizerRole() {
        roles.add(UserRole.ORGANIZER);
        touch();
    }

    public void revokeOrganizerRole() {
        if (immutableAdmin) {
            throw AppException.conflict("El administrador principal no puede perder privilegios.");
        }
        roles.remove(UserRole.ORGANIZER);
        touch();
    }

    public void ensureAdminRole() {
        roles.add(UserRole.ADMIN);
        roles.add(UserRole.PARTICIPANT);
        immutableAdmin = true;
        active = true;
        touch();
    }

    public void block() {
        if (immutableAdmin) {
            throw AppException.conflict("El administrador principal no puede bloquearse.");
        }
        active = false;
        touch();
    }

    public void unblock() {
        active = true;
        touch();
    }

    private void touch() {
        updatedAt = LocalDateTime.now();
    }
}
