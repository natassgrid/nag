# Walkthrough: Tri-Mode Architecture (Microservices ↔ Macro-Services ↔ Monolith)

NAG implements a **Tri-Mode Architecture** for the Open Digital Public Infrastructure (DPI) Platform. From the **exact same codebase**, you can compile and deploy in any of three topologies:

1. **Full Microservices Mode**: 14 distinct microservices + API Gateway with Apache Kafka (for Kubernetes, large-scale multi-tenant production).
2. **Consolidated Macro-Services Mode**: 5 deployable units + API Gateway with lightweight RabbitMQ (for single EC2 instances, demo environments, and state boards).
3. **Single JVM Monolith Mode**: 1 consolidated Spring Boot application (`monolith-app`) embedding all 14 modules with zero external broker (for local development, CI/CD, and low-resource environments).

---

## Topology Architecture Diagram

```
                                  ┌──────────────────────────────┐
                                  │     Angular Frontends        │
                                  │   Admin (4200) / User (4300) │
                                  └──────────────┬───────────────┘
                                                 │
                  ┌──────────────────────────────┼──────────────────────────────┐
                  ▼                              ▼                              ▼
     ┌──────────────────────────┐   ┌──────────────────────────┐   ┌──────────────────────────┐
     │ 1. MICROSERVICES (K8s)   │   │ 2. MACRO-SERVICES (VM)   │   │ 3. MONOLITH (Single JVM) │
     ├──────────────────────────┤   ├──────────────────────────┤   ├──────────────────────────┤
     │   API Gateway (9000)     │   │   API Gateway (9000)     │   │   monolith-app (9000)    │
     │            │             │   │            │             │   │  (All 14 Modules inside) │
     │  ┌─────────┼─────────┐   │   │  ┌─────────┼─────────┐   │   │                          │
     │  ▼         ▼         ▼   │   │  ▼         ▼         ▼   │   │ • In-Memory Events       │
     │ 14 Standalone Pods       │   │ 5 Consolidated Apps:     │   │ • Multi-Schema Flyway    │
     │ (8081 .. 8095)           │   │ • auth-admin-app (8081)  │   │ • Single DB Pool         │
     │            │             │   │ • content-app (8083)     │   │ • Zero External Broker   │
     │            ▼             │   │ • execution-app (8087)   │   │                          │
     │   Apache Kafka (9092)    │   │ • post-exam-app (8089)   │   │                          │
     │   PostgreSQL (Multi-DB/S)│   │ • audit-service (8091)   │   │                          │
     │   Redis (Cluster)        │   │            │             │   │                          │
     │                          │   │            ▼             │   │                          │
     │                          │   │   RabbitMQ Alpine (5672) │   │                          │
     │                          │   │   PostgreSQL + Redis     │   │                          │
     └──────────────────────────┘   └──────────────────────────┘   └──────────────────────────┘
```

---

## Comparison Matrix

| Dimension | Microservices Mode | Macro-Services Mode | Monolith Mode |
|---|---|---|---|
| **Deployable JARs** | 14 services + Gateway (15) | 5 services + Gateway (6) | 1 single `monolith-app` |
| **Message Broker** | Apache Kafka (~512 MB RAM) | RabbitMQ Alpine (~80 MB RAM) | In-memory Spring Events (0 MB broker) |
| **Min RAM Footprint** | ~7.0 – 12.0 GB | ~2.5 – 4.0 GB | ~1.0 – 1.5 GB |
| **Target Infrastructure**| Kubernetes (EKS/GKE/OCP) | Single EC2 VM / Docker Compose | Laptop / Local Dev / CI / Low-cost VM |
| **Scaling Granularity**| Scale each of 14 pods independently | Scale hot-path (`execution-app`) independently | Scale the entire monolith instance |
| **Audit Compliance**| `audit-service` standalone | `audit-service` standalone (preserved) | Embedded in monolith runtime |

---

## Core Components

### 1. Broker-Agnostic Messaging Abstraction (`backend/shared-lib`)
- [`EventPublisher`](file:///C:/Users/sheel/IdeaProjects/nag/backend/shared-lib/src/main/java/com/examplatform/shared/messaging/EventPublisher.java): Universal publishing interface implemented across all modes.
- [`KafkaEventPublisher`](file:///C:/Users/sheel/IdeaProjects/nag/backend/shared-lib/src/main/java/com/examplatform/shared/messaging/KafkaEventPublisher.java): Active when `platform.messaging.broker=kafka` (Microservices mode).
- [`RabbitEventPublisher`](file:///C:/Users/sheel/IdeaProjects/nag/backend/shared-lib/src/main/java/com/examplatform/shared/messaging/RabbitEventPublisher.java): Active when `platform.messaging.broker=rabbit` (Macro mode).
- [`SpringEventPublisher`](file:///C:/Users/sheel/IdeaProjects/nag/backend/shared-lib/src/main/java/com/examplatform/shared/messaging/SpringEventPublisher.java): In-process asynchronous event bus active when `platform.messaging.broker=spring` or `in-memory` (Monolith mode).
- [`MessagingAutoConfiguration`](file:///C:/Users/sheel/IdeaProjects/nag/backend/shared-lib/src/main/java/com/examplatform/shared/messaging/MessagingAutoConfiguration.java): Automatically configures the appropriate publisher bean.

### 2. Multi-Schema Database Migration (`backend/monolith-app`)
- [`MonolithFlywayConfig`](file:///C:/Users/sheel/IdeaProjects/nag/backend/monolith-app/src/main/java/com/examplatform/app/config/MonolithFlywayConfig.java): Executes Flyway migrations sequentially across all 14 schema paths (`classpath:db/migration/identity`, `classpath:db/migration/question`, etc.) at startup.

### 3. Unified Async Thread Pool (`backend/monolith-app`)
- [`MonolithAsyncConfig`](file:///C:/Users/sheel/IdeaProjects/nag/backend/monolith-app/src/main/java/com/examplatform/app/config/MonolithAsyncConfig.java): Provides a unified thread pool for non-blocking in-memory event dispatch and asynchronous task processing.

---

## How to Deploy & Run

### 1. Monolith Mode (Ultralight / Dev / PoC)
```bash
./infrastructure/docker-compose/redeploy-monolith.sh
```

### 2. Macro-Services Mode (EC2 / VM)
```bash
./infrastructure/docker-compose/redeploy-macro.sh
```

### 3. Microservices Mode (Kubernetes / Large Production)
```bash
./infrastructure/docker-compose/redeploy-micro.sh
```
