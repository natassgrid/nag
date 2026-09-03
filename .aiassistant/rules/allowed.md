---
apply: always
---

# Allowed Commands and Operations

The AI assistant is permitted and encouraged to execute the following commands without requiring additional confirmation:

## 1. Gradle Build & Test Operations
- Execute Gradle wrapper commands directly or via shell scripts:
  - `./gradlew test`, `.\gradlew.bat test`
  - `./gradlew build`, `.\gradlew.bat build`
  - `./gradlew compileJava`, `.\gradlew.bat compileJava`
  - Subproject/module specific tasks (e.g., `./gradlew :backend:<service-name>:test`, `./gradlew :backend:<service-name>:build`)
  - Clean and check commands (`./gradlew clean`, `./gradlew check`)

## 2. NPM & Frontend Build Operations
- Execute npm and Angular CLI commands in frontend directories:
  - `npm run build`, `cmd /c "npm run build"`
  - `npm test`, `cmd /c "npm test"`
  - `npm run lint`, `cmd /c "npm run lint"`
  - `npx ng build`, `npx ng test`, `npx ng lint`
  - `npm install`, `npm ci` when dependencies are modified

## 3. Git Operations
- Execute version control commands for inspecting, staging, committing, and syncing changes:
  - `git status`, `git status -s`
  - `git diff`, `git diff --cached`
  - `git add <files>`
  - `git commit -m "<message>"`
  - `git push`, `git push origin <branch>`
  - `git log`, `git log -n <count>`
  - `git branch`, `git checkout <branch>`, `git switch <branch>`
