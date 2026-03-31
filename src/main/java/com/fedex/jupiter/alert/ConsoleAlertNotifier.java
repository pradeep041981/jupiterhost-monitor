package com.fedex.jupiter.alert;

public class ConsoleAlertNotifier implements AlertNotifier {
    @Override
    public void notifyAlert(String message) {
        System.out.printf("[ALERT] %s%n", message);
    }
}

