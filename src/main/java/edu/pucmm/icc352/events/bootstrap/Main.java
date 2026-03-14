package edu.pucmm.icc352.events.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.pucmm.icc352.events.application.security.AuthGuard;
import edu.pucmm.icc352.events.application.security.BCryptPasswordHasher;
import edu.pucmm.icc352.events.application.security.PasswordHasher;
import edu.pucmm.icc352.events.application.service.AdminBootstrapService;
import edu.pucmm.icc352.events.application.service.AttendanceService;
import edu.pucmm.icc352.events.application.service.AuthService;
import edu.pucmm.icc352.events.application.service.EventService;
import edu.pucmm.icc352.events.application.service.EventStatisticsService;
import edu.pucmm.icc352.events.application.service.RegistrationService;
import edu.pucmm.icc352.events.application.service.UserAdministrationService;
import edu.pucmm.icc352.events.config.AppSettings;
import edu.pucmm.icc352.events.infrastructure.persistence.DatabaseServer;
import edu.pucmm.icc352.events.infrastructure.persistence.EventRepository;
import edu.pucmm.icc352.events.infrastructure.persistence.HibernateSessionFactoryProvider;
import edu.pucmm.icc352.events.infrastructure.persistence.MigrationRunner;
import edu.pucmm.icc352.events.infrastructure.persistence.RegistrationRepository;
import edu.pucmm.icc352.events.infrastructure.persistence.TransactionManager;
import edu.pucmm.icc352.events.infrastructure.persistence.UserRepository;
import edu.pucmm.icc352.events.infrastructure.qr.QrCodeGenerator;
import edu.pucmm.icc352.events.infrastructure.qr.QrImageDecoder;
import edu.pucmm.icc352.events.infrastructure.qr.QrPayloadCodec;
import edu.pucmm.icc352.events.web.AdminController;
import edu.pucmm.icc352.events.web.ApiExceptionMapper;
import edu.pucmm.icc352.events.web.AuthController;
import edu.pucmm.icc352.events.web.EventController;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import io.javalin.http.staticfiles.Location;
import org.hibernate.SessionFactory;

import java.time.Clock;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        AppSettings settings = AppSettings.fromEnvironment();
        Clock clock = Clock.systemDefaultZone();
        ObjectMapper objectMapper = createObjectMapper();

        DatabaseServer databaseServer = DatabaseServer.start(settings.database());
        MigrationRunner.migrate(settings.database());

        SessionFactory sessionFactory = new HibernateSessionFactoryProvider().build(settings.database());
        TransactionManager transactionManager = new TransactionManager(sessionFactory);
        PasswordHasher passwordHasher = new BCryptPasswordHasher();

        UserRepository userRepository = new UserRepository();
        EventRepository eventRepository = new EventRepository();
        RegistrationRepository registrationRepository = new RegistrationRepository();
        QrPayloadCodec qrPayloadCodec = new QrPayloadCodec(objectMapper);
        QrCodeGenerator qrCodeGenerator = new QrCodeGenerator();
        QrImageDecoder qrImageDecoder = new QrImageDecoder();

        AuthService authService = new AuthService(transactionManager, userRepository, passwordHasher, clock);
        AuthGuard authGuard = new AuthGuard(authService);
        EventService eventService = new EventService(
                transactionManager,
                eventRepository,
                userRepository,
                registrationRepository,
                clock
        );
        RegistrationService registrationService = new RegistrationService(
                transactionManager,
                eventRepository,
                registrationRepository,
                userRepository,
                eventService,
                qrPayloadCodec,
                clock
        );
        AttendanceService attendanceService = new AttendanceService(
                transactionManager,
                registrationRepository,
                eventService,
                qrImageDecoder,
                qrPayloadCodec,
                clock
        );
        EventStatisticsService eventStatisticsService = new EventStatisticsService(
                transactionManager,
                registrationRepository,
                eventService
        );
        UserAdministrationService userAdministrationService = new UserAdministrationService(transactionManager, userRepository);
        AdminBootstrapService adminBootstrapService = new AdminBootstrapService(
                transactionManager,
                userRepository,
                passwordHasher,
                clock
        );
        adminBootstrapService.ensureBootstrapAdmin(settings.admin());

        Javalin app = Javalin.create(config -> {
            config.startup.showJavalinBanner = false;
            config.jsonMapper(new JavalinJackson(objectMapper, false));
            config.staticFiles.add("/public", Location.CLASSPATH);
            config.spaRoot.addFile("/", "/public/index.html", Location.CLASSPATH);
        });

        ApiExceptionMapper.register(app);
        new AuthController(authService, authGuard).register(app);
        new EventController(
                authGuard,
                eventService,
                registrationService,
                attendanceService,
                eventStatisticsService,
                qrCodeGenerator
        ).register(app);
        new AdminController(authGuard, userAdministrationService, eventService).register(app);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            sessionFactory.close();
            databaseServer.close();
        }));

        app.start(settings.port());
    }

    private static ObjectMapper createObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
