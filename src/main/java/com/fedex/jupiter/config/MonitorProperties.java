package com.fedex.jupiter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@ConfigurationProperties(prefix = "monitor")
public class MonitorProperties {

    // Default hosts for fallback when not configured via properties
    private static final String DEFAULT_HOST = "jcswebt11.ftn.fedex.com,jcswebt111.ftn.fedex.com,jcswebt112.ftn.fedex.com";

    private String host;
    private int port;
    private int timeoutMillis;
    private int intervalSeconds;
    private int failuresBeforeAlert;
    private int alertCooldownMinutes;
    @NestedConfigurationProperty
    private Smtp smtp = new Smtp();

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public int getIntervalSeconds() {
        return intervalSeconds;
    }

    public void setIntervalSeconds(int intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }

    public int getFailuresBeforeAlert() {
        return failuresBeforeAlert;
    }

    public void setFailuresBeforeAlert(int failuresBeforeAlert) {
        this.failuresBeforeAlert = failuresBeforeAlert;
    }

    public int getAlertCooldownMinutes() {
        return alertCooldownMinutes;
    }

    public void setAlertCooldownMinutes(int alertCooldownMinutes) {
        this.alertCooldownMinutes = alertCooldownMinutes;
    }

    public Smtp getSmtp() {
        return smtp;
    }

    public void setSmtp(Smtp smtp) {
        this.smtp = smtp;
    }

    public List<String> hosts() {
        String hostsToUse = (host == null || host.trim().isEmpty()) ? DEFAULT_HOST : host;
        
        List<String> parsedHosts = Arrays.stream(hostsToUse.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        return Collections.unmodifiableList(parsedHosts);
    }

    public String host() {
        List<String> hosts = hosts();
        return hosts.isEmpty() ? "" : hosts.get(0);
    }

    public int port() {
        return normalizePort(port, 443);
    }

    public int connectionTimeoutMillis() {
        return normalizeMin(timeoutMillis, 100, 3000);
    }

    public int checkIntervalSeconds() {
        return normalizeMin(intervalSeconds, 1, 30);
    }

    public int failuresBeforeAlert() {
        return normalizeMin(failuresBeforeAlert, 1, 3);
    }

    public int alertCooldownMinutes() {
        return normalizeMin(alertCooldownMinutes, 1, 1);
    }

    public String smtpHost() {
        return isBlank(smtpHostRaw()) ? "smtp.gmail.com" : smtpHostRaw().trim();
    }

    public int smtpPort() {
        return normalizePort(smtpPortRaw(), 587);
    }

    public boolean smtpStartTls() {
        return smtpSafe().isStartTls();
    }

    public String smtpFrom() {
        return trimToEmpty(smtpSafe().getFrom());
    }

    public String smtpTo() {
        return trimToEmpty(smtpSafe().getTo());
    }

    public boolean smtpEnabled() {
        return !isBlank(smtpFrom())
                && !isBlank(smtpTo());
    }

    private String smtpHostRaw() {
        return smtpSafe().getHost();
    }

    private int smtpPortRaw() {
        return smtpSafe().getPort();
    }

    private Smtp smtpSafe() {
        return smtp == null ? new Smtp() : smtp;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String trimToEmpty(String value) {
        return isBlank(value) ? "" : value.trim();
    }

    private static int normalizeMin(int value, int min, int fallback) {
        return value >= min ? value : fallback;
    }


    private static int normalizePort(int value, int fallback) {
        return value >= 1 && value <= 65535 ? value : fallback;
    }

    public static class Smtp {
        private String host = "smtp.gmail.com";
        private int port = 587;
        private boolean startTls = true;
        private String from = "";
        private String to = "";

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public boolean isStartTls() {
            return startTls;
        }

        public void setStartTls(boolean startTls) {
            this.startTls = startTls;
        }


        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getTo() {
            return to;
        }

        public void setTo(String to) {
            this.to = to;
        }
    }
}

