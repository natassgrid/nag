# Architecture Overview — National Assessment Grid (NAG)

## 1. System Vision & Purpose

The **National Assessment Grid (NAG)** is an open-source, enterprise-grade Open Digital Public Infrastructure (DPI) Platform designed to manage end-to-end competitive examinations, question bank authoring, automated cryptographic paper generation, nationwide exam delivery, evaluation, and analytics.

### Core Objectives
- **Security & Leak Prevention**: Zero-trust cryptography, time-locked paper decryption keys (Shamir Secret Sharing), and watermarked delivery.
- **High Concurrency**: Scale horizontally to support over 1,000,000 concurrent candidate test sessions.
- **Multi-Tenancy**: Logical data and authentication isolation for multiple state educational boards, universities, and recruitment agencies.
- **Auditable Integrity**: Immutable, append-only hash-chained audit trail logging for all administrative actions and workflow state changes.
- **Deployment Flexibility**: Support deployments ranging from lightweight single-node VMs to multi-region cloud Kubernetes clusters.

---

## 2. Tri-Mode Architectural Topology

NAG employs a **Tri-Mode Architecture** designed around modular domain-driven design (DDD) bounded contexts. From the **same codebase**, the platform compiles and deploys into three operational topologies:

```
+---------------------------------------------------------------------------------------+
|                                    NAG CORE ENGINE                                    |
|                      (14 Bounded Contexts + Shared Infrastructure)                    |
+---------------------------------------------------------------------------------------+
                                           │
         ┌─────────────────────────────────┼─────────────────────────────────┐
         ▼                                 ▼                                 ▼
┌───────────────────┐             ┌───────────────────┐             ┌───────────────────┐
│ 1. MICROSERVICES  │             │ 2. MACRO-SERVICES │             │   3. MONOLITH     │
├───────────────────┤             ├───────────────────┤             ├───────────────────┤
│ • 14 Standalone   │             │ • 5 Aggregated    │             │ • 1 Unified JVM   │
│   Spring Boot JARs│             │   Deployable JARs │             │   Spring Boot JAR │
│ • Apache Kafka    │             │ • RabbitMQ / Kafka│             │ • In-Memory Bus   │
│ • API Gateway     │             │ • API Gateway     │             │ • Zero External MB│
│ • Kubernetes HPA  │             │ • Single/Dual VM  │             │ • Single DB Pool  │
│ • Enterprise Multi│             │ • Medium scale /  │             │ • Dev / PoC / Low │
│   Node Production │             │   State Boards    │             │   Resource Deploy │
└───────────────────┘             └───────────────────┘             └───────────────────┘
```

### Topology Comparison Matrix

| Dimension | 1. Microservices Mode | 2. Macro-Services Mode | 3. Monolith Mode |
|---|---|---|---|
| **Deployable JARs** | 14 services + API Gateway | 5 services + API Gateway | 1 single `monolith-app` |
| **Event Bus** | Apache Kafka | RabbitMQ / Kafka | In-memory Spring Events (Zero Broker) |
| **Database Schemas** | 14 isolated PostgreSQL schemas | Shared PostgreSQL with domain schemas | Single connection pool, multi-schema Flyway |
| **Min RAM Footprint** | ~7.0 GB – 12.0 GB | ~2.5 GB – 4.0 GB | ~1.0 GB – 1.5 GB |
| **Target Environment** | Multi-AZ Kubernetes (EKS/GKE) | Single EC2 VM (`t3.medium`/`large`) | Local Dev, Laptop, Single VM, CI/CD |
| **Scaling Mechanism** | Independent pod autoscaling (HPA) | Hot-path scaling (`execution-app`) | Vertical or full-instance scaling |
| **Audit Isolation** | Standalone microservice | Standalone microservice (preserved)| Embedded domain module |

---

## 3. Modular Bounded Contexts

Each domain capability is strictly encapsulated into an isolated module:

1. **`identity-service`**: Keycloak OIDC/OAuth2 integration, MFA, WebAuthn, role-based access control (RBAC).
2. **`candidate-service`**: Candidate registration, profile verification, DigiLocker integration, biometric metadata.
3. **`admin-service`**: Tenant lifecycle, system configuration, audit logs, feature flags.
4. **`notification-service`**: Multi-channel delivery (Email, SMS, Webhook, Server-Sent Events).
5. **`question-bank-service`**: Multilingual authoring, LaTeX/MathML support, review workflows, item taxonomy.
6. **`examination-service`**: Examination lifecycle, shifts, schedule versions, exam center & seat allocation.
7. **`paper-generator`**: Blueprint balancing, question randomization, multi-set AES-256 envelope encryption.
8. **`asset-service`**: Multimedia asset storage, sanitization, virus scanning, and pre-signed access.
9. **`delivery-service`**: High-concurrency candidate test runtime, session timer, offline sync validation.
10. **`response-service`**: Real-time response heartbeat ingestion, answer bundling, encryption verification.
11. **`evaluation-service`**: Automated objective grading, anonymized rubric-based subjective scoring.
12. **`result-service`**: Mark normalization (percentile/z-score), merit list & cryptographically verifiable scorecard generation.
13. **`analytics-service`**: Live operational telemetry, item discrimination index, psychometric analysis.
14. **`audit-service`**: Immutable, append-only hash-chained audit logging for regulatory compliance.

---

## 4. Pluggable Infrastructure & Messaging

The platform utilizes a unified messaging interface (`EventPublisher`) with automatic environment-aware binding:

- **`KafkaEventPublisher`**: Active in microservices mode (`platform.messaging.broker=kafka`).
- **`RabbitEventPublisher`**: Active in macro-services mode (`platform.messaging.broker=rabbit`).
- **`SpringEventPublisher`**: Active in monolith mode (`platform.messaging.broker=in-memory` or `spring`), allowing zero-broker in-process async event dispatch.

---

## 5. Technology Stack Summary

| Architecture Layer | Core Technology | Selection Rationale |
|---|---|---|
| **Frontend Framework** | Angular 21 (Standalone) | Modern component architecture, RxJS state management |
| **UI Component Library** | Angular Material 21 | Accessible (WCAG 2.1 AA / GIGW), standardized UI components |
| **Backend Framework** | Java 21 / Spring Boot 3.x | Enterprise reliability, Spring Security, robust ecosystem |
| **Identity Provider** | Keycloak | OIDC/OAuth2 compliance, multi-realm tenancy, FIDO2/MFA |
| **Primary Datastore** | PostgreSQL 16 | ACID compliance, Row-Level Security (RLS), multi-schema isolation |
| **Event Bus & Messaging** | Kafka / RabbitMQ / Spring Events | Pluggable messaging adapting from multi-broker to zero-broker |
| **Distributed Cache** | Redis 7 | Session state caching, API rate limiting counters |
| **Key Management** | AWS KMS / HashiCorp Vault | FIPS 140-3 cryptographic key protection |
| **AI Subsystem** | Spring AI (Ollama, LiteLLM, IndicTrans2) | Local & sovereign AI paper generation, proctoring, translation |
