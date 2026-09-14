# Infrastructure — Local Runtime (Stage 2)

## Purpose

Make the local runtime reproducible: a single Docker Compose entry point
provides PostgreSQL with persistent storage, automatic database bootstrapping,
and health checks. The Java microservices run on the host and connect through
environment-based configuration (see [configuration.md](configuration.md)).

Only **PostgreSQL** is containerized. Redis and Kafka are added in later stages.

```
Developer Machine
       │
       ▼
 Docker Compose ──► postgres (17-alpine)
       │                ├─ logical databases (init SQL)
       │                ├─ persistent volume
       │                └─ health check (pg_isready)
       │
       ▼
 localhost:5432 / DB_PORT
       │
       ▼
Java microservices (host / IDE / Maven)
       │
       ▼
 service-registry (Eureka :8761)
```

## Prerequisites

- Java 21 (Temurin/Adoptium recommended)
- Maven 3.9+ or the repository Maven wrapper (`./mvnw.cmd`)
- Docker Engine with Docker Compose v2 (Compose plugin). The root
  `docker-compose.yml` uses the modern Compose format (no obsolete
  `version:` field).

## Infrastructure startup

```bash
# One command brings up PostgreSQL
docker compose up -d postgres
```

First start pulls `postgres:17-alpine` and initializes a fresh data volume:
- creates the `postgres` superuser from `DB_USERNAME` / `DB_PASSWORD`
  (defaults `postgres` / `postgres`)
- creates `ride_booking` via `POSTGRES_DB`
- runs `docker/postgres/init/01-create-databases.sql`, which creates the
  remaining logical databases (system databases only; application tables are
  managed by the services via Hibernate `ddl-auto`)

```bash
docker compose ps          # container + health status
docker compose logs postgres
```

## Databases

| Database | Service(s) | Notes |
|---|---|---|
| `ride_booking` | auth-service, user-service | Shared by design (legacy); split planned |
| `ride_booking_driver` | driver-service | |
| `ride_service_db` | ride-service | |
| `payment_db` | payment-service | |
| `notification_db` | notification-service | |

Initialization scripts only create databases/users/extensions. They never
create application tables.

## Local development flow

```
Docker PostgreSQL (container)
        │
        ▼
  host localhost:<DB_PORT>
        │
        ▼
  Java services (IDE / Maven, DB_HOST=localhost)
```

Services are configured via `DB_HOST`, `DB_PORT`, `DB_USERNAME`, `DB_PASSWORD`
and per-service `*_DB_NAME`. The exposed host port is `${DB_PORT:-5432}`.

- **Services on the host** → `DB_HOST=localhost` (also the default).
- **Future containerized services** → `DB_HOST=postgres` inside the
  `ride-booking-network` Compose network (service name `postgres`, port 5432).

There is no need to edit Java configuration when switching between host and
Docker development; only the environment variables change.

### Port conflict with an existing PostgreSQL

Some developer machines already run a native PostgreSQL on `5432`. If the
Docker container cannot bind `5432`, or `localhost:5432` resolves to the native
instance, override the host port:

```bash
# .env
DB_PORT=5433
```

Then start the container (recreates the mapping) and run the services with the
same `DB_PORT`:

```bash
docker compose up -d --force-recreate postgres
# PowerShell:   $env:DB_PORT = "5433"
# bash:         export DB_PORT=5433
```

## Persistence & cleanup

Data lives in the named volume `ride-booking-postgres-data` (Compose volume
`postgres_data`).

| Command | Effect |
|---|---|
| `docker compose down` | Stops the container. **Data persists.** |
| `docker compose down -v` | Stops the container **and destroys** the volume (all local database data is gone). |

Because init scripts run only when the volume is empty, `down`/`up` does not
re-bootstrap databases. Deleting the volume (`down -v`) and re-running `up`
re-runs initialization from scratch.

## Health check

The container runs `pg_isready -U <DB_USERNAME> -d ride_booking` every 10 s
(5 s timeout, 5 retries, 10 s start period). `docker compose ps` shows
`(healthy)` only once PostgreSQL accepts connections.

## Verified state (Stage 2)

- `docker compose config` — valid (all 5 logical databases created on first init)
- All 6 persistence services start against the Docker PostgreSQL and complete
  Hibernate DDL in their own database.
- Context-load tests for auth/user/driver/ride/payment now pass against the
  Docker PostgreSQL.

## Later stages

- Redis (geospatial driver location) — containerized in a later stage
- Kafka (event backbone) — containerized in a later stage
- Java microservices themselves — containerized in a later stage