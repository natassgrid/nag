# Deployment Architecture — National Assessment Grid

## 1. Overview

The production deployment architecture for the National Assessment Grid (NAG) relies on Kubernetes container orchestration across multi-availability zone (Multi-AZ) cloud or high-security government data center infrastructure (NIC / State Data Centers).

---

## 2. Infrastructure Deployment Diagram (Mermaid)

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
        Ingress["NGINX Ingress Controller"]:::k8s

        subgraph SystemNamespace ["Namespace: nag-system"]
            Gatekeeper["OPA Gatekeeper (Policy Engine)"]:::k8s
            Vault["HashiCorp Vault (Secrets Agent)"]:::k8s
        end

        subgraph CoreNamespace ["Namespace: nag-core"]
            GatewayPods["API Gateway Pods (x4)"]:::k8s
            SchedulePods["Scheduling Svc Pods (x3)"]:::k8s
            PaperPods["Paper Gen Svc Pods (x4)"]:::k8s
            DeliveryPods["Exam Delivery Svc Pods (x20+)"]:::k8s
        end

        subgraph AuthNamespace ["Namespace: nag-auth"]
            KeycloakPods["Keycloak IAM Pods (x3)"]:::k8s
        end
    end

    subgraph PersistenceLayer ["Managed High-Availability Datastores"]
        PostgreSQL_HA[("PostgreSQL Cluster (Primary + Standby)")]:::db
        Kafka_HA[("Apache Kafka Cluster (3 Brokers + KRaft)")]:::db
        Redis_Cluster[("Redis Enterprise Cluster")]:::db
        KMS_HSM["Hardware Security Module (HSM)"]:::db
    end

    Internet --> CloudWAF
    CloudWAF --> LoadBalancer
    LoadBalancer --> Ingress
    Ingress --> GatewayPods

    GatewayPods --> KeycloakPods
    GatewayPods --> SchedulePods
    GatewayPods --> PaperPods
    GatewayPods --> DeliveryPods

    SchedulePods --> PostgreSQL_HA
    DeliveryPods --> PostgreSQL_HA
    DeliveryPods --> Redis_Cluster
    DeliveryPods --> Kafka_HA
    PaperPods --> KMS_HSM
```

---

## 3. High Availability & Scaling Strategy

### 3.1 Horizontal Pod Autoscaling (HPA)
- **Exam Delivery Service**: Scales dynamically between 5 and 50 pods based on CPU utilization ($>70\%$) and active HTTP request rate per second.
- **API Gateway**: Scales between 3 and 10 pods during scheduled exam start windows.

### 3.2 Database High Availability
- **Primary-Standby Replication**: PostgreSQL configured with Streaming Replication across Multi-AZ zones with automatic failover via Patroni.
- **PgBouncer**: Deployed as sidecars / DaemonSet to handle connection pooling for microservices.

### 3.3 Zero-Downtime Rolling Upgrades
- Kubernetes deployment strategies use `maxSurge: 25%` and `maxUnavailable: 0` during rolling updates to guarantee zero service interruption for active candidates.
