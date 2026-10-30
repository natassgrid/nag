#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "Checking sysctl vm.max_map_count requirement (>= 262144)..."
CURRENT_MAX_MAP=$(sysctl -n vm.max_map_count 2>/dev/null || echo 0)
if [ "$CURRENT_MAX_MAP" -lt 262144 ]; then
  echo "WARNING: vm.max_map_count is $CURRENT_MAX_MAP. SonarQube ElasticSearch requires >= 262144."
  echo "Run: sudo sysctl -w vm.max_map_count=262144"
fi

echo "Starting SonarQube & PostgreSQL containers..."
docker compose up -d

echo "Waiting for SonarQube to become healthy/available at http://localhost:9000..."
until curl -s http://localhost:9000/api/system/status | grep -q "UP"; do
  printf "."
  sleep 3
done

echo ""
echo "=========================================================="
echo "SonarQube is UP and running!"
echo "Web UI: http://localhost:9000"
echo "Default credentials: admin / admin"
echo "=========================================================="
