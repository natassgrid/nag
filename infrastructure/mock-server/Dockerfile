# =============================================================================
# Multi-stage Dockerfile for NAG Mock Third-Party API Server
# Simulates DigiLocker, Aadhaar e-KYC, and MSG91 SMS gateway for E2E testing
# =============================================================================

FROM node:22-alpine AS runner

WORKDIR /app

# Copy package manifests
COPY package.json ./

# Install production dependencies
RUN npm install --omit=dev --no-audit --no-fund

# Copy server sources
COPY src/ ./src/

EXPOSE 8099

ENV NODE_ENV=production \
    MOCK_SERVER_PORT=8099

HEALTHCHECK --interval=15s --timeout=5s --start-period=5s --retries=3 \
    CMD wget -qO- http://127.0.0.1:8099/health || exit 1

CMD ["node", "src/server.js"]
