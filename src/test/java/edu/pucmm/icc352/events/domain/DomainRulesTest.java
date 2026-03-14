package edu.pucmm.icc352.events.domain;

import edu.pucmm.icc352.events.domain.model.Event;
import edu.pucmm.icc352.events.domain.model.Registration;
import edu.pucmm.icc352.events.domain.model.User;
import edu.pucmm.icc352.events.shared.error.AppException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DomainRulesTest {
    @Test
    void bootstrapAdminCannotBeBlocked() {
        User admin = User.bootstrapAdmin(
                "admin",
                "Administrador",
                "admin@demo.test",
                "hashed-password",
                LocalDateTime.now()
        );

        AppException exception = assertThrows(AppException.class, admin::block);

        assertEquals(409, exception.statusCode());
    }

    @Test
    void attendanceCannotBeRegisteredTwice() {
        LocalDateTime now = LocalDateTime.now();
        User owner = User.bootstrapAdmin("admin", "Administrador", "admin@demo.test", "hashed", now);
        User participant = User.participant("user1", "Participante", "user@demo.test", "hashed", now);
        Event event = Event.create("Evento", "Descripcion", now.plusDays(1), "Aula", 20, owner, now);
        Registration registration = Registration.create(event, participant, "token-1", now);

        registration.markAttendance(now);

        AppException exception = assertThrows(AppException.class, () -> registration.markAttendance(now.plusMinutes(5)));

        assertEquals(409, exception.statusCode());
    }
}
