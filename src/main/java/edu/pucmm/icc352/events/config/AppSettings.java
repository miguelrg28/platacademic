package edu.pucmm.icc352.events.config;

public record AppSettings(
        int port,
        DatabaseSettings database,
        AdminSettings admin
) {
    public static AppSettings fromEnvironment() {
        return new AppSettings(
                readInt("APP_PORT", 7070),
                new DatabaseSettings(
                        read("DB_HOST", "localhost"),
                        readInt("DB_PORT", 9092),
                        read("DB_PATH", "./data/academic-events"),
                        read("DB_USERNAME", "sa"),
                        read("DB_PASSWORD", ""),
                        readBoolean("DB_ALLOW_REMOTE_CONNECTIONS", false)
                ),
                new AdminSettings(
                        read("ADMIN_USERNAME", "admin"),
                        read("ADMIN_PASSWORD", "Admin123!"),
                        read("ADMIN_FULL_NAME", "Administrador Principal"),
                        read("ADMIN_EMAIL", "admin@pucmm.local")
                )
        );
    }

    private static String read(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static int readInt(String key, int defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(value.trim());
    }

    private static boolean readBoolean(String key, boolean defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }
}
