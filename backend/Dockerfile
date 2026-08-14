# =============================================================================
# Multi-stage Dockerfile for Spring Boot microservices
# Usage: docker build --build-arg SERVICE_NAME=identity-service -t exam/identity-service .
# Must be built from the repo root (context = .)
# Requires: docker build -f backend/Dockerfile.base -t exam/builder-base .
# =============================================================================

# --- Stage 1: Build with Gradle (uses pre-built base with Gradle installed) ---
FROM exam/builder-base:latest AS builder

ARG SERVICE_NAME

WORKDIR /workspace

# Copy build config (layer caching)
COPY build.gradle settings.gradle ./

# Copy all backend source (needed for multi-module resolution)
COPY backend/ backend/

# Build the target service jar
RUN --mount=type=cache,target=/root/.gradle/caches \
    --mount=type=cache,target=/root/.gradle/wrapper \
    gradle :backend:${SERVICE_NAME}:bootJar -x test --no-daemon

# --- Stage 2: Runtime ---
FROM eclipse-temurin:21-jre-alpine

ARG SERVICE_NAME
ENV SERVICE_NAME=${SERVICE_NAME}

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Create asset storage directory writable by appuser (used by asset-service)
RUN mkdir -p /data/asset-storage && chown appuser:appgroup /data/asset-storage

# Copy the built jar
COPY --from=builder /workspace/backend/${SERVICE_NAME}/build/libs/*.jar app.jar

# Spring Boot Actuator health endpoint
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=5 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
