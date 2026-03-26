# Host Monitor

Monitors `jcswebt11.ftn.fedex.com` and prints an alert message to the console when the host is considered down.

## Behavior

- Checks TCP connectivity on a configured host/port every interval.
- Triggers an alert after N consecutive failures.
- Applies an alert cooldown to avoid repeated alert spam.
- Uses console logging for alert delivery.
- Invalid numeric config values fall back to safe defaults.

## Configuration (Environment Variables)

- `MONITOR_HOST` (default: `jcswebt11.ftn.fedex.com`)
- `MONITOR_PORT` (default: `443`)
- `MONITOR_TIMEOUT_MILLIS` (default: `3000`)
- `MONITOR_INTERVAL_SECONDS` (default: `30`)
- `MONITOR_FAILURES_BEFORE_ALERT` (default: `3`)
- `MONITOR_ALERT_COOLDOWN_MINUTES` (default: `15`)

## Run

### One-command launcher (Windows)

```powershell
.\run-monitor.ps1
```

```bat
run-monitor.cmd
```

Use `-SkipBuild` in PowerShell if you already built the project.

### Manual run

```powershell
mvn test
mvn package
java -cp target\classes com.fedex.jupiter.MonitorJupiterHostServer
```

> Alerts are written to console output.
