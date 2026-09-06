# Running Locally — National Assessment Grid

## 1. Quick Start Options

NAG supports **three local runtime modes** depending on your development workflow, testing goals, and machine resource capacity:

| Mode | Memory | External Broker | Best For | Start Command |
|---|---|---|---|---|
| **Monolith Mode** | ~1.0 - 1.5 GB | None (In-memory) | Rapid daily feature development, UI testing, debugging | `./infrastructure/docker-compose/redeploy-monolith.sh` |
| **Macro Mode** | ~2.5 - 3.5 GB | RabbitMQ | Service boundary integration testing, low-memory staging | `./infrastructure/docker-compose/redeploy-macro.sh` |
| **Micro Mode** | ~7.0 - 10 GB | Kafka | Full distributed architecture testing, distributed tracing | `./infrastructure/docker-compose/redeploy-micro.sh` |

---

## 2. Option A: Running the Single JVM Monolith (Recommended for Developers)

The single JVM monolith combines all 14 domain modules in one Spring Boot process and runs directly with PostgreSQL and Redis.

### Using Docker Compose
```bash
./infrastructure/docker-compose/redeploy-monolith.sh
```

### Running Directly from IDE / Gradle
1. **Start Core Datastores**:
   ```bash
   docker compose -f infrastructure/docker-compose/docker-compose.yml up -d postgres redis vault
   ```

2. **Run Monolith Application via Gradle**:
   ```bash
   ./gradlew :backend:monolith-app:bootRun
   ```

3. **Start Angular Frontends**:
   ```bash
   # Terminal 1: Admin Frontend
   cd frontend
   npm install && npm start

   # Terminal 2: Candidate Frontend
   cd candidate-frontend
   npm install && npm start
   ```

---

## 3. Option B: Running in Macro-Services Mode

Runs 5 aggregated services (`auth-admin-app`, `content-app`, `execution-app`, `post-exam-app`, and `audit-service`) with RabbitMQ:

```bash
./infrastructure/docker-compose/redeploy-macro.sh
```

To run a specific macro-service in your IDE (e.g. `execution-app`):
```bash
./gradlew :backend:execution-app:bootRun
```

---

## 4. Option C: Running in Full Microservices Mode

Runs all 14 standalone microservices with Apache Kafka and Spring Cloud Gateway:

```bash
./infrastructure/docker-compose/redeploy-micro.sh
```

To run an individual microservice (e.g. `delivery-service`):
```bash
./gradlew :backend:delivery-service:bootRun
```

---

## 5. Optional Feature Flags

You can pass feature flags to any redeploy script:

- **Observability (Prometheus, Grafana, Jaeger)**:
  ```bash
  ./infrastructure/docker-compose/redeploy-monolith.sh --observability
  ```
- **AI Subsystem (Ollama, LiteLLM, IndicTrans2)**:
  ```bash
  ./infrastructure/docker-compose/redeploy-monolith.sh --ai
  ```
- **Force Rebuild Without Cache**:
  ```bash
  ./infrastructure/docker-compose/redeploy-monolith.sh --no-cache
  ```
- **Health Verification**:
  ```bash
  ./infrastructure/docker-compose/redeploy-monolith.sh --health
  ```

---

## 6. Accessing Local Service Endpoints

- **Admin UI**: [http://localhost:4200](http://localhost:4200)
- **Candidate Portal UI**: [http://localhost:4300](http://localhost:4300)
- **API Gateway / Monolith Ingress**: [http://localhost:9000](http://localhost:9000)
- **Keycloak IAM**: [http://localhost:8080](http://localhost:8080)
- **Vault UI**: [http://localhost:8200](http://localhost:8200)
- **Grafana Dashboard** *(when `--observability` is active)*: [http://localhost:3000](http://localhost:3000)
- **Jaeger UI** *(when `--observability` is active)*: [http://localhost:16686](http://localhost:16686)
- **RabbitMQ Management UI** *(in Macro mode)*: [http://localhost:15672](http://localhost:15672) (User: `guest`, Pass: `guest`)
