package edu.pucmm.icc352.events.infrastructure.persistence;

import edu.pucmm.icc352.events.config.DatabaseSettings;
import org.flywaydb.core.Flyway;

public final class MigrationRunner {
    private MigrationRunner() {
    }

    public static void migrate(DatabaseSettings settings) {
        Flyway.configure()
                .dataSource(settings.jdbcUrl(), settings.username(), settings.password())
                .cleanDisabled(true)
                .load()
                .migrate();
    }
}
