package com.fedex.jupiter.alert;

@FunctionalInterface
public interface AlertNotifier {
    void notifyAlert(String message);
}

