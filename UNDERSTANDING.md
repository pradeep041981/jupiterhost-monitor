# jupiterhost-monitor - Understanding Document

> Updated: 2026-03-26
> Language: Java 17 | Build tool: Maven 3

---

## 1. Purpose

`jupiterhost-monitor` is a lightweight Java daemon that continuously probes a remote host over TCP
and prints a **console alert** when the host is considered down.

The default monitored target is `jcswebt11.ftn.fedex.com:443`, and behavior is configurable via
environment variables.

---

## 2. High-Level Architecture

```
┌─────────────────────────────────────────────────┐
│          MonitorJupiterHostServer (main)       │
│                                                 │
│  Reads MonitorConfig -> builds collaborators    │
│                                                 │
│  ┌───────────────┐                              │
│  │ TcpHostChecker│                              │
│  │ (HostChecker) │                              │
│  └───────┬───────┘                              │
│          │  injected into                       │
│          ▼                                      │
│        MonitorService  <--- MonitorConfig       │
│                                                 │
│  ScheduledExecutorService calls                 │
│    monitorService::runCheck every N seconds     │
└─────────────────────────────────────────────────┘
```

---

## 3. Component Breakdown

### 3.1 `MonitorConfig`

Immutable value object containing all tunable parameters.

- `MonitorConfig.fromEnv()` loads from environment variables.
- `MonitorConfig.of(...)` is used by tests.

**Defaults:**

| Environment Variable | Default |
|---|---|
| `MONITOR_HOST` | `jcswebt11.ftn.fedex.com` |
| `MONITOR_PORT` | `443` |
| `MONITOR_TIMEOUT_MILLIS` | `3000` |
| `MONITOR_INTERVAL_SECONDS` | `30` |
| `MONITOR_FAILURES_BEFORE_ALERT` | `3` |
| `MONITOR_ALERT_COOLDOWN_MINUTES` | `15` |

**Normalization and safety rules:**
- Blank host falls back to default.
- Invalid/out-of-range port falls back to `443`.
- Invalid integer env values fall back to safe defaults.
- Minimum bounds are enforced for timeout/interval/threshold/cooldown.

---

### 3.2 `HostChecker` and `TcpHostChecker`

`HostChecker` defines:

```java
public interface HostChecker {
    boolean isHostUp();
}
```

`TcpHostChecker` performs a TCP socket connect to `host:port` with timeout.
- Connect success -> host is up.
- `IOException` (timeout/refused/DNS/etc.) -> host is down.

---

### 3.3 `MonitorService`

Core stateful engine with:
- `consecutiveFailures`
- `lastAlertAt`

**`runCheck()` behavior:**

1. Run connectivity check.
2. If host is up:
   - Log recovery when needed.
   - Reset consecutive failures.
3. If host is down:
   - Increment consecutive failures.
   - Suppress alert until failure threshold is reached.
   - Suppress alert during cooldown window.
   - Emit console alert when allowed.

Time is injected via `Clock` to make cooldown logic deterministic in tests.

---

### 3.4 `MonitorJupiterHostServer`

Entry point orchestration:

1. Load config from env.
2. Build `TcpHostChecker`.
3. Build `MonitorService` with `Clock.systemUTC()`.
4. Schedule periodic checks with `ScheduledExecutorService`.
5. Register shutdown hook and print startup details.

---

## 4. End-to-End Runtime Flow

```
JVM starts
|
+- MonitorConfig.fromEnv()
|    reads env vars, applies defaults/normalization
|
+- Build TcpHostChecker(host, port, timeout)
+- Build MonitorService(checker, config, Clock.systemUTC())
|
+- Register shutdown hook
+- Print startup banner
|
`- scheduler.scheduleAtFixedRate(monitorService::runCheck, 0, interval, SECONDS)
         |
         | [every interval seconds]
         v
    monitorService.runCheck()
         |
         +- TcpHostChecker.isHostUp()
         |
         +- [host UP] -> reset failures, optional recovery log
         |
         `- [host DOWN]
              consecutiveFailures++
              |
              +- below threshold? -> suppress
              |
              +- cooldown active? -> suppress
              |
              `- emit console alert and record lastAlertAt = now
```

---

## 5. Alert Suppression Controls

### 5.1 Failure Threshold (`MONITOR_FAILURES_BEFORE_ALERT`)

Avoids noise from transient blips by requiring N consecutive failures before first alert.

### 5.2 Cooldown (`MONITOR_ALERT_COOLDOWN_MINUTES`)

After an alert is emitted, further alerts are blocked until cooldown expires.

`nextAllowedAlert = lastAlertAt + cooldownMinutes * 60 seconds`

---

## 6. Dependencies

| Dependency | Scope | Purpose |
|---|---|---|
| `org.junit.jupiter:junit-jupiter:5.11.4` | test | Unit testing |

Production code otherwise relies on Java 17 standard library features for sockets, scheduling, and
time abstractions.

---

## 7. Testing Strategy

### `MonitorConfigTest`

Verifies:
- Invalid values fall back to defaults.
- Valid values are preserved.

### `MonitorServiceTest`

Uses test doubles:
- `QueueHostChecker` for predefined up/down sequence.
- `MutableClock` for deterministic cooldown tests.

Validates:
- Alert trigger at configured failure threshold.
- Alerts are suppressed during cooldown and resume after cooldown elapses.

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
java -cp target\classes com.fedex.jupiter.MonitorJupiterHostServer
```

---

## 9. Extension Points

| Change needed | How |
|---|---|
| Different connectivity strategy (HTTP/DB/ICMP) | Implement `HostChecker` |
| New config source (file/secrets manager) | Add `MonitorConfig` factory method |

---

## 10. Responsibility Summary

| Class / Interface | Role |
|---|---|
| `MonitorJupiterHostServer` | App wiring, scheduler lifecycle |
| `MonitorConfig` | Env parsing, defaults, normalization |
| `HostChecker` | Connectivity abstraction |
| `TcpHostChecker` | TCP reachability probe |
| `MonitorService` | Threshold + cooldown + alert emission orchestration |
