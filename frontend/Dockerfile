# =============================================================================
# Multi-stage Dockerfile for Angular frontend
# Builds the Angular app and serves with Nginx
# =============================================================================

# --- Stage 1: Build with Node ---
FROM node:22-alpine AS builder

WORKDIR /app

# Copy package files first (layer caching)
COPY package.json package-lock.json* ./

# Install dependencies
RUN npm ci --ignore-scripts

# Copy source
COPY . .

# Build production bundle
RUN npm run build -- --configuration production

# --- Stage 2: Serve with Nginx ---
FROM nginx:alpine

# Remove default nginx config
RUN rm /etc/nginx/conf.d/default.conf

# Copy custom nginx config
COPY nginx.conf /etc/nginx/conf.d/

# Copy built Angular app from builder
COPY --from=builder /app/dist/exam-platform/browser /usr/share/nginx/html

# Healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -qO- http://localhost:80/health || exit 1

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
