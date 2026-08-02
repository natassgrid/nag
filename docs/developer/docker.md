# Docker Containerization & Local Compose — National Assessment Grid

## 1. Multi-Stage Dockerfile Strategy

All NAG services utilize optimized multi-stage Docker builds to produce lightweight, secure, non-root production images.

---

## 2. Frontend Dockerfile (`frontend/Dockerfile`)

```dockerfile
# Stage 1: Build Angular SPA
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build -- --configuration production

# Stage 2: Serve with NGINX Minimal
FROM nginx:alpine
COPY --from=build /app/dist/exam-platform /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

---

## 3. Backend Microservice Dockerfile (`backend/exam-service/Dockerfile`)

```dockerfile
# Stage 1: Build Java Jar
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

# Stage 2: Runtime Container
FROM eclipse-temurin:21-jr-alpine
RUN addgroup -S nag && adduser -S naguser -G nag
USER naguser:nag
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 4. Local Development Docker Compose (`docker-compose.dev.yml`)

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: nag-postgres
    environment:
      POSTGRES_DB: nag_exam_db
      POSTGRES_USER: nag_user
      POSTGRES_PASSWORD: nag_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  kafka:
    image: confluentinc/cp-kafka:7.6.0
    container_name: nag-kafka
    ports:
      - "9092:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: 'CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT'
      KAFKA_ADVERTISED_LISTENERS: 'PLAINTEXT://nag-kafka:29092,PLAINTEXT_HOST://localhost:9092'
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1

  redis:
    image: redis:7-alpine
    container_name: nag-redis
    ports:
      - "6379:6379"

volumes:
  postgres_data:
```
