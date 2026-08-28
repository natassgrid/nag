# =============================================================================
# Multi-stage Dockerfile for Spring Boot microservices
# Usage: docker build --build-arg SERVICE_NAME=identity-service -t exam/identity-service .
# Must be built from the repo root (context = .)
# Requires: docker build -f backend/Dockerfile.base -t exam/builder-base .
# =============================================================================

# --- Stage 1: Build with Gradle (uses project's Gradle wrapper) ---
FROM exam/builder-base:latest AS builder

ARG SERVICE_NAME

WORKDIR /workspace

# Copy Gradle wrapper and build config (layer caching)
COPY gradlew ./
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

# Copy local Gradle distribution to avoid network download during build
COPY backend/gradle/gradle-8.14.5-bin.zip /tmp/gradle-8.14.5-bin.zip

# Fix Windows CRLF line endings, make executable, point wrapper at local zip
RUN sed -i 's/\r$//' gradlew && \
    chmod +x gradlew && \
    sed -i 's|distributionUrl=.*|distributionUrl=file\\:/tmp/gradle-8.14.5-bin.zip|' gradle/wrapper/gradle-wrapper.properties && \
    ./gradlew --version

# Copy all backend source (needed for multi-module resolution)
COPY backend/ backend/

# Build the target service jar
RUN --mount=type=cache,target=/root/.gradle/caches \
    --mount=type=cache,target=/root/.gradle/wrapper \
    ./gradlew :backend:${SERVICE_NAME}:bootJar -x test --no-daemon

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
