# Host Monitor

`jupiterhost-monitor` is a Java 17 service that probes a TCP host on a fixed interval and emits alerts when the host appears down.

## Current behavior

- Connectivity is checked with a TCP socket connect (`TcpHostChecker`).
- A host-down alert is emitted only after `MONITOR_FAILURES_BEFORE_ALERT` consecutive failures.
- After an alert is sent, additional alerts are suppressed for `MONITOR_ALERT_COOLDOWN_MINUTES`.
- Console alerting is always enabled.
- SMTP email alerting is optional and enabled only when all required SMTP credentials/addresses are present.
- When SMTP is enabled, alerts are sent through a composite notifier so console alerts still run even if email sending fails.

## Environment variables

### Monitoring settings

| Variable | Default |
|---|---|
| `MONITOR_HOST` | `jcswebt110.ftn.fedex.com` |
| `MONITOR_PORT` | `443` |
| `MONITOR_TIMEOUT_MILLIS` | `3000` |
| `MONITOR_INTERVAL_SECONDS` | `30` |
| `MONITOR_FAILURES_BEFORE_ALERT` | `3` |
| `MONITOR_ALERT_COOLDOWN_MINUTES` | `15` |

Invalid values fall back to safe defaults; minimum bounds are enforced for timeout, interval, failures-before-alert, and cooldown.

### SMTP settings (optional)

| Variable | Default / rule |
|---|---|
| `MONITOR_SMTP_HOST` | `smtp.gmail.com` |
| `MONITOR_SMTP_PORT` | `587` |
| `MONITOR_SMTP_STARTTLS` | `true` |
| `MONITOR_SMTP_USERNAME` | required to enable SMTP |
| `MONITOR_SMTP_PASSWORD` | required to enable SMTP |
| `MONITOR_SMTP_FROM` | required to enable SMTP |
| `MONITOR_SMTP_TO` | required to enable SMTP |

`smtpEnabled()` requires all four credential/address values: username, password, from, and to.

## Build and test

```powershell
mvn test
mvn package
```

## Run

After packaging, run the shaded jar:

```powershell
java -jar target\jupiterhost-monitor-1.0.jar
```

Alternative (classes only):

```powershell
java -cp target\classes com.fedex.jupiter.MonitorJupiterHostServer
```

At startup the app prints whether SMTP alerting is enabled. Alerts are always printed to console as `[ALERT] ...`.
