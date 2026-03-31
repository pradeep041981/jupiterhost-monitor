package com.fedex.jupiter.validate;

@FunctionalInterface
public interface HostCheckerFactory {
    HostChecker create(String host, int port, int timeoutMillis);
}
