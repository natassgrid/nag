# Deployment Architecture — National Assessment Grid

## 1. Overview

The National Assessment Grid (NAG) is designed for versatile operational deployment across multiple environments. The platform provides pre-packaged configurations for three distinct deployment topologies:

1. **Microservices Deployment** (Production Kubernetes / Cloud Multi-AZ / High Scale)
2. **Macro-Services Deployment** (Consolidated Containers / EC2 Single or Dual VM / Medium Scale)
3. **Single JVM Monolith Deployment** (Standalone Single JVM / Ultralight / Development & Testing)

---

## 2. Deployment Topologies

### Topology 1: Full Microservices Deployment (Kubernetes)

Ideal for national testing agencies, multi-tenant state boards, and peak examination sessions handling 100,000 to 1,000,000+ concurrent candidates.

```mermaid
graph TB
    classDef edge fill:#1F77B4,color:#fff,stroke:#0E4D7B;
    classDef k8s fill:#2CA02C,color:#fff,stroke:#1A601A;
    classDef db fill:#FF7F0E,color:#fff,stroke:#A64B00;

    Internet(("Public Internet / Center Network"))

    subgraph EdgeLayer ["Edge Security Layer"]
        CloudWAF["Cloud WAF / DDoS Mitigation"]:::edge
        LoadBalancer["External Load Balancer (ALB / NGINX)"]:::edge
    end

    subgraph K8sCluster ["Kubernetes Cluster (Production)"]
        Ingress["NGINX Ingress / Spring Cloud Gateway"]:::k8s

        subgraph CoreMicroservices ["14 Domain Microservices"]
            IdentityPod["Identity Svc Pods (x3)"]:::k8s
            CandidatePod["Candidate Svc Pods (x3)"]:::k8s
            DeliveryPods["Delivery Svc Pods (x20+ Auto-scaled)"]:::k8s
            ResponsePods["Response Svc Pods (x20+ Auto-scaled)"]:::k8s
            QuestionPod["Question Bank Pods (x3)"]:::k8s
            PaperPod["Paper Gen Pods (x4)"]:::k8s
            EvalPod["Evaluation Svc Pods (x5)"]:::k8s
            ResultPod["Result Svc Pods (x3)"]:::k8s
            AuditPod["Audit Svc Pods (x3, Isolated)"]:::k8s
        end
    end

    subgraph Datastores ["Persistence & Streaming Tier"]
        PostgreSQL_HA[("PostgreSQL HA (Multi-Schema / Patroni)")]:::db
        Kafka_HA[("Apache Kafka Cluster (3 Brokers + KRaft)")]:::db
        Redis_Cluster[("Redis Enterprise Cluster")]:::db
        Vault_KMS["HashiCorp Vault / Cloud KMS"]:::db
    end

    Internet --> CloudWAF --> LoadBalancer --> Ingress
    Ingress --> CoreMicroservices
    CoreMicroservices --> PostgreSQL_HA
    CoreMicroservices --> Redis_Cluster
    CoreMicroservices --> Kafka_HA
    PaperPod --> Vault_KMS
```

---

### Topology 2: Macro-Services Deployment (EC2 / VM / Docker Compose)

Consolidates the 14 domain modules into 5 high-cohesion deployable units + API Gateway + RabbitMQ. Requires ~2.5 GB RAM and runs comfortably on a single AWS EC2 `t3.medium` or `t3.large`.

```mermaid
graph TB
    classDef macro fill:#0077B6,color:#fff,stroke:#023E8A;
    classDef infra fill:#6C757D,color:#fff,stroke:#495057;

    Client["Frontend SPA (Port 4200 / 4300)"]
    Gateway["API Gateway (Port 9000)"]

    subgraph MacroUnits ["Macro-Service Aggregators"]
        AuthAdmin["auth-admin-app (Port 8081)\n• identity-service\n• candidate-service\n• admin-service\n• notification-service"]:::macro
        Content["content-app (Port 8083)\n• question-bank-service\n• examination-service\n• paper-generator\n• asset-service"]:::macro
        Execution["execution-app (Port 8087)\n• delivery-service\n• response-service"]:::macro
        PostExam["post-exam-app (Port 8089)\n• evaluation-service\n• result-service\n• analytics-service"]:::macro
        AuditSvc["audit-service (Port 8091)\n• Standalone Compliance Audit"]:::macro
    end

    subgraph MacroInfra ["Infra Tier"]
        RabbitMQ[("RabbitMQ 3.13 Alpine")]:::infra
        Postgres[("PostgreSQL 16")]:::infra
        Redis[("Redis 7")]:::infra
    end

    Client --> Gateway
    Gateway --> AuthAdmin
    Gateway --> Content
    Gateway --> Execution
    Gateway --> PostExam
    Gateway --> AuditSvc

    MacroUnits --> RabbitMQ
    MacroUnits --> Postgres
    MacroUnits --> Redis
```

---

### Topology 3: Single JVM Monolith Deployment (Ultralight / Dev / PoC)

Embeds all 14 domain services into a single Spring Boot application (`monolith-app`) using Spring in-memory event dispatching without external message brokers.

```mermaid
graph TB
    classDef mono fill:#2D6A4F,color:#fff,stroke:#1B4332;
    classDef infra fill:#52B788,color:#fff,stroke:#2D6A4F;

    Client["Frontend SPA (Port 4200 / 4300)"]

    subgraph MonolithRuntime ["Single JVM Spring Boot Process (Port 9000)"]
        MonolithApp["monolith-app\n• 14 Domain Modules\n• In-Memory Event Dispatcher (Spring Events)\n• MonolithFlywayConfig (Multi-Schema Migration)\n• MonolithAsyncConfig (Unified Thread Pool)"]:::mono
    end

    subgraph MonolithInfra ["Minimal Infrastructure Tier"]
        Postgres[("PostgreSQL 16 (All Schemas)")]:::infra
        Redis[("Redis 7 (Sessions/Cache)")]:::infra
        Vault["HashiCorp Vault (Optional)"]:::infra
    end

    Client --> MonolithRuntime
    MonolithRuntime --> Postgres
    MonolithRuntime --> Redis
    MonolithRuntime --> Vault
```

---

## 3. Deployment Scripts & Orchestration

The project includes purpose-built shell scripts located in `infrastructure/docker-compose/`:

| Script | Mode | Use Case |
|---|---|---|
| [`redeploy-monolith.sh`](file:///C:/Users/sheel/IdeaProjects/nag/infrastructure/docker-compose/redeploy-monolith.sh) | **Monolith** | Clean build, migration, and startup of the single JVM stack. |
| [`redeploy-macro.sh`](file:///C:/Users/sheel/IdeaProjects/nag/infrastructure/docker-compose/redeploy-macro.sh) | **Macro** | Clean build and launch of the 5 macro units + RabbitMQ. |
| [`redeploy-micro.sh`](file:///C:/Users/sheel/IdeaProjects/nag/infrastructure/docker-compose/redeploy-micro.sh) | **Micro** | Clean build and deployment of the 14 microservices + Kafka. |
| [`build-and-deploy.sh`](file:///C:/Users/sheel/IdeaProjects/nag/infrastructure/docker-compose/build-and-deploy.sh) | **Flexible** | Build with optional flags (`--observability`, `--ai`, `--service <name>`). |

### Common Flags for Redeploy Scripts
- `--observability` : Enables Prometheus, Grafana, and Jaeger tracing.
- `--ai` : Enables local Ollama, LiteLLM, and IndicTrans2 neural translation pipeline.
- `--no-cache` : Rebuilds Docker images without Docker cache.
- `--restart` : Restarts existing containers without rebuilding.
- `--health` : Checks container and actuator health endpoints.

---

## 4. Kubernetes & Helm Support

Helm charts in `infrastructure/helm/examination-platform/` support both microservices and macro-services through values overlays:

- **Microservices Deployment**:
  ```bash
  helm upgrade --install exam-platform ./infrastructure/helm/examination-platform \
    -f ./infrastructure/helm/examination-platform/values.yaml \
    -f ./infrastructure/helm/examination-platform/values-production.yaml
  ```

- **Macro-Services Deployment**:
  ```bash
  helm upgrade --install exam-platform ./infrastructure/helm/examination-platform \
    -f ./infrastructure/helm/examination-platform/values.yaml \
    -f ./infrastructure/helm/examination-platform/values-macro.yaml
  ```
