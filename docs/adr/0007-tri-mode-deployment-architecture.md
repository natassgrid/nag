# 0007: Tri-Mode Deployment Architecture (Microservices, Macro-Services, Monolith)

- **Status**: Accepted
- **Date**: 2026-09-06
- **Deciders**: Core Architecture Team, Infrastructure Team

---

## 1. Context

The National Assessment Grid (NAG) is designed to serve a diverse spectrum of adopting organizations:
1. **National Testing Agencies / Large Ministries**: Require massive horizontal elasticity (>1,000,000 concurrent candidates), independent team deployments, and multi-region Kubernetes clusters.
2. **State Examination Boards / Universities**: Require predictable operational costs and simple operations on single/dual virtual machines (e.g., AWS EC2 `t3.medium`/`t3.large`) with low maintenance overhead.
3. **Developers & Small Evaluators**: Require immediate local startup with minimal memory footprint (~1 GB RAM) without having to launch heavy external message brokers (Kafka) or 15 separate JVM processes.

Maintaining multiple codebase forks for these varying requirements would introduce severe technical debt and synchronization overhead.

---

## 2. Decision

We will implement a **Tri-Mode Architecture** supported by a single, unified codebase:

1. **Microservices Mode**:
   - 14 independent domain microservices + Spring Cloud Gateway.
   - Apache Kafka as the distributed event streaming backbone.
   - Independent pod autoscaling and deployment lifecycle per domain context.

2. **Macro-Services Mode**:
   - 5 consolidated deployable units (`auth-admin-app`, `content-app`, `execution-app`, `post-exam-app`, and standalone `audit-service`) + Gateway.
   - RabbitMQ Alpine (or Kafka) for lightweight messaging (~80 MB RAM footprint).
   - Allows targeted scaling of the high-throughput test delivery path (`execution-app`).

3. **Single JVM Monolith Mode**:
   - 1 unified Spring Boot application (`monolith-app`) aggregating all 14 domain modules into a single JVM process.
   - In-memory event bus (`SpringEventPublisher`) utilizing Spring's internal async event framework, eliminating the need for an external message broker.
   - Automated multi-schema migration via `MonolithFlywayConfig` and unified thread execution via `MonolithAsyncConfig`.

---

## 3. Implementation Mechanism

- **Broker-Agnostic Messaging**: Defined `EventPublisher` interface with `KafkaEventPublisher`, `RabbitEventPublisher`, and `SpringEventPublisher` adapters conditionally loaded via `platform.messaging.broker` property.
- **Shared Security & Beans**: Guarded security filter chains and configuration beans with `@ConditionalOnMissingBean` to prevent filter chain collisions when modules run in aggregate or monolithic modes.
- **Docker & Deployment Orchestration**: Composable Docker Compose overlays (`docker-compose.services.yml`, `docker-compose.macro.yml`, `docker-compose.monolith.yml`) and specialized redeploy scripts (`redeploy-micro.sh`, `redeploy-macro.sh`, `redeploy-monolith.sh`).

---

## 4. Consequences

### Positive
- **Zero Forking**: Single codebase and single test suite powering all three deployment modes.
- **Developer Productivity**: Sub-second startup and minimal resource consumption during local feature development via Monolith mode.
- **Cost Efficiency for Adopters**: Smaller institutions can host NAG on low-spec VMs without Kubernetes or Kafka overhead.
- **Sovereign & Large Scale Ready**: National agencies can deploy full microservices mode on Kubernetes with full elasticity.

### Considerations
- Module boundaries and package structures must be strictly maintained to avoid circular dependencies in aggregate/monolith modes.
- Multi-schema database migrations must be carefully coordinated across Flyway paths.
