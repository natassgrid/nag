# 0001: Microservices Architecture for Government Examination Platform

- **Status**: Accepted
- **Date**: 2026-08-02
- **Deciders**: Core Architecture Team, Lead Engineers

---

## 1. Context

The National Assessment Grid (NAG) is designed to support national-level competitive examinations across millions of concurrent candidates, multi-tenant state educational boards, and independent modules (Question Authoring, Paper Generation, Exam Scheduling, Delivery, Evaluation, Audit).

A monolithic architecture presents several operational challenges for this domain:
- **Scaling Bottlenecks**: Exam Delivery requires massive horizontal throughput during peak shift start windows, whereas Question Authoring and Evaluation experience steady, low-concurrency usage.
- **Organizational Coupling**: Independent teams (Content Authors, Exam Controllers, Security Administrators) need to deploy changes without redeploying the entire application stack.
- **Fault Domain Isolation**: A failure in the evaluation module must not impact active live exam delivery sessions.

---

## 2. Decision

We will adopt a **Microservices Architecture** built using Spring Boot services for the backend, separated along domain-driven design (DDD) bounded contexts:

1. **Question Bank Service**: Authoring, reviewing, subject tagging, item pooling.
2. **Paper Generation Service**: Rule-based template rendering, cryptographic paper packaging.
3. **Scheduling & Centre Service**: Schedule versioning, shift timing, centre capacity & seat allocation.
4. **Exam Delivery Service**: Highly available session management, candidate response ingestion.
5. **Evaluation Service**: Subjective/objective grading, score aggregation, anonymized evaluation.
6. **Audit & Compliance Service**: Immutable audit log processing and reporting.

---

## 3. Consequences

### Positive
- **Targeted Elastic Scaling**: Exam Delivery microservices can be horizontally auto-scaled to hundreds of pods during exam windows independently of authoring services.
- **Fault Isolation**: Outages in reporting or evaluation services do not interrupt active candidate exam delivery.
- **Independent Release Cycles**: Modules can be updated, patched, and deployed independently.

### Negative
- **Operational Complexity**: Requires container orchestration (Kubernetes), distributed tracing, and centralized log aggregation.
- **Distributed Consistency**: Requires eventual consistency patterns and saga orchestration for cross-service workflows.
