package edu.pucmm.icc352.events.application.service;

import edu.pucmm.icc352.events.application.security.AuthenticatedUser;
import edu.pucmm.icc352.events.domain.model.User;
import edu.pucmm.icc352.events.infrastructure.persistence.TransactionManager;
import edu.pucmm.icc352.events.infrastructure.persistence.UserRepository;
import edu.pucmm.icc352.events.shared.error.AppException;

import java.util.List;

public final class UserAdministrationService {
    private final TransactionManager transactionManager;
    private final UserRepository userRepository;

    public UserAdministrationService(TransactionManager transactionManager, UserRepository userRepository) {
        this.transactionManager = transactionManager;
        this.userRepository = userRepository;
    }

    public List<User> listUsers(AuthenticatedUser actor) {
        requireAdmin(actor);
        return transactionManager.read(userRepository::findAll);
    }

    public User updateBlockedState(AuthenticatedUser actor, Long userId, boolean blocked) {
        requireAdmin(actor);
        if (actor.id().equals(userId) && blocked) {
            throw AppException.conflict("No puedes bloquear tu propia cuenta.");
        }

        return transactionManager.write(session -> {
            User user = userRepository.findById(session, userId)
                    .orElseThrow(() -> AppException.notFound("Usuario no encontrado."));

            if (blocked) {
                user.block();
            } else {
                user.unblock();
            }

            return user;
        });
    }

    public User updateOrganizerRole(AuthenticatedUser actor, Long userId, boolean enabled) {
        requireAdmin(actor);
        return transactionManager.write(session -> {
            User user = userRepository.findById(session, userId)
                    .orElseThrow(() -> AppException.notFound("Usuario no encontrado."));

            if (enabled) {
                user.grantOrganizerRole();
            } else {
                user.revokeOrganizerRole();
            }

            return user;
        });
    }

    private void requireAdmin(AuthenticatedUser actor) {
        if (!actor.isAdmin()) {
            throw AppException.forbidden("Esta operación requiere rol de administrador.");
        }
    }
}
