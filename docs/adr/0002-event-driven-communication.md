# 0002: Event-Driven Communication for Exam Workflows

- **Status**: Accepted
- **Date**: 2026-08-02
- **Deciders**: Core Architecture Team

---

## 1. Context

In a high-throughput examination platform, synchronous REST calls between microservices introduce tight coupling, cascading latency, and risk of request timeouts during peak exam start/submission windows.

Key requirements:
- Asynchronous processing of candidate response uploads without blocking user HTTP response times.
- Real-time audit event fan-out across multiple consumers (SIEM, audit log storage, analytics).
- Reliable state transitions across workflow boundaries (e.g., Exam Schedule Published $\rightarrow$ Pre-cache paper packages at test centres).

---

## 2. Decision

We decide to adopt an **Event-Driven Architecture (EDA)** for inter-service notifications and background state transitions:

1. **Synchronous REST / HTTP**: Used strictly for user-facing API interactions (Angular UI $\leftrightarrow$ Gateway $\leftrightarrow$ Service).
2. **Asynchronous Messaging**: Microservices publish domain events (e.g., `SchedulePublishedEvent`, `AnswerSubmittedEvent`, `PaperGeneratedEvent`) to an enterprise event bus.
3. **Outbox Pattern**: Transactional DB writes and message publishing use the Transactional Outbox pattern to guarantee at-least-once delivery without distributed 2PC transactions.

---

## 3. Consequences

### Positive
- **High Throughput & Resiliency**: Candidate answer submissions are acknowledged immediately while background processes process and persist responses asynchronously.
- **Decoupled Producer/Consumer**: New downstream services (e.g., real-time monitoring dashboard) can subscribe to events without modifying producing services.
- **Backpressure Handling**: Event queues buffer traffic spikes during nationwide exam completion windows.

### Negative
- **Eventual Consistency**: UI and background reporting must handle transient non-consistent states gracefully.
- **Idempotency Requirement**: Consumers must implement idempotent processing to handle duplicate message redelivery.
