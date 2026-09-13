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

## Question Generation & Formatting Standards (Markdown, LaTeX & Newline Rules)

All AI agents, prompt generators, seed script creators, and backend services generating or manipulating examination questions MUST adhere to these strict standards:

### 1. LaTeX Math Formatting
- **Enclosing Delimiters**: Enclose ALL mathematical, chemical, and physical formulas, expressions, variables, powers, fractions, square roots, matrices, angles, and numbers with units in `$$ ... $$` delimiters.
- **Strictly Prohibited Delimiters**: NEVER use single `$ ... $`, `\( ... \)`, or `\[ ... \]`.
- **Field Ubiquity**: `$$ ... $$` MUST be applied in ALL text fields:
  - `content` (question stem)
  - `options[].text` (every single option)
  - `answerKey` (if formulaic/algebraic/numerical)
  - `explanation` (step-by-step solution derivation)
- **Percentage Symbols in LaTeX**:
  - `%` is a LaTeX comment character. In LaTeX math mode, write `\%` or `\text{%}` (e.g. `$$99.9\%$$`).
- **JSON & SQL Escaping**:
  - In raw JSON strings or SQL migration literals, LaTeX backslashes MUST be escaped (e.g., `"$$\\frac{a}{b}$$"`, `"$$\\sqrt{3}$$"`, `"$$99.9\\%$$"`).

### 2. Markdown & Paragraph Formatting
- **Standard Markdown (GFM)**: Use standard Markdown for headings (`**Statements:**`, `**Conclusions:**`), bold text (`**term**`), italic text (`*term*`), and tables (`| Header 1 | Header 2 |`).
- **Newline & Line Break Conventions**:
  - Use real newlines (`\n`) for line breaks. In JSON payloads, `\n` represents a newline character.
  - Separate headings, statement groups, and conclusion groups with double newlines (`\n\n`) to create distinct paragraph blocks.
  - Separate individual numbered statements or lettered items with single newlines (`\n1. ...\n2. ...`).

### 3. Reasoning & Syllogism Questions (Statements & Conclusions)
Structure multi-premise reasoning questions with clear Markdown headings and numbered/roman lists:
```text
**Statements:**
1. All quantum computers capable of Shor's algorithm ($$Q$$) require coherent qubits with fidelity exceeding $$99.9\%$$ ($$F$$).
2. No noisy intermediate-scale quantum ($$NISQ$$) system achieves coherent qubit fidelity exceeding $$99.9\%$$.
3. System $$Psi$$ is a $$NISQ$$ system.

**Conclusions:**
I. System $$Psi$$ does not have coherent qubit fidelity exceeding $$99.9\%$$.
II. System $$Psi$$ is not capable of executing Shor's algorithm for large integers.
```

### 4. Option Structure & Integrity
- **`SINGLE_MCQ`**: Exactly 4 options (`A`, `B`, `C`, `D`). Exactly ONE option with `isCorrect: true`, exactly THREE with `isCorrect: false`. `answerKey` MUST equal the correct option ID (`"A"`, `"B"`, `"C"`, or `"D"`).
- **`MULTI_MCQ`**: Exactly 4 options (`A`, `B`, `C`, `D`) with 2 or more having `isCorrect: true`.
- **`NUMERICAL`**: `options` is `null` or `[]`, `answerKey` contains the numeric string value.
- **`DESCRIPTIVE`**: `options` is `null`, `answerKey` contains the comprehensive model answer/scoring rubric.

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
