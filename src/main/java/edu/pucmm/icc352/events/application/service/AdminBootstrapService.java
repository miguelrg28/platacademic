package edu.pucmm.icc352.events.application.service;

import edu.pucmm.icc352.events.application.security.PasswordHasher;
import edu.pucmm.icc352.events.config.AdminSettings;
import edu.pucmm.icc352.events.domain.model.User;
import edu.pucmm.icc352.events.infrastructure.persistence.TransactionManager;
import edu.pucmm.icc352.events.infrastructure.persistence.UserRepository;

import java.time.Clock;
import java.time.LocalDateTime;

public final class AdminBootstrapService {
    private final TransactionManager transactionManager;
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public AdminBootstrapService(
            TransactionManager transactionManager,
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            Clock clock
    ) {
        this.transactionManager = transactionManager;
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    public void ensureBootstrapAdmin(AdminSettings settings) {
        transactionManager.write(session -> {
            userRepository.findByUsername(session, settings.username().trim().toLowerCase())
                    .ifPresentOrElse(
                            User::ensureAdminRole,
                            () -> userRepository.save(
                                    session,
                                    User.bootstrapAdmin(
                                            settings.username().trim().toLowerCase(),
                                            settings.fullName().trim(),
                                            settings.email().trim().toLowerCase(),
                                            passwordHasher.hash(settings.password()),
                                            LocalDateTime.now(clock)
                                    )
                            )
                    );
            return null;
        });
    }
}
