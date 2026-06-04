#!/bin/bash
# =============================================================================
# Clean redeploy — tears down everything, rebuilds, and starts fresh
# Usage: ./redeploy-clean.sh [--no-cache] [--service <name>]
# =============================================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

NO_CACHE=""
SERVICE=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --no-cache) NO_CACHE="--no-cache"; shift ;;
        --service) SERVICE="$2"; shift 2 ;;
        *) echo "Unknown option: $1"; exit 1 ;;
    esac
done

COMPOSE="docker compose -f docker-compose.yml -f docker-compose.services.yml"

echo "============================================="
echo "  Exam Platform — Clean Redeploy"
echo "============================================="

# Step 1: Stop and remove all containers + volumes
echo ""
echo "▶ Stopping all containers and removing volumes..."
$COMPOSE down -v --remove-orphans 2>/dev/null || true

# Step 2: Remove dangling images from previous builds
echo ""
echo "▶ Pruning old images..."
docker image prune -f 2>/dev/null || true

# Step 3: Start infrastructure
echo ""
echo "▶ Starting infrastructure (postgres, kafka, redis, vault, keycloak, jaeger, prometheus, grafana)..."
docker compose -f docker-compose.yml up -d
echo "  Waiting for infrastructure to be healthy..."
docker compose -f docker-compose.yml up --wait -d postgres kafka vault

# Step 4: Build services
echo ""
if [ -n "$SERVICE" ]; then
    echo "▶ Building service: $SERVICE"
    $COMPOSE build $NO_CACHE "$SERVICE"
else
    echo "▶ Building all services sequentially..."
    SERVICES=(
        identity-service candidate-service question-bank-service translation-service
        examination-service paper-generator delivery-service response-service
        evaluation-service result-service audit-service notification-service
        admin-service analytics-service api-gateway
    )
    for svc in "${SERVICES[@]}"; do
        echo "  Building $svc..."
        $COMPOSE build $NO_CACHE "$svc"
    done
fi

# Step 5: Start services
echo ""
if [ -n "$SERVICE" ]; then
    echo "▶ Starting service: $SERVICE"
    $COMPOSE up -d "$SERVICE"
else
    echo "▶ Starting all services..."
    $COMPOSE up -d
fi

# Step 6: Show status
echo ""
echo "============================================="
echo "  ✓ Clean redeploy complete!"
echo "============================================="
echo ""
$COMPOSE ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || $COMPOSE ps
