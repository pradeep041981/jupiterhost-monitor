package com.fedex.jupiter.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonitorPropertiesTest {

    @Test
    void fallsBackToDefaultsForInvalidValues() {
        MonitorProperties config = new MonitorProperties();
        config.setHost("   ");
        config.setPort(70000);
        config.setTimeoutMillis(-100);
        config.setIntervalSeconds(0);
        config.setFailuresBeforeAlert(0);
        config.setAlertCooldownMinutes(-2);

        assertEquals("jcswebt11.ftn.fedex.com", config.host());
        assertEquals(443, config.port());
        assertEquals(3000, config.connectionTimeoutMillis());
        assertEquals(30, config.checkIntervalSeconds());
        assertEquals(3, config.failuresBeforeAlert());
        assertEquals(1, config.alertCooldownMinutes());
    }

    @Test
    void keepsProvidedValidValues() {
        MonitorProperties config = new MonitorProperties();
        config.setHost("example.org");
        config.setPort(8443);
        config.setTimeoutMillis(2000);
        config.setIntervalSeconds(10);
        config.setFailuresBeforeAlert(4);
        config.setAlertCooldownMinutes(20);

        assertEquals("example.org", config.host());
        assertEquals(8443, config.port());
        assertEquals(2000, config.connectionTimeoutMillis());
        assertEquals(10, config.checkIntervalSeconds());
        assertEquals(4, config.failuresBeforeAlert());
        assertEquals(20, config.alertCooldownMinutes());
    }

    @Test
    void enablesSmtpWhenRequiredFieldsAreProvided() {
        MonitorProperties config = new MonitorProperties();
        MonitorProperties.Smtp smtp = new MonitorProperties.Smtp();
        smtp.setHost("smtp.gmail.com");
        smtp.setPort(587);
        smtp.setStartTls(true);
        smtp.setFrom("user@example.org");
        smtp.setTo("alerts@example.org");
        config.setSmtp(smtp);

        assertEquals("smtp.gmail.com", config.smtpHost());
        assertEquals(587, config.smtpPort());
        assertTrue(config.smtpStartTls());
        assertTrue(config.smtpEnabled());
    }

    @Test
    void disablesSmtpWhenRequiredFieldsAreMissing() {
        MonitorProperties config = new MonitorProperties();
        config.setSmtp(new MonitorProperties.Smtp());

        assertFalse(config.smtpEnabled());
    }

    @Test
    void usesSharedDefaults() {
        MonitorProperties config = new MonitorProperties();

        assertEquals("jcswebt11.ftn.fedex.com", config.host());
        assertEquals(443, config.port());
        assertEquals(3000, config.connectionTimeoutMillis());
        assertEquals(30, config.checkIntervalSeconds());
        assertEquals(3, config.failuresBeforeAlert());
        assertEquals(1, config.alertCooldownMinutes());
        assertEquals("smtp.gmail.com", config.smtpHost());
        assertEquals(587, config.smtpPort());
        assertTrue(config.smtpStartTls());
    }
}
