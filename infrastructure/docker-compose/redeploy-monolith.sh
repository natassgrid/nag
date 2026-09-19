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
