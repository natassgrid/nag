# syntax=docker/dockerfile:1.7
# =============================================================================
# Multi-stage Dockerfile for Nx Frontend Workspace Applications
# Builds specified Angular/Nx app and serves with high-performance Nginx
# Uses persistent BuildKit cache mounts for NPM packages and Nx build cache
# =============================================================================

# --- Stage 1: Dependency Installation (Shared Cache Layer) ---
FROM node:22-alpine AS deps

WORKDIR /app

# Copy package manifests first for maximum Docker layer caching
COPY package.json package-lock.json* ./

# Install workspace dependencies using persistent Docker BuildKit npm cache
RUN --mount=type=cache,target=/root/.npm,sharing=shared \
    npm ci --legacy-peer-deps --ignore-scripts --prefer-offline

# Copy full workspace source code
COPY . .

# --- Stage 2: Application Builder ---
FROM deps AS builder

ARG APP_NAME=admin-portal

# Build targeted production application bundle using persistent Nx cache
RUN --mount=type=cache,target=/root/.npm,sharing=shared \
    --mount=type=cache,target=/app/.nx/cache,sharing=shared \
    npx nx build ${APP_NAME} --configuration=production

# --- Stage 3: Serve with Nginx ---
FROM nginx:alpine

ARG APP_NAME=admin-portal

# Remove default nginx configuration
RUN rm /etc/nginx/conf.d/default.conf

# Copy custom reverse proxy nginx config
COPY nginx.conf /etc/nginx/conf.d/

# Copy built application assets from builder stage
COPY --from=builder /app/dist/apps/${APP_NAME}/browser /usr/share/nginx/html

# Healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -qO- http://127.0.0.1:80/health || exit 1

EXPOSE 80
