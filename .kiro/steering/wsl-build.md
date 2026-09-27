---
inclusion: auto
---

# WSL & IntelliJ Build Instructions

This project builds and tests through WSL Ubuntu 24.04 (`/mnt/c/Users/sheel/IdeaProjects/nag`) and IntelliJ IDEA build integration.

## Build Rules

1. **Java / Backend Builds**:
   - Primary: Use IntelliJ IDEA `build_project` tool for instant compilation diagnostics.
   - CLI / Terminal: Run Gradle commands inside WSL Ubuntu 24.04 (`/mnt/c/Users/sheel/IdeaProjects/nag`) or native `./gradlew.bat`.
2. **Frontend Builds (`frontend` & `candidate-frontend`)**:
   - Run local build, test, and lint commands.
3. **MANDATORY: Docker Build & Production Build for UI (CRITICAL)**:
   - Always run the complete production build and Docker build for any modified UI application before considering a task complete.
   - For `frontend` (Angular):
     - Production Build: `npm run build -- --configuration production` (inside `frontend/`)
     - Docker Build: `docker build -t exam-frontend:latest ./frontend`
   - For `candidate-frontend` (React):
     - Production Build: `npm run build` (inside `candidate-frontend/`)
     - Docker Build: `docker build -t candidate-frontend:latest ./candidate-frontend`

## Command Reference

### Backend (IntelliJ or WSL Gradle)
- **IntelliJ**: Call `build_project` (MCP server `idea`).
- **WSL Gradle**:
  ```powershell
  wsl -d Ubuntu-24.04 -e bash -lic "cd /mnt/c/Users/sheel/IdeaProjects/nag && ./gradlew build -x test --parallel --build-cache"
  wsl -d Ubuntu-24.04 -e bash -lic "cd /mnt/c/Users/sheel/IdeaProjects/nag && ./gradlew :backend:<service-name>:compileJava"
  ```
- **Backend Docker Build**:
  ```powershell
  docker build -f backend/Dockerfile.base -t exam-backend-base:latest ./backend
  docker build -f backend/Dockerfile -t exam-monolith:latest ./backend
  ```

### Angular (`frontend`)
```powershell
# Development / Production compilation checks
npm run build -- --configuration production

# MANDATORY Docker Image Build
docker build -t exam-frontend:latest ./frontend
```

### React (`candidate-frontend`)
```powershell
# Development / Production compilation checks
npm run build

# MANDATORY Docker Image Build
docker build -t candidate-frontend:latest ./candidate-frontend
```
