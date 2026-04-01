package com.fedex.jupiter.config;

import com.fedex.jupiter.alert.AlertNotifier;
import com.fedex.jupiter.alert.smtp.SmtpEmailAlertNotifier;
import com.fedex.jupiter.service.MonitorService;
import com.fedex.jupiter.validate.HostCheckerFactory;
import com.fedex.jupiter.validate.TcpHostChecker;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(MonitorProperties.class)
public class SpringMonitorConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public AlertNotifier alertNotifier(MonitorProperties monitorProperties) {
        return new SmtpEmailAlertNotifier(monitorProperties);
    }

    @Bean
    public HostCheckerFactory hostCheckerFactory() {
        return TcpHostChecker::new;
    }

    @Bean
    public MonitorService monitorService(
            MonitorProperties monitorProperties,
            Clock clock,
            AlertNotifier alertNotifier,
            HostCheckerFactory hostCheckerFactory) {
        return new MonitorService(monitorProperties, clock, alertNotifier, hostCheckerFactory);
    }
}

