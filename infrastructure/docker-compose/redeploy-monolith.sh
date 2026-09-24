#!/usr/bin/env bash
# =============================================================================
# Redeploy NAG Single JVM Monolith Stack (Clean Docker Compose Workflow)
#
# Architecture:
#   - 1 Monolith container combining all 14 services + Flyway + Embedded In-Memory Bus
#   - 1 Postgres 16 container with pgvector
#   - 1 Redis 7 container
#   - 1 HashiCorp Vault container
#   - 1 Keycloak container
#   - 1 Admin Portal SPA (Nx Workspace / Angular 22 - Port 4200)
#   - 1 Candidate Delivery SPA (Nx Workspace / Angular 22 - Port 4300)
#   - 1 Public Verifier SPA (Nx Workspace / Angular 22 - Port 4400)
#
# Usage:
#   ./redeploy-monolith.sh                  # Deploy Monolith + all 3 Nx Frontends
#   ./redeploy-monolith.sh --backend-only   # Deploy only Backend Monolith + DB/Vault/Redis (Skip Frontend builds)
#   ./redeploy-monolith.sh --rabbit         # Deploy with RabbitMQ Broker
#   ./redeploy-monolith.sh --clean-db       # Drop all volumes / fresh Postgres schema
#   ./redeploy-monolith.sh --observability  # Start with Prometheus, Grafana, and Jaeger
#   ./redeploy-monolith.sh --ai             # Start with Ollama, LiteLLM, IndicTrans2
#   ./redeploy-monolith.sh --no-cache       # Force rebuild without Docker cache
#   ./redeploy-monolith.sh --restart        # Restart services without rebuilding
#   ./redeploy-monolith.sh --health         # Check health status of monolith
# =============================================================================
set -e

export BUILDX_NO_DEFAULT_ATTESTATIONS=1
export DOCKER_BUILDKIT=1

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$SCRIPT_DIR"

NO_CACHE=""
RESTART_ONLY=false
HEALTH_CHECK=false
OBSERVABILITY=false
AI=false
CLEAN_DB=false
RABBIT=false
BACKEND_ONLY=false

# If PLATFORM_MESSAGING_BROKER environment variable is pre-set to rabbit
if [ "${PLATFORM_MESSAGING_BROKER:-}" = "rabbit" ]; then
    RABBIT=true
fi

while [[ $# -gt 0 ]]; do
    case $1 in
        --no-cache) NO_CACHE="--no-cache"; shift ;;
        --restart) RESTART_ONLY=true; shift ;;
        --health) HEALTH_CHECK=true; shift ;;
        --clean-db|--clean-volumes|--delete-db-volume|--reset-db|--drop-db) CLEAN_DB=true; shift ;;
        --backend-only|--monolith-only|--no-frontends) BACKEND_ONLY=true; shift ;;
        --rabbit|--with-rabbit) RABBIT=true; shift ;;
        --observability|--with-observability) OBSERVABILITY=true; shift ;;
        --ai|--with-ai) AI=true; shift ;;
        *) echo "Unknown option: $1"; exit 1 ;;
    esac
done

PROFILES_ARGS=()
if [ "$RABBIT" = true ]; then
    PROFILES_ARGS+=(--profile rabbit)
    export PLATFORM_MESSAGING_BROKER=rabbit
    export MANAGEMENT_HEALTH_RABBIT_ENABLED=true
    export SPRING_RABBITMQ_LISTENER_AUTO_STARTUP=true
else
    export PLATFORM_MESSAGING_BROKER="${PLATFORM_MESSAGING_BROKER:-in-memory}"
    export MANAGEMENT_HEALTH_RABBIT_ENABLED=false
    export SPRING_RABBITMQ_LISTENER_AUTO_STARTUP=false
fi

if [ "$OBSERVABILITY" = true ]; then
    PROFILES_ARGS+=(--profile observability)
fi
if [ "$AI" = true ]; then
    PROFILES_ARGS+=(--profile ai)
fi

COMPOSE="docker compose ${PROFILES_ARGS[*]} -f docker-compose.yml -f docker-compose.monolith.yml"

echo "============================================="
echo "  NAG Platform — Single JVM Monolith Mode"
echo "  Architecture: 1 JVM Monolith + Postgres + Redis"
if [ "$RABBIT" = true ]; then
echo "  Messaging:    RabbitMQ (exam-monolith-rabbitmq:5672)"
else
echo "  Messaging:    In-Memory Spring Events (Zero External Broker)"
fi
if [ "$BACKEND_ONLY" = true ]; then
echo "  Frontends:    Skipped (--backend-only mode active)"
else
echo "  Frontends:    Nx Angular Apps (Admin:4200, Candidate:4300, Verifier:4400)"
fi
if [ "$CLEAN_DB" = true ]; then
echo "  Database:     Reset (Volumes will be deleted)"
else
echo "  Database:     Preserved (Use --clean-db to reset)"
fi
if [ "$OBSERVABILITY" = true ]; then
echo "  Observability: Enabled (Prometheus, Grafana, Jaeger)"
fi
if [ "$AI" = true ]; then
echo "  AI Pipeline:   Enabled (Ollama, LiteLLM, IndicTrans2)"
fi
echo "============================================="

# --- Ensure builder base image exists ---
ensure_builder_base() {
    if ! docker image inspect exam/builder-base:latest >/dev/null 2>&1; then
        echo "🔧 Building builder base image (one-time)..."
        cd "$PROJECT_ROOT"
        docker build -f backend/Dockerfile.base -t exam/builder-base:latest .
        cd "$SCRIPT_DIR"
        echo "✅ Builder base image ready."
    fi
}

ensure_builder_base

# --- Health check mode ---
if [ "$HEALTH_CHECK" = true ]; then
    echo ""
    echo "🔍 Checking health status of monolith services..."
    echo ""

    container="exam-monolith-app"
    if ! docker ps --format '{{.Names}}' | grep -q "^${container}$"; then
        echo "  monolith-app: ❌ DOWN (Container not running)"
        exit 1
    fi

    health_url="http://localhost:9000/actuator/health"
    health_response=$(curl -s --connect-timeout 3 --max-time 5 "$health_url" 2>/dev/null || echo "")

    if [ -z "$health_response" ]; then
        health_response=$(docker exec "$container" wget -qO- "$health_url" 2>/dev/null || echo "")
    fi

    if echo "$health_response" | grep -q -E '"status":"UP"|healthy'; then
        echo "  monolith-app: ✅ UP (Port 9000)"
    else
        echo "  monolith-app: ⚠️ $health_response"
    fi
    exit 0
fi

# --- Target app services to manage ---
if [ "$BACKEND_ONLY" = true ]; then
    APP_TARGETS="monolith-app"
else
    APP_TARGETS="monolith-app admin-portal candidate-delivery public-verifier"
fi

# --- Restart only mode ---
if [ "$RESTART_ONLY" = true ]; then
    echo ""
    echo "🔄 Restarting monolith services (no build)..."
    $COMPOSE stop $APP_TARGETS
    $COMPOSE up -d $APP_TARGETS
    echo ""
    echo "✅ Monolith restarted."
    exit 0
fi

# --- Full deploy mode ---
echo ""
echo "🛑 Stopping all containers..."
$COMPOSE down --remove-orphans 2>/dev/null || true
docker stop exam-kafka 2>/dev/null || true
docker rm exam-kafka 2>/dev/null || true
if [ "$RABBIT" = false ]; then
    docker stop exam-monolith-rabbitmq exam-rabbitmq 2>/dev/null || true
    docker rm exam-monolith-rabbitmq exam-rabbitmq 2>/dev/null || true
fi

# --- Volume deletion based on explicit option ---
if [ "$CLEAN_DB" = true ]; then
    echo "🧹 Explicit option provided: Removing DB & ephemeral volumes (postgres_data, redis_data, keycloak_data)..."
    docker volume ls --format '{{.Name}}' | grep -E 'postgres_data|redis_data|keycloak_data' | grep -v -E 'vault_data|ollama_data|indictrans2_cache' | xargs -r docker volume rm 2>/dev/null || true
else
    echo "💾 Preserving DB and data volumes (pass --clean-db or --delete-db-volume to remove)..."
fi

echo ""
INFRA_TARGETS="postgres redis vault vault-init keycloak"
if [ "$RABBIT" = true ]; then
    INFRA_TARGETS="$INFRA_TARGETS rabbitmq"
fi
if [ "$OBSERVABILITY" = true ]; then
    INFRA_TARGETS="$INFRA_TARGETS prometheus grafana jaeger"
fi
if [ "$AI" = true ]; then
    INFRA_TARGETS="$INFRA_TARGETS ollama litellm indictrans2"
fi

echo "🚀 Starting infrastructure ($INFRA_TARGETS)..."
$COMPOSE up -d $INFRA_TARGETS
echo "  Waiting for infrastructure to be healthy..."
$COMPOSE up --wait -d postgres vault redis
if [ "$RABBIT" = true ]; then
    $COMPOSE up --wait -d rabbitmq
fi
# Ensure vault-init has unsealed Vault and provisioned transit keys
echo "  Ensuring Vault is unsealed and transit keys are initialized..."
$COMPOSE up -d vault-init

echo ""
echo "📦 Building targets: $APP_TARGETS..."
$COMPOSE build $NO_CACHE $APP_TARGETS

echo ""
echo "🚀 Starting monolith stack ($APP_TARGETS)..."
$COMPOSE up -d $APP_TARGETS

echo ""
echo "============================================="
echo "  🎉 Single JVM Monolith redeploy complete!"
echo "============================================="
echo "  Monolith API:        http://localhost:9000"
echo "  Actuator Health:     http://localhost:9000/actuator/health"
if [ "$BACKEND_ONLY" = false ]; then
echo "  Admin Portal:        http://localhost:4200"
echo "  Candidate Delivery:  http://localhost:4300"
echo "  Public Verifier:     http://localhost:4400"
fi
echo "  Postgres Database:   localhost:5432"
echo "  Redis Cache:         localhost:6379"
echo "============================================="
echo ""
$COMPOSE ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || $COMPOSE ps
