# Jupiter Host Monitor

Spring Boot 3 (Java 17) service that checks TCP reachability for one or more hosts and sends alert notifications when a host is considered down.

## Security Notice

This project currently relies on configuration defaults in `MonitorProperties` for SMTP host/port/TLS when related values are missing.

- Treat this as security debt.
- Do not use these fallbacks in production.
- Prefer secret-only injection through environment variables or a secret manager.

## What It Does

- Monitors one or more hosts on a configured TCP port.
- Runs checks on a fixed schedule.
- Tracks failure counters independently per host.
- Sends alert after configured consecutive failures.
- Suppresses repeated alerts during per-host cooldown.
- Logs recovery when a previously failing host comes back up.

## Architecture

### Main components

- `MonitorJupiterHostServer`
  - Spring Boot entry point (`@SpringBootApplication`).
  - Enables periodic checks using `@EnableScheduling`.
- `MonitorProperties`
  - Binds and normalizes monitor configuration values.
  - Exposes typed config values.
  - Decides whether SMTP is enabled via `smtpEnabled()`.
- `MonitorService`
  - Runs host checks and state transitions.
  - Stores in-memory state per host (`consecutiveFailures`, `lastAlertAt`).
  - Applies threshold and cooldown logic before notifying.
- `MonitorScheduler`
  - Spring-managed scheduler (`@Scheduled`) that triggers monitor cycles.
  - Logs startup/shutdown monitor status.
- `TcpHostChecker`
  - Performs TCP socket connect probe with timeout.
- `SmtpEmailAlertNotifier`
  - Sends email with Jakarta Mail when SMTP is enabled.

### Package map

- `com.fedex.jupiter` - bootstrap and scheduling
- `com.fedex.jupiter.config` - configuration
- `com.fedex.jupiter.service` - monitoring logic
- `com.fedex.jupiter.validate` - host-check abstractions and TCP implementation
- `com.fedex.jupiter.alert` - notifier abstraction
- `com.fedex.jupiter.alert.smtp` - SMTP notifier adapter

## Monitoring Logic

For each scheduled cycle:

1. Loop through all configured hosts.
2. Probe host reachability using TCP connect.
3. If host is up:
   - log recovery when failure count was non-zero,
   - reset failure count to `0`.
4. If host is down:
   - increment failure count,
   - if below threshold, stop.
5. If threshold reached:
   - enforce cooldown per host,
   - send alert when cooldown has expired,
   - update last-alert timestamp.

State is in-memory only and resets on process restart.

## Configuration

Configuration is property-driven via Spring Boot binding (`monitor.*`), which can be provided through environment variables.

### Monitoring variables

| Variable | Default |
|---|---|
| `MONITOR_HOST` | `jcswebt11.ftn.fedex.com,jcswebt111.ftn.fedex.com,jcswebt112.ftn.fedex.com` |
| `MONITOR_PORT` | `443` |
| `MONITOR_TIMEOUT_MILLIS` | `3000` |
| `MONITOR_INTERVAL_SECONDS` | `30` |
| `MONITOR_FAILURES_BEFORE_ALERT` | `3` |
| `MONITOR_ALERT_COOLDOWN_MINUTES` | `1` |

### SMTP variables

| Variable | Default / behavior |
|---|---|
| `MONITOR_SMTP_HOST` | `smtp.gmail.com` |
| `MONITOR_SMTP_PORT` | `587` |
| `MONITOR_SMTP_STARTTLS` | `true` |
| `MONITOR_SMTP_FROM` | Uses fallback value in source if unset |
| `MONITOR_SMTP_TO` | Uses fallback value in source if unset |

SMTP is enabled only when `MONITOR_SMTP_FROM` and `MONITOR_SMTP_TO` are both non-blank.

## Build and Run

### Prerequisites

- Java 17+
- Maven 3.8+

### Build and test

```powershell
mvn test
mvn package
```

### Run with Spring Boot plugin

```powershell
mvn spring-boot:run
```

### Run packaged JAR

```powershell
java -jar target\jupiterhost-monitor-1.0.jar
```

### Run with environment setup script

```powershell
. .\setEnv.ps1
java -jar target\jupiterhost-monitor-1.0.jar
```

### Shutdown

- Stop process with `Ctrl+C` (or process signal).
- Spring lifecycle shutdown logs `Host monitor stopped.`

## Testing

Current unit tests cover:

- single-host threshold alert behavior,
- cooldown suppression,
- alert timestamp updates,
- multi-host independent counters and cooldown windows,
- basic config accessors and SMTP enablement checks.

Potential additions:

- SMTP integration tests for transport failures/retries,
- stronger config parsing edge-case tests,
- long-running scheduler resiliency tests.

## Operational Notes

- Runtime model is a single Spring Boot JVM process.
- Checks run serially per cycle (single scheduled task).
- No persistent state; restart resets counters and cooldown memory.
- Observability is console logs plus SMTP alerts.

## Known Risks and Next Improvements

1. Remove SMTP host/port/TLS source defaults and enforce secrets-only configuration.
2. Add pluggable notifiers (Webhook/Slack/PagerDuty).
3. Add structured logging and metrics (`alerts_sent`, `checks_failed`, `cooldown_suppressed`).
4. Consider state persistence if restart continuity is required.
5. Add deployment assets (`Dockerfile`, health checks, runtime profiles).
