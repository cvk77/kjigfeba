package de.toyotafinance.keycloak.spi.user.database;

import java.time.Duration;
import java.util.Map;

public record DatabaseConfig(
        String jdbcUrl,
        String username,
        String password,
        int minPoolSize,
        int maxPoolSize,
        Duration acquisitionTimeout
) {
    public static DatabaseConfig fromMap(Map<String, String> config, String urlKey, String userKey, String passKey) {
        String url = config.get(urlKey);
        String user = config.get(userKey);
        String pass = config.get(passKey);

        int min = Integer.parseInt(config.getOrDefault("minPoolSize", "1"));
        int max = Integer.parseInt(config.getOrDefault("maxPoolSize", "10"));
        
        // Keycloak config values are always strings. Safely parse timeout.
        String timeoutStr = config.getOrDefault("acquisitionTimeout", "30");
        Duration timeout = Duration.ofSeconds(Long.parseLong(timeoutStr));

        return new DatabaseConfig(url, user, pass, min, max, timeout);
    }
}
