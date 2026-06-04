# =============================================================================
# Multi-stage Dockerfile for Spring Boot microservices
# Usage: docker build --build-arg SERVICE_NAME=identity-service -t exam/identity-service .
# Must be built from the repo root (context = .)
# =============================================================================

# --- Stage 1: Build with Gradle ---
FROM eclipse-temurin:21-jdk AS builder

ARG SERVICE_NAME

WORKDIR /workspace

# Copy Gradle wrapper and config first (layer caching)
COPY gradlew gradlew.bat ./
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

# Copy all backend source (needed for multi-module resolution)
COPY backend/ backend/

# Fix Windows CRLF line endings and make gradlew executable
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Build the target service jar
RUN ./gradlew :backend:${SERVICE_NAME}:bootJar -x test --no-daemon --parallel

# --- Stage 2: Runtime ---
FROM eclipse-temurin:21-jre-alpine

ARG SERVICE_NAME
ENV SERVICE_NAME=${SERVICE_NAME}

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy the built jar
COPY --from=builder /workspace/backend/${SERVICE_NAME}/build/libs/*.jar app.jar

# Spring Boot Actuator health endpoint
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=5 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
