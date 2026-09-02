# AGENTS.md - NAG Project Setup & Build Directives

## Core Build Directives

1. **Java / Backend Builds**:
   - Primary: Use IntelliJ IDEA MCP `build_project` tool (`idea` server) for immediate compilation feedback and IDE index sync.
   - Terminal: Use WSL Ubuntu-24.04 (`wsl -d Ubuntu-24.04 -e bash -lic "cd /mnt/c/Users/sheel/IdeaProjects/nag && ./gradlew ..."`).

2. **Frontend Builds (`frontend` - Angular & `candidate-frontend` - Vite/React)**:
   - Always run through WSL Ubuntu 24.04:
     - Angular: `wsl -d Ubuntu-24.04 -e bash -lic "cd /mnt/c/Users/sheel/IdeaProjects/nag/frontend && npm run build"`
     - Vite/React: `wsl -d Ubuntu-24.04 -e bash -lic "cd /mnt/c/Users/sheel/IdeaProjects/nag/candidate-frontend && npm run build"`

3. **Path Mapping**:
   - Windows: `C:\Users\sheel\IdeaProjects\nag`
   - WSL: `/mnt/c/Users/sheel/IdeaProjects/nag` (Distribution: `Ubuntu-24.04`)
