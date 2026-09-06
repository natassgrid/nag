#!/bin/bash

# SPDX-License-Identifier: AGPL-3.0-only
#
# National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
# Copyright (C) 2025 NAG Contributors
#
# =============================================================================
# Smart redeploy script for NAG Macro-Services Mode (Lightweight / Demo / EC2)
# Consolidates 14 microservices into 5 backend services + Gateway + RabbitMQ.
#
# Usage:
#   ./redeploy-macro.sh                  # Full clean: tear down ALL, rebuild ALL, start ALL
#   ./redeploy-macro.sh --observability  # Start with Prometheus, Grafana, and Jaeger
#   ./redeploy-macro.sh --ai             # Start with Ollama, LiteLLM, IndicTrans2
#   ./redeploy-macro.sh --service <name> # Rebuild and restart ONE service (keeps others running)
#   ./redeploy-macro.sh --smart          # Only rebuild macro-services with code changes (uses git diff)
#   ./redeploy-macro.sh --no-cache       # Force rebuild without Docker cache
#   ./redeploy-macro.sh --restart        # Restart ALL services without rebuilding (keeps images)
#   ./redeploy-macro.sh --restart --service <name>  # Restart ONE service without rebuilding
#   ./redeploy-macro.sh --health         # Check health status of all running macro-services
# =============================================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$SCRIPT_DIR"

NO_CACHE=""
SERVICE=""
SMART=false
RESTART_ONLY=false
HEALTH_CHECK=false
OBSERVABILITY=false
AI=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --no-cache) NO_CACHE="--no-cache"; shift ;;
        --service) SERVICE="$2"; shift 2 ;;
        --smart) SMART=true; shift ;;
        --restart) RESTART_ONLY=true; shift ;;
        --health) HEALTH_CHECK=true; shift ;;
        --observability|--with-observability) OBSERVABILITY=true; shift ;;
        --ai|--with-ai) AI=true; shift ;;
        *) echo "Unknown option: $1"; exit 1 ;;
    esac
done

PROFILES_ARGS=()
if [ "$OBSERVABILITY" = true ]; then
    PROFILES_ARGS+=(--profile observability)
fi
if [ "$AI" = true ]; then
    PROFILES_ARGS+=(--profile ai)
fi

COMPOSE="docker compose ${PROFILES_ARGS[*]} -f docker-compose.yml -f docker-compose.macro.yml"

ALL_SERVICES=(
    auth-admin-service
    content-service
    execution-service
    post-exam-service
    audit-service
    api-gateway
    frontend
    candidate-frontend
)

# Service name → source paths for change detection
get_service_src_paths() {
    local svc="$1"
    case "$svc" in
        auth-admin-service)
            echo "backend/auth-admin-app/src backend/identity-service/src backend/candidate-service/src backend/admin-service/src backend/notification-service/src backend/shared-lib/src build.gradle settings.gradle"
            ;;
        content-service)
            echo "backend/content-app/src backend/question-bank-service/src backend/examination-service/src backend/paper-generator/src backend/asset-service/src backend/shared-lib/src build.gradle settings.gradle"
            ;;
        execution-service)
            echo "backend/execution-app/src backend/delivery-service/src backend/response-service/src backend/shared-lib/src build.gradle settings.gradle"
            ;;
        post-exam-service)
            echo "backend/post-exam-app/src backend/evaluation-service/src backend/result-service/src backend/analytics-service/src backend/shared-lib/src build.gradle settings.gradle"
            ;;
        audit-service)
            echo "backend/audit-service/src backend/shared-lib/src build.gradle settings.gradle"
            ;;
        api-gateway)
            echo "backend/api-gateway/src backend/shared-lib/src build.gradle settings.gradle"
            ;;
        frontend)
            echo "frontend/src frontend/package.json"
            ;;
        candidate-frontend)
            echo "candidate-frontend/src candidate-frontend/package.json"
            ;;
        *)
            echo "backend/${svc} backend/shared-lib/src build.gradle settings.gradle"
            ;;
    esac
}

# --- Detect which services have code changes (git-based) ---
get_changed_services() {
    local changed=()
    cd "$PROJECT_ROOT"

    for svc in "${ALL_SERVICES[@]}"; do
        local marker="/tmp/.exam-macro-build-marker-${svc}"
        local paths
        paths=$(get_service_src_paths "$svc")
        local current_hash
        # shellcheck disable=SC2086
        current_hash=$(git log -1 --format="%H" -- $paths 2>/dev/null || echo "none")

        if [ -f "$marker" ]; then
            local last_hash
            last_hash=$(cat "$marker")
            if [ "$current_hash" != "$last_hash" ]; then
                changed+=("$svc")
            fi
        else
            # No marker = never built, needs build
            changed+=("$svc")
        fi
    done

    cd "$SCRIPT_DIR"
    echo "${changed[@]}"
}

# --- Mark a service as built (store git hash) ---
mark_built() {
    local svc="$1"
    cd "$PROJECT_ROOT"
    local paths
    paths=$(get_service_src_paths "$svc")
    # shellcheck disable=SC2086
    git log -1 --format="%H" -- $paths 2>/dev/null > "/tmp/.exam-macro-build-marker-${svc}"
    cd "$SCRIPT_DIR"
}

echo "============================================="
echo "  NAG Platform — Macro-Services Redeploy"
echo "  Architecture: 5 Services + Gateway + RabbitMQ"
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
    echo "🔍 Checking health status of all macro services..."
    echo ""

    # Service name → port mapping
    declare -A SERVICE_PORTS=(
        [auth-admin-service]=8081
        [content-service]=8083
        [execution-service]=8087
        [post-exam-service]=8089
        [audit-service]=8091
        [api-gateway]=9000
        [frontend]=4200
        [candidate-frontend]=4300
    )

    HEALTHY=0
    UNHEALTHY=0
    DOWN=0
    TOTAL=${#ALL_SERVICES[@]}

    printf "  %-25s %-12s %-8s %s\n" "SERVICE" "STATUS" "PORT" "DETAILS"
    printf "  %-25s %-12s %-8s %s\n" "-------" "------" "----" "-------"

    for svc in "${ALL_SERVICES[@]}"; do
        port=${SERVICE_PORTS[$svc]}
        container="exam-${svc}"

        # Check if container is running
        if ! docker ps --format '{{.Names}}' | grep -q "^${container}$"; then
            printf "  %-25s %-12s %-8s %s\n" "$svc" "❌ DOWN" "$port" "Container not running"
            DOWN=$((DOWN + 1))
            continue
        fi

        # First check Docker's own healthcheck status
        docker_health=$(docker inspect --format='{{.State.Health.Status}}' "$container" 2>/dev/null || echo "none")

        if [ "$docker_health" = "starting" ]; then
            printf "  %-25s %-12s %-8s %s\n" "$svc" "⏳ STARTING" "$port" "Still initializing..."
            UNHEALTHY=$((UNHEALTHY + 1))
            continue
        fi

        # Query health endpoint
        health_url="http://localhost:${port}/actuator/health"
        if [[ "$svc" == *"frontend"* ]]; then
            health_url="http://localhost:${port}/health"
        fi

        health_response=$(curl -s --connect-timeout 3 --max-time 5 "$health_url" 2>/dev/null || echo "")

        if [ -z "$health_response" ]; then
            health_response=$(docker exec "$container" wget -qO- "$health_url" 2>/dev/null || echo "")
        fi

        if [ -z "$health_response" ]; then
            printf "  %-25s %-12s %-8s %s\n" "$svc" "⚠️ NO RESP" "$port" "No response from health endpoint"
            UNHEALTHY=$((UNHEALTHY + 1))
            continue
        fi

        if echo "$health_response" | grep -q -E '"status":"UP"|healthy'; then
            components=$(echo "$health_response" | grep -o '"[a-zA-Z]*":{"status":"[^"]*"' | \
                sed 's/"\([^"]*\)":{"status":"\([^"]*\)"/\1:\2/g' | tr '\n' ' ')
            printf "  %-25s %-12s %-8s %s\n" "$svc" "✅ UP" "$port" "$components"
            HEALTHY=$((HEALTHY + 1))
        elif echo "$health_response" | grep -q -E '"status":"DOWN"|unhealthy'; then
            components=$(echo "$health_response" | grep -o '"[a-zA-Z]*":{"status":"DOWN"' | \
                sed 's/"\([^"]*\)":{"status":"DOWN"/\1:DOWN/g' | tr '\n' ' ')
            printf "  %-25s %-12s %-8s %s\n" "$svc" "❌ DOWN" "$port" "$components"
            UNHEALTHY=$((UNHEALTHY + 1))
        else
            printf "  %-25s %-12s %-8s %s\n" "$svc" "⚠️ OK" "$port" ""
            HEALTHY=$((HEALTHY + 1))
        fi
    done

    echo ""
    echo "============================================="
    echo "  Health Summary: $HEALTHY healthy, $UNHEALTHY unhealthy, $DOWN down (of $TOTAL total)"
    echo "============================================="
    exit 0
fi

# --- Single service mode ---
if [ -n "$SERVICE" ]; then
    echo ""
    if [ "$RESTART_ONLY" = true ]; then
        echo "🔄 Restarting service (no build): $SERVICE"
        $COMPOSE stop "$SERVICE"
        $COMPOSE up -d --no-recreate "$SERVICE" 2>/dev/null || $COMPOSE up -d "$SERVICE"
        echo ""
        echo "✅ $SERVICE restarted (image unchanged)."
        exit 0
    fi

    echo "📦 Rebuilding service: $SERVICE"
    $COMPOSE build $NO_CACHE "$SERVICE"
    echo ""
    echo "🚀 Restarting service: $SERVICE"
    $COMPOSE up -d --force-recreate "$SERVICE"
    mark_built "$SERVICE"
    echo ""
    echo "✅ $SERVICE redeployed."
    exit 0
fi

# --- Smart mode: only rebuild changed services ---
if [ "$SMART" = true ]; then
    echo ""
    echo "🔍 Detecting changed macro services..."
    CHANGED=($(get_changed_services))

    if [ ${#CHANGED[@]} -eq 0 ]; then
        echo "  No code changes detected. Nothing to rebuild."
        exit 0
    fi

    echo "  Changed: ${CHANGED[*]}"
    echo ""

    total=${#CHANGED[@]}
    built=0

    for svc in "${CHANGED[@]}"; do
        built=$((built + 1))
        echo "📦 [$built/$total] Building $svc... ($(( total - built )) remaining)"
        $COMPOSE build $NO_CACHE "$svc"
        mark_built "$svc"
    done

    echo ""
    echo "🚀 Restarting changed services..."
    $COMPOSE up -d --force-recreate "${CHANGED[@]}"

    echo ""
    echo "============================================="
    echo "  ✅ Smart macro redeploy complete (${#CHANGED[@]} services rebuilt)"
    echo "============================================="
    exit 0
fi

# --- Restart only mode: restart all services without rebuilding ---
if [ "$RESTART_ONLY" = true ]; then
    echo ""
    echo "🔄 Restarting all macro services (no build)..."
    $COMPOSE stop
    $COMPOSE up -d

    echo ""
    echo "============================================="
    echo "  ✅ All macro services restarted (images unchanged)"
    echo "============================================="
    echo ""
    $COMPOSE ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || $COMPOSE ps
    exit 0
fi

# --- Full clean mode ---
echo ""
echo "🛑 Stopping all containers..."
$COMPOSE down --remove-orphans 2>/dev/null || true
docker stop exam-kafka 2>/dev/null || true
docker rm exam-kafka 2>/dev/null || true

echo "🧹 Removing ephemeral volumes (preserving vault_data and AI model caches)..."
docker volume ls --format '{{.Name}}' | grep -E 'postgres_data|rabbitmq_data|redis_data|keycloak_data|prometheus_data|grafana_data' | grep -v -E 'vault_data|ollama_data|indictrans2_cache' | xargs -r docker volume rm 2>/dev/null || true

echo ""
echo "🧹 Pruning old images..."
docker image prune -f 2>/dev/null || true

echo ""
INFRA_TARGETS="postgres redis vault keycloak"
if [ "$OBSERVABILITY" = true ]; then
    INFRA_TARGETS="$INFRA_TARGETS prometheus grafana jaeger"
fi
if [ "$AI" = true ]; then
    INFRA_TARGETS="$INFRA_TARGETS ollama litellm indictrans2"
fi

echo "🚀 Starting core infrastructure ($INFRA_TARGETS, RabbitMQ)..."
docker compose "${PROFILES_ARGS[@]}" -f docker-compose.yml up -d $INFRA_TARGETS
$COMPOSE up -d rabbitmq
echo "  Waiting for infrastructure to be healthy..."
docker compose -f docker-compose.yml up --wait -d postgres vault redis
$COMPOSE up --wait -d rabbitmq

echo ""
echo "📦 Building all macro services sequentially..."
TOTAL=${#ALL_SERVICES[@]}
BUILT=0
for svc in "${ALL_SERVICES[@]}"; do
    BUILT=$((BUILT + 1))
    echo "  [$BUILT/$TOTAL] Building $svc... ($((TOTAL - BUILT)) remaining)"
    $COMPOSE build $NO_CACHE "$svc"
    mark_built "$svc"
done

echo ""
echo "🚀 Starting all macro services..."
$COMPOSE up -d

echo ""
echo "============================================="
echo "  🎉 Full clean macro redeploy complete!"
echo "============================================="
echo ""
$COMPOSE ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || $COMPOSE ps
