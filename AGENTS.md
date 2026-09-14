# AGENTS.md

## What This Project Is

A microservices ride-booking platform built with Java 21, Spring Boot 3.4.7, Spring Cloud, PostgreSQL, and React/Vite.

## Repository Structure

```
ride-booking-system/
├── service-registry/        (port 8761)  — Eureka discovery server
├── api-gateway/             (port 8080)  — Spring Cloud Gateway + JWT validation
├── auth-service/            (port 8081)  — Authentication, registration, JWT
├── user-service/            (port 8082)  — User profile management
├── driver-service/          (port 8083)  — Driver management, availability
├── ride-service/            (port 8084)  — Ride lifecycle, driver matching
├── payment-service/         (port 8086)  — Payments, wallet, invoices
├── notification-service/    (port 8085)  — Notifications (email, in-app)
├── common-dtos/                          — Shared DTO library (payment)
├── ride-booking-system-frontend/         — React + Vite frontend
├── pom.xml                              — Root Maven reactor
└── docker-compose.yml                   — Local infrastructure (PostgreSQL)
```

## Development Conventions

- **Language:** Java 21 (no --release flag; source/target both 21)
- **Build:** Maven (wrapper available at root and each service)
- **Config:** All services use `application.yml` with env-var substitution
- **Profiles:** `local` (dev convenience), `prod` (production-safe defaults)
- **Secrets:** Externally supplied via environment; never committed
- **Entities:** Service-owned; no cross-service shared tables
- **DTOs:** Service-local unless genuinely cross-service (payment DTOs in `common-dtos`)

## Running Locally

```bash
# 1. Start infrastructure (PostgreSQL in Docker; Redis/Kafka added later)
docker compose up -d postgres

# 2. Start Eureka first
mvn -pl service-registry spring-boot:run

# 3. Start other services (any order after Eureka)
mvn -pl api-gateway spring-boot:run
mvn -pl auth-service spring-boot:run
# ...etc

# 4. Frontend
cd ride-booking-system-frontend && npm install && npm run dev
```

## Testing

```bash
# Compile-only (no live DB required)
mvn clean compile

# Full test suite (requires PostgreSQL)
mvn clean test

# Frontend
cd ride-booking-system-frontend && npm run lint && npm run build
```

## Key Files for Agents

| File | Purpose |
|------|---------|
| `pom.xml` | Root POM — dependency management, module list |
| `*/application.yml` | Per-service configuration |
| `*/application-prod.yml` | Production overrides (no secrets) |
| `.env.example` | Documented environment variables |
| `docs/configuration.md` | Configuration reference |
| `docs/infrastructure.md` | Docker infrastructure, databases, cleanup |
| `IMPLEMENTATION_STATUS.md` | Current build/test status |
