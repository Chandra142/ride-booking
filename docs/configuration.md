# Configuration Reference

## Overview

Every service reads configuration from environment variables at startup. Defaults are provided for **local development only** so the system can run with zero extra configuration against a locally available PostgreSQL on `localhost:5432` with `postgres`/`postgres`.

**Production deployments must supply secrets explicitly** via the environment. The `prod` profile overrides database password and JWT secret with *no* defaults, so a service fails fast if a required secret is missing.

## Profiles

| Profile | When active | Behavior |
|---|---|---|
| (none) | default | Safe local defaults, env-var substitution |
| `local` | `SPRING_PROFILES_ACTIVE=local` | `show-sql=true`, DEBUG logging for service packages + Hibernate SQL |
| `prod` | `SPRING_PROFILES_ACTIVE=prod` | `ddl-auto=validate`, `show-sql=false`, INFO logging, secrets required |

## Environment Variables

### Core

| Variable | Default (local dev) | Required in prod | Description |
|---|---|---|---|
| `DB_HOST` | `localhost` | yes | PostgreSQL host |
| `DB_PORT` | `5432` | yes | PostgreSQL port |
| `DB_USERNAME` | `postgres` | yes | PostgreSQL user |
| `DB_PASSWORD` | `postgres` | **yes** | PostgreSQL password (no default in prod) |

### Per-service database names

| Variable | Default | Owned by |
|---|---|---|
| `AUTH_DB_NAME` | `ride_booking` | Auth Service |
| `USER_DB_NAME` | `ride_booking` | User Service (shared DB, legacy) |
| `DRIVER_DB_NAME` | `ride_booking_driver` | Driver Service |
| `RIDE_DB_NAME` | `ride_service_db` | Ride Service |
| `PAYMENT_DB_NAME` | `payment_db` | Payment Service |
| `NOTIFICATION_DB_NAME` | `notification_db` | Notification Service |

### Eureka

| Variable | Default | Description |
|---|---|---|
| `EUREKA_HOSTNAME` | `localhost` | Registry instance hostname |
| `EUREKA_SERVER_URL` | `http://localhost:8761/eureka/` | Registry default zone URL |
| `EUREKA_ENABLE_SELF_PRESERVATION` | `false` | Registry self-preservation (use `true` in prod) |

### JWT

| Variable | Default (dev only) | Required in prod | Description |
|---|---|---|---|
| `JWT_SECRET` | `dev-only-change-me-...` (see `.env.example`) | **yes** | HMAC secret, ≥ 32 bytes for HS256 |
| `JWT_EXPIRATION` | `86400000` (24h) | recommended | Token lifetime in milliseconds |

### Ride Matching

| Variable | Default | Service | Description |
|---|---|---|---|
| `DRIVER_MATCHING_RADIUS_KM` | `10.0` | ride-service | Search radius for nearby drivers (km) |

### Driver Location (Redis)

| Variable | Default | Service | Description |
|---|---|---|---|
| `DRIVER_LOCATION_TTL_SECONDS` | `300` | driver-service | Per-driver location freshness TTL |
| `DRIVER_LOCATION_DEFAULT_RADIUS_KM` | `5.0` | driver-service | Default nearby search radius |
| `DRIVER_LOCATION_MAX_RADIUS_KM` | `50.0` | driver-service | Maximum allowed search radius |

### Ports

| Variable | Default | Service |
|---|---|---|
| `SERVER_PORT` | per-service (below) | applies to all services when needed |

## Service Ports (defaults)

| Service | Port |
|---|---|
| Service Registry (Eureka) | 8761 |
| API Gateway | 8080 |
| Auth Service | 8081 |
| User Service | 8082 |
| Driver Service | 8083 |
| Ride Service | 8084 |
| Notification Service | **8085** |
| Payment Service | 8086 |

> **Note:** The Notification Service runs on **8085** in the actual configuration. Earlier documentation mentioning 8087 was incorrect and has been corrected.

## Security Notes

- Never log passwords, JWT tokens, authorization headers, card numbers, or CVV values.
- The `prod` profile uses `spring.jpa.hibernate.ddl-auto=validate` and prints no SQL.
- The gateway should use the same `JWT_SECRET` as the Auth Service.
- `.env` files are git-ignored; only `.env.example` is committed with placeholders.

## Running Locally

```bash
# 0. If not already running, start the infrastructure (see docs/infrastructure.md)
docker compose up -d postgres

# From the repository root (Maven 3.9+ or ./mvnw):
mvn -pl service-registry spring-boot:run      # start Eureka first
mvn -pl api-gateway spring-boot:run
mvn -pl auth-service spring-boot:run
mvn -pl user-service spring-boot:run
mvn -pl driver-service spring-boot:run
mvn -pl ride-service spring-boot:run
mvn -pl payment-service spring-boot:run
mvn -pl notification-service spring-boot:run

# Frontend
cd ride-booking-system-frontend
npm install
npm run dev
```

The frontend talks only to the API Gateway (`http://localhost:8080` by default, configurable via `VITE_API_BASE_URL`).

## Infrastructure Notes (Docker)

- PostgreSQL runs in Docker (`docker compose up -d postgres`); services run on
  the host and use `DB_HOST=localhost` (the default). The container maps host
  port `${DB_PORT:-5432}` to container port 5432.
- Other processes inside the `ride-booking` Compose network reach the database
  as `postgres:5432` (see `DB_HOST=postgres` in the example below).
- If a native PostgreSQL already occupies host port 5432, override `DB_PORT`
  (e.g. `5433`) in `.env` — services and Compose both honor it.
- The logical databases are created automatically on first container init by
  `docker/postgres/init/01-create-databases.sql`. See
  [docs/infrastructure.md](docs/infrastructure.md).
- `docker compose down -v` destroys all local database data (volume wipe).

## Example

Set these in your shell or in a `docker-compose` environment:

```env
DB_HOST=postgres
DB_PORT=5432
DB_USERNAME=postgres
DB_PASSWORD=postgres
EUREKA_SERVER_URL=http://service-registry:8761/eureka/
JWT_SECRET=$(openssl rand -base64 48)
SPRING_PROFILES_ACTIVE=prod
```