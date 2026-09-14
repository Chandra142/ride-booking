# AUDIT REPORT — Stage 0

**Date inspected:** 2026-09-14
**Auditor:** opencode assistant (Stage 0)
**Repo:** ride-booking-system — Java 21 / Spring Boot 3.4.7 / Spring Cloud 2024.0.2 / PostgreSQL / React (Vite)

## 1. Scope & Method

- 10 Maven modules inspected (root POM + 9 children): compile config, dependencies, module list.
- 116 Java source files reviewed for config references, hardcoded values, security gaps, resilience.
- All 9 original configuration files (`*.properties`, `*.yaml`, `*.yml`) inspected.
- Docker assets (missing Dockerfiles; empty `docker-compose.yml`) audited.
- Frontend (`ride-booking-system-frontend`, 48 files): env handling, API base URL, `fetch` targets.
- 11 test classes reviewed (payment 3, ride 1, notification 8, auth/user/... contextLoads).
- Git: branch `main`, clean tree, 16 remote branches, 20 recent commits reviewed for secret leakage.
- Baseline build verified: `mvn clean test` fails only at DB-dependent context-load tests (no live PostgreSQL); compilation succeeds.

## 2. Architecture at Audit Time

| Component | Port | Technology | Notes |
|---|---|---|---|
| service-registry | 8761 | Eureka | No self-registration config |
| api-gateway | 8080 | Spring Cloud Gateway + JWT | Routes incomplete (no ride/notification) |
| auth-service | 8081 | Spring Security + jjwt 0.12.7 | Token + registration |
| user-service | 8082 | JPA | Shares DB with auth-service |
| driver-service | 8083 | JPA + in-memory availability | |
| ride-service | 8084 | JPA + RestTemplate | Matching = first ONLINE driver |
| notification-service | 8085 | Spring Mail + web | README claimed 8087 |
| payment-service | 8086 | JPA + wallet/invoice | |

## 3. Findings by Severity

### CRITICAL
| # | Finding | Location |
|---|---|---|
| C1 | Docker `docker-compose.yml` is empty boilerplate; no Dockerfiles exist — nothing can run as containers | `docker-compose.yml`, root |
| C2 | Kafka and Redis are declared in the tech stack but have zero implementation anywhere | — |

### HIGH
| # | Finding | Location |
|---|---|---|
| H1 | JWT secret hardcoded (`your-256-bit-secret-key-for-jwt-token-validation`), committed to git | `auth-service` + `api-gateway` configs |
| H2 | DB credentials hardcoded (`postgres`/`postgres`; `1234` in payment/notification), committed to git | all service configs |
| H3 | Gateway has no routes for `/api/rides/**` or `/api/v1/notifications/**`, yet the frontend calls them | `api-gateway` `application.yml` |
| H4 | Driver matching selects the first ONLINE driver, not the nearest | `RideService.java:51` (`availableDrivers.get(0)`) |
| H5 | `spring.jpa.hibernate.ddl-auto=update` in all services; no versioned migrations | all service configs |
| H6 | Notification port mismatch: README/config 8087 vs actual 8085 | `README.md`, configs |
| H7 | Endpoints accept `userId`/`riderId` from path/body without ownership verification → IDOR risk | user/ride/driver controllers |

### MEDIUM
| # | Finding | Location |
|---|---|---|
| M1 | ride-service couples to DRIVER-SERVICE via `RestTemplate` with no timeout/circuit breaker | `RideService.java` |
| M2 | Gateway reads `email`/`role` JWT claims that auth-service never writes → null identity headers downstream | gateway `JwtAuthenticationFilter`, auth `JwtService` |
| M3 | Mixed config formats: `.properties`, `.yaml`, `.yml` across services | all modules |
| M4 | `devtools` dependency + DEBUG logging + Hibernate SQL logging in several modules | ride and others |
| M5 | Actuator inconsistent: not exposed on most services; `show-details: always` on gateway | per-service configs |
| M6 | No tests for api-gateway, service-registry, common-dtos | — |
| M7 | auth-service and user-service share the same database + both define `User` entities | `application*.yml` |
| M8 | Duplicate `ApiResponse` DTO/controller-advice code across payment and notification | both modules |

### LOW
| # | Finding | Location |
|---|---|---|
| L1 | No root Maven wrapper; 8 identical per-service wrappers | `*/mvnw*` |
| L2 | `lombok.version` property defined but unused in parent POM | `pom.xml` |
| L3 | Generic `Main.java` in common-dtos | `common-dtos` |
| L4 | Frontend `.gitignore` lacks `.env` protection; base URL hardcoded in client calls | `ride-booking-system-frontend` |
| L5 | `spring-boot-devtools` present for non-local use | per-service POMs |

## 4. Baseline Verification

```bash
mvn -B clean test
# Stopped at auth-service contextLoads:
# FATAL: password authentication failed for user "postgres"
# => infrastructure-gated, not a code defect. All non-DB work compiled.
```

Tests present at audit time: payment (23), ride (1), notification (8), contextLoads (auth/user/driver/ride/payment) — none green in full without PostgreSQL.

## 5. Recommended Remediation Order (stage mapping)

1. **Stage 1 ✅ (done):** configuration foundation, env-based secrets, gateway routes, docs. See final Stage 1 report.
2. **Stage 2 (next):** standardize project foundation; common DTO review; backend service "nailing"; hardcoded-value sweep; API contract freeze.
3. **Stage 4:** database architecture, schema ownership split, Flyway migrations.
4. **Stage 5:** auth redesign — real JWT claims (`email`, `role`), refresh tokens, IDOR/ownership enforcement.
5. **Stage 6:** gateway hardening — rate limiting, CORS, correlation IDs, actuator exposure.
6. **Stage 10:** ride lifecycle + nearest-driver matching (fix `RideServiceTest.requestRide_shouldAssignNearestDriver`).
7. **Stages 9/11/18:** Redis, Kafka, Docker — fulfill C1/C2.

## 6. Deliberately Out of Scope (Stage 0)

The audit intentionally did **not** modify any code. JWT/DB *placeholder* policy and every service config were actively changed in Stage 1. Frontend functionality, Redis/Kafka/Docker, ride matching, and deployment remain untouched until their assigned stages.

## 7. Continuity

This report's findings feed `docs/configuration.md` and `IMPLEMENTATION_STATUS.md`. Any finding re-verified as resolved is removed from the active list in `IMPLEMENTATION_STATUS.md`.