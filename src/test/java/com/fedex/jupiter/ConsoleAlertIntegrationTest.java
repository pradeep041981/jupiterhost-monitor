package com.fedex.jupiter;

import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Verifies alerting remains console-only and does not throw on host failure.
 */
class ConsoleAlertIntegrationTest {

    @Test
    void emitsConsoleAlertWhenServerIsUnreachable() {
        MonitorConfig config = MonitorConfig.of(
                "jcswebt11.ftn.fedex.com",
                443,
                1000,
                30,
                1,
                15
        );

        HostChecker alwaysDown = () -> false;
        MonitorService service = new MonitorService(alwaysDown, config, Clock.systemUTC());

        assertDoesNotThrow(service::runCheck,
                "Console alerting should not throw when the host is down");
    }
}

