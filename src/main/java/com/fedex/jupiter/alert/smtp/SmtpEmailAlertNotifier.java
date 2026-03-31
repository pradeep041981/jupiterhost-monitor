package com.fedex.jupiter.alert.smtp;

import com.fedex.jupiter.alert.AlertNotifier;
import com.fedex.jupiter.config.MonitorConfig;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

public class SmtpEmailAlertNotifier implements AlertNotifier {
    private final MonitorConfig config;

    public SmtpEmailAlertNotifier(MonitorConfig config) {
        this.config = config;
    }

    @Override
    public void notifyAlert(String message) {
        if (!config.smtpEnabled()) {
            return;
        }

        try {
            Session session = Session.getInstance(buildProperties(), new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(config.smtpUsername(), config.smtpPassword());
                }
            });

            Message email = new MimeMessage(session);
            email.setFrom(new InternetAddress(config.smtpFrom()));
            email.setRecipients(Message.RecipientType.TO, InternetAddress.parse(config.smtpTo()));
            email.setSubject(String.format("[JupiterHostMonitor] Host Down: %s:%d", config.host(), config.port()));
            email.setText(message);

            Transport.send(email);
            System.out.printf("Email alert sent to %s%n", config.smtpTo());
        } catch (MessagingException ex) {
            throw new IllegalStateException("Failed to send SMTP email alert", ex);
        }
    }

    private Properties buildProperties() {
        Properties props = new Properties();
        props.put("mail.smtp.host", config.smtpHost());
        props.put("mail.smtp.port", String.valueOf(config.smtpPort()));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", Boolean.toString(config.smtpStartTls()));
        return props;
    }
}

