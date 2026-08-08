# C4 Model Level 2: Container Diagram — National Assessment Grid

## 1. Overview

The Container Diagram illustrates the high-level technical building blocks of the NAG platform: the Angular Single Page Application (SPA), API Gateway, Spring Boot backend microservices, datastores, and message brokers.

---

## 2. Container Diagram (Mermaid)

```mermaid
graph TB
    classDef client fill:#2C7DA0,color:#fff,stroke:#014F86;
    classDef gateway fill:#014F86,color:#fff,stroke:#012A4A;
    classDef service fill:#468FA6,color:#fff,stroke:#2C7DA0;
    classDef store fill:#61A5C2,color:#fff,stroke:#2C7DA0;

    SPA["Angular SPA Frontend (Angular 21 / Material)"]:::client
    Gateway["API Gateway / Ingress (Spring Cloud Gateway / NGINX)"]:::gateway

    subgraph Microservices ["Backend Microservices"]
        AuthSvc["Keycloak IAM Service"]:::service
        QuestionSvc["Question Bank Service"]:::service
        PaperSvc["Paper Generation Service"]:::service
        ScheduleSvc["Exam Scheduling Service"]:::service
        DeliverySvc["Exam Delivery Service"]:::service
        EvalSvc["Evaluation Service"]:::service
    end

    subgraph Datastores ["Persistence & Messaging"]
        DB_Question[("PostgreSQL: Question DB")]:::store
        DB_Schedule[("PostgreSQL: Schedule DB")]:::store
        DB_Delivery[("PostgreSQL: Delivery DB")]:::store
        Redis[("Redis Cache")]:::store
        Kafka[("Apache Kafka Event Bus")]:::store
    end

    SPA -->|"HTTPS / REST / JSON"| Gateway
    Gateway -->|"Route & Authenticate"| AuthSvc
    Gateway -->|"Route Request"| QuestionSvc
    Gateway -->|"Route Request"| PaperSvc
    Gateway -->|"Route Request"| ScheduleSvc
    Gateway -->|"Route Request"| DeliverySvc
    Gateway -->|"Route Request"| EvalSvc

    QuestionSvc --> DB_Question
    ScheduleSvc --> DB_Schedule
    DeliverySvc --> DB_Delivery
    DeliverySvc --> Redis

    QuestionSvc -->|"Publish Events"| Kafka
    ScheduleSvc -->|"Publish Events"| Kafka
    DeliverySvc -->|"Publish Events"| Kafka
```

---

## 3. Container Descriptions

| Container | Technology | Responsibilities |
|---|---|---|
| **Angular SPA Frontend** | Angular 21, SCSS, Material | Client UI for candidate portal, authoring tool, scheduling dashboard, and evaluation console. |
| **API Gateway** | Spring Cloud Gateway | Ingress routing, TLS termination, global rate limiting, tenant header validation (`X-Tenant-Id`). |
| **Question Bank Service** | Java 21, Spring Boot | Manages question authoring, section definitions, taxonomy metadata, and review workflows. |
| **Paper Generation Service**| Java 21, Spring Boot, Cryptography | Automated question selection engine, multi-set paper packaging, paper AES-256 encryption. |
| **Exam Scheduling Service** | Java 21, Spring Boot | Manages schedule versions, approval state transitions, shift timing rules, and center seat allocations. |
| **Exam Delivery Service** | Java 21, Spring Boot | High-throughput candidate test session management, response bundle ingestion, time-lock enforcement. |
| **Evaluation Service** | Java 21, Spring Boot | Automated grading for objective items, anonymized token assignment for subjective evaluation. |
| **Apache Kafka** | Apache Kafka 3.x | Asynchronous event bus for domain events and audit log streaming. |
| **PostgreSQL Datastores** | PostgreSQL 16 | Dedicated relational datastore per microservice with Row-Level Security (RLS). |
