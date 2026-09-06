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
# Build and Deploy all services using Docker Compose
# Usage: ./build-and-deploy.sh [--no-cache] [--service <name>] [--observability] [--ai]
# =============================================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

NO_CACHE=""
SERVICE=""
OBSERVABILITY=false
AI=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --no-cache) NO_CACHE="--no-cache"; shift ;;
        --service) SERVICE="$2"; shift 2 ;;
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

echo "============================================="
echo "  Exam Platform — Docker Build & Deploy"
if [ "$OBSERVABILITY" = true ]; then
echo "  Observability: Enabled (Prometheus, Grafana, Jaeger)"
fi
if [ "$AI" = true ]; then
echo "  AI Pipeline:   Enabled (Ollama, LiteLLM, IndicTrans2)"
fi
echo "============================================="
echo ""

cd "$SCRIPT_DIR"

COMPOSE="docker compose ${PROFILES_ARGS[*]} -f docker-compose.yml -f docker-compose.services.yml"

# Step 1: Start infrastructure services
INFRA_TARGETS="postgres kafka vault keycloak"
if [ "$OBSERVABILITY" = true ]; then
    INFRA_TARGETS="$INFRA_TARGETS prometheus grafana jaeger"
fi
if [ "$AI" = true ]; then
    INFRA_TARGETS="$INFRA_TARGETS ollama litellm indictrans2"
fi

echo "▶ Starting infrastructure services ($INFRA_TARGETS)..."
docker compose "${PROFILES_ARGS[@]}" -f docker-compose.yml up -d $INFRA_TARGETS
echo "  Waiting for infrastructure to be healthy..."
docker compose -f docker-compose.yml up -d --wait postgres kafka vault

# Step 2: Start local Docker registry
echo ""
echo "▶ Starting local Docker registry..."
$COMPOSE up -d registry

# Step 3: Build and start application services
echo ""
if [ -n "$SERVICE" ]; then
    echo "▶ Building and starting service: $SERVICE"
    $COMPOSE build $NO_CACHE "$SERVICE"
    $COMPOSE up -d "$SERVICE"
else
    echo "▶ Building all application services sequentially (shared Gradle cache)..."
    SERVICES=(
        identity-service candidate-service question-bank-service
        examination-service paper-generator delivery-service response-service
        evaluation-service result-service audit-service notification-service
        admin-service analytics-service asset-service api-gateway
        frontend candidate-frontend
    )
    for svc in "${SERVICES[@]}"; do
        echo "  Building $svc..."
        $COMPOSE build $NO_CACHE "$svc"
    done
    echo ""
    echo "▶ Starting all application services..."
    $COMPOSE up -d
fi

# Step 4: Push to local registry
echo ""
echo "▶ Pushing images to local registry (localhost:5000)..."
SERVICES=(
    identity-service candidate-service question-bank-service
    examination-service paper-generator delivery-service response-service
    evaluation-service result-service audit-service notification-service
    admin-service analytics-service asset-service api-gateway
    frontend candidate-frontend
)

for svc in "${SERVICES[@]}"; do
    if docker image inspect "localhost:5000/exam/${svc}:latest" &>/dev/null; then
        docker push "localhost:5000/exam/${svc}:latest" 2>/dev/null || true
    fi
done

echo ""
echo "============================================="
echo "  ✓ Deployment complete!"
echo "============================================="
echo ""
echo "Service endpoints:"
echo "  API Gateway:     http://localhost:9000"
echo "  Admin UI (Web):  http://localhost:4200"
echo "  Candidate UI:    http://localhost:4300"
echo "  Identity:        http://localhost:8081"
echo "  Candidate API:   http://localhost:8082"
echo "  Question Bank:   http://localhost:8083"
echo "  Examination:     http://localhost:8085"
echo "  Paper Generator: http://localhost:8086"
echo "  Delivery:        http://localhost:8087"
echo "  Response:        http://localhost:8088"
echo "  Evaluation:      http://localhost:8089"
echo "  Result:          http://localhost:8090"
echo "  Audit:           http://localhost:8091"
echo "  Notification:    http://localhost:8092"
echo "  Admin:           http://localhost:8093"
echo "  Analytics:       http://localhost:8094"
echo "  Asset:           http://localhost:8095"
echo ""
echo "Infrastructure:"
echo "  Keycloak:        http://localhost:8080"
echo "  Vault:           http://localhost:8200"
if [ "$OBSERVABILITY" = true ]; then
echo "  Prometheus:      http://localhost:9090"
echo "  Grafana:         http://localhost:3000"
echo "  Jaeger:          http://localhost:16686"
fi
if [ "$AI" = true ]; then
echo "  Ollama:          http://localhost:11434"
echo "  LiteLLM:         http://localhost:4000"
echo "  IndicTrans2:     http://localhost:7860"
fi
echo "  Docker Registry: http://localhost:5000"
echo ""
echo "Useful commands:"
echo "  docker compose -f docker-compose.yml -f docker-compose.services.yml logs -f <service>"
echo "  docker compose -f docker-compose.yml -f docker-compose.services.yml ps"
echo "  docker compose -f docker-compose.yml -f docker-compose.services.yml down"
