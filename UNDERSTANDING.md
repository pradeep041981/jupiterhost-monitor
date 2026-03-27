# jupiterhost-monitor - Understanding Document

> Updated: 2026-03-27
> Language: Java 17 | Build tool: Maven 3

---

## 1. Purpose

`jupiterhost-monitor` is a lightweight Java daemon that continuously probes a remote host over TCP
and emits alerts when the host is considered down.

By default, it monitors `jcswebt110.ftn.fedex.com:443`. Alerts are always written to console and
can optionally be sent through SMTP email when SMTP env vars are fully configured.

---

## 2. High-Level Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                MonitorJupiterHostServer (main)                      │
│                                                                      │
│  Reads MonitorConfig -> wires checker + notifier + service           │
│                                                                      │
│  ┌───────────────┐            ┌──────────────────────────────────┐   │
│  │ TcpHostChecker│            │ AlertNotifier                    │   │
│  │ (HostChecker) │            │  - ConsoleAlertNotifier          │   │
│  └───────┬───────┘            │  - (optional) SmtpEmailAlert...  │   │
│          │                     │  - CompositeAlertNotifier        │   │
│          └──────────────┬──────┴──────────────────────────────────┘   │
│                         ▼                                              │
│                 MonitorService (stateful logic)                        │
│                                                                      │
│  ScheduledExecutorService calls monitorService::runCheck every N sec  │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 3. Component Breakdown

### 3.1 `MonitorConfig`

Immutable value object containing monitor and SMTP settings.

- `MonitorConfig.fromEnv()` loads values from environment variables.
- `MonitorConfig.of(...)` overloads are used by tests and direct construction.
- `smtpEnabled()` returns `true` only when username/password/from/to are all non-blank.

**Defaults:**

| Environment Variable | Default |
|---|---|
| `MONITOR_HOST` | `jcswebt110.ftn.fedex.com` |
| `MONITOR_PORT` | `443` |
| `MONITOR_TIMEOUT_MILLIS` | `3000` |
| `MONITOR_INTERVAL_SECONDS` | `30` |
| `MONITOR_FAILURES_BEFORE_ALERT` | `3` |
| `MONITOR_ALERT_COOLDOWN_MINUTES` | `15` |
| `MONITOR_SMTP_HOST` | `smtp.gmail.com` |
| `MONITOR_SMTP_PORT` | `587` |
| `MONITOR_SMTP_STARTTLS` | `true` |

**Normalization and safety rules:**
- Blank host falls back to default.
- Invalid or out-of-range monitor/SMTP ports fall back to defaults.
- Invalid integer env values fall back to defaults.
- Minimum bounds are enforced for timeout/interval/failure-threshold/cooldown.

---

### 3.2 `HostChecker` and `TcpHostChecker`

`HostChecker` is the connectivity abstraction:

```java
public interface HostChecker {
    boolean isHostUp();
}
```

`TcpHostChecker` attempts a TCP connect to `host:port` using configured timeout.

- Connect success -> host is up.
- `IOException` (timeout/refused/DNS/etc.) -> host is down.

---

### 3.3 Notifier Abstractions

- `AlertNotifier`: functional interface with `notifyAlert(String message)`.
- `ConsoleAlertNotifier`: emits `[ALERT] ...` to stdout.
- `SmtpEmailAlertNotifier`: sends email using Jakarta Mail and throws `IllegalStateException`
  on SMTP send errors.
- `CompositeAlertNotifier`: invokes all notifiers and catches per-notifier runtime failures so one
  failing notifier does not prevent others from running.

---

### 3.4 `MonitorService`

Core stateful engine with:

- `consecutiveFailures`
- `lastAlertAt`

`runCheck()` flow:

1. Perform host connectivity check.
2. If host is up:
   - Log recovery if there were prior failures.
   - Reset `consecutiveFailures` to 0.
3. If host is down:
   - Increment `consecutiveFailures`.
   - Suppress alert until failure threshold is reached.
   - Suppress alert while cooldown is active.
   - Build alert message and call injected `AlertNotifier`.
   - Record `lastAlertAt = now`.

Time is injected through `Clock` for deterministic cooldown tests.

---

### 3.5 `MonitorJupiterHostServer`

Main orchestration:

1. Load `MonitorConfig` from env.
2. Build `TcpHostChecker`.
3. Build notifier pipeline:
   - Always include `ConsoleAlertNotifier`.
   - If `smtpEnabled()`, wrap console + SMTP notifier in `CompositeAlertNotifier`.
4. Build `MonitorService(checker, config, Clock.systemUTC(), notifier)`.
5. Start fixed-rate scheduler for `runCheck()`.
6. Register shutdown hook to stop scheduler.

---

## 4. End-to-End Runtime Flow

```
JVM starts
|
+- MonitorConfig.fromEnv()
|    reads env vars, applies defaults/normalization
|
+- Build TcpHostChecker(host, port, timeout)
+- Build notifier:
|    console always, +SMTP when smtpEnabled()
|
+- Build MonitorService(checker, config, Clock.systemUTC(), notifier)
|
+- Register shutdown hook
+- Print startup status (SMTP enabled/disabled + monitor target)
|
`- scheduler.scheduleAtFixedRate(monitorService::runCheck, 0, interval, SECONDS)
         |
         | [every interval seconds]
         v
    monitorService.runCheck()
         |
         +- TcpHostChecker.isHostUp()
         |
         +- [host UP] -> optional recovery log + reset failures
         |
         `- [host DOWN]
              consecutiveFailures++
              |
              +- below threshold? -> suppress
              |
              +- cooldown active? -> suppress
              |
              `- notifier.notifyAlert(...) and lastAlertAt = now
```

---

## 5. Alert Suppression Controls

### 5.1 Failure Threshold (`MONITOR_FAILURES_BEFORE_ALERT`)

Reduces noise from transient blips by requiring N consecutive failures before alerting.

### 5.2 Cooldown (`MONITOR_ALERT_COOLDOWN_MINUTES`)

After an alert is emitted, further alerts are blocked until the cooldown window expires.

`nextAllowedAlert = lastAlertAt + (cooldownMinutes * 60 seconds)`

---

## 6. Dependencies

| Dependency | Scope | Purpose |
|---|---|---|
| `org.eclipse.angus:jakarta.mail:2.0.3` | compile | SMTP email alerts |
| `org.junit.jupiter:junit-jupiter:5.11.4` | test | Unit/integration tests |

Build uses Maven Shade Plugin to produce an executable jar with
`com.fedex.jupiter.MonitorJupiterHostServer` as main class.

---

## 7. Testing Strategy

### `MonitorServiceTest`

Uses test doubles:

- `QueueHostChecker` for predefined up/down sequences.
- `MutableClock` for deterministic cooldown timing.
- `RecordingNotifier` to assert alert dispatch behavior.

Validates threshold, cooldown suppression, and notifier invocation semantics.

### `MonitorConfigTest`

Validates configuration preservation for valid values and SMTP enable/disable behavior.

### `ConsoleAlertIntegrationTest`

Verifies console alerting path does not throw when host checks fail.

---

## 8. Build and Run

### Build

```powershell
mvn package
```

### Run tests

```powershell
mvn test
```

### Run the monitor

```powershell
java -jar target\jupiterhost-monitor-1.0.jar
```

Alternative:

```powershell
java -cp target\classes com.fedex.jupiter.MonitorJupiterHostServer
```

---

## 9. Extension Points

| Change needed | How |
|---|---|
| Different connectivity strategy (HTTP/DB/ICMP) | Implement `HostChecker` |
| Additional alert channels (Slack, webhook, PagerDuty) | Implement `AlertNotifier` and compose via `CompositeAlertNotifier` |
| New config source (file/secrets manager) | Add another `MonitorConfig` factory |

---

## 10. Responsibility Summary

| Class / Interface | Role |
|---|---|
| `MonitorJupiterHostServer` | App wiring, notifier composition, scheduler lifecycle |
| `MonitorConfig` | Env parsing, defaults, normalization, SMTP enablement gate |
| `HostChecker` | Connectivity abstraction |
| `TcpHostChecker` | TCP reachability probe |
| `AlertNotifier` | Alert channel abstraction |
| `ConsoleAlertNotifier` | Console alert output |
| `SmtpEmailAlertNotifier` | SMTP email alert output |
| `CompositeAlertNotifier` | Fan-out with per-channel failure isolation |
| `MonitorService` | Threshold + cooldown + alert orchestration |
