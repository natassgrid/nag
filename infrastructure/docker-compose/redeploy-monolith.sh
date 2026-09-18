#!/bin/bash
# SPDX-License-Identifier: AGPL-3.0-only
#
# National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
# Copyright (C) 2025 NAG Contributors
#
# =============================================================================
# Smart redeploy script for NAG Single JVM Monolith Mode
# Consolidates all 14 microservices + audit into 1 JVM Spring Boot runtime + Postgres + Redis.
#
# Usage:
#   ./redeploy-monolith.sh                  # Rebuild and start monolith (preserves DB volumes)
#   ./redeploy-monolith.sh --clean-db       # Rebuild & restart, explicitly deleting DB volumes
#   ./redeploy-monolith.sh --observability  # Start with Prometheus, Grafana, and Jaeger
#   ./redeploy-monolith.sh --ai             # Start with Ollama, LiteLLM, IndicTrans2
#   ./redeploy-monolith.sh --no-cache       # Force rebuild without Docker cache
#   ./redeploy-monolith.sh --restart        # Restart services without rebuilding
#   ./redeploy-monolith.sh --health         # Check health status of monolith
# =============================================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$SCRIPT_DIR"

NO_CACHE=""
RESTART_ONLY=false
HEALTH_CHECK=false
OBSERVABILITY=false
AI=false
CLEAN_DB=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --no-cache) NO_CACHE="--no-cache"; shift ;;
        --restart) RESTART_ONLY=true; shift ;;
        --health) HEALTH_CHECK=true; shift ;;
        --clean-db|--clean-volumes|--delete-db-volume|--reset-db|--drop-db) CLEAN_DB=true; shift ;;
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

COMPOSE="docker compose ${PROFILES_ARGS[*]} -f docker-compose.yml -f docker-compose.monolith.yml"

echo "============================================="
echo "  NAG Platform — Single JVM Monolith Mode"
echo "  Architecture: 1 JVM Monolith + Postgres + Redis"
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

    health_url="http://localhost:8080/actuator/health"
    health_response=$(curl -s --connect-timeout 3 --max-time 5 "$health_url" 2>/dev/null || echo "")

    if [ -z "$health_response" ]; then
        health_response=$(docker exec "$container" wget -qO- "$health_url" 2>/dev/null || echo "")
    fi

    if echo "$health_response" | grep -q -E '"status":"UP"|healthy'; then
        echo "  monolith-app: ✅ UP (Port 8080)"
    else
        echo "  monolith-app: ⚠️ $health_response"
    fi
    exit 0
fi

# --- Restart only mode ---
if [ "$RESTART_ONLY" = true ]; then
    echo ""
    echo "🔄 Restarting monolith services (no build)..."
    $COMPOSE stop
    $COMPOSE up -d
    echo ""
    echo "✅ Monolith restarted."
    exit 0
fi

# --- Full deploy mode ---
echo ""
echo "🛑 Stopping all containers..."
$COMPOSE down --remove-orphans 2>/dev/null || true

# --- Volume deletion based on explicit option ---
if [ "$CLEAN_DB" = true ]; then
    echo "🧹 Explicit option provided: Removing DB & ephemeral volumes (postgres_data, redis_data, keycloak_data)..."
    docker volume ls --format '{{.Name}}' | grep -E 'postgres_data|redis_data|keycloak_data' | grep -v -E 'vault_data|ollama_data|indictrans2_cache' | xargs -r docker volume rm 2>/dev/null || true
else
    echo "💾 Preserving DB and data volumes (pass --clean-db or --delete-db-volume to remove)..."
fi

echo ""
INFRA_TARGETS="postgres redis vault keycloak"
if [ "$OBSERVABILITY" = true ]; then
    INFRA_TARGETS="$INFRA_TARGETS prometheus grafana jaeger"
fi
if [ "$AI" = true ]; then
    INFRA_TARGETS="$INFRA_TARGETS ollama litellm indictrans2"
fi

echo "🚀 Starting infrastructure ($INFRA_TARGETS)..."
docker compose "${PROFILES_ARGS[@]}" -f docker-compose.yml up -d $INFRA_TARGETS
echo "  Waiting for infrastructure to be healthy..."
docker compose -f docker-compose.yml up --wait -d postgres vault redis

echo ""
echo "📦 Building monolith-app and frontends..."
$COMPOSE build $NO_CACHE monolith-app frontend candidate-frontend

echo ""
echo "🚀 Starting monolith stack..."
$COMPOSE up -d

echo ""
echo "============================================="
echo "  🎉 Single JVM Monolith redeploy complete!"
echo "============================================="
echo ""
$COMPOSE ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || $COMPOSE ps
