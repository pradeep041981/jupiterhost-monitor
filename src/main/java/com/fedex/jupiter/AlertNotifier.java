package com.fedex.jupiter;

@FunctionalInterface
public interface AlertNotifier {
    void notifyAlert(String message);
}

