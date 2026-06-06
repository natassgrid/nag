#!/bin/bash
# =============================================================================
# Build and Deploy all services using Docker Compose
# Usage: ./build-and-deploy.sh [--no-cache] [--service <name>]
# =============================================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

NO_CACHE=""
SERVICE=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --no-cache) NO_CACHE="--no-cache"; shift ;;
        --service) SERVICE="$2"; shift 2 ;;
        *) echo "Unknown option: $1"; exit 1 ;;
    esac
done

echo "============================================="
echo "  Exam Platform — Docker Build & Deploy"
echo "============================================="
echo ""

cd "$SCRIPT_DIR"

# Step 1: Start infrastructure services
echo "▶ Starting infrastructure services..."
docker compose -f docker-compose.yml up -d postgres kafka vault keycloak jaeger prometheus grafana
echo "  Waiting for infrastructure to be healthy..."
docker compose -f docker-compose.yml up -d --wait postgres kafka vault

# Step 2: Start local Docker registry
echo ""
echo "▶ Starting local Docker registry..."
docker compose -f docker-compose.yml -f docker-compose.services.yml up -d registry

# Step 3: Build and start application services
echo ""
if [ -n "$SERVICE" ]; then
    echo "▶ Building and starting service: $SERVICE"
    docker compose -f docker-compose.yml -f docker-compose.services.yml build $NO_CACHE "$SERVICE"
    docker compose -f docker-compose.yml -f docker-compose.services.yml up -d "$SERVICE"
else
    echo "▶ Building all application services sequentially (shared Gradle cache)..."
    SERVICES=(
        identity-service candidate-service question-bank-service translation-service
        examination-service paper-generator delivery-service response-service
        evaluation-service result-service audit-service notification-service
        admin-service analytics-service api-gateway
    )
    for svc in "${SERVICES[@]}"; do
        echo "  Building $svc..."
        docker compose -f docker-compose.yml -f docker-compose.services.yml build $NO_CACHE "$svc"
    done
    echo ""
    echo "▶ Starting all application services..."
    docker compose -f docker-compose.yml -f docker-compose.services.yml up -d
fi

# Step 4: Push to local registry
echo ""
echo "▶ Pushing images to local registry (localhost:5000)..."
SERVICES=(
    identity-service candidate-service question-bank-service translation-service
    examination-service paper-generator delivery-service response-service
    evaluation-service result-service audit-service notification-service
    admin-service analytics-service api-gateway
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
echo "  Identity:        http://localhost:8081"
echo "  Candidate:       http://localhost:8082"
echo "  Question Bank:   http://localhost:8083"
echo "  Translation:     http://localhost:8084"
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
echo ""
echo "Infrastructure:"
echo "  Keycloak:        http://localhost:8080"
echo "  Vault:           http://localhost:8200"
echo "  Prometheus:      http://localhost:9090"
echo "  Grafana:         http://localhost:3000"
echo "  Jaeger:          http://localhost:16686"
echo "  Docker Registry: http://localhost:5000"
echo ""
echo "Useful commands:"
echo "  docker compose -f docker-compose.yml -f docker-compose.services.yml logs -f <service>"
echo "  docker compose -f docker-compose.yml -f docker-compose.services.yml ps"
echo "  docker compose -f docker-compose.yml -f docker-compose.services.yml down"
