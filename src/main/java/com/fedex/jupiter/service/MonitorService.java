package com.fedex.jupiter.service;

import com.fedex.jupiter.alert.AlertNotifier;
import com.fedex.jupiter.alert.smtp.SmtpEmailAlertNotifier;
import com.fedex.jupiter.config.MonitorConfig;
import com.fedex.jupiter.validate.HostChecker;
import com.fedex.jupiter.validate.HostCheckerFactory;
import com.fedex.jupiter.validate.TcpHostChecker;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class MonitorService {
    private final MonitorConfig config;
    private final Clock clock;
    private final AlertNotifier alertNotifier;
    private final HostCheckerFactory hostCheckerFactory;

    private final Map<String, Integer> consecutiveFailures = new HashMap<>();
    private final Map<String, Instant> lastAlertAt = new HashMap<>();

    public MonitorService(MonitorConfig config, Clock clock) {
        this(config, clock, new SmtpEmailAlertNotifier(config), TcpHostChecker::new);
    }

    public MonitorService(MonitorConfig config, Clock clock, AlertNotifier alertNotifier) {
        this(config, clock, alertNotifier, TcpHostChecker::new);
    }

    public MonitorService(MonitorConfig config, Clock clock, HostCheckerFactory hostCheckerFactory) {
        this(config, clock, new SmtpEmailAlertNotifier(config), hostCheckerFactory);
    }

    public MonitorService(MonitorConfig config, Clock clock, AlertNotifier alertNotifier, HostCheckerFactory hostCheckerFactory) {
        this.config = config;
        this.clock = clock;
        this.alertNotifier = alertNotifier;
        this.hostCheckerFactory = hostCheckerFactory;
    }

    public void runCheck() {
        for (String host : config.hosts()) {
            HostChecker checker = hostCheckerFactory.create(host, config.port(), config.connectionTimeoutMillis());
            boolean up = checker.isHostUp();

            if (up) {
                if (consecutiveFailures.getOrDefault(host, 0) > 0) {
                    System.out.printf("Host recovered: %s:%d%n", host, config.port());
                }
                consecutiveFailures.put(host, 0);
                continue;
            }

            int fails = consecutiveFailures.getOrDefault(host, 0) + 1;
            consecutiveFailures.put(host, fails);
            System.out.printf(
                    "Host check failed for %s:%d (%d/%d)%n",
                    host,
                    config.port(),
                    fails,
                    config.failuresBeforeAlert());

            if (fails < config.failuresBeforeAlert()) {
                continue;
            }

            Instant nextAllowed = nextAllowedAlertAt(host);
            if (nextAllowed != null && Instant.now(clock).isBefore(nextAllowed)) {
                System.out.printf(
                        "Alert suppressed: cooldown active until %s UTC.%n",
                        nextAllowed);
                continue;
            }

            String message = String.format("Host %s:%d appears to be down.", host, config.port());
            alertNotifier.notifyAlert(message);
            lastAlertAt.put(host, Instant.now(clock));
        }
    }

    private Instant nextAllowedAlertAt(String host) {
        Instant last = lastAlertAt.get(host);
        if (last == null) {
            return null;
        }
        return last.plusSeconds(config.alertCooldownMinutes() * 60L);
    }
}
