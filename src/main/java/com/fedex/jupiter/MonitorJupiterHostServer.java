package com.fedex.jupiter;

import java.time.Clock;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MonitorJupiterHostServer {
    public static void main(String[] args) {
        MonitorConfig config = MonitorConfig.fromEnv();

        HostChecker checker = new TcpHostChecker(config.host(), config.port(), config.connectionTimeoutMillis());
        MonitorService monitorService = new MonitorService(checker, config, Clock.systemUTC());
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduler.shutdownNow();
            System.out.println("Host monitor stopped.");
        }));

        System.out.printf(
                "Monitoring host %s:%d every %d seconds.%n",
                config.host(),
                config.port(),
                config.checkIntervalSeconds());

        scheduler.scheduleAtFixedRate(
                monitorService::runCheck,
                0,
                config.checkIntervalSeconds(),
                TimeUnit.SECONDS
        );
    }
}