# Architecture Overview — National Assessment Grid (NAG)

## 1. System Vision & Purpose

The **National Assessment Grid (NAG)** is an open-source, enterprise-grade Open Digital Public Infrastructure (DPI) Platform designed to manage end-to-end competitive examinations, question bank authoring, automated cryptographic paper generation, nationwide exam delivery, and evaluation.

### Core Objectives
- **Security & Leak Prevention**: Zero-trust cryptography, time-locked paper decryption keys (Shamir Secret Sharing), and watermarked delivery.
- **High Concurrency**: Scale horizontally to support over 1,000,000 concurrent candidate test sessions.
- **Multi-Tenancy**: Logical data and authentication isolation for multiple state educational boards and recruitment agencies.
- **Auditable Integrity**: Immutable, append-only audit trail logging for all administrative actions and workflow state changes.

---

## 2. Architectural Paradigm

NAG is designed following modern software architecture patterns:
1. **Microservices Architecture**: Domain-driven bounded contexts running independently in containerized pods.
2. **Event-Driven Architecture (EDA)**: Asynchronous event messaging via Apache Kafka for audit streams and workflow decoupled notifications.
3. **Decoupled Angular SPA Frontend**: Angular 21 standalone component architecture consuming backend REST APIs.
4. **Stateless Security**: OIDC / OAuth2 JSON Web Tokens (JWT) issued by Keycloak and validated statelessly across all microservices.

---

## 3. Technology Stack Summary

| Architecture Layer | Core Technology | Selection Rationale |
|---|---|---|
| **Frontend Framework** | Angular 21 (Standalone) | Modern component architecture, RxJS state management |
| **UI Component Library** | Angular Material 21 | Accessible (WCAG/GIGW), standardized UI components |
| **Backend Framework** | Java / Spring Boot 3.x | Enterprise reliability, Spring Security, robust ecosystem |
| **Identity Provider** | Keycloak | OIDC/OAuth2 compliance, multi-realm tenancy, FIDO2/MFA |
| **Primary Datastore** | PostgreSQL | ACID compliance, Row-Level Security (RLS), JSONB support |
| **Event Bus & Messaging** | Apache Kafka | High-throughput, distributed event replayability |
| **Distributed Cache** | Redis | Session state caching, API rate limiting counters |
| **Key Management** | AWS KMS / Vault / HSM | FIPS 140-3 cryptographic key protection |
