# AGENTS.md - NAG Project Setup & Build Directives

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

3. **Mandatory Immediate `git diff` Verification**:
   - After writing to any file, IMMEDIATELY run `git diff <path>` to review the line-by-line diff.
   - If any unintended deletions, wiped sections, or missing seed records are detected, restore and correct them immediately before proceeding.

4. **Verify Clean Git Status**:
   - Run `git status` prior to completing any task or reporting back to ensure no files were corrupted or accidentally overwritten.

---

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
