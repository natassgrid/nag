# Dual-Mode Deployment: Microservices ↔ Macro-Services

## Goal

Same codebase — deploy in two modes **without touching any existing service code**:

| Mode | When to use | Containers | Broker | Min RAM |
|---|---|---|---|---|
| **Micro** (current) | K8s / production / large tenants | 14 services + gateway | **Kafka** | ~7 GB |
| **Macro** (new) | EC2 / demo / small tenants | 5 JARs + gateway | **RabbitMQ** | ~2.5 GB |

> [!IMPORTANT]
> The 5 JARs in macro mode are: `auth-admin`, `content`, `execution`, `post-exam`, plus `audit-service` stays **always isolated** (compliance).

---

## Proposed Macro-Service Groupings

```
auth-admin-app   ←  identity-service + candidate-service + admin-service + notification-service
content-app      ←  question-bank-service + examination-service + paper-generator + asset-service
execution-app    ←  delivery-service + response-service
post-exam-app    ←  evaluation-service + result-service + analytics-service
audit-service    ←  stays standalone (compliance — unchanged)
api-gateway      ←  stays standalone (unchanged)
```

---

## How It Works (No Service Code Changes)

Each aggregator module is a **thin Gradle module** that:
1. Has its own `@SpringBootApplication` entrypoint
2. Declares `implementation project(':backend:xxx-service')` dependencies
3. Merges all component schemas under one datasource URL
4. Uses `@ConditionalOnProperty` beans to share Flyway/JPA contexts cleanly

All existing `XxxServiceApplication.java` classes are **untouched**. The aggregator just imports their `@Configuration` and `@Component` beans via classpath scanning.

---

## Broker Strategy: Kafka (Micro) ↔ RabbitMQ (Macro)

### Why RabbitMQ for macro mode?

| | Kafka | RabbitMQ |
|---|---|---|
| RAM footprint | ~512 MB (JVM-based, needs KRaft) | ~80 MB (Erlang VM) |
| Docker image size | ~800 MB | ~180 MB |
| Startup time | 10–20s | 2–3s |
| Best for | High-throughput streams, replay | Task queues, fanout, RPC |
| Spring support | `spring-kafka` | `spring-rabbit` (AMQP) |

> [!TIP]
> RabbitMQ saves ~430 MB vs Kafka. Combined with merging 14 → 5 JARs, macro mode needs **~2.5 GB RAM** vs 7 GB — fits comfortably on a `t3.medium` (4 GB).

---

### Kafka Topics in Current Codebase (full map)

| Kafka Topic | Producer(s) | Consumer(s) | Cross-JAR in macro? |
|---|---|---|---|
| `exam.audit.events` | identity, candidate, delivery, evaluation, examination, asset, admin, paper-gen | **audit-service** | ✅ Yes — always cross-JAR |
| `exam.notifications.outbound` | identity, examination-schedule | notification-service | ❌ No — merged in `auth-admin-app` |
| `exam.session.events` | delivery | response-service, evaluation-service | ⚠️ Partial — delivery+response merged; evaluation is separate JAR |
| `exam.evaluation.events` | evaluation | result-service | ❌ No — merged in `post-exam-app` |
| `exam.paper.events` | examination | paper-generator | ❌ No — merged in `content-app` |
| `exam.proctoring.alerts` | delivery | delivery (self-consume) | ❌ No — both in `execution-app` |
| `exam.config.events` | admin | shared-lib `DynamicConfigInvalidationListener` | ⚠️ Fanout to all JARs |

### Messaging in Macro Mode: Two-Layer Approach

```
Within the same macro-JAR:
  publisher → Spring ApplicationEventPublisher → @EventListener (zero broker, zero network)

Across macro-JAR boundaries:
  publisher → RabbitMQ exchange → consumer (AMQP, lightweight)
```

#### Topics that become in-process Spring Events (intra-JAR)
| Topic (Kafka name) | Becomes | Where |
|---|---|---|
| `exam.notifications.outbound` | `@ApplicationEvent` | `auth-admin-app` |
| `exam.evaluation.events` | `@ApplicationEvent` | `post-exam-app` |
| `exam.paper.events` | `@ApplicationEvent` | `content-app` |
| `exam.proctoring.alerts` | `@ApplicationEvent` | `execution-app` |

#### Topics that move to RabbitMQ (cross-JAR)
| Topic → RabbitMQ Exchange | From JAR | To JAR |
|---|---|---|
| `exam.audit.events` → `exam.audit` exchange | all JARs | `audit-service` |
| `exam.session.events` → `exam.session` exchange | `execution-app` | `post-exam-app` |
| `exam.config.events` → `exam.config` fanout | `auth-admin-app` | all JARs |

---

### How to Implement the Broker Abstraction (Spring Cloud Stream)

**This is the key architectural decision.** We use **Spring Cloud Stream** as an abstraction layer over both Kafka and RabbitMQ. Services declare *channels* — the binder (Kafka or Rabbit) is injected by profile.

#### Phase 1 — Add `EventPublisher` abstraction to `shared-lib` (1 new class, no service changes yet)

```java
// shared-lib: com.examplatform.shared.messaging.EventPublisher
public interface EventPublisher {
    void publish(String topic, String key, Object payload);
}

// Kafka implementation — active on profiles: docker, kubernetes
@Component
@Profile({"docker", "kubernetes", "default"})
public class KafkaEventPublisher implements EventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    public void publish(String topic, String key, Object payload) {
        kafkaTemplate.send(topic, key, payload);
    }
}

// RabbitMQ implementation — active on profile: macro
@Component
@Profile("macro")
public class RabbitEventPublisher implements EventPublisher {
    private final RabbitTemplate rabbitTemplate;
    public void publish(String topic, String key, Object payload) {
        // topic name maps to RabbitMQ exchange name
        rabbitTemplate.convertAndSend(topic, key, payload);
    }
}
```

#### Phase 2 — Migrating services (done incrementally, one service at a time)

Replace `kafkaTemplate.send(topic, key, payload)` with `eventPublisher.publish(topic, key, payload)` in each service. This is a **mechanical find-and-replace** with no logic changes.

> [!NOTE]
> This is the **only change** required to existing service code — and it's purely an injection swap. All business logic, topic names, and payload shapes stay identical.

#### Phase 3 — Aggregator `@KafkaListener` → `@RabbitListener` bridge

In each aggregator module, add a `MacroBrokerConfig` that:
- Declares RabbitMQ exchanges + queues mirroring Kafka topic names
- Converts `@KafkaListener` consumers to `@RabbitListener` via profile

```java
// execution-app: MacroBrokerConfig.java
@Configuration
@Profile("macro")
public class MacroBrokerConfig {

    // Cross-JAR: exam.session.events → post-exam-app
    @Bean TopicExchange sessionExchange() {
        return new TopicExchange("exam.session.events");
    }

    // Intra-JAR: proctoring alerts stay in-process via ApplicationEvents
    // (ProctoringAnalysisConsumer repurposed as @EventListener in macro profile)
}
```

---

### RabbitMQ Container (Macro Mode)

#### [MODIFY] `docker-compose.macro.yml` — add RabbitMQ, remove Kafka dependency

```yaml
services:
  rabbitmq:
    image: rabbitmq:3.13-management-alpine   # 180MB, management UI included
    container_name: exam-rabbitmq
    restart: unless-stopped
    ports:
      - "5672:5672"     # AMQP
      - "15672:15672"   # Management UI (http://localhost:15672, guest/guest)
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_USER:-exam_rabbit}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASS:-rabbit_secret}
    healthcheck:
      test: rabbitmq-diagnostics check_port_connectivity
      interval: 10s
      timeout: 5s
      retries: 10
    networks:
      - exam-network

  # All macro-services get SPRING_RABBITMQ_* env vars instead of KAFKA
  auth-admin-service:
    environment:
      SPRING_PROFILES_ACTIVE: macro
      SPRING_RABBITMQ_HOST: rabbitmq
      SPRING_RABBITMQ_PORT: "5672"
      SPRING_RABBITMQ_USERNAME: ${RABBITMQ_USER:-exam_rabbit}
      SPRING_RABBITMQ_PASSWORD: ${RABBITMQ_PASS:-rabbit_secret}
    depends_on:
      rabbitmq:
        condition: service_healthy
    # ... (Kafka env vars omitted — not needed in macro profile)
```

> [!IMPORTANT]
> In macro mode, **Kafka container is NOT started** — removing ~512 MB RAM and the KRaft/Zookeeper overhead entirely.

---

### Dependency Changes

#### [MODIFY] `shared-lib/build.gradle`

Add `spring-rabbit` as optional (not forced on services):
```groovy
// Optional — only pulled in by macro aggregators
compileOnly libs.spring.boot.starter.amqp
```

#### [MODIFY] Each aggregator `build.gradle` (macro mode only)

```groovy
// auth-admin-app, content-app, execution-app, post-exam-app
dependencies {
    // ... existing project dependencies ...
    implementation libs.spring.boot.starter.amqp   // RabbitMQ for macro profile
    // spring-kafka stays on classpath via service deps — disabled by profile
}
```

---

## Updated Files Summary

| File | Status | Purpose |
|---|---|---|
| `backend/shared-lib/.../messaging/EventPublisher.java` | NEW | Broker-agnostic publish interface |
| `backend/shared-lib/.../messaging/KafkaEventPublisher.java` | NEW | Kafka impl (profiles: docker, kubernetes) |
| `backend/shared-lib/.../messaging/RabbitEventPublisher.java` | NEW | RabbitMQ impl (profile: macro) |
| `backend/auth-admin-app/build.gradle` | NEW | Aggregator + AMQP dep |
| `backend/auth-admin-app/.../AuthAdminApplication.java` | NEW | Spring Boot entrypoint |
| `backend/content-app/build.gradle` | NEW | Aggregator + AMQP dep |
| `backend/content-app/.../ContentApplication.java` | NEW | Spring Boot entrypoint |
| `backend/execution-app/build.gradle` | NEW | Aggregator + AMQP dep |
| `backend/execution-app/.../ExecutionApplication.java` | NEW | Spring Boot entrypoint |
| `backend/post-exam-app/build.gradle` | NEW | Aggregator + AMQP dep |
| `backend/post-exam-app/.../PostExamApplication.java` | NEW | Spring Boot entrypoint |
| `backend/*/service/*.java` | MODIFY (mechanical) | `kafkaTemplate.send` → `eventPublisher.publish` |
| `settings.gradle` | MODIFY | Add 4 new module includes |
| `backend/Dockerfile.macro` | NEW | Builds aggregator JARs |
| `infrastructure/docker-compose/docker-compose.macro.yml` | NEW | 5-service + RabbitMQ overlay |
| `infrastructure/helm/examination-platform/values-macro.yaml` | NEW | Helm macro overlay |
| `scripts/build-macro.sh` | NEW | Convenience build script |



### New Gradle Modules (4 aggregators)

---

#### [NEW] `backend/auth-admin-app/build.gradle`
```groovy
plugins {
    id 'java'
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.lombok)
}
version = '0.0.1-SNAPSHOT'

dependencyManagement {
    imports {
        mavenBom "org.springframework.cloud:spring-cloud-dependencies:${libs.versions.spring.cloud.get()}"
    }
}

dependencies {
    implementation project(':backend:shared-lib')
    implementation project(':backend:identity-service')
    implementation project(':backend:candidate-service')
    implementation project(':backend:admin-service')
    implementation project(':backend:notification-service')
}

springBoot {
    mainClass = 'com.examplatform.app.AuthAdminApplication'
}
```

#### [NEW] `backend/content-app/build.gradle`
```groovy
dependencies {
    implementation project(':backend:shared-lib')
    implementation project(':backend:question-bank-service')
    implementation project(':backend:examination-service')
    implementation project(':backend:paper-generator')
    implementation project(':backend:asset-service')
}
springBoot { mainClass = 'com.examplatform.app.ContentApplication' }
```

#### [NEW] `backend/execution-app/build.gradle`
```groovy
dependencies {
    implementation project(':backend:shared-lib')
    implementation project(':backend:delivery-service')
    implementation project(':backend:response-service')
}
springBoot { mainClass = 'com.examplatform.app.ExecutionApplication' }
```

#### [NEW] `backend/post-exam-app/build.gradle`
```groovy
dependencies {
    implementation project(':backend:shared-lib')
    implementation project(':backend:evaluation-service')
    implementation project(':backend:result-service')
    implementation project(':backend:analytics-service')
}
springBoot { mainClass = 'com.examplatform.app.PostExamApplication' }
```

---

#### [NEW] Aggregator `@SpringBootApplication` entry points (Java)

Each aggregator needs one tiny entrypoint class under `src/main/java/com/examplatform/app/`:

```java
// AuthAdminApplication.java
@SpringBootApplication(
    scanBasePackages = {
        "com.examplatform.identity",
        "com.examplatform.candidate",
        "com.examplatform.admin",
        "com.examplatform.notification",
        "com.examplatform.shared"
    }
)
public class AuthAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthAdminApplication.class, args);
    }
}
```

> [!NOTE]
> The key is `scanBasePackages` — it pulls in all beans from the component JARs on the classpath. No changes to existing service classes needed.

---

#### [MODIFY] [`settings.gradle`](file:///C:/Users/sheel/IdeaProjects/nag/settings.gradle)

Add 4 new aggregator modules at the end:
```groovy
// ── Macro-service aggregators (demo / EC2 / small-tenant mode) ──────────────
include 'backend:auth-admin-app'
include 'backend:content-app'
include 'backend:execution-app'
include 'backend:post-exam-app'
```

---

### Docker — Macro Mode

#### [NEW] `backend/Dockerfile.macro`

Variant of the existing Dockerfile that accepts `APP_NAME` instead of `SERVICE_NAME`:
```dockerfile
FROM exam/builder-base:latest AS builder
ARG APP_NAME
WORKDIR /workspace
COPY gradlew ./
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./
COPY backend/gradle/gradle-8.14.5-bin.zip /tmp/gradle-8.14.5-bin.zip
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && \
    sed -i 's|distributionUrl=.*|distributionUrl=file\:/tmp/gradle-8.14.5-bin.zip|' \
    gradle/wrapper/gradle-wrapper.properties
COPY backend/ backend/
RUN --mount=type=cache,target=/root/.gradle/caches \
    --mount=type=cache,target=/root/.gradle/wrapper \
    ./gradlew :backend:${APP_NAME}:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
ARG APP_NAME
ENV APP_NAME=${APP_NAME}
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY --from=builder /workspace/backend/${APP_NAME}/build/libs/*.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

#### [NEW] `infrastructure/docker-compose/docker-compose.macro.yml`

A **drop-in overlay** replacing the 14-service file. Used as:
```bash
docker compose -f docker-compose.yml -f docker-compose.macro.yml up --build
```

Defines 5 services:
- `auth-admin-service` (port 8081) — merges identity/candidate/admin/notification
- `content-service` (port 8083) — merges question-bank/examination/paper-gen/asset
- `execution-service` (port 8087) — merges delivery/response
- `post-exam-service` (port 8089) — merges evaluation/result/analytics
- `audit-service` (port 8091) — unchanged, standalone
- `api-gateway` (port 9000) — unchanged, routes updated to 5 upstreams

Each service uses `DATABASE_SCHEMAS: schema1,schema2` env var, and a single `SPRING_DATASOURCE_URL`.

---

### Helm — Macro Values Overlay

#### [NEW] `infrastructure/helm/examination-platform/values-macro.yaml`

```yaml
# Macro-service mode: 5 deployments instead of 14
# Usage: helm upgrade exam-platform . -f values.yaml -f values-macro.yaml

# Disable all individual services
identityService:    { enabled: false }
candidateService:   { enabled: false }
questionBankService:{ enabled: false }
examinationService: { enabled: false }
paperGenerator:     { enabled: false }
deliveryService:    { enabled: false }
responseService:    { enabled: false }
evaluationService:  { enabled: false }
resultService:      { enabled: false }
notificationService:{ enabled: false }
adminService:       { enabled: false }
analyticsService:   { enabled: false }
assetService:       { enabled: false }

# Enable macro-services
authAdminService:
  enabled: true
  replicas: 2
  image: { name: auth-admin-app }
  port: 8081

contentService:
  enabled: true
  replicas: 2
  image: { name: content-app }
  port: 8083

executionService:
  enabled: true
  replicas: 3
  image: { name: execution-app }
  port: 8087
  hpa: { minReplicas: 3, maxReplicas: 15 }

postExamService:
  enabled: true
  replicas: 2
  image: { name: post-exam-app }
  port: 8089

# auditService stays enabled (compliance isolation — never merged)
```

---

### Build Script

#### [NEW] `scripts/build-macro.sh`

```bash
#!/bin/bash
# Build all macro-service Docker images
set -e
APPS=("auth-admin-app" "content-app" "execution-app" "post-exam-app")
for APP in "${APPS[@]}"; do
  echo "Building $APP..."
  docker build \
    --build-arg APP_NAME=$APP \
    -f backend/Dockerfile.macro \
    -t localhost:5000/exam/$APP:latest \
    .
  docker push localhost:5000/exam/$APP:latest
done
echo "Done. audit-service and api-gateway use existing images."
```

---

## Deployment Commands

### EC2 / Demo (Macro Mode)
```bash
# Infrastructure (Postgres, Kafka, Redis, Vault, Keycloak)
docker compose -f docker-compose.yml up -d

# 5 macro-services instead of 14
docker compose -f docker-compose.yml -f docker-compose.macro.yml up --build
```

### K8s / Production (Micro Mode — unchanged)
```bash
helm upgrade --install exam-platform ./infrastructure/helm/examination-platform \
  -f values.yaml \
  -f values-production.yaml
```

### K8s / Demo on K8s (Macro Mode)
```bash
helm upgrade --install exam-platform ./infrastructure/helm/examination-platform \
  -f values.yaml \
  -f values-macro.yaml
```

---

## Files Summary

| File | Status | Purpose |
|---|---|---|
| `backend/auth-admin-app/build.gradle` | NEW | Aggregator: identity+candidate+admin+notification |
| `backend/auth-admin-app/src/main/java/.../AuthAdminApplication.java` | NEW | Spring Boot entrypoint |
| `backend/content-app/build.gradle` | NEW | Aggregator: question-bank+examination+paper-gen+asset |
| `backend/content-app/src/main/java/.../ContentApplication.java` | NEW | Spring Boot entrypoint |
| `backend/execution-app/build.gradle` | NEW | Aggregator: delivery+response |
| `backend/execution-app/src/main/java/.../ExecutionApplication.java` | NEW | Spring Boot entrypoint |
| `backend/post-exam-app/build.gradle` | NEW | Aggregator: evaluation+result+analytics |
| `backend/post-exam-app/src/main/java/.../PostExamApplication.java` | NEW | Spring Boot entrypoint |
| `settings.gradle` | MODIFY | Add 4 new module includes |
| `backend/Dockerfile.macro` | NEW | Builds aggregator JARs |
| `infrastructure/docker-compose/docker-compose.macro.yml` | NEW | 5-service compose overlay |
| `infrastructure/helm/examination-platform/values-macro.yaml` | NEW | Helm overlay for macro mode |
| `scripts/build-macro.sh` | NEW | Convenience build script |

> [!NOTE]
> **Zero changes** to any existing service code, `build.gradle`, `Dockerfile`, or `docker-compose.services.yml`. The micro mode continues to work exactly as before.

---

## Open Risk: Bean Conflicts in Aggregators

> [!WARNING]
> When merging multiple Spring Boot apps into one JVM, **bean name collisions** are the #1 risk.
>
> **Mitigations:**
> - Each service already uses its own package (`com.examplatform.identity`, etc.) — `@Component` names are scoped by class name, reducing collision risk.
> - Flyway migrations: each service has its own schema — use `spring.flyway.schemas` to scope per datasource.
> - Multiple `DataSource` beans: use `@Primary` on one; others qualify via `@Qualifier`.
> - We'll verify this empirically during the execution phase with a test build.

---

## Verification Plan

### Automated
```bash
# Build all 4 aggregators
./gradlew :backend:auth-admin-app:bootJar \
          :backend:content-app:bootJar \
          :backend:execution-app:bootJar \
          :backend:post-exam-app:bootJar
```

### Integration (Docker)
```bash
docker compose -f docker-compose.yml -f docker-compose.macro.yml up
# Check each actuator/health endpoint
curl http://localhost:8081/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8087/actuator/health
curl http://localhost:8089/actuator/health
curl http://localhost:8091/actuator/health  # audit — standalone
```

### Regression (Micro Mode)
```bash
# Ensure existing mode still works unchanged
docker compose -f docker-compose.yml -f docker-compose.services.yml up
```
