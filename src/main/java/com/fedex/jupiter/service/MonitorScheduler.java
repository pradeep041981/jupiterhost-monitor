package com.fedex.jupiter.service;

import com.fedex.jupiter.config.MonitorProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MonitorScheduler {
    private final MonitorService monitorService;
    private final MonitorProperties config;

    public MonitorScheduler(MonitorService monitorService, MonitorProperties config) {
        this.monitorService = monitorService;
        this.config = config;
    }

    @PostConstruct
    void logStartupConfiguration() {
        if (config.smtpEnabled()) {
            System.out.printf(
                    "SMTP alerting enabled via %s:%d (STARTTLS=%s).%n",
                    config.smtpHost(),
                    config.smtpPort(),
                    config.smtpStartTls());
        } else {
            System.out.println("SMTP alerting disabled: set MONITOR_SMTP_FROM and MONITOR_SMTP_TO to enable email alerts.");
        }

        System.out.printf(
                "Monitoring hosts %s:%d every %d seconds.%n",
                String.join(", ", config.hosts()),
                config.port(),
                config.checkIntervalSeconds());
    }

    // Use direct property binding to avoid resolving this bean inside its own @Scheduled expression.
    @Scheduled(fixedRateString = "${monitor.interval-seconds:30}000")
    public void runScheduledCheck() {
        monitorService.runCheck();
    }

    @PreDestroy
    void logShutdown() {
        System.out.println("Host monitor stopped.");
    }
}
