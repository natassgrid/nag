---
inclusion: auto
---

# WSL Build & Test Instructions

This project builds and tests exclusively through WSL (Windows Subsystem for Linux). The Windows filesystem is mounted at `/mnt/f/code/IdeaProjects/nag` inside WSL.

## Rules for Kiro

1. **All build, test, and lint commands MUST be executed via WSL**, not native PowerShell.
2. Prefix commands with `wsl` or use `wsl bash -c "..."` to run them in the default WSL distribution.
3. The project root inside WSL is: `/mnt/f/code/IdeaProjects/nag`

## Command Patterns

### Gradle (Backend)

```powershell
# Build all services (skip tests)
wsl bash -c "cd /mnt/f/code/IdeaProjects/nag && ./gradlew build -x test --parallel --build-cache"

# Build a single service
wsl bash -c "cd /mnt/f/code/IdeaProjects/nag && ./gradlew :backend:<service-name>:build --parallel --build-cache"

# Run tests for a single service
wsl bash -c "cd /mnt/f/code/IdeaProjects/nag && ./gradlew :backend:<service-name>:test --parallel"

# Run all tests
wsl bash -c "cd /mnt/f/code/IdeaProjects/nag && ./gradlew test --parallel"

# Compile only (fast check)
wsl bash -c "cd /mnt/f/code/IdeaProjects/nag && ./gradlew :backend:<service-name>:compileJava"

# Clean build
wsl bash -c "cd /mnt/f/code/IdeaProjects/nag && ./gradlew clean build --parallel --build-cache"
```

### Angular (Frontend)

```powershell
# Install dependencies
wsl bash -c "cd /mnt/f/code/IdeaProjects/nag/frontend && npm install"

# Build production
wsl bash -c "cd /mnt/f/code/IdeaProjects/nag/frontend && npm run build"

# Run lint
wsl bash -c "cd /mnt/f/code/IdeaProjects/nag/frontend && npm run lint"

# Run tests (single run, no watch)
wsl bash -c "cd /mnt/f/code/IdeaProjects/nag/frontend && npx ng test --watch=false --browsers=ChromeHeadless"
```

### Using Existing Scripts

```powershell
# Full backend build via script
wsl bash -c "cd /mnt/f/code/IdeaProjects/nag && bash scripts/build-backend.sh"

# Backend build, skip tests
wsl bash -c "cd /mnt/f/code/IdeaProjects/nag && bash scripts/build-backend.sh --skip-tests"

# Build single service
wsl bash -c "cd /mnt/f/code/IdeaProjects/nag && bash scripts/build-backend.sh --service question-bank-service"
```

## Verification After Code Changes

After modifying Java files, verify by compiling the affected service:
```powershell
wsl bash -c "cd /mnt/f/code/IdeaProjects/nag && ./gradlew :backend:<service-name>:compileJava"
```

After modifying Angular/TypeScript files, verify by building the frontend:
```powershell
wsl bash -c "cd /mnt/f/code/IdeaProjects/nag/frontend && npm run build"
```

## Important Notes

- Never use `./gradlew` directly in PowerShell — always route through `wsl`.
- Never use `npm` or `ng` directly in PowerShell for this project — always route through `wsl`.
- The WSL environment has Java 21, Node.js 22, and npm pre-installed (via `scripts/setup-wsl-dev.sh`).
- Use `--no-daemon` flag if Gradle daemon issues arise in WSL.
