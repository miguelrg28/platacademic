package edu.pucmm.icc352.events.application.security;

import edu.pucmm.icc352.events.application.service.AuthService;
import edu.pucmm.icc352.events.shared.error.AppException;
import io.javalin.http.Context;

import java.util.Optional;

public final class AuthGuard {
    private static final String SESSION_USER_ID = "userId";

    private final AuthService authService;

    public AuthGuard(AuthService authService) {
        this.authService = authService;
    }

    public AuthenticatedUser requireAuthenticated(Context ctx) {
        Long userId = ctx.sessionAttribute(SESSION_USER_ID);
        if (userId == null) {
            throw AppException.unauthorized("Debes iniciar sesión.");
        }

        return authService.findAuthenticatedUser(userId)
                .orElseGet(() -> {
                    invalidateSession(ctx);
                    throw AppException.unauthorized("La sesión expiró o el usuario está bloqueado.");
                });
    }

    public Optional<AuthenticatedUser> optional(Context ctx) {
        Long userId = ctx.sessionAttribute(SESSION_USER_ID);
        if (userId == null) {
            return Optional.empty();
        }

        Optional<AuthenticatedUser> user = authService.findAuthenticatedUser(userId);
        if (user.isEmpty()) {
            invalidateSession(ctx);
        }
        return user;
    }

    public AuthenticatedUser requireAdmin(Context ctx) {
        AuthenticatedUser actor = requireAuthenticated(ctx);
        if (!actor.isAdmin()) {
            throw AppException.forbidden("Debes ser administrador para realizar esta operación.");
        }
        return actor;
    }

    public AuthenticatedUser requireOrganizerOrAdmin(Context ctx) {
        AuthenticatedUser actor = requireAuthenticated(ctx);
        if (!actor.canManageEvents()) {
            throw AppException.forbidden("Debes ser organizador o administrador para realizar esta operación.");
        }
        return actor;
    }

    public void startSession(Context ctx, AuthenticatedUser authenticatedUser) {
        if (ctx.req().getSession(false) != null) {
            ctx.req().changeSessionId();
        }
        ctx.sessionAttribute(SESSION_USER_ID, authenticatedUser.id());
        ctx.req().getSession().setMaxInactiveInterval(60 * 60 * 4);
    }

    public void endSession(Context ctx) {
        invalidateSession(ctx);
    }

    private void invalidateSession(Context ctx) {
        if (ctx.req().getSession(false) != null) {
            ctx.req().getSession().invalidate();
        }
    }
}
