package edu.pucmm.icc352.events.application.service;

import edu.pucmm.icc352.events.application.security.AuthenticatedUser;
import edu.pucmm.icc352.events.application.security.PasswordHasher;
import edu.pucmm.icc352.events.domain.model.User;
import edu.pucmm.icc352.events.infrastructure.persistence.TransactionManager;
import edu.pucmm.icc352.events.infrastructure.persistence.UserRepository;
import edu.pucmm.icc352.events.shared.error.AppException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

public final class AuthService {
    private final TransactionManager transactionManager;
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public AuthService(
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

    public User register(RegisterCommand command) {
        String username = normalizeUsername(command.username());
        String fullName = requireText(command.fullName(), "El nombre es obligatorio.");
        String email = normalizeEmail(command.email());
        String password = requirePassword(command.password());

        return transactionManager.write(session -> {
            if (userRepository.findByUsername(session, username).isPresent()) {
                throw AppException.conflict("El nombre de usuario ya existe.");
            }
            if (userRepository.findByEmail(session, email).isPresent()) {
                throw AppException.conflict("El correo ya existe.");
            }

            User user = User.participant(
                    username,
                    fullName,
                    email,
                    passwordHasher.hash(password),
                    LocalDateTime.now(clock)
            );
            userRepository.save(session, user);
            return user;
        });
    }

    public AuthenticatedUser login(LoginCommand command) {
        String username = normalizeUsername(command.username());
        String password = requireText(command.password(), "La contraseña es obligatoria.");

        return transactionManager.read(session -> {
            User user = userRepository.findByUsername(session, username)
                    .orElseThrow(() -> AppException.unauthorized("Credenciales inválidas."));

            if (!passwordHasher.matches(password, user.getPasswordHash())) {
                throw AppException.unauthorized("Credenciales inválidas.");
            }

            user.ensureCanAuthenticate();
            return toAuthenticatedUser(user);
        });
    }

    public User getUserById(Long userId) {
        return transactionManager.read(session -> userRepository.findById(session, userId)
                .orElseThrow(() -> AppException.notFound("Usuario no encontrado.")));
    }

    public AuthenticatedUser getAuthenticatedUser(Long userId) {
        return transactionManager.read(session -> {
            User user = userRepository.findById(session, userId)
                    .orElseThrow(() -> AppException.unauthorized("La sesión no es válida."));
            user.ensureCanAuthenticate();
            return toAuthenticatedUser(user);
        });
    }

    public Optional<AuthenticatedUser> findAuthenticatedUser(Long userId) {
        return transactionManager.read(session -> userRepository.findById(session, userId)
                .filter(User::isActive)
                .map(this::toAuthenticatedUser));
    }

    private AuthenticatedUser toAuthenticatedUser(User user) {
        return new AuthenticatedUser(user.getId(), user.getUsername(), user.getRoles());
    }

    private String normalizeUsername(String username) {
        String value = requireText(username, "El nombre de usuario es obligatorio.").toLowerCase();
        if (value.length() < 4) {
            throw AppException.badRequest("El nombre de usuario debe tener al menos 4 caracteres.");
        }
        return value;
    }

    private String normalizeEmail(String email) {
        String value = requireText(email, "El correo es obligatorio.").toLowerCase();
        if (!value.contains("@")) {
            throw AppException.badRequest("El correo no es válido.");
        }
        return value;
    }

    private String requirePassword(String password) {
        String value = requireText(password, "La contraseña es obligatoria.");
        if (value.length() < 8) {
            throw AppException.badRequest("La contraseña debe tener al menos 8 caracteres.");
        }
        return value;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw AppException.badRequest(message);
        }
        return value.trim();
    }

    public record RegisterCommand(
            String username,
            String fullName,
            String email,
            String password
    ) {
    }

    public record LoginCommand(
            String username,
            String password
    ) {
    }
}
