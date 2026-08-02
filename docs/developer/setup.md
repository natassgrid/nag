# Developer Setup Guide — National Assessment Grid

## 1. Prerequisites

Before setting up the project, ensure your development workstation meets the following minimum system requirements and software dependencies:

| Dependency | Minimum Version | Recommended Version | Download / Install Link |
|---|---|---|---|
| **Java JDK** | OpenJDK 21 | OpenJDK 21 LTS | [Adoptium Temurin 21](https://adoptium.net/) |
| **Node.js** | 20.x LTS | 20.18.x LTS | [Node.js Downloads](https://nodejs.org/) |
| **Angular CLI** | 21.0.0 | 21.2.x | `npm install -g @angular/cli@21` |
| **Docker Desktop**| 24.0+ | Docker Desktop 26+ | [Docker Install](https://www.docker.com/) |
| **PostgreSQL** | 16.0 | PostgreSQL 16.x | [PostgreSQL Downloads](https://www.postgresql.org/) |
| **Maven** | 3.9.0 | Maven 3.9.6 | [Apache Maven](https://maven.apache.org/) |

---

## 2. Repository Architecture & Directory Structure

```
f:\code\IdeaProjects\nag\
├── docs/                      # Architectural, Security & API Documentation
│   ├── adr/                   # Architecture Decision Records
│   ├── architecture/          # C4 diagrams & system architecture
│   ├── developer/             # Developer guides & setup instructions
│   ├── modules/               # Module specifications
│   ├── security/              # Security model & threat analysis
│   └── ui-implementation/     # Angular UI reference specs
├── frontend/                  # Angular 21 Standalone Component Frontend
│   ├── src/app/
│   │   ├── core/              # Guards, Services, Interceptors
│   │   ├── features/          # Domain Feature Modules (Exam, Questions, Papers, Scheduling)
│   │   └── shared/            # Shared Components (PaginatedTable, Dialogs)
│   ├── package.json
│   └── angular.json
└── backend/                   # Spring Boot Microservices
    ├── exam-service/          # Exam & Scheduling Microservice
    ├── question-service/      # Question Bank Microservice
    ├── paper-service/         # Cryptographic Paper Generation Microservice
    └── delivery-service/      # Test Delivery Microservice
```

---

## 3. Environment Variables Configuration

Copy `.env.example` to `.env` in your project root or configure your local environment:

```env
# General Platform Configuration
SPRING_PROFILES_ACTIVE=dev
PORT=8080

# Multi-Tenant & Security
DEFAULT_TENANT_ID=state-board-main
JWT_SECRET=supersecretkeyforlocaldevelopmentonly1234567890

# PostgreSQL Database Connections
DB_HOST=localhost
DB_PORT=5432
DB_NAME=nag_exam_db
DB_USERNAME=nag_user
DB_PASSWORD=nag_password

# Apache Kafka Event Bus
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Redis Cache
REDIS_HOST=localhost
REDIS_PORT=6379

# Keycloak OIDC IAM
KEYCLOAK_AUTH_SERVER_URL=http://localhost:8080/auth
KEYCLOAK_REALM=nag-realm
KEYCLOAK_CLIENT_ID=nag-frontend
```
