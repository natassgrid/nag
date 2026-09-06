# NAG – Next-generation Assessment Grid

> **An Open Digital Public Infrastructure (DPI) platform for secure, scalable, AI-ready assessment, entrance examination, certification, and recruitment systems.**

[![License](https://img.shields.io/badge/license-AGPL%203.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)]()
[![Status](https://img.shields.io/badge/status-Active%20Development-success.svg)]()

---

# What is NAG?

**Next-generation Assessment Grid (NAG)** is an open-source Digital Public Infrastructure (DPI) platform for building secure, scalable, transparent, and AI-ready assessment ecosystems.

NAG enables governments, public service commissions, universities, certification bodies, enterprises, and educational institutions to design, conduct, evaluate, and audit high-stakes examinations at national scale. Built on modern cloud-native architecture, it supports computer-based testing (CBT), entrance examinations, recruitment, certifications, campus hiring, and continuous assessments.

---

# Architecture & Flexible Deployment Modes (Tri-Mode)

NAG is engineered from a single unified codebase to support **three operational deployment topologies** (Tri-Mode Architecture). Organizations can seamlessly switch topologies without changing business logic:

```
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│                                    UNIFIED CODEBASE                                         │
│                      (14 Domain Modules + Shared Security & Messaging)                      │
└─────────────────────────────────────────────────────────────────────────────────────────────┘
                                               │
             ┌─────────────────────────────────┼─────────────────────────────────┐
             ▼                                 ▼                                 ▼
   ┌───────────────────┐             ┌───────────────────┐             ┌───────────────────┐
   │  1. MICROSERVICES │             │  2. MACROSERVICES │             │   3. MONOLITH     │
   │      (Scale)      │             │    (Balanced)     │             │    (Ultralight)   │
   ├───────────────────┤             ├───────────────────┤             ├───────────────────┤
   │ • 14 Indep. Svcs  │             │ • 5 Aggregators   │             │ • 1 Single JAR    │
   │ • Apache Kafka    │             │ • RabbitMQ / Kafka│             │ • In-Memory Bus   │
   │ • API Gateway     │             │ • API Gateway     │             │ • Zero External MB│
   │ • Multi-AZ K8s    │             │ • Single EC2 / K8s│             │ • Local / Demo/Dev│
   │ • > 1M Candidates │             │ • Medium Workloads│             │ • ~1 GB RAM       │
   └───────────────────┘             └───────────────────┘             └───────────────────┘
```

### 1. 🚀 Full Microservices Mode (`micro`)
- **14 Independent Microservices** + API Gateway.
- **Message Broker**: Distributed Apache Kafka.
- **Best for**: Large-scale national examinations, high-concurrency peak windows (>1,000,000 candidates), multi-region Kubernetes clusters (EKS/GKE/OpenShift).
- **Redeploy script**: `./infrastructure/docker-compose/redeploy-micro.sh`

### 2. ⚡ Consolidated Macro-Services Mode (`macro`)
- **5 Deployable Service Units** (`auth-admin-app`, `content-app`, `execution-app`, `post-exam-app`, and standalone `audit-service`) + API Gateway.
- **Message Broker**: Lightweight RabbitMQ (or Kafka).
- **Best for**: State boards, medium universities, single VM deployments (e.g. AWS EC2 `t3.medium` / `t3.large`), and low-ops infrastructure.
- **Redeploy script**: `./infrastructure/docker-compose/redeploy-macro.sh`

### 3. 💡 Single JVM Monolith Mode (`monolith`)
- **1 Single Consolidated Spring Boot JAR** (`monolith-app`) embedding all 14 domain modules.
- **Message Broker**: Zero external message broker required (uses Spring in-process event bus).
- **Best for**: Developer workstations, rapid local testing, lightweight single-node evaluation/PoC, CI/CD pipelines, and memory-constrained environments (~1-2 GB RAM).
- **Redeploy script**: `./infrastructure/docker-compose/redeploy-monolith.sh`

---

# Core Modules

The platform is structured into modular domain-driven contexts:

| Module | Microservice Port | Macro Aggregator | Description |
|---|---|---|---|
| **Identity & Access** | `8081` | `auth-admin-app` | Keycloak OIDC/OAuth2 integration, MFA, WebAuthn, RBAC |
| **Candidate Service** | `8082` | `auth-admin-app` | Candidate profile, registration, biometric & consent management |
| **Admin Service** | `8093` | `auth-admin-app` | Tenant management, system config, dynamic feature flags |
| **Notification Service** | `8092` | `auth-admin-app` | Email, SMS, Webhook, and SSE notification dispatching |
| **Question Bank** | `8083` | `content-app` | Multilingual authoring, LaTeX/MathML, taxonomies, item pooling |
| **Examination Service** | `8085` | `content-app` | Exam lifecycles, schedule versions, shifts, center & seat allocation |
| **Paper Generator** | `8086` | `content-app` | Blueprint-based randomization, balancing, AES-256 envelope encryption |
| **Asset Service** | `8095` | `content-app` | Secure multimedia asset management (S3 / Local storage) |
| **Delivery Service** | `8087` | `execution-app` | High-throughput CBT candidate test runtime, time-lock verification |
| **Response Service** | `8088` | `execution-app` | Candidate answer ingestion, auto-save heartbeat, response bundling |
| **Evaluation Service** | `8089` | `post-exam-app` | Automated objective grading, anonymized subjective evaluation |
| **Result Service** | `8090` | `post-exam-app` | Score normalization, percentile calculation, merit list & scorecard generation |
| **Analytics Service** | `8094` | `post-exam-app` | Real-time delivery dashboards, psychometric & item discrimination analysis |
| **Audit Service** | `8091` | `audit-service` (Standalone) | Immutable, append-only hash-chained audit trails for legal compliance |
| **API Gateway** | `9000` | Gateway | Central ingress routing, rate limiting, and JWT validation |

---

# Quick Start

### Prerequisites
- **Java JDK 21** LTS
- **Node.js 20+** & **npm**
- **Docker Desktop 24+** & Docker Compose

### 1. Run in Single JVM Monolith Mode (Fastest & Lightest)
```bash
# Redeploy everything cleanly in single-process monolith mode
./infrastructure/docker-compose/redeploy-monolith.sh
```
- **Monolith API**: `http://localhost:9000` (or `http://localhost:8080`)
- **Admin UI**: `http://localhost:4200`
- **Candidate UI**: `http://localhost:4300`

### 2. Run in Macro-Services Mode
```bash
# Redeploy macro-services stack (5 apps + RabbitMQ + Gateway)
./infrastructure/docker-compose/redeploy-macro.sh
```

### 3. Run in Full Microservices Mode
```bash
# Redeploy full microservices stack (14 apps + Kafka + Gateway)
./infrastructure/docker-compose/redeploy-micro.sh
```

---

# Technology Stack

## Backend
- **Java 21** / **Spring Boot 3.x**
- **Spring Security** (OAuth2 Resource Server / JWT)
- **Spring Cloud Gateway**
- **Spring AI** (Ollama, LiteLLM, IndicTrans2)
- **Apache PDFBox** & **Apache Tika**

## Frontend
- **Angular 21** (Standalone Components)
- **Angular Material** (WCAG 2.1 AA / GIGW Accessible)
- **Tailwind CSS**

## Infrastructure & Messaging
- **PostgreSQL 16** with Multi-Schema Isolation
- **Redis 7** (Session cache & rate limiting)
- **Apache Kafka** / **RabbitMQ** / **Spring In-Memory Events**
- **Keycloak** (OIDC / OAuth2 IAM)
- **HashiCorp Vault** (Secrets & Key Management)
- **Kubernetes / Helm** & **Docker Compose**

---

# Repository Layout

```
nag/
├── backend/
│   ├── shared-lib/              # Shared entities, DTOs, security, & messaging abstractions
│   ├── identity-service/        # IAM & authentication service
│   ├── candidate-service/       # Candidate profile & registration
│   ├── question-bank-service/   # Question authoring & item bank
│   ├── examination-service/     # Examination lifecycle & scheduling
│   ├── paper-generator/         # Automated cryptographic paper packaging
│   ├── delivery-service/        # CBT examination delivery engine
│   ├── response-service/        # Candidate response ingestion
│   ├── evaluation-service/      # Evaluation & scoring engine
│   ├── result-service/          # Normalization & scorecard generation
│   ├── audit-service/           # Immutable compliance audit log
│   ├── notification-service/    # Email/SMS/Webhook notifications
│   ├── admin-service/           # Platform administration & dynamic configuration
│   ├── analytics-service/       # Psychometric & operations analytics
│   ├── asset-service/           # Multimedia asset management
│   ├── api-gateway/             # Ingress API Gateway
│   ├── auth-admin-app/          # Macro aggregator: Auth & Admin
│   ├── content-app/             # Macro aggregator: Content & Exam setup
│   ├── execution-app/           # Macro aggregator: Test delivery hot-path
│   ├── post-exam-app/           # Macro aggregator: Evaluation & Analytics
│   └── monolith-app/            # Single JVM Monolith aggregator (all 14 modules)
├── frontend/                    # Admin / Controller Angular SPA
├── candidate-frontend/          # Candidate Test Taking Angular SPA
├── infrastructure/
│   ├── docker-compose/          # Micro, Macro, and Monolith compose configs & scripts
│   └── helm/                    # Kubernetes Helm charts & values overlays
└── docs/                        # Architecture, Developer, Security & Module Documentation
```

---

# Documentation

Comprehensive documentation is available in the [`/docs`](docs/) directory:

- [**Architecture Overview**](docs/architecture/overview.md)
- [**Tri-Mode Deployment Architecture**](docs/architecture/deployment.md)
- [**Developer Setup Guide**](docs/developer/setup.md)
- [**Running Locally Guide**](docs/developer/running-locally.md)
- [**Docker & Compose Guide**](docs/developer/docker.md)
- [**Dual/Tri-Mode Walkthrough**](docs/micro-marco/walkthrough.md)
- [**Architecture Decision Records (ADR)**](docs/adr/)

---

# License

Licensed under the [GNU Affero General Public License v3.0 (AGPL-3.0-only)](LICENSE).
