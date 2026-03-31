package com.fedex.jupiter.config;

public final class MonitorConfig {
    private static final String DEFAULT_HOST = "jcswebt110.ftn.fedex.com";

    private static final int DEFAULT_PORT = 443;
    private static final int DEFAULT_TIMEOUT_MILLIS = 3000;
    private static final int DEFAULT_INTERVAL_SECONDS = 30;
    private static final int DEFAULT_FAILURES_BEFORE_ALERT = 3;
    private static final int DEFAULT_COOLDOWN_MINUTES = 1; // 15
    private static final String DEFAULT_SMTP_HOST = "smtp.gmail.com";
    private static final int DEFAULT_SMTP_PORT = 587;
    private static final boolean DEFAULT_SMTP_STARTTLS = true;

    private final String host;
    private final int port;
    private final int connectionTimeoutMillis;
    private final int checkIntervalSeconds;
    private final int failuresBeforeAlert;
    private final int alertCooldownMinutes;
    private final String smtpHost;
    private final int smtpPort;
    private final boolean smtpStartTls;
    private final String smtpUsername;
    private final String smtpPassword;
    private final String smtpFrom;
    private final String smtpTo;

    private MonitorConfig(
            String host,
            int port,
            int connectionTimeoutMillis,
            int checkIntervalSeconds,
            int failuresBeforeAlert,
            int alertCooldownMinutes,
            String smtpHost,
            int smtpPort,
            boolean smtpStartTls,
            String smtpUsername,
            String smtpPassword,
            String smtpFrom,
            String smtpTo) {
        this.host = isBlank(host) ? DEFAULT_HOST : host.trim();
        this.port = normalizePort(port);
        this.connectionTimeoutMillis = normalizeMin(connectionTimeoutMillis, 100, DEFAULT_TIMEOUT_MILLIS);
        this.checkIntervalSeconds = normalizeMin(checkIntervalSeconds, 1, DEFAULT_INTERVAL_SECONDS);
        this.failuresBeforeAlert = normalizeMin(failuresBeforeAlert, 1, DEFAULT_FAILURES_BEFORE_ALERT);
        this.alertCooldownMinutes = normalizeMin(alertCooldownMinutes, 1, DEFAULT_COOLDOWN_MINUTES);
        this.smtpHost = isBlank(smtpHost) ? DEFAULT_SMTP_HOST : smtpHost.trim();
        this.smtpPort = normalizeSmtpPort(smtpPort);
        this.smtpStartTls = smtpStartTls;
        this.smtpUsername = trimToEmpty(smtpUsername);
        this.smtpPassword = trimToEmpty(smtpPassword);
        this.smtpFrom = trimToEmpty(smtpFrom);
        this.smtpTo = trimToEmpty(smtpTo);
    }

    public static MonitorConfig fromEnv() {
        return new MonitorConfig(
                readString("MONITOR_HOST", DEFAULT_HOST),
                readInt("MONITOR_PORT", DEFAULT_PORT),
                readInt("MONITOR_TIMEOUT_MILLIS", DEFAULT_TIMEOUT_MILLIS),
                readInt("MONITOR_INTERVAL_SECONDS", DEFAULT_INTERVAL_SECONDS),
                readInt("MONITOR_FAILURES_BEFORE_ALERT", DEFAULT_FAILURES_BEFORE_ALERT),
                readInt("MONITOR_ALERT_COOLDOWN_MINUTES", DEFAULT_COOLDOWN_MINUTES),
                readString("MONITOR_SMTP_HOST", DEFAULT_SMTP_HOST),
                readInt("MONITOR_SMTP_PORT", DEFAULT_SMTP_PORT),
                readBoolean("MONITOR_SMTP_STARTTLS", DEFAULT_SMTP_STARTTLS),
                readString("MONITOR_SMTP_USERNAME", "pradeepbr2003@gmail.com"),
                readString("MONITOR_SMTP_PASSWORD", "lugl ekbi jdrh rswy"),
                readString("MONITOR_SMTP_FROM", "jupiter_email_alert@fedex.com"),
                readString("MONITOR_SMTP_TO", "pradeep.ramaiah.osv@fedex.com")
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
                alertCooldownMinutes,
                DEFAULT_SMTP_HOST,
                DEFAULT_SMTP_PORT,
                DEFAULT_SMTP_STARTTLS,
                "",
                "",
                "",
                "");
    }

    public static MonitorConfig of(
            String host,
            int port,
            int connectionTimeoutMillis,
            int checkIntervalSeconds,
            int failuresBeforeAlert,
            int alertCooldownMinutes,
            String smtpHost,
            int smtpPort,
            boolean smtpStartTls,
            String smtpUsername,
            String smtpPassword,
            String smtpFrom,
            String smtpTo) {
        return new MonitorConfig(
                host,
                port,
                connectionTimeoutMillis,
                checkIntervalSeconds,
                failuresBeforeAlert,
                alertCooldownMinutes,
                smtpHost,
                smtpPort,
                smtpStartTls,
                smtpUsername,
                smtpPassword,
                smtpFrom,
                smtpTo);
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

    private static boolean readBoolean(String key, boolean fallback) {
        String value = System.getenv(key);
        if (isBlank(value)) {
            return fallback;
        }
        return Boolean.parseBoolean(value.trim());
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

    private static int normalizeSmtpPort(int value) {
        return value >= 1 && value <= 65535 ? value : DEFAULT_SMTP_PORT;
    }

    private static String trimToEmpty(String value) {
        return isBlank(value) ? "" : value.trim();
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

    public String smtpHost() {
        return smtpHost;
    }

    public int smtpPort() {
        return smtpPort;
    }

    public boolean smtpStartTls() {
        return smtpStartTls;
    }

    public String smtpUsername() {
        return smtpUsername;
    }

    public String smtpPassword() {
        return smtpPassword;
    }

    public String smtpFrom() {
        return smtpFrom;
    }

    public String smtpTo() {
        return smtpTo;
    }

    public boolean smtpEnabled() {
        return !isBlank(smtpUsername)
                && !isBlank(smtpPassword)
                && !isBlank(smtpFrom)
                && !isBlank(smtpTo);
    }
}
