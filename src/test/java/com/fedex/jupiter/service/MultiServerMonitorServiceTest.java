package com.fedex.jupiter.service;

import com.fedex.jupiter.alert.AlertNotifier;
import com.fedex.jupiter.config.MonitorConfig;
import com.fedex.jupiter.validate.HostChecker;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Map;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for multi-server monitoring functionality.
 * Verifies that the MonitorService can handle monitoring multiple servers
 * independently with per-server failure tracking and alert state.
 */
class MultiServerMonitorServiceTest {

    @Test
    void monitorsMultipleServersIndependently() {
        String host1 = "jcswebt110.ftn.fedex.com";
        String host2 = "jcswebt111.ftn.fedex.com";
        String host3 = "jcswebt112.ftn.fedex.com";

        QueueHostChecker checker = new QueueHostChecker(
                // First check: all servers up
                true, true, true,
                // Second check: host2 down, others up
                true, false, true,
                // Third check: host2 still down, others up
                true, false, true
        );

        MutableClock clock = new MutableClock(Instant.parse("2026-03-23T00:00:00Z"));
        RecordingNotifier notifier = new RecordingNotifier();

        MonitorConfig config = MonitorConfig.of(
                Arrays.asList(host1, host2, host3),
                443,
                1000,
                30,
                2,
                15
        );

        MonitorService service = new MonitorService(config, clock, notifier, (h, p, t) -> checker);

        // First check - all servers up
        service.runCheck();
        assertEquals(0, notifier.notifications, "No alerts on first check when all servers up");

        // Second check - host2 starts failing
        service.runCheck();
        assertEquals(0, notifier.notifications, "No alert yet, failure threshold not reached");

        // Third check - host2 fails again (reaches threshold)
        service.runCheck();
        assertEquals(1, notifier.notifications, "Alert triggered for host2 after threshold reached");
        assertEquals(
                String.format("Host %s:443 appears to be down.", host2),
                notifier.lastMessage,
                "Alert message should reference the failing host"
        );
    }

    @Test
    void maintainsIndependentFailureCountPerServer() throws Exception {
        String host1 = "jcswebt110.ftn.fedex.com";
        String host2 = "jcswebt111.ftn.fedex.com";

        QueueHostChecker checker = new QueueHostChecker(
                // First check: host1 up, host2 down
                true, false,
                // Second check: host1 down, host2 up (host1 count=1)
                false, true,
                // Third check: host1 down, host2 down (host1 count=2=threshold, host2 count=1)
                false, false,
                // Fourth check: both up
                true, true
        );

        MutableClock clock = new MutableClock(Instant.parse("2026-03-23T00:00:00Z"));
        RecordingNotifier notifier = new RecordingNotifier();

        MonitorConfig config = MonitorConfig.of(
                Arrays.asList(host1, host2),
                443,
                1000,
                30,
                2,
                15
        );

        MonitorService service = new MonitorService(config, clock, notifier, (h, p, t) -> checker);

        // Check 1: host1 up, host2 fails (count: host2=1)
        service.runCheck();
        assertEquals(0, notifier.notifications);

        // Check 2: host1 fails (count: host1=1), host2 recovers (count: host2=0)
        service.runCheck();
        assertEquals(0, notifier.notifications, "No alert yet for either host");

        // Check 3: host1 fails (count: host1=2, reaches threshold), host2 fails again (count: host2=1)
        service.runCheck();
        assertEquals(1, notifier.notifications, "Alert for host1 reaching threshold");

        // Verify only host1 was alerted
        assertEquals(
                String.format("Host %s:443 appears to be down.", host1),
                notifier.lastMessage
        );

        // Check 4: both recover
        service.runCheck();
        assertEquals(1, notifier.notifications, "No new alerts when servers recover");
    }

    @Test
    void maintainsIndependentCooldownPerServer() throws Exception {
        String host1 = "jcswebt111.ftn.fedex.com";
        String host2 = "jcswebt112.ftn.fedex.com";

        QueueHostChecker checker = new QueueHostChecker(
                false, false,  // Check 1: both down (count: host1=1, host2=1)
                false, false,  // Check 2: both down (count: host1=2, host2=2, both reach threshold and alert)
                false, true,   // Check 3: host1 down (cooldown suppresses), host2 up (recovers)
                false, false   // Check 4: host1 down (alerts again), host2 down (alerts again)
        );

        MutableClock clock = new MutableClock(Instant.parse("2026-03-23T00:00:00Z"));
        RecordingNotifier notifier = new RecordingNotifier();

        MonitorConfig config = MonitorConfig.of(
                Arrays.asList(host1, host2),
                443,
                1000,
                30,
                1,  // Threshold is 1
                15
        );

        MonitorService service = new MonitorService(config, clock, notifier, (h, p, t) -> checker);

        // Check 1: both fail once (reach threshold of 1)
        service.runCheck();
        assertEquals(2, notifier.notifications, "Both hosts alert when threshold=1");

        // Check 2: both fail again but cooldown suppresses alerts
        service.runCheck();
        assertEquals(2, notifier.notifications, "Both alerts suppressed by cooldown");

        // Check 3: host1 still down (suppressed by cooldown), host2 recovers
        service.runCheck();
        assertEquals(2, notifier.notifications, "No new alerts, host2 recovers");

        // Advance time past cooldown for both hosts
        clock.plusSeconds(15 * 60L);

        // Check 4: host1 down again (cooldown expired), host2 down again
        service.runCheck();
        assertEquals(4, notifier.notifications, "host1 alerts after cooldown, host2 alerts again");
    }

    @Test
    void handlesDefaultMultipleServersConfiguration() {
        MonitorConfig config = MonitorConfig.fromEnv();

        // Default config should have 3 hosts
        assertEquals(3, config.hosts().size());
//        assertEquals("jcswebt110.ftn.fedex.com", config.hosts().get(0));
        assertEquals("jcswebt11.ftn.fedex.com", config.hosts().get(0));
        assertEquals("jcswebt111.ftn.fedex.com", config.hosts().get(1));
        assertEquals("jcswebt112.ftn.fedex.com", config.hosts().get(2));
    }

    private static class QueueHostChecker implements HostChecker {
        private final Queue<Boolean> results = new ArrayDeque<>();

        QueueHostChecker(Boolean... values) {
            for (Boolean value : values) {
                results.add(value);
            }
        }

        @Override
        public boolean isHostUp() {
            if (results.isEmpty()) {
                return false;
            }
            return results.remove();
        }
    }

    private static class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void plusSeconds(long seconds) {
            now = now.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private static class RecordingNotifier implements AlertNotifier {
        private int notifications;
        private String lastMessage;

        @Override
        public void notifyAlert(String message) {
            notifications++;
            lastMessage = message;
        }
    }
}

