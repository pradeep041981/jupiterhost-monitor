package com.fedex.jupiter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MonitorJupiterHostServer {
    public static void main(String[] args) {
        SpringApplication.run(MonitorJupiterHostServer.class, args);
    }
}