package com.fedex.jupiter.service;

import com.fedex.jupiter.alert.AlertNotifier;
import com.fedex.jupiter.config.MonitorConfig;
import com.fedex.jupiter.validate.HostChecker;
import com.fedex.jupiter.validate.HostCheckerFactory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class MonitorServiceTest {

    @Test
    void notifiesAlertChannelWhenServerIsUnreachable() {
        QueueHostChecker checker = new QueueHostChecker(false);
        MutableClock clock = new MutableClock(Instant.parse("2026-03-23T00:00:00Z"));
        RecordingNotifier notifier = new RecordingNotifier();

        MonitorConfig config = MonitorConfig.of(
                "jcswebt11.ftn.fedex.com",
                443,
                1000,
                30,
                1,
                15
        );

        MonitorService service = new MonitorService(config, clock, notifier, (h, p, t) -> checker);
        service.runCheck();

        assertEquals(1, notifier.notifications);
        assertEquals("Host jcswebt11.ftn.fedex.com:443 appears to be down.", notifier.lastMessage);
    }

    @Test
    void emitsAlertWhenServerIsUnreachable() throws Exception {
        QueueHostChecker checker = new QueueHostChecker(false);
        MutableClock clock = new MutableClock(Instant.parse("2026-03-23T00:00:00Z"));

        MonitorConfig config = MonitorConfig.of(
                "jcswebt11.ftn.fedex.com",
                443,
                1000,
                30,
                1,
                15
        );

        MonitorService service = new MonitorService(config, clock, (h, p, t) -> checker);
        service.runCheck();

        assertNotNull(readLastAlertAt(service), "Expected alert timestamp when server is unreachable");
    }

    @Test
    void doesNotEmitAlertBeforeFailureThresholdIsReached() throws Exception {
        QueueHostChecker checker = new QueueHostChecker(false, false);
        MutableClock clock = new MutableClock(Instant.parse("2026-03-23T00:00:00Z"));

        MonitorConfig config = MonitorConfig.of(
                "jcswebt11.ftn.fedex.com",
                443,
                1000,
                30,
                3,
                15
        );

        MonitorService service = new MonitorService(config, clock, (h, p, t) -> checker);
        service.runCheck();
        service.runCheck();

        assertNull(readLastAlertAt(service), "Expected no alert before failure threshold is reached");
    }

    @Test
    void emitsAfterConfiguredFailures() throws Exception {
        QueueHostChecker checker = new QueueHostChecker(false, false, false);
        MutableClock clock = new MutableClock(Instant.parse("2026-03-23T00:00:00Z"));

        MonitorConfig config = MonitorConfig.of(
                "jcswebt11.ftn.fedex.com",
                443,
                1000,
                30,
                2,
                15
        );

        MonitorService service = new MonitorService(config, clock, (h, p, t) -> checker);
        service.runCheck();
        service.runCheck();

        assertEquals(Instant.parse("2026-03-23T00:00:00Z"), readLastAlertAt(service));
    }

    @Test
    void suppressesAlertsDuringCooldown() throws Exception {
        QueueHostChecker checker = new QueueHostChecker(false, false, false, false);
        MutableClock clock = new MutableClock(Instant.parse("2026-03-23T00:00:00Z"));

        MonitorConfig config = MonitorConfig.of(
                "jcswebt11.ftn.fedex.com",
                443,
                1000,
                30,
                2,
                15
        );

        MonitorService service = new MonitorService(config, clock, (h, p, t) -> checker);

        service.runCheck();
        service.runCheck();
        Instant firstAlertAt = readLastAlertAt(service);

        service.runCheck();
        assertEquals(firstAlertAt, readLastAlertAt(service), "Expected cooldown to suppress additional alert");

        clock.plusSeconds(15 * 60L);
        service.runCheck();

        assertEquals(clock.instant(), readLastAlertAt(service));
    }

    private static Instant readLastAlertAt(MonitorService service) throws Exception {
        Field field = MonitorService.class.getDeclaredField("lastAlertAt");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Instant> map = (Map<String, Instant>) field.get(service);
        return map.values().stream().findFirst().orElse(null);
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
