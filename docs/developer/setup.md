# Developer Setup Guide — National Assessment Grid

## 1. Prerequisites

Before setting up the project, ensure your development workstation meets the following minimum system requirements:

| Dependency | Minimum Version | Recommended Version | Download / Install Link |
|---|---|---|---|
| **Java JDK** | OpenJDK 21 LTS | OpenJDK 21 LTS | [Adoptium Temurin 21](https://adoptium.net/) |
| **Node.js** | 20.x LTS | 20.18.x LTS | [Node.js Downloads](https://nodejs.org/) |
| **Angular CLI** | 21.0.0 | 21.2.x | `npm install -g @angular/cli@21` |
| **Docker & Compose** | Docker 24.0+ | Docker Desktop 26+ | [Docker Install](https://www.docker.com/) |
| **Gradle** | 8.14 (Included) | Use `./gradlew` | Built into repository wrapper |

---

## 2. Multi-Module Project Architecture

The NAG repository is organized as a Gradle multi-project build encompassing 14 domain microservices, 4 macro-service aggregators, 1 single JVM monolith, and 2 Angular frontends:

```
nag/
├── backend/
│   ├── shared-lib/              # Common models, DTOs, security, event publishing interfaces
│   │
│   ├── # ── Domain Microservices ────────────────────────────────
│   ├── identity-service/        # Port 8081: IAM, WebAuthn, OAuth2/OIDC
│   ├── candidate-service/       # Port 8082: Candidate profile, registration
│   ├── question-bank-service/   # Port 8083: Question authoring, taxonomy
│   ├── examination-service/     # Port 8085: Exam lifecycle, schedules, shifts
│   ├── paper-generator/         # Port 8086: Paper blueprint & crypto packaging
│   ├── delivery-service/        # Port 8087: Live examination delivery engine
│   ├── response-service/        # Port 8088: Real-time response ingestion
│   ├── evaluation-service/      # Port 8089: Automated & manual scoring
│   ├── result-service/          # Port 8090: Score normalization, merit lists
│   ├── audit-service/           # Port 8091: Immutable audit trail logging
│   ├── notification-service/    # Port 8092: Multi-channel notifications
│   ├── admin-service/           # Port 8093: Tenant management & config
│   ├── analytics-service/       # Port 8094: Telemetry & psychometric analytics
│   ├── asset-service/           # Port 8095: Multimedia asset storage
│   ├── api-gateway/             # Port 9000: Spring Cloud API Gateway
│   │
│   ├── # ── Macro-Service Aggregators ───────────────────────────
│   ├── auth-admin-app/          # Port 8081: Identity + Candidate + Admin + Notif
│   ├── content-app/             # Port 8083: QuestionBank + Exam + PaperGen + Asset
│   ├── execution-app/           # Port 8087: Delivery + Response (Hot path)
│   ├── post-exam-app/           # Port 8089: Evaluation + Result + Analytics
│   │
│   └── # ── Single JVM Monolith ─────────────────────────────────
│       └── monolith-app/        # Port 9000 (or 8080): All 14 modules in 1 process
│
├── frontend/                    # Admin / Controller Angular SPA (Port 4200)
├── candidate-frontend/          # Candidate Test-Taking Angular SPA (Port 4300)
├── infrastructure/              # Docker Compose topologies & Helm charts
└── docs/                        # Architecture, Developer, Security specs & ADRs
```

---

## 3. Initial Build & Verification

To verify that the complete multi-project build succeeds:

```bash
# Build all modules and generate bootJars
./gradlew assemble -x test
```

---

## 4. Running a Local Environment

Choose the deployment mode that matches your machine specs:

- **Single JVM Monolith (Fastest & recommended for everyday feature coding)**:
  ```bash
  ./infrastructure/docker-compose/redeploy-monolith.sh
  ```
- **Macro-Services (Consolidated 5 apps + RabbitMQ)**:
  ```bash
  ./infrastructure/docker-compose/redeploy-macro.sh
  ```
- **Microservices (14 apps + Kafka)**:
  ```bash
  ./infrastructure/docker-compose/redeploy-micro.sh
  ```
