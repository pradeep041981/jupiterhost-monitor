package com.fedex.jupiter;

import com.fedex.jupiter.alert.AlertNotifier;
import com.fedex.jupiter.alert.smtp.SmtpEmailAlertNotifier;
import com.fedex.jupiter.config.MonitorConfig;
import com.fedex.jupiter.service.MonitorService;

import java.time.Clock;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MonitorJupiterHostServer {
    public static void main(String[] args) {
        MonitorConfig config = MonitorConfig.fromEnv();

        AlertNotifier notifier = new SmtpEmailAlertNotifier(config);
        if (config.smtpEnabled()) {
            System.out.printf(
                    "SMTP alerting enabled via %s:%d (STARTTLS=%s).%n",
                    config.smtpHost(),
                    config.smtpPort(),
                    config.smtpStartTls());
        } else {
            System.out.println("SMTP alerting disabled: set MONITOR_SMTP_* env vars to enable email alerts.");
        }

        MonitorService monitorService = new MonitorService(config, Clock.systemUTC(), notifier);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduler.shutdownNow();
            System.out.println("Host monitor stopped.");
        }));

        System.out.printf(
                "Monitoring hosts %s:%d every %d seconds.%n",
                String.join(", ", config.hosts()),
                config.port(),
                config.checkIntervalSeconds());

        scheduler.scheduleAtFixedRate(
                monitorService::runCheck,
                0,
                config.checkIntervalSeconds(),
                TimeUnit.SECONDS
        );
    }
}