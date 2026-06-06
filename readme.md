# From the project root, build and start everything:
docker compose -f infrastructure/docker-compose/docker-compose.yml \
               -f infrastructure/docker-compose/docker-compose.services.yml up --build

# Or build a single service:
docker compose -f infrastructure/docker-compose/docker-compose.yml \
               -f infrastructure/docker-compose/docker-compose.services.yml build identity-service

# Or use the convenience script:
chmod +x infrastructure/docker-compose/build-and-deploy.sh
./infrastructure/docker-compose/build-and-deploy.sh


chmod +x infrastructure/docker-compose/redeploy-clean.sh

# Full clean redeploy (all services)
./infrastructure/docker-compose/redeploy-clean.sh

# Clean redeploy with no Docker cache (force rebuild from scratch)
./infrastructure/docker-compose/redeploy-clean.sh --no-cache

# Clean redeploy a single service
./infrastructure/docker-compose/redeploy-clean.sh --service identity-service
