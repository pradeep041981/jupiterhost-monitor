package com.fedex.jupiter.alert;

import java.util.Arrays;
import java.util.List;

public class CompositeAlertNotifier implements AlertNotifier {
    private final List<AlertNotifier> notifiers;

    public CompositeAlertNotifier(AlertNotifier... notifiers) {
        this.notifiers = Arrays.asList(notifiers);
    }

    @Override
    public void notifyAlert(String message) {
        for (AlertNotifier notifier : notifiers) {
            try {
                notifier.notifyAlert(message);
            } catch (RuntimeException ex) {
                System.err.printf("Alert notifier failed: %s%n", ex.getMessage());
            }
        }
    }
}

