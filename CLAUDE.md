# NAG Project Instructions & Build Setup

## Build & Environment Rules

### 1. Java / Backend Builds
- **Primary Method (IntelliJ IDEA Integration)**: Use the IntelliJ IDEA build feature (`build_project` tool via `idea` MCP server) to build the project or compile files and check for compilation errors/warnings.
- **CLI / Gradle Method (via WSL)**: When running Gradle commands from the terminal, ALWAYS route through WSL Ubuntu 24.04:
  ```powershell
  # Compile all Java services (skip tests)
  wsl -d Ubuntu-24.04 -e bash -lic "cd /mnt/c/Users/sheel/IdeaProjects/nag && ./gradlew build -x test --parallel --build-cache"

  # Fast compile check for a single service
  wsl -d Ubuntu-24.04 -e bash -lic "cd /mnt/c/Users/sheel/IdeaProjects/nag && ./gradlew :backend:<service-name>:compileJava"

  # Run tests for a single service
  wsl -d Ubuntu-24.04 -e bash -lic "cd /mnt/c/Users/sheel/IdeaProjects/nag && ./gradlew :backend:<service-name>:test --parallel"

  # Clean build
  wsl -d Ubuntu-24.04 -e bash -lic "cd /mnt/c/Users/sheel/IdeaProjects/nag && ./gradlew clean build --parallel --build-cache"
  ```

---

### 2. Frontend Builds (`frontend` - Angular)
- **All build, test, and lint commands MUST run through WSL Ubuntu 24.04**:
  ```powershell
  # Build production
  wsl -d Ubuntu-24.04 -e bash -lic "cd /mnt/c/Users/sheel/IdeaProjects/nag/frontend && npm run build"

  # Run lint
  wsl -d Ubuntu-24.04 -e bash -lic "cd /mnt/c/Users/sheel/IdeaProjects/nag/frontend && npm run lint"

  # Run tests (single run)
  wsl -d Ubuntu-24.04 -e bash -lic "cd /mnt/c/Users/sheel/IdeaProjects/nag/frontend && npx ng test --watch=false --browsers=ChromeHeadless"
  ```

---

### 3. Candidate Frontend Builds (`candidate-frontend` - Vite/React)
- **All build, test, and lint commands MUST run through WSL Ubuntu 24.04**:
  ```powershell
  # Build production
  wsl -d Ubuntu-24.04 -e bash -lic "cd /mnt/c/Users/sheel/IdeaProjects/nag/candidate-frontend && npm run build"

  # Run lint
  wsl -d Ubuntu-24.04 -e bash -lic "cd /mnt/c/Users/sheel/IdeaProjects/nag/candidate-frontend && npm run lint"
  ```

---

## Environment & Path Mapping
- **Host OS**: Windows
- **WSL Distribution**: `Ubuntu-24.04`
- **Windows Root**: `C:\Users\sheel\IdeaProjects\nag`
- **WSL Root**: `/mnt/c/Users/sheel/IdeaProjects/nag`
- **WSL Installed Toolchains**:
  - Java: OpenJDK 21
  - Node.js: v22.x
  - Gradle: 8.14.5
