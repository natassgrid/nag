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

# Copy local Gradle distribution from local repo to avoid network download during build
COPY backend/gradle/gradle-8.14.5-bin.zip /tmp/gradle-8.14.5-bin.zip

# Fix Windows CRLF line endings, make executable, point wrapper at local zip, then clean up temp zip
RUN sed -i 's/\r$//' gradlew && \
    chmod +x gradlew && \
    sed -i 's|distributionUrl=.*|distributionUrl=file\:/tmp/gradle-8.14.5-bin.zip|' gradle/wrapper/gradle-wrapper.properties && \
    sh ./gradlew --version && \
    rm -f /tmp/gradle-8.14.5-bin.zip

# Copy all backend source (needed for multi-module resolution)
COPY backend/ backend/

# Build the target service jar
RUN --mount=type=cache,target=/root/.gradle/caches \
    --mount=type=cache,target=/root/.gradle/wrapper \
    sh ./gradlew :backend:${SERVICE_NAME}:bootJar -x test --no-daemon

# --- Stage 2: Runtime ---
FROM eclipse-temurin:21-jre-alpine

ARG SERVICE_NAME
ENV SERVICE_NAME=${SERVICE_NAME}

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Create asset storage directory writable by appuser (used by asset-service)
RUN mkdir -p /data/asset-storage && chown -R appuser:appgroup /data /app

# Copy the built jar
COPY --from=builder /workspace/backend/${SERVICE_NAME}/build/libs/*.jar app.jar

# Run as non-root user
USER appuser

EXPOSE 8080 8081 8082 8083 8084 8085 8086 8087 8088 8089 8090 8091 9000

# Health check using wget (built into alpine)
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || \
        wget -qO- http://localhost:8081/actuator/health || \
        wget -qO- http://localhost:8083/actuator/health || \
        wget -qO- http://localhost:8087/actuator/health || \
        wget -qO- http://localhost:8089/actuator/health || \
        wget -qO- http://localhost:8091/actuator/health || \
        wget -qO- http://localhost:9000/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseG1GC", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
