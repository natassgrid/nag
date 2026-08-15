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
#   ./redeploy-clean.sh                  # Full clean: tear down ALL, rebuild ALL, start ALL
#   ./redeploy-clean.sh --service <name> # Rebuild and restart ONE service (keeps others running)
#   ./redeploy-clean.sh --smart          # Only rebuild services with code changes (uses git diff)
#   ./redeploy-clean.sh --no-cache       # Force rebuild without Docker cache
#   ./redeploy-clean.sh --restart        # Restart ALL services without rebuilding (keeps images)
#   ./redeploy-clean.sh --restart --service <name>  # Restart ONE service without rebuilding
# =============================================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$SCRIPT_DIR"

NO_CACHE=""
SERVICE=""
SMART=false
RESTART_ONLY=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --no-cache) NO_CACHE="--no-cache"; shift ;;
        --service) SERVICE="$2"; shift 2 ;;
        --smart) SMART=true; shift ;;
        --restart) RESTART_ONLY=true; shift ;;
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

    for svc in "${CHANGED[@]}"; do
        echo "▶ Building $svc..."
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
echo "▶ Stopping all containers and removing volumes..."
$COMPOSE down -v --remove-orphans 2>/dev/null || true

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
for svc in "${ALL_SERVICES[@]}"; do
    echo "  Building $svc..."
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
