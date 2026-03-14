package edu.pucmm.icc352.events.application.security;

import edu.pucmm.icc352.events.domain.model.UserRole;

import java.util.Set;

public record AuthenticatedUser(
        Long id,
        String username,
        Set<UserRole> roles
) {
    public boolean hasRole(UserRole role) {
        return roles.contains(role);
    }

    public boolean isAdmin() {
        return hasRole(UserRole.ADMIN);
    }

    public boolean canManageEvents() {
        return isAdmin() || hasRole(UserRole.ORGANIZER);
    }
}
