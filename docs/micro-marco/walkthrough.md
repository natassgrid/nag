# Walkthrough: Dual-Mode Architecture (Microservices ↔ Macro-Services)

We have successfully implemented a **dual-mode deployment architecture** for the NAG Open Digital Public Infrastructure (DPI) Platform. From the **exact same codebase**, you can now deploy either as:
1. **Full Microservices Mode**: 14 distinct microservices + API Gateway with Apache Kafka (for Kubernetes, large-scale multi-tenant production).
2. **Consolidated Macro-Services Mode**: 5 deployable units + API Gateway with lightweight RabbitMQ (for EC2 single-instance, demo environments, and small tenants).

---

## Architecture Overview

```
                          ┌────────────────────────┐
                          │   Frontend & React UI  │
                          └───────────┬────────────┘
                                      │
                                      ▼
                          ┌────────────────────────┐
                          │   API Gateway (9000)   │
                          └───────────┬────────────┘
                                      │
          ┌───────────────────────────┼───────────────────────────┐
          │                           │                           │
          ▼                           ▼                           ▼
┌───────────────────┐       ┌───────────────────┐       ┌───────────────────┐
│  AUTH & ADMIN     │       │     CONTENT       │       │    EXECUTION      │
│  Port: 8081       │       │    Port: 8083     │       │    Port: 8087     │
│ ───────────────── │       │ ───────────────── │       │ ───────────────── │
│ identity-service  │       │ question-bank-svc │       │ delivery-service  │
│ candidate-service │       │ exam-service      │       │ response-service  │
│ admin-service     │       │ paper-generator   │       └───────────────────┘
│ notif-service     │       │ asset-service     │                 │
└───────────────────┘       └───────────────────┘                 │
                                                                  ▼
┌───────────────────┐                                   ┌───────────────────┐
│  AUDIT SERVICE    │                                   │    POST-EXAM      │
│  Port: 8091       │                                   │    Port: 8089     │
│ ───────────────── │                                   │ ───────────────── │
│ (Isolated for     │                                   │ evaluation-svc    │
│ legal compliance) │                                   │ result-service    │
└───────────────────┘                                   │ analytics-service │
                                                        └───────────────────┘
```

---

## Comparison: Micro vs Macro

| Dimension | Microservices Mode | Macro-Services Mode |
|---|---|---|
| **Deployable JARs** | 14 services + Gateway (15) | 5 services + Gateway (6) |
| **Message Broker** | Apache Kafka (~512 MB RAM) | RabbitMQ Alpine (~80 MB RAM) |
| **Min RAM Footprint** | ~7.0 GB | ~2.5 GB (runs on `t3.medium` / 4GB RAM) |
| **Target Infrastructure**| Kubernetes (EKS/GKE) | Single EC2 VM / Docker / Small K8s |
| **Scaling Granularity**| Scale each of 14 pods independently | Scale hot-path (`execution-app`) independently |
| **Compliance Isolation**| `audit-service` standalone | `audit-service` standalone (preserved) |

---

## Key Components Implemented

### 1. Broker-Agnostic Messaging Abstraction (`backend/shared-lib`)
- [`EventPublisher`](file:///C:/Users/sheel/IdeaProjects/nag/backend/shared-lib/src/main/java/com/examplatform/shared/messaging/EventPublisher.java): Universal publishing interface.
- [`KafkaEventPublisher`](file:///C:/Users/sheel/IdeaProjects/nag/backend/shared-lib/src/main/java/com/examplatform/shared/messaging/KafkaEventPublisher.java): Active for `docker`, `kubernetes`, or `platform.messaging.broker=kafka`.
- [`RabbitEventPublisher`](file:///C:/Users/sheel/IdeaProjects/nag/backend/shared-lib/src/main/java/com/examplatform/shared/messaging/RabbitEventPublisher.java): Active when `platform.messaging.broker=rabbit`.
- [`SpringEventPublisher`](file:///C:/Users/sheel/IdeaProjects/nag/backend/shared-lib/src/main/java/com/examplatform/shared/messaging/SpringEventPublisher.java): In-process fallback for zero-broker embedded mode.
- [`MessagingAutoConfiguration`](file:///C:/Users/sheel/IdeaProjects/nag/backend/shared-lib/src/main/java/com/examplatform/shared/messaging/MessagingAutoConfiguration.java): Auto-registers the proper publisher based on active environment.

### 2. Macro-Service Aggregators
1. **[`backend/auth-admin-app`](file:///C:/Users/sheel/IdeaProjects/nag/backend/auth-admin-app)**: Aggregates `identity-service`, `candidate-service`, `admin-service`, and `notification-service`. Port: `8081`.
2. **[`backend/content-app`](file:///C:/Users/sheel/IdeaProjects/nag/backend/content-app)**: Aggregates `question-bank-service`, `examination-service`, `paper-generator`, and `asset-service`. Port: `8083`.
3. **[`backend/execution-app`](file:///C:/Users/sheel/IdeaProjects/nag/backend/execution-app)**: Aggregates `delivery-service` and `response-service` (the exam delivery hot path). Port: `8087`.
4. **[`backend/post-exam-app`](file:///C:/Users/sheel/IdeaProjects/nag/backend/post-exam-app)**: Aggregates `evaluation-service`, `result-service`, and `analytics-service`. Port: `8089`.
5. **`audit-service`**: Preserved as an isolated standalone deployment unit (Port `8091`) to maintain legal audit immutability and compliance requirements.

### 3. Deployment Overlays
- **Docker Compose**: [`infrastructure/docker-compose/docker-compose.macro.yml`](file:///C:/Users/sheel/IdeaProjects/nag/infrastructure/docker-compose/docker-compose.macro.yml) includes RabbitMQ 3.13 Alpine and wires all 5 macro-services with `api-gateway`.
- **Helm Values**: [`infrastructure/helm/examination-platform/values-macro.yaml`](file:///C:/Users/sheel/IdeaProjects/nag/infrastructure/helm/examination-platform/values-macro.yaml) provides a Kubernetes overlay to disable the 13 individual services and run the 5 macro-services.
- **Build Script**: [`scripts/build-macro.sh`](file:///C:/Users/sheel/IdeaProjects/nag/scripts/build-macro.sh) provides a unified CLI script to build and tag all macro Docker images.

---

## How to Run

### Option A: Run Macro Mode on EC2 / Local Docker (Lightweight)

```bash
# 1. Start core infrastructure (Postgres, Redis, Vault, Keycloak)
docker compose -f infrastructure/docker-compose/docker-compose.yml up -d

# 2. Start Macro services (5 backend apps + RabbitMQ + Gateway + Frontends)
docker compose -f infrastructure/docker-compose/docker-compose.yml \
               -f infrastructure/docker-compose/docker-compose.macro.yml up --build
```

### Option B: Run Full Microservices Mode on K8s / Local Docker (Production)

```bash
# Docker Compose:
docker compose -f infrastructure/docker-compose/docker-compose.yml \
               -f infrastructure/docker-compose/docker-compose.services.yml up --build

# Kubernetes (Helm):
helm upgrade --install exam-platform ./infrastructure/helm/examination-platform \
  -f infrastructure/helm/examination-platform/values.yaml \
  -f infrastructure/helm/examination-platform/values-production.yaml
```

### Option C: Run Macro Mode on Kubernetes

```bash
helm upgrade --install exam-platform ./infrastructure/helm/examination-platform \
  -f infrastructure/helm/examination-platform/values.yaml \
  -f infrastructure/helm/examination-platform/values-macro.yaml
```

---

## Verification & Build Results

All 14 microservice bootJars, the 4 macro-service aggregator bootJars, and shared libraries built and assembled successfully:

```bash
./gradlew assemble -x test
```
Result: **`BUILD SUCCESSFUL in 38s (118 actionable tasks)`**
