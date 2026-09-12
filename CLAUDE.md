# NAG Project Instructions & Build Setup

## File Editing Safety Guards & Overwrite Prevention

> [!IMPORTANT]
> **STRICT RULE FOR ALL AI ASSISTANTS**:
> `client_edit_file` replaces the **ENTIRE** content of a file. It is NOT a partial patch tool.

1. **Mandatory Full File Inspection**:
   - Before modifying ANY file, use `view_file` to read the entire file from line 1 to end-of-file.
   - Never assume what other lines or sections exist in the file.

2. **No Partial Snippet Writes**:
   - The `code_content` passed to `client_edit_file` MUST contain the 100% complete file text including all headers, license comments, seed data, imports, configuration keys, and trailing blocks.
   - Passing only the modified lines or snippet to `client_edit_file` is strictly forbidden as it destroys the rest of the file.

3. **Frontend-Specific Preservation Rules**:
   - **HTML Templates (`*.component.html`, `*.html`, `*.tsx`, `*.jsx`)**:
     - NEVER output only the inner child elements, updated form rows, or newly added modal/drawer tags.
     - ALWAYS preserve the full document structure: `<div class="page-layout">`, `<app-page-header>`, main table/cards container, `<app-paginated-table>`, action templates (`<ng-template #actionsTmpl>`), result banners, and all existing drawer/dialog components.
   - **Styles (`*.scss`, `*.css`)**:
     - NEVER output only the newly added class selectors.
     - ALWAYS retain all existing class rules, layout styles, themes, and media queries.
   - **TypeScript Logic (`*.ts`, `*.service.ts`, `*.component.ts`)**:
     - NEVER replace a component with just the new methods.
     - ALWAYS retain all existing imports, class properties, `@ViewChild` refs, lifecycle hooks (`ngOnInit`, `ngOnChanges`), constructor injections, and helper functions.

4. **Mandatory Immediate `git diff` Verification**:
   - After writing to any file, IMMEDIATELY run `git diff <path>` to review the line-by-line diff.
   - If any unintended deletions, wiped sections, or missing template blocks are detected, restore and correct them immediately before proceeding.

5. **Verify Clean Git Status**:
   - Run `git status` prior to completing any task or reporting back to ensure no files were corrupted or accidentally overwritten.

---

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
