# Ride Booking System

A microservices ride-booking platform built with Java 21, Spring Boot 3.4.7, Spring Cloud (2024.0.2), PostgreSQL, and React/Vite.

## Services

| Service | Port | Purpose |
|---|---|---|
| Service Registry (Eureka) | 8761 | Service discovery |
| API Gateway | 8080 | Routing, JWT validation, circuit breakers |
| Auth Service | 8081 | Registration, login, JWT issuance |
| User Service | 8082 | User profile management |
| Driver Service | 8083 | Driver & vehicle management, availability |
| Ride Service | 8084 | Ride lifecycle, driver matching |
| Notification Service | 8085 | In-app/email notifications |
| Payment Service | 8086 | Payments, wallet, invoices |

## Tech Stack

- Java 21
- Spring Boot 3.4.7
- Spring Cloud 2024.0.2 (Eureka, Gateway, circuit breaker)
- PostgreSQL
- JWT (jjwt 0.12.7)
- Lombok
- React 18 + Vite 5

## Configuration

All services are configured through environment variables. See:

- [docs/configuration.md](docs/configuration.md) — full configuration reference
- `.env.example` — documented environment variables (copy to `.env` and adjust)

Default local development values require only a PostgreSQL instance reachable at `localhost:5432` with `postgres`/`postgres`. For production, activate the `prod` profile and supply secrets via the environment.

## Getting Started

### Prerequisites

- JDK 21
- Maven 3.9+ (or use the Maven wrapper at the repository root)
- Docker with Docker Compose (infrastructure: PostgreSQL)
- Node.js 18+ (frontend, optional)

### Start the infrastructure (PostgreSQL)

PostgreSQL runs in Docker; the microservices run on the host (IDE/Maven) and
connect to `localhost:<DB_PORT>`. This is the only containerized piece.

```bash
# Start PostgreSQL (creates all logical databases on first run)
docker compose up -d postgres

# Check status / logs / stop
docker compose ps
docker compose logs postgres
docker compose down          # stops container, keeps data
docker compose down -v       # ALSO destroys local database data
```

Databases (`ride_booking`, `ride_booking_driver`, `ride_service_db`,
`payment_db`, `notification_db`) are created automatically on first
initialization by `docker/postgres/init/01-create-databases.sql`.

> If another PostgreSQL already occupies port 5432 on the host, override the
> port in your `.env` (e.g. `DB_PORT=5433`) and run the same command; the
> services honor `DB_PORT`.

See [docs/infrastructure.md](docs/infrastructure.md) for details.

### Running the backend

Start the Service Registry first, then the remaining services in any order:

```bash
# From the repository root
mvn -pl service-registry spring-boot:run
mvn -pl api-gateway spring-boot:run
mvn -pl auth-service spring-boot:run
mvn -pl user-service spring-boot:run
mvn -pl driver-service spring-boot:run
mvn -pl ride-service spring-boot:run
mvn -pl notification-service spring-boot:run
mvn -pl payment-service spring-boot:run
```

### Running the frontend

```bash
cd ride-booking-system-frontend
npm install
npm run dev
```

The frontend calls the API Gateway at `http://localhost:8080` (override with `VITE_API_BASE_URL`).

## Testing

```bash
# Compile only (no live database required)
mvn clean compile

# Full test suite (context-load tests require PostgreSQL)
mvn clean test

# Frontend
cd ride-booking-system-frontend
npm run lint
npm run build
```

## Documentation

- [ARCHITECTURE.md](ARCHITECTURE.md) — system design, service boundaries, gateway routes
- [IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md) — current build/test/feature status
- [docs/configuration.md](docs/configuration.md) — environment variables, profiles, ports
- [docs/infrastructure.md](docs/infrastructure.md) — Docker infrastructure, databases, cleanup

## Known Limitations

- Kafka and Redis are planned but not yet implemented.
- Driver matching currently assigns the first ONLINE driver (advanced matching is a planned stage).
- Notification Service runs on port **8085** (8087 in older documentation was incorrect).
- Production migration strategy (Flyway) and access-control hardening are planned future stages.
- The Java microservices themselves are not containerized yet (planned stage).