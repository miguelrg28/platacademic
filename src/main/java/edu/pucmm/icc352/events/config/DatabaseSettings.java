package edu.pucmm.icc352.events.config;

import java.nio.file.Path;

public record DatabaseSettings(
        String host,
        int tcpPort,
        String databasePath,
        String username,
        String password,
        boolean allowRemoteConnections
) {
    public String jdbcUrl() {
        return "jdbc:h2:tcp://%s:%d/%s;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
                .formatted(host, tcpPort, databasePath);
    }

    public Path filePath() {
        return Path.of(databasePath);
    }
}
