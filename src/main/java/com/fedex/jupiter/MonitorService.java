package com.fedex.jupiter;

import java.time.Clock;
import java.time.Instant;

public class MonitorService {
    private final HostChecker hostChecker;
    private final MonitorConfig config;
    private final Clock clock;

    private int consecutiveFailures;
    private Instant lastAlertAt;

    public MonitorService(HostChecker hostChecker, MonitorConfig config, Clock clock) {
        this.hostChecker = hostChecker;
        this.config = config;
        this.clock = clock;
    }

    public void runCheck() {
        boolean up = hostChecker.isHostUp();

        if (up) {
            if (consecutiveFailures > 0) {
                System.out.printf("Host recovered: %s:%d%n", config.host(), config.port());
            }
            consecutiveFailures = 0;
            return;
        }

        consecutiveFailures++;
        System.out.printf(
                "Host check failed for %s:%d (%d/%d)%n",
                config.host(),
                config.port(),
                consecutiveFailures,
                config.failuresBeforeAlert());

        if (consecutiveFailures < config.failuresBeforeAlert()) {
            System.out.printf(
                    "Alert suppressed: waiting for failure threshold (%d/%d).%n",
                    consecutiveFailures,
                    config.failuresBeforeAlert());
            return;
        }

        Instant nextAllowedAlert = nextAllowedAlertAt();
        if (nextAllowedAlert != null && Instant.now(clock).isBefore(nextAllowedAlert)) {
            System.out.printf(
                    "Alert suppressed: cooldown active until %s UTC.%n",
                    nextAllowedAlert);
            return;
        }

        String message = String.format("Host %s:%d appears to be down.", config.host(), config.port());
        System.out.printf("[ALERT] %s%n", message);
        lastAlertAt = Instant.now(clock);
    }

    private Instant nextAllowedAlertAt() {
        if (lastAlertAt == null) {
            return null;
        }
        return lastAlertAt.plusSeconds(config.alertCooldownMinutes() * 60L);
    }
}
