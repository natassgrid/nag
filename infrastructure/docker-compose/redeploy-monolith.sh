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
#   - 1 Admin Frontend (React/Vite)
#   - 1 Candidate Frontend (React/Vite)
#
# Usage (supports multiple arguments combined in any order):
#   ./redeploy-monolith.sh                            # Default standard JVM Monolith (In-Memory Bus)
#   ./redeploy-monolith.sh --native --ai              # Native GraalVM + AI Pipeline (Ollama/LiteLLM/IndicTrans2)
#   ./redeploy-monolith.sh --native --clean-db        # Native GraalVM + fresh DB volume wipe
#   ./redeploy-monolith.sh --native --observability   # Native GraalVM + Prometheus, Grafana, Jaeger
#   ./redeploy-monolith.sh --restart                  # Quick restart running containers without rebuilding
#   ./redeploy-monolith.sh --restart --ai             # Restart app containers and ensure AI pipeline is up
#   ./redeploy-monolith.sh --health                   # Probe actuator health of monolith
# =============================================================================
set -e

# Enable Docker BuildKit for multi-stage cache mounts in WSL / Linux
export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$SCRIPT_DIR"

# Ensure .env exists for Docker Compose
if [ ! -f .env ] && [ -f .env.example ]; then
    echo "📋 Creating .env from .env.example..."
    cp .env.example .env
fi

NO_CACHE=""
RESTART_ONLY=false
HEALTH_CHECK=false
OBSERVABILITY=false
AI=false
CLEAN_DB=false
RABBIT=false
NATIVE=false

# Support environment variables
if [ "${USE_NATIVE:-}" = "true" ] || [ "${NATIVE:-}" = "true" ]; then
    NATIVE=true
fi

# If PLATFORM_MESSAGING_BROKER environment variable is pre-set to rabbit
if [ "${PLATFORM_MESSAGING_BROKER:-}" = "rabbit" ]; then
    RABBIT=true
fi

while [[ $# -gt 0 ]]; do
    case $1 in
        --no-cache) NO_CACHE="--no-cache"; shift ;;
        --restart) RESTART_ONLY=true; shift ;;
        --health) HEALTH_CHECK=true; shift ;;
        --native|--with-native|--graalvm) NATIVE=true; shift ;;
        --clean-db|--clean-volumes|--delete-db-volume|--reset-db|--drop-db) CLEAN_DB=true; shift ;;
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
if [ "$NATIVE" = true ]; then
echo "  Runtime:      🚀 GraalVM Native Image (AOT Compiled / Distroless CC)"
else
echo "  Runtime:      ☕ OpenJDK 21 JVM (Temurin JRE Alpine)"
fi
echo "  Architecture: 1 Monolith App + Postgres + Redis"
if [ "$RABBIT" = true ]; then
echo "  Messaging:    RabbitMQ (exam-monolith-rabbitmq:5672)"
else
echo "  Messaging:    In-Memory Spring Events (Zero External Broker)"
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

# --- Ensure builder base image exists for standard JVM build ---
ensure_builder_base() {
    if [ "$NATIVE" = false ] && ! docker image inspect exam/builder-base:latest >/dev/null 2>&1; then
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

    target_port="${MONOLITH_PORT:-9000}"
    health_url="http://localhost:${target_port}/actuator/health"
    health_response=$(curl -s --connect-timeout 3 --max-time 5 "$health_url" 2>/dev/null || echo "")

    if [ -z "$health_response" ]; then
        health_response=$(docker exec "$container" wget -qO- "http://localhost:${target_port}/actuator/health" 2>/dev/null || echo "")
    fi

    if echo "$health_response" | grep -q -E '"status":"UP"|healthy'; then
        echo "  monolith-app: ✅ UP (Port ${target_port})"
    else
        echo "  monolith-app: ⚠️ $health_response"
    fi
    exit 0
fi

# --- Target app services to manage ---
APP_TARGETS="monolith-app frontend candidate-frontend"

# --- Restart only mode ---
if [ "$RESTART_ONLY" = true ]; then
    echo ""
    echo "🔄 Restarting monolith services (no build)..."
    if [ "$AI" = true ] || [ "$OBSERVABILITY" = true ] || [ "$RABBIT" = true ]; then
        $COMPOSE up -d
    else
        $COMPOSE stop $APP_TARGETS
        $COMPOSE up -d $APP_TARGETS
    fi
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
echo "📦 Building monolith-app and frontends..."
if [ "$NATIVE" = true ]; then
    echo "  Building monolith-app using GraalVM Native Image (backend/Dockerfile.native)..."
    docker build $NO_CACHE -f "$PROJECT_ROOT/backend/Dockerfile.native" --build-arg SERVICE_NAME=monolith-app -t "localhost:5000/exam/monolith-app:latest" "$PROJECT_ROOT"
    echo "  Building frontend and candidate-frontend..."
    $COMPOSE build $NO_CACHE frontend candidate-frontend
else
    $COMPOSE build $NO_CACHE $APP_TARGETS
fi

echo ""
echo "🚀 Starting monolith stack..."
$COMPOSE up -d $APP_TARGETS

echo ""
echo "============================================="
echo "  🎉 Single JVM Monolith redeploy complete!"
echo "============================================="
echo ""
$COMPOSE ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || $COMPOSE ps
