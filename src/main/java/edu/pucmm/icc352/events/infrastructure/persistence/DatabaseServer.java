package edu.pucmm.icc352.events.infrastructure.persistence;

import edu.pucmm.icc352.events.config.DatabaseSettings;
import org.h2.tools.Server;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class DatabaseServer implements AutoCloseable {
    private final Server tcpServer;

    private DatabaseServer(Server tcpServer) {
        this.tcpServer = tcpServer;
    }

    public static DatabaseServer start(DatabaseSettings settings) {
        try {
            Path parent = settings.filePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            List<String> arguments = new ArrayList<>(List.of(
                    "-tcp",
                    "-ifNotExists",
                    "-tcpPort",
                    String.valueOf(settings.tcpPort())
            ));

            if (settings.allowRemoteConnections()) {
                arguments.add("-tcpAllowOthers");
            }

            Server server = Server.createTcpServer(arguments.toArray(String[]::new)).start();
            return new DatabaseServer(server);
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo iniciar el servidor H2.", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo preparar el directorio de la base de datos.", exception);
        }
    }

    @Override
    public void close() {
        if (tcpServer != null) {
            tcpServer.stop();
        }
    }
}
