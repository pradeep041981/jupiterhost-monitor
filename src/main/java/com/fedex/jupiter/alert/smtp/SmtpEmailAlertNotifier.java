package com.fedex.jupiter.alert.smtp;

import com.fedex.jupiter.alert.AlertNotifier;
import com.fedex.jupiter.config.MonitorProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

public class SmtpEmailAlertNotifier implements AlertNotifier {
    private final MonitorProperties config;

    @Autowired
    private JavaMailSender sender;

    public SmtpEmailAlertNotifier(MonitorProperties config) {
        this.config = config;
    }

    @Override
    public void notifyAlert(String message) {
        if (!config.smtpEnabled()) {
            return;
        }

        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(config.smtpFrom());
            mail.setTo(config.smtpTo().split(","));
            mail.setSubject("Alert : Jupiter Web Application Server Down!");
            mail.setText(message);
            sender.send(mail);

            System.out.printf("Email alert sent to %s%n", config.smtpTo());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to send SMTP email alert", ex);
        }
    }
}
