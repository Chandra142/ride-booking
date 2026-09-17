# IMPLEMENTATION STATUS

## Current Stage: 7 — API Gateway Hardening

- **Stage 0 (Audit): COMPLETE** — full repository audit; findings recorded in `docs/AUDIT_REPORT.md`.
- **Stage 1 (Backend Foundation & Configuration): COMPLETE** — env-based config, profiles, gateway routes, docs.
- **Stage 2 (Infrastructure & Database Runtime): COMPLETE** — Docker PostgreSQL, health checks, database bootstrapping.
- **Stage 3 (Redis Geospatial Driver Location): COMPLETE** — Redis GEO index, per-driver freshness markers, location CRUD API.
- **Stage 4 (Location-Aware Driver Matching): COMPLETE** — geospatial matching with Redis GEO, atomic CAS assignment, driver release.
- **Stage 5 (Security, Payment Safety & Notification Hardening): COMPLETE** — JWT contract fix, ownership checks, payment idempotency/state machine, notification sender abstraction.
- **Stage 6 (Production-Quality Frontend & API Integration): COMPLETE** — React/Vite frontend with JWT auth.
- **Stage 7 (API Gateway Hardening): COMPLETE** — CORS, rate limiting, correlation IDs, security headers, resilience.

### Build Status (verified 2026-09-17)

Backend: `mvn clean test` over the full reactor: **BUILD SUCCESS** — 85 tests, 0 failures.
Frontend: `npm run lint` + `npm run build`: **PASS** — 0 lint errors, production build succeeds.

| Service | Compile | Unit Tests | Notes |
|---------|---------|------------|-------|
| common-dtos | ✅ PASS | N/A (library) | |
| service-registry | ✅ PASS | N/A (no tests) | |
| api-gateway | ✅ PASS | 13/13 pass | 1 context + 1 CORS + 11 filter tests |
| auth-service | ✅ PASS | 1/1 pass | contextLoads |
| user-service | ✅ PASS | 1/1 pass | contextLoads |
| driver-service | ✅ PASS | 11/11 pass | 2 context + 9 matching tests |
| ride-service | ✅ PASS | 22/22 pass | 1 context + 21 service tests |
| payment-service | ✅ PASS | 28/28 pass | 1 context + 6 invoice + 14 payment + 7 wallet |
| notification-service | ✅ PASS | 9/9 pass | 1 context + 8 integration tests |
| Frontend | ✅ PASS | N/A | lint + build pass |

### Stage 5 Implementation Summary

**JWT Contract Fix (auth-service + gateway):**
- `JwtService.generateToken(email, userId, role)`: subject = numeric userId, custom claims email + role
- Gateway `JwtAuthenticationFilter`: strips spoofed X-User-* headers, extracts trusted claims from JWT
- All services now trust gateway-injected X-User-Id, X-User-Email, X-User-Role

**Ownership Checks:**
- Ride-service: rider can only request/view/cancel own rides; driver role required for accept/start/complete
- Payment-service: ownership checks on create/get/refund; idempotency via `idempotencyKey` (unique constraint)
- Notification-service: ownership checks on create/get/send; `NotificationSender` interface + `MockNotificationSender`
- User-service: users can only update/delete own profile (admin exempt)

**Payment State Machine:** PENDING → SUCCESS/FAILED, SUCCESS → REFUNDED; reject invalid transitions

### Stage 6 Implementation Summary

**Frontend Architecture:**
- React 18 + Vite 5 + React Router 6
- Centralized auth via React Context (`AuthContext`)
- JWT decoded client-side for role-based routing (no arbitrary user IDs trusted)
- Axios interceptor attaches Bearer token; 401 → redirect to login
- Environment-based API base URL (`VITE_API_BASE_URL`, default `http://localhost:8080`)

**Routes Implemented:**
| Route | Role | Description |
|-------|------|-------------|
| `/login` | Public | Login page |
| `/register` | Public | Registration page |
| `/app/dashboard` | RIDER | Rider dashboard |
| `/app/request-ride` | RIDER | Request a ride |
| `/app/rides` | RIDER | Ride history |
| `/app/rides/:id` | RIDER | Ride details + cancel |
| `/app/payments` | RIDER | Payment list + new payment |
| `/app/notifications` | RIDER/DRIVER | Notification list |
| `/app/profile` | RIDER/DRIVER | Profile view/edit |
| `/driver/dashboard` | DRIVER | Driver dashboard + location |
| `/driver/rides` | DRIVER | Driver ride list |
| `/driver/rides/:id` | DRIVER | Ride details + accept/start/complete |

**Rider Functionality:**
- Dashboard with active ride, recent rides, quick actions
- Request ride with pickup/drop (location + coordinates)
- Ride list with status badges
- Ride details with cancel action
- Payment processing with idempotency key
- Notification list

**Driver Functionality:**
- Dashboard with availability toggle (ONLINE/OFFLINE/BUSY)
- Browser geolocation integration with 30-second updates
- Ride list and details with accept/start/complete actions
- Profile view/edit with vehicle information

**UI/UX:**
- Consistent design system (CSS custom properties)
- Responsive layout (sidebar + main content)
- Loading spinners, error states, empty states
- Status badges with color coding
- Card-based layout with proper spacing

### Stage 7 Implementation Summary

**Gateway Filters (new):**
- `CorrelationIdFilter` (order=-2): generates/propagates `X-Correlation-ID`; validates format (alphanumeric + hyphens + underscores), max 128 chars
- `SecurityHeadersFilter` (order=-3): X-Content-Type-Options, X-Frame-Options, Referrer-Policy, X-XSS-Protection, Cache-Control, Pragma
- `CorsConfig`: environment-aware CORS via `GATEWAY_CORS_ALLOWED_ORIGINS` etc.
- `RateLimitConfig`: `KeyResolver` bean using X-User-Id → IP fallback strategy

**Rate Limiting:**
- Redis-backed `RequestRateLimiter` as a default filter on all routes
- Configurable per-environment: `RATE_LIMIT_REPLENISH_RATE`, `RATE_LIMIT_BURST_CAPACITY`
- Local defaults: 50/100 requests; prod defaults: 20/40 requests

**Resilience:**
- Resilience4j CircuitBreaker per route (auth, user, driver, ride, payment, notification)
- TimeLimiter per route (5s for simple services, 10s for ride/payment)
- Fallback endpoints per service for circuit-breaker open state

**Configuration (new env vars):**

| Variable | Default (local) | Required in prod | Description |
|---|---|---|---|
| `REDIS_HOST` | `localhost` | yes | Redis host for rate limiting |
| `REDIS_PORT` | `6379` | yes | Redis port |
| `RATE_LIMIT_REPLENISH_RATE` | `50` (local) / `20` (prod) | recommended | Requests per second replenishment |
| `RATE_LIMIT_BURST_CAPACITY` | `100` (local) / `40` (prod) | recommended | Maximum burst capacity |
| `RATE_LIMIT_REQUESTED_TOKENS` | `1` | recommended | Tokens per request |
| `GATEWAY_CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | yes | Comma-separated allowed origins |
| `GATEWAY_CORS_ALLOWED_METHODS` | `GET,POST,PUT,PATCH,DELETE,OPTIONS` | recommended | Allowed HTTP methods |
| `GATEWAY_CORS_ALLOWED_HEADERS` | `*` | recommended | Allowed headers |
| `GATEWAY_CORS_ALLOW_CREDENTIALS` | `true` | recommended | Allow credentials |
| `GATEWAY_CORS_MAX_AGE` | `3600` | recommended | CORS preflight cache (seconds) |

**Tests Added (api-gateway):**
- `GatewayIntegrationTest`: 11 unit tests for CorrelationIdFilter, SecurityHeadersFilter, JwtAuthenticationFilter
- `CorsConfigTest`: 1 test verifying CorsWebFilter bean creation
- `ApiGatewayApplicationTests`: context load test

**Known Limitations:**
1. Driver rides page uses a simplified approach (no dedicated "rides by driver" endpoint exists)
2. Location updates are polling-based (30s interval), not real-time WebSocket
3. Payment form collects card details (mock gateway only — not real payment processing)
4. Profile editing for drivers is basic (first/last name + phone)