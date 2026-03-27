package com.fedex.jupiter;

public class ConsoleAlertNotifier implements AlertNotifier {
    @Override
    public void notifyAlert(String message) {
        System.out.printf("[ALERT] %s%n", message);
    }
}

