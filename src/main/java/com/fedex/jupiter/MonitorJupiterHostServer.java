package com.fedex.jupiter;

import com.fedex.jupiter.alert.AlertNotifier;
import com.fedex.jupiter.alert.CompositeAlertNotifier;
import com.fedex.jupiter.alert.ConsoleAlertNotifier;
import com.fedex.jupiter.alert.smtp.SmtpEmailAlertNotifier;
import com.fedex.jupiter.config.MonitorConfig;
import com.fedex.jupiter.service.MonitorService;
import com.fedex.jupiter.validate.HostChecker;
import com.fedex.jupiter.validate.TcpHostChecker;

import java.time.Clock;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MonitorJupiterHostServer {
    public static void main(String[] args) {
        MonitorConfig config = MonitorConfig.fromEnv();

        HostChecker checker = new TcpHostChecker(config.host(), config.port(), config.connectionTimeoutMillis());
        AlertNotifier notifier = new ConsoleAlertNotifier();
        if (config.smtpEnabled()) {
            notifier = new CompositeAlertNotifier(notifier, new SmtpEmailAlertNotifier(config));
            System.out.printf(
                    "SMTP alerting enabled via %s:%d (STARTTLS=%s).%n",
                    config.smtpHost(),
                    config.smtpPort(),
                    config.smtpStartTls());
        } else {
            System.out.println("SMTP alerting disabled: set MONITOR_SMTP_* env vars to enable email alerts.");
        }

        MonitorService monitorService = new MonitorService(checker, config, Clock.systemUTC(), notifier);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduler.shutdownNow();
            System.out.println("Host monitor stopped.");
        }));

        System.out.printf(
                "Monitoring host %s:%d every %d seconds.%n",
                config.host(),
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