# Docker Containerization & Compose Topologies — National Assessment Grid

## 1. Multi-Stage Docker Strategy

NAG backend containers leverage optimized multi-stage Docker builds:
- **Base Builder (`backend/Dockerfile.base`)**: Pre-populates the Gradle 8.14 wrapper, project build scripts, and shared dependencies to maximize build layer caching.
- **Service Dockerfile (`backend/Dockerfile`)**: Builds individual microservices or the single `monolith-app` using the cached builder.
- **Macro Dockerfile (`backend/Dockerfile.macro`)**: Builds the consolidated macro-service aggregators (`auth-admin-app`, `content-app`, `execution-app`, `post-exam-app`).
- **Frontend Dockerfiles (`frontend/Dockerfile`, `candidate-frontend/Dockerfile`)**: Build Angular standalone applications and serve them through minimal Alpine NGINX images.

---

## 2. Compose Configuration Files

The platform maintains composable Docker Compose definitions under `infrastructure/docker-compose/`:

| Compose File | Purpose | Key Services |
|---|---|---|
| `docker-compose.yml` | Base Infrastructure | PostgreSQL, Redis, Vault, Keycloak, Observability, AI |
| `docker-compose.monolith.yml` | Monolith Mode | `monolith-app` (Port 9000), `frontend`, `candidate-frontend` |
| `docker-compose.macro.yml` | Macro-Services Mode | 5 Macro apps, RabbitMQ (Alpine), `api-gateway`, frontends |
| `docker-compose.services.yml` | Microservices Mode | 14 Microservices, Apache Kafka, `api-gateway`, frontends |

---

## 3. Running with Docker Compose

### 1. Monolith Mode (Ultralight ~1GB RAM)
```bash
docker compose -f infrastructure/docker-compose/docker-compose.yml \
               -f infrastructure/docker-compose/docker-compose.monolith.yml up --build -d
```

### 2. Macro Mode (Consolidated ~2.5GB RAM)
```bash
docker compose -f infrastructure/docker-compose/docker-compose.yml \
               -f infrastructure/docker-compose/docker-compose.macro.yml up --build -d
```

### 3. Microservices Mode (Full Distributed ~8GB RAM)
```bash
docker compose -f infrastructure/docker-compose/docker-compose.yml \
               -f infrastructure/docker-compose/docker-compose.services.yml up --build -d
```

---

## 4. Helper Shell Scripts

Automated redeployment scripts handle teardown, dependency health checks, Docker build caching, and service startup:

```bash
# Monolith
./infrastructure/docker-compose/redeploy-monolith.sh [--observability] [--ai] [--no-cache]

# Macro
./infrastructure/docker-compose/redeploy-macro.sh [--observability] [--ai] [--no-cache]

# Micro
./infrastructure/docker-compose/redeploy-micro.sh [--observability] [--ai] [--no-cache]
```
