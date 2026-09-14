# Architecture

## Overview

The Ride Booking System is a distributed microservices platform implementing a complete ride-hailing workflow.

## Services

```
                      React Frontend (Vite)
                              │
                              ▼
                        API Gateway (:8080)
                     JWT validation, CORS,
                     circuit breakers
                              │
           ┌──────────┬───────┴───────┬──────────┐
           ▼          ▼               ▼          ▼
      Auth Service  User Service  Driver Service  Ride Service
       (:8081)      (:8082)       (:8083)        (:8084)
           │                        │              │
           ▼                        ▼              ▼
       PostgreSQL              PostgreSQL     PostgreSQL
     (ride_booking)       (ride_booking_driver) (ride_service_db)
                                                      │
                                                      ▼
                                               Payment Service (:8086)
                                                      │
                                                      ▼
                                               Notification Service (:8085)
                                                      │
                                                      ▼
                                               PostgreSQL
                                             (payment_db, notification_db)
```

## Service Registry

Eureka runs on port **8761**. All services register with Eureka and resolve each other by service name (e.g., `http://DRIVER-SERVICE/api/v1/drivers`).

## API Gateway

All frontend traffic routes through the gateway. The gateway validates JWT tokens for protected endpoints, adds `X-User-Id`/`X-User-Email`/`X-User-Role` headers for downstream services, and applies Resilience4j circuit breakers.

### Gateway Routes

| Path Pattern | Target Service | Circuit Breaker |
|---|---|---|
| `/api/auth/**` | AUTH-SERVICE | authService |
| `/api/v1/users/**` | USER-SERVICE | userService |
| `/api/v1/drivers/**` | DRIVER-SERVICE | driverService |
| `/api/rides/**` | RIDE-SERVICE | rideService |
| `/api/v1/payments/**`, `/api/v1/wallet/**`, `/api/v1/invoices/**` | PAYMENT-SERVICE | paymentService |
| `/api/v1/notifications/**` | NOTIFICATION-SERVICE | notificationService |

### Public Endpoints (no JWT required)

- `POST /api/auth/login`
- `POST /api/auth/register`
- `GET /actuator/health`
- `GET /actuator/info`

## Database Design

Each service owns its own PostgreSQL database. Auth and User currently share `ride_booking` (legacy; will be separated in a future stage).

| Database | Service | Purpose |
|---|---|---|
| `ride_booking` | auth, user | User credentials and profiles |
| `ride_booking_driver` | driver | Driver and vehicle records |
| `ride_service_db` | ride | Ride lifecycle |
| `payment_db` | payment | Payments, wallet, invoices |
| `notification_db` | notification | Notification records |

## Environment Configuration

All services are configured via environment variables. See [docs/configuration.md](docs/configuration.md) and `.env.example`.

## Current Limitations (Stage 2)

- Kafka not implemented (planned for Stage 11)
- Redis not implemented (planned for Stage 9)
- Driver matching is basic (selects first ONLINE driver; advanced matching in Stage 10)
- Event-driven payment/notification integration incomplete (Stage 12–13)
- Java services run on the host; only PostgreSQL is containerized
  (via `docker-compose.yml` — see [docs/infrastructure.md](docs/infrastructure.md))
- No CI/CD pipelines (Stage 20)

## Local Infrastructure

PostgreSQL runs in Docker (single container). See
[docs/infrastructure.md](docs/infrastructure.md) for startup, database list,
health checks, and cleanup. The Java microservices connect to it through
environment variables (`DB_HOST:localhost`, `DB_PORT`) and do not run in
containers yet.
