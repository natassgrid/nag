---
inclusion: auto
---

# WSL & IntelliJ Build Instructions

This project builds and tests through WSL Ubuntu 24.04 (`/mnt/c/Users/sheel/IdeaProjects/nag`) and IntelliJ IDEA build integration.

## Build Rules

1. **Java / Backend Builds**:
   - Primary: Use IntelliJ IDEA `build_project` tool for instant compilation diagnostics.
   - CLI / Terminal: Run Gradle commands inside WSL Ubuntu 24.04 (`/mnt/c/Users/sheel/IdeaProjects/nag`).
2. **Frontend Builds (`frontend` & `candidate-frontend`)**:
   - All build, test, and lint commands MUST run via WSL Ubuntu 24.04.

## Command Reference

### Backend (IntelliJ or WSL Gradle)
- **IntelliJ**: Call `build_project` (MCP server `idea`).
- **WSL Gradle**:
  ```powershell
  wsl -d Ubuntu-24.04 -e bash -lic "cd /mnt/c/Users/sheel/IdeaProjects/nag && ./gradlew build -x test --parallel --build-cache"
  wsl -d Ubuntu-24.04 -e bash -lic "cd /mnt/c/Users/sheel/IdeaProjects/nag && ./gradlew :backend:<service-name>:compileJava"
  ```

### Angular (`frontend`)
```powershell
wsl -d Ubuntu-24.04 -e bash -lic "cd /mnt/c/Users/sheel/IdeaProjects/nag/frontend && npm run build"
wsl -d Ubuntu-24.04 -e bash -lic "cd /mnt/c/Users/sheel/IdeaProjects/nag/frontend && npm run lint"
```

### React (`candidate-frontend`)
```powershell
wsl -d Ubuntu-24.04 -e bash -lic "cd /mnt/c/Users/sheel/IdeaProjects/nag/candidate-frontend && npm run build"
wsl -d Ubuntu-24.04 -e bash -lic "cd /mnt/c/Users/sheel/IdeaProjects/nag/candidate-frontend && npm run lint"
```
