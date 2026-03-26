package com.fedex.jupiter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MonitorConfigTest {

    @Test
    void fallsBackToDefaultsForInvalidValues() {
        MonitorConfig config = MonitorConfig.of(
                "   ",
                70000,
                -100,
                0,
                0,
                -2
        );

        assertEquals("jcswebt11.ftn.fedex.com", config.host());
        assertEquals(443, config.port());
        assertEquals(3000, config.connectionTimeoutMillis());
        assertEquals(30, config.checkIntervalSeconds());
        assertEquals(3, config.failuresBeforeAlert());
        assertEquals(15, config.alertCooldownMinutes());
    }

    @Test
    void keepsProvidedValidValues() {
        MonitorConfig config = MonitorConfig.of(
                "example.org",
                8443,
                2000,
                10,
                4,
                20
        );

        assertEquals("example.org", config.host());
        assertEquals(8443, config.port());
        assertEquals(2000, config.connectionTimeoutMillis());
        assertEquals(10, config.checkIntervalSeconds());
        assertEquals(4, config.failuresBeforeAlert());
        assertEquals(20, config.alertCooldownMinutes());
    }
}
