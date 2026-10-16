# Uptime Monitor
   ![CI](https://github.com/l-odom26/uptime-monitor/actions/workflows/ci.yml/badge.svg)
A self-hosted service that checks HTTP endpoints on a schedule, records status and latency, shows a
live dashboard, and sends an alert when a target goes down or recovers.

Built with **Kotlin, Spring Boot 3, Spring Data JPA, PostgreSQL**, tested with JUnit 5, packaged with
Docker, and built by GitHub Actions on every push.

![Dashboard](/dashboard.png)

## Features

- Add, edit, and delete monitors (URL, interval, expected status code, timeout) through a REST API or the dashboard
- Concurrent checks on a worker pool, so one slow target never delays the others
- Uptime %, average latency, and p95 latency over a 24-hour window
- A monitor goes DOWN only after N consecutive failures (default 3), then alerts once; it alerts once more on recovery
- Alerts to the log and, optionally, to a Slack or Discord-compatible webhook
- Old check results are purged on a schedule (default: 30 days)
- SSRF protection: loopback, private, and link-local targets are rejected by default

## Architecture

```
            +-----------------+       +-----------------+
 Browser -> |  REST API       | ----> |  MonitorService | ----> PostgreSQL
 (dashboard)|  (controllers)  |       |  (state + alerts)|        monitors
            +-----------------+       +--------+--------+        check_results
                                               ^
                                               | recordResult()
 +-----------------+  due monitors   +---------+--------+   HTTP GET
 |  CheckScheduler | --------------> |  worker pool     | ---------> target URLs
 |  (1s tick)      |                 |  (CheckRunner)   |
 +-----------------+                 +------------------+
```

- `CheckScheduler` runs a short tick loop, finds monitors whose interval has elapsed, and hands each
  check to a fixed thread pool. An in-flight set prevents overlapping checks of the same monitor.
- `CheckRunner` performs one HTTP request and always returns a result. Timeouts, DNS failures, and
  refused connections are recorded as failed checks, never thrown.
- `StatusEvaluator` is a pure state machine (UNKNOWN / UP / DOWN plus a consecutive-failure counter).
  `StatsCalculator` is pure too. Both are unit-tested without Spring or a database.

## Quick start

**With Docker (app + PostgreSQL):**

```bash
docker compose up --build
# dashboard: http://localhost:8080
```

**Local development:**

```bash
docker compose up db -d        # just PostgreSQL
./gradlew bootRun
```

Targets on localhost or private networks are blocked by default. To monitor something local while
developing, run with `ALLOW_PRIVATE_TARGETS=true`.

## API

| Method | Path | Description |
|---|---|---|
| POST | `/api/monitors` | Create a monitor |
| GET | `/api/monitors` | List monitors |
| GET | `/api/monitors/{id}` | Get one monitor |
| PUT | `/api/monitors/{id}` | Replace a monitor's settings |
| DELETE | `/api/monitors/{id}` | Delete a monitor and its history |
| GET | `/api/monitors/{id}/results?limit=50` | Recent checks, newest first (max 500) |
| GET | `/api/monitors/{id}/stats?hours=24` | Uptime %, average and p95 latency (max 720 hours) |
| GET | `/actuator/health` | Health check |

```bash
curl -X POST localhost:8080/api/monitors \
  -H 'Content-Type: application/json' \
  -d '{"name":"GitHub API","url":"https://api.github.com","intervalSeconds":30}'
```

Errors use RFC 7807 problem responses (`application/problem+json`).

## Configuration

| Setting | Env var | Default |
|---|---|---|
| Database URL / user / password | `DB_URL`, `DB_USER`, `DB_PASSWORD` | local PostgreSQL |
| Alert webhook | `ALERT_WEBHOOK_URL` | none (log only) |
| Allow private targets | `ALLOW_PRIVATE_TARGETS` | `false` |
| Failures before DOWN | `app.alert.failure-threshold` | `3` |
| Worker threads | `app.scheduler.worker-threads` | `8` |
| Result retention (days) | `app.retention.days` | `30` |
| Admin username |	ADMIN_USERNAME |	admin |
| Admin password (required, 8+ chars) |	ADMIN_PASSWORD |	none; the app won’t start without it |

## Tests

```bash
./gradlew test
```

- **Unit:** status state machine, stats and percentile math, scheduling logic, URL validation
- **Component:** `CheckRunner` against a real local HTTP server (success, wrong status, timeout, refused connection)
- **API:** MockMvc tests against an in-memory H2 database (validation, 404s, create/list/delete)

## Design decisions and trade-offs

- **Consecutive-failure threshold** instead of alerting on the first failure, which avoids noisy alerts from a single dropped request.
- **Pure functions for the core logic** so the interesting behavior is testable without mocks or a database.
- **Stats computed in memory** over the last N hours. Simple and fine at this scale; at higher volume, push aggregation into SQL or a rollup table.
- **Polling scheduler (1s tick) instead of one timer per monitor**: easy to reason about, survives restarts, and a monitor edited in the database is picked up automatically.
- **`ddl-auto: update`** keeps setup simple; see the roadmap for migrations.

## Security notes

- Monitor URLs are user input that the server fetches, which is an SSRF risk. Non-HTTP schemes and private/loopback/link-local targets are rejected at creation time.
- Known limitation: DNS is checked at creation only, so a hostname that later resolves to a private address (DNS rebinding) is not caught. A fix is to re-validate the resolved address on every check.
- The MVP has no authentication. Do not expose it publicly as-is; see the roadmap.
- The dashboard and API are protected by a single admin login (HTTP Basic). Credentials are sent on every request, so only deploy behind HTTPS (Render, Railway, and Fly.io provide this).
- No rate limiting on login attempts yet; use a long password.
- CSRF protection is disabled because no cookies or sessions are used and the API accepts only JSON with no CORS rules.

## Roadmap

- [ ] Multi-user accounts (Spring Security + JWT) with per-user monitors
- [ ] Flyway migrations instead of `ddl-auto: update`
- [ ] Incident history (open/close incidents with duration)
- [ ] Public status page
- [ ] Re-validate resolved IPs on every check (DNS rebinding)
- [ ] SQL-level aggregation / rollups for long retention
- [ ] Micrometer metrics + Prometheus endpoint

## Deployment

Any host that runs a Docker image and provides PostgreSQL works (Render, Railway, Fly.io). Set
`DB_URL`, `DB_USER`, `DB_PASSWORD`, and optionally `ALERT_WEBHOOK_URL`. Put authentication in front of
it (or finish the auth roadmap item) before making it public.
