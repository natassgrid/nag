# Open Source Government Examination Platform (national Assesment Grid)

A comprehensive, microservices-based examination platform for conducting large-scale government examinations. Built with Java 21, Spring Boot 3.x, Angular 21, and deployed via Docker.

## Architecture

```
┌─────────────┐     ┌─────────────────┐     ┌──────────────────────┐
│   Angular   │────▶│   API Gateway   │────▶│  15 Microservices    │
│  Frontend   │     │  (Port 9000)    │     │  (Ports 8081-8094)   │
│ (Port 4200) │     └─────────────────┘     └──────────────────────┘
└─────────────┘              │
      nginx                  │         ┌───────────────────────────┐
                             └────────▶│  Infrastructure           │
                                       │  PostgreSQL, Kafka, Redis │
                                       │  Vault, Keycloak, Jaeger  │
                                       └───────────────────────────┘
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21, Spring Boot 3.5.x, Spring Cloud 2025.x |
| Frontend | Angular 21, Angular Material (Indigo + Amber) |
| Database | PostgreSQL 16 (per-service schemas) |
| Messaging | Apache Kafka (KRaft mode) |
| Cache | Redis (standalone for dev) |
| Auth | Keycloak (OAuth2/OIDC), HS256 JWT (dev mode) |
| Secrets | HashiCorp Vault |
| Observability | Prometheus, Grafana, Jaeger (OpenTelemetry) |
| Build | Gradle 8.10.2, npm |
| Deploy | Docker Compose, Helm (production) |

## Backend Services

| Service | Port | Description |
|---------|------|-------------|
| identity-service | 8081 | Auth, MFA, WebAuthn, user management |
| candidate-service | 8082 | PII encryption, DigiLocker, face verification |
| question-bank-service | 8083 | Question CRUD, versioning, lifecycle FSM |
| translation-service | 8084 | 22 languages, review workflow |
| examination-service | 8085 | Exam creation, sections, publication |
| paper-generator | 8086 | Blueprint assembly, encryption |
| delivery-service | 8087 | Session management, navigation, proctoring |
| response-service | 8088 | Save/auto-save, revision history |
| evaluation-service | 8089 | Auto-eval, partial marking |
| result-service | 8090 | Score computation, PDF scorecards |
| audit-service | 8091 | HSM signing, tamper detection |
| notification-service | 8092 | Email, in-app SSE |
| admin-service | 8093 | User deactivation, config |
| analytics-service | 8094 | Dashboard API, CSV/PDF export |
| api-gateway | 9000 | Spring Cloud Gateway, rate limiting |

## Prerequisites

- Java 21 (OpenJDK)
- Node.js 22.x + npm
- Docker + Docker Compose
- Git

## Quick Start (Docker)

```bash
# Clone
git clone https://github.com/sheelprabhakar/nag.git
cd nag

# Full clean deploy (builds all services + starts infrastructure)
cd infrastructure/docker-compose
chmod +x redeploy-clean.sh build-and-deploy.sh
./redeploy-clean.sh
```

This will:
1. Tear down any existing containers and volumes
2. Start infrastructure (PostgreSQL, Kafka, Redis, Vault, Keycloak, Prometheus, Grafana, Jaeger)
3. Build all 15 backend services sequentially (shared Gradle cache)
4. Build Angular frontend
5. Start everything

## Shell Scripts

| Script | Description |
|--------|-------------|
| `infrastructure/docker-compose/redeploy-clean.sh` | Full clean redeploy |
| `infrastructure/docker-compose/redeploy-clean.sh --service <name>` | Rebuild and restart one service |
| `infrastructure/docker-compose/redeploy-clean.sh --smart` | Only rebuild services with code changes |
| `infrastructure/docker-compose/redeploy-clean.sh --no-cache` | Force rebuild without Docker cache |
| `infrastructure/docker-compose/build-and-deploy.sh` | Build and start (no teardown) |

## Build Commands

### Backend (Gradle)

```bash
# Build all (skip tests)
./gradlew build -x test

# Build specific service
./gradlew :backend:identity-service:bootJar

# Run tests for a service
./gradlew :backend:identity-service:test

# Run all tests
./gradlew test
```

### Frontend (Angular)

```bash
cd frontend

# Install dependencies
npm ci

# Development server (proxies API to localhost:9000)
ng serve

# Production build
ng build --configuration production

# Run tests
ng test
```

### Docker

```bash
cd infrastructure/docker-compose

# Start infrastructure only
docker compose -f docker-compose.yml up -d

# Start infrastructure + services
docker compose -f docker-compose.yml -f docker-compose.services.yml up -d

# Build one service image
docker compose -f docker-compose.yml -f docker-compose.services.yml build identity-service

# View logs
docker compose -f docker-compose.yml -f docker-compose.services.yml logs -f identity-service

# Stop everything
docker compose -f docker-compose.yml -f docker-compose.services.yml down

# Stop + remove volumes (fresh DB)
docker compose -f docker-compose.yml -f docker-compose.services.yml down -v
```

## Access URLs (Docker deployment)

| Service | URL |
|---------|-----|
| Frontend | http://localhost:4200 |
| API Gateway | http://localhost:9000 |
| Keycloak | http://localhost:8080 |
| Vault | http://localhost:8200 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 |
| Jaeger | http://localhost:16686 |
| PostgreSQL | localhost:5432 |

## Default Credentials

### PostgreSQL
- User: `exam_admin`
- Password: `exam_secret`
- Database: `exam_platform`

### Grafana
- User: `admin`
- Password: `grafana_secret`

### Vault
- Token: `vault_root_token`

### Seed Users (dev mode)

| Username | Role | Password |
|----------|------|----------|
| superadmin | SUPER_ADMIN | any (dev mode) |
| secadmin | SECURITY_ADMIN | any |
| author1 | QUESTION_AUTHOR | any |
| reviewer1 | REVIEWER | any |
| controller1 | EXAM_CONTROLLER | any |
| candidate1 | CANDIDATE | any |
| translator1 | TRANSLATOR | any |
| evaluator1 | EVALUATOR | any |
| auditor1 | AUDITOR | any |
| approver1 | APPROVER | any |

Tenant: `exam-authority-1`

> In Docker/dev profile, `DevKeycloakService` issues HS256-signed JWT tokens. Any password works.

## Project Structure

```
nag/
├── backend/
│   ├── shared-lib/              # Common DTOs, BaseEntity, utilities
│   ├── identity-service/        # Auth, users, roles
│   ├── candidate-service/       # Candidate profiles
│   ├── question-bank-service/   # Questions CRUD + lifecycle
│   ├── translation-service/     # Multi-language translations
│   ├── examination-service/     # Exam management
│   ├── paper-generator/         # Paper assembly
│   ├── delivery-service/        # Exam sessions
│   ├── response-service/        # Answer collection
│   ├── evaluation-service/      # Scoring
│   ├── result-service/          # Results + scorecards
│   ├── audit-service/           # Audit trail
│   ├── notification-service/    # Notifications
│   ├── admin-service/           # Admin operations
│   ├── analytics-service/       # Analytics
│   ├── api-gateway/             # Spring Cloud Gateway
│   └── Dockerfile               # Multi-stage build for all services
├── frontend/
│   ├── src/app/
│   │   ├── core/                # Auth service, guards, interceptors
│   │   └── features/            # Login, Dashboard, Questions, Exams, etc.
│   ├── Dockerfile               # Multi-stage build (Node + Nginx)
│   ├── nginx.conf               # API proxy + SPA routing
│   └── proxy.conf.json          # ng serve proxy config
├── infrastructure/
│   ├── docker-compose/
│   │   ├── docker-compose.yml           # Infrastructure services
│   │   ├── docker-compose.services.yml  # Application services
│   │   ├── init-db.sql                  # Database schema init
│   │   ├── redeploy-clean.sh            # Build + deploy script
│   │   └── .env                         # Environment variables
│   ├── helm/                    # Kubernetes Helm charts
│   ├── observability/           # OpenTelemetry config
│   └── docs/                    # API docs, ISO 27001
├── docs/
│   └── test-curls.md            # API test commands
├── .github/workflows/ci.yml     # CI pipeline (manual trigger)
├── build.gradle                 # Root Gradle config
├── settings.gradle              # Module declarations
└── .kiro/specs/                 # Design specs and task tracking
```

## Security (Dev Mode)

In Docker/dev profile:
1. Login → `DevKeycloakService` signs JWT with HS256 using shared secret
2. All services validate JWT using the same secret (`DevJwtConfig`)
3. Roles extracted from `realm_access.roles` in the JWT payload
4. API Gateway enforces auth on all routes except `/api/v1/identity/auth/**`

Production uses Keycloak JWKS endpoint for RS256 token validation.

## API Documentation

See [docs/test-curls.md](docs/test-curls.md) for curl commands to test all endpoints.

## Contributing

1. Create a feature branch from `develop`
2. Make changes
3. Run tests: `./gradlew test`
4. Push and create a PR to `develop`

## License

Apache 2.0
