package com.fedex.jupiter;

public final class MonitorConfig {
    private static final String DEFAULT_HOST = "jcswebt11.ftn.fedex.com";

    private static final int DEFAULT_PORT = 443;
    private static final int DEFAULT_TIMEOUT_MILLIS = 3000;
    private static final int DEFAULT_INTERVAL_SECONDS = 30;
    private static final int DEFAULT_FAILURES_BEFORE_ALERT = 3;
    private static final int DEFAULT_COOLDOWN_MINUTES = 15;

    private final String host;
    private final int port;
    private final int connectionTimeoutMillis;
    private final int checkIntervalSeconds;
    private final int failuresBeforeAlert;
    private final int alertCooldownMinutes;

    private MonitorConfig(
            String host,
            int port,
            int connectionTimeoutMillis,
            int checkIntervalSeconds,
            int failuresBeforeAlert,
            int alertCooldownMinutes) {
        this.host = isBlank(host) ? DEFAULT_HOST : host.trim();
        this.port = normalizePort(port);
        this.connectionTimeoutMillis = normalizeMin(connectionTimeoutMillis, 100, DEFAULT_TIMEOUT_MILLIS);
        this.checkIntervalSeconds = normalizeMin(checkIntervalSeconds, 1, DEFAULT_INTERVAL_SECONDS);
        this.failuresBeforeAlert = normalizeMin(failuresBeforeAlert, 1, DEFAULT_FAILURES_BEFORE_ALERT);
        this.alertCooldownMinutes = normalizeMin(alertCooldownMinutes, 1, DEFAULT_COOLDOWN_MINUTES);
    }

    public static MonitorConfig fromEnv() {
        return new MonitorConfig(
                readString("MONITOR_HOST", DEFAULT_HOST),
                readInt("MONITOR_PORT", DEFAULT_PORT),
                readInt("MONITOR_TIMEOUT_MILLIS", DEFAULT_TIMEOUT_MILLIS),
                readInt("MONITOR_INTERVAL_SECONDS", DEFAULT_INTERVAL_SECONDS),
                readInt("MONITOR_FAILURES_BEFORE_ALERT", DEFAULT_FAILURES_BEFORE_ALERT),
                readInt("MONITOR_ALERT_COOLDOWN_MINUTES", DEFAULT_COOLDOWN_MINUTES)
        );
    }

    public static MonitorConfig of(
            String host,
            int port,
            int connectionTimeoutMillis,
            int checkIntervalSeconds,
            int failuresBeforeAlert,
            int alertCooldownMinutes) {
        return new MonitorConfig(
                host,
                port,
                connectionTimeoutMillis,
                checkIntervalSeconds,
                failuresBeforeAlert,
                alertCooldownMinutes);
    }

    private static String readString(String key, String fallback) {
        String value = System.getenv(key);
        return isBlank(value) ? fallback : value.trim();
    }

    private static int readInt(String key, int fallback) {
        String value = System.getenv(key);
        if (isBlank(value)) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static int normalizeMin(int value, int min, int fallback) {
        return value >= min ? value : fallback;
    }

    private static int normalizePort(int value) {
        return value >= 1 && value <= 65535 ? value : DEFAULT_PORT;
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public int connectionTimeoutMillis() {
        return connectionTimeoutMillis;
    }

    public int checkIntervalSeconds() {
        return checkIntervalSeconds;
    }

    public int failuresBeforeAlert() {
        return failuresBeforeAlert;
    }

    public int alertCooldownMinutes() {
        return alertCooldownMinutes;
    }
}
