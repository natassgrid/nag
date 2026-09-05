#!/bin/bash

# SPDX-License-Identifier: AGPL-3.0-only
#
# National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
# Copyright (C) 2025 NAG Contributors
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as published
# by the Free Software Foundation, version 3 of the License.

# =============================================================================
# Smart redeploy — only rebuilds services whose code has changed
# Usage:
#   ./redeploy-clean.sh                  # Full clean: tear down ALL, rebuild ALL, start ALL (preserves vault_data)
#   ./redeploy-clean.sh --service <name> # Rebuild and restart ONE service (keeps others running)
#   ./redeploy-clean.sh --smart          # Only rebuild services with code changes (uses git diff)
#   ./redeploy-clean.sh --no-cache       # Force rebuild without Docker cache
#   ./redeploy-clean.sh --restart        # Restart ALL services without rebuilding (keeps images)
#   ./redeploy-clean.sh --restart --service <name>  # Restart ONE service without rebuilding
#   ./redeploy-clean.sh --health         # Check health status of all running services
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

while [[ $# -gt 0 ]]; do
    case $1 in
        --no-cache) NO_CACHE="--no-cache"; shift ;;
        --service) SERVICE="$2"; shift 2 ;;
        --smart) SMART=true; shift ;;
        --restart) RESTART_ONLY=true; shift ;;
        --health) HEALTH_CHECK=true; shift ;;
        *) echo "Unknown option: $1"; exit 1 ;;
    esac
done

COMPOSE="docker compose -f docker-compose.yml -f docker-compose.services.yml"

ALL_SERVICES=(
    identity-service candidate-service question-bank-service translation-service
    examination-service paper-generator delivery-service response-service
    evaluation-service result-service audit-service notification-service
    admin-service analytics-service asset-service api-gateway
)

# --- Detect which services have code changes (git-based) ---
get_changed_services() {
    local changed=()
    cd "$PROJECT_ROOT"

    for svc in "${ALL_SERVICES[@]}"; do
        # Check if service source files changed since last image was built
        # Compare against the git hash stored in a marker file
        local marker="/tmp/.exam-build-marker-${svc}"
        local current_hash
        current_hash=$(git log -1 --format="%H" -- "backend/${svc}/src" "backend/shared-lib/src" "build.gradle" 2>/dev/null || echo "none")

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
    git log -1 --format="%H" -- "backend/${svc}/src" "backend/shared-lib/src" "build.gradle" 2>/dev/null > "/tmp/.exam-build-marker-${svc}"
    cd "$SCRIPT_DIR"
}

echo "============================================="
echo "  Exam Platform — Redeploy"
echo "============================================="

# --- Ensure builder base image exists ---
ensure_builder_base() {
    if ! docker image inspect exam/builder-base:latest >/dev/null 2>&1; then
        echo "▶ Building builder base image (one-time)..."
        cd "$PROJECT_ROOT"
        docker build -f backend/Dockerfile.base -t exam/builder-base:latest .
        cd "$SCRIPT_DIR"
        echo "✓ Builder base image ready."
    fi
}

ensure_builder_base

# --- Health check mode ---
if [ "$HEALTH_CHECK" = true ]; then
    echo ""
    echo "▶ Checking health status of all services..."
    echo ""

    # Service name → port mapping
    declare -A SERVICE_PORTS=(
        [identity-service]=8081
        [candidate-service]=8082
        [question-bank-service]=8083
        [translation-service]=8084
        [examination-service]=8085
        [paper-generator]=8086
        [delivery-service]=8087
        [response-service]=8088
        [evaluation-service]=8089
        [result-service]=8090
        [audit-service]=8091
        [notification-service]=8092
        [admin-service]=8093
        [analytics-service]=8094
        [asset-service]=8095
        [api-gateway]=9000
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
            printf "  %-25s %-12s %-8s %s\n" "$svc" "⬇ DOWN" "$port" "Container not running"
            DOWN=$((DOWN + 1))
            continue
        fi

        # First check Docker's own healthcheck status (most reliable)
        docker_health=$(docker inspect --format='{{.State.Health.Status}}' "$container" 2>/dev/null || echo "none")

        if [ "$docker_health" = "starting" ]; then
            printf "  %-25s %-12s %-8s %s\n" "$svc" "⏳ STARTING" "$port" "Still initializing..."
            UNHEALTHY=$((UNHEALTHY + 1))
            continue
        fi

        # Query health endpoint — try host first, fallback to docker exec
        health_response=$(curl -s --connect-timeout 3 --max-time 5 "http://localhost:${port}/actuator/health" 2>/dev/null || echo "")

        if [ -z "$health_response" ]; then
            health_response=$(docker exec "$container" wget -qO- "http://localhost:${port}/actuator/health" 2>/dev/null || echo "")
        fi

        if [ -z "$health_response" ]; then
            printf "  %-25s %-12s %-8s %s\n" "$svc" "⚠ NO RESP" "$port" "No response from actuator"
            UNHEALTHY=$((UNHEALTHY + 1))
            continue
        fi

        # Parse top-level status from JSON (last "status" field or the one after "groups")
        # The top-level status in Spring Boot actuator is the outermost "status" field
        status=$(echo "$health_response" | grep -o '"status":"[^"]*"' | tail -1 | cut -d'"' -f4)

        if [ "$status" = "UP" ]; then
            # Get component statuses
            components=$(echo "$health_response" | grep -o '"[a-zA-Z]*":{"status":"[^"]*"' | \
                sed 's/"\([^"]*\)":{"status":"\([^"]*\)"/\1:\2/g' | tr '\n' ' ')
            printf "  %-25s %-12s %-8s %s\n" "$svc" "✓ UP" "$port" "$components"
            HEALTHY=$((HEALTHY + 1))
        elif [ "$status" = "DOWN" ]; then
            components=$(echo "$health_response" | grep -o '"[a-zA-Z]*":{"status":"DOWN"' | \
                sed 's/"\([^"]*\)":{"status":"DOWN"/\1:DOWN/g' | tr '\n' ' ')
            printf "  %-25s %-12s %-8s %s\n" "$svc" "✗ DOWN" "$port" "$components"
            UNHEALTHY=$((UNHEALTHY + 1))
        else
            printf "  %-25s %-12s %-8s %s\n" "$svc" "⚠ ${status:-UNKNOWN}" "$port" ""
            UNHEALTHY=$((UNHEALTHY + 1))
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
        echo "▶ Restarting service (no build): $SERVICE"
        $COMPOSE stop "$SERVICE"
        $COMPOSE up -d --no-recreate "$SERVICE" 2>/dev/null || $COMPOSE up -d "$SERVICE"
        echo ""
        echo "✓ $SERVICE restarted (image unchanged)."
        exit 0
    fi

    echo "▶ Rebuilding service: $SERVICE"
    $COMPOSE build $NO_CACHE "$SERVICE"
    echo ""
    echo "▶ Restarting service: $SERVICE"
    $COMPOSE up -d --force-recreate "$SERVICE"
    mark_built "$SERVICE"
    echo ""
    echo "✓ $SERVICE redeployed."
    exit 0
fi

# --- Smart mode: only rebuild changed services ---
if [ "$SMART" = true ]; then
    echo ""
    echo "▶ Detecting changed services..."
    CHANGED=($(get_changed_services))

    if [ ${#CHANGED[@]} -eq 0 ]; then
        echo "  No code changes detected. Nothing to rebuild."
        exit 0
    fi

    echo "  Changed: ${CHANGED[*]}"
    echo ""

    local total=${#CHANGED[@]}
    local built=0

    for svc in "${CHANGED[@]}"; do
        built=$((built + 1))
        echo "▶ [$built/$total] Building $svc... ($(( total - built )) remaining)"
        $COMPOSE build $NO_CACHE "$svc"
        mark_built "$svc"
    done

    echo ""
    echo "▶ Restarting changed services..."
    $COMPOSE up -d --force-recreate "${CHANGED[@]}"

    echo ""
    echo "============================================="
    echo "  ✓ Smart redeploy complete (${#CHANGED[@]} services rebuilt)"
    echo "============================================="
    exit 0
fi

# --- Restart only mode: restart all services without rebuilding ---
if [ "$RESTART_ONLY" = true ]; then
    echo ""
    echo "▶ Restarting all services (no build)..."
    $COMPOSE stop
    $COMPOSE up -d

    echo ""
    echo "============================================="
    echo "  ✓ All services restarted (images unchanged)"
    echo "============================================="
    echo ""
    $COMPOSE ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || $COMPOSE ps
    exit 0
fi

# --- Full clean mode ---
echo ""
echo "▶ Stopping all containers..."
$COMPOSE down --remove-orphans 2>/dev/null || true

echo "▶ Removing ephemeral volumes (preserving vault_data and AI model caches)..."
docker volume ls --format '{{.Name}}' | grep -E 'postgres_data|kafka_data|redis_data|keycloak_data|prometheus_data|grafana_data' | grep -v -E 'vault_data|ollama_data|indictrans2_cache' | xargs -r docker volume rm 2>/dev/null || true

echo ""
echo "▶ Pruning old images..."
docker image prune -f 2>/dev/null || true

echo ""
echo "▶ Starting infrastructure..."
docker compose -f docker-compose.yml up -d
echo "  Waiting for infrastructure to be healthy..."
docker compose -f docker-compose.yml up --wait -d postgres kafka vault

echo ""
echo "▶ Building all services sequentially..."
TOTAL=${#ALL_SERVICES[@]}
BUILT=0
for svc in "${ALL_SERVICES[@]}"; do
    BUILT=$((BUILT + 1))
    echo "  [$BUILT/$TOTAL] Building $svc... ($((TOTAL - BUILT)) remaining)"
    $COMPOSE build $NO_CACHE "$svc"
    mark_built "$svc"
done

echo ""
echo "▶ Starting all services..."
$COMPOSE up -d

echo ""
echo "============================================="
echo "  ✓ Full clean redeploy complete!"
echo "============================================="
echo ""
$COMPOSE ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || $COMPOSE ps
