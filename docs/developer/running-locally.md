# Running Locally — National Assessment Grid

## 1. Quick Start (Development Environment)

Follow these steps to run the complete NAG platform locally for development and testing.

---

## 2. Step 1: Start Infrastructure Services (Docker Compose)

Spin up PostgreSQL, Apache Kafka, Keycloak, and Redis background services:

```bash
# Navigate to project root
cd f:\code\IdeaProjects\nag

# Start infrastructure dependencies in background
docker compose -f docker-compose.dev.yml up -d
```

Verify that containers are healthy:
```bash
docker ps
```
Required services running: `nag-postgres`, `nag-kafka`, `nag-keycloak`, `nag-redis`.

---

## 3. Step 2: Run Backend Microservices

In your terminal or IDE (IntelliJ IDEA / Eclipse):

```bash
# Run Spring Boot backend microservice
cd backend/exam-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The backend server will start on `http://localhost:8080` with API docs available at `http://localhost:8080/swagger-ui.html`.

---

## 4. Step 3: Run Angular Frontend Application

1. Install frontend NPM dependencies:
   ```bash
   cd frontend
   npm install
   ```

2. Start Angular dev server:
   ```bash
   npm run start
   # or
   npx ng serve --open
   ```

3. Open your browser and navigate to `http://localhost:4200`.

---

## 5. Development Proxy & CORS Configuration

During local development, frontend HTTP requests sent to `/api/v1/...` are proxied to the backend at `http://localhost:8080` via `frontend/src/proxy.conf.json`:

```json
{
  "/api": {
    "target": "http://localhost:8080",
    "secure": false,
    "changeOrigin": true,
    "logLevel": "debug"
  }
}
```
