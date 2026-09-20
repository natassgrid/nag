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

3. **No Literal `\n` / `\r` Escape Characters in Source Code (CRITICAL)**:
   - When generating or modifying source code files (`.ts`, `.js`, `.html`, `.scss`, `.css`, `.java`, etc.), **NEVER** write literal `\n` or `\r\n` escape sequences in place of real line breaks in code statements, arrays, or object literals (e.g., `const list = [\n { id: 1 } \n]`).
   - Literal `\n` strings outside of explicit quoted string values break compilers and parsers with errors like:
     - `TS1127: Invalid character`
     - `TS2304: Cannot find name 'n'`
     - `TS1005: ',' expected`
   - Always emit genuine whitespace line breaks in multiline code structures.
   - Always inspect `git diff` to verify no escaped `\n` sequences were injected into source code.

4. **Frontend-Specific Preservation Rules**:
   - **HTML Templates (`*.component.html`, `*.html`, `*.tsx`, `*.jsx`)**:
     - NEVER output only the inner child elements, updated form rows, or newly added modal/drawer tags.
     - ALWAYS preserve the full document structure: `<div class="page-layout">`, `<app-page-header>`, main table/cards container, `<app-paginated-table>`, action templates (`<ng-template #actionsTmpl>`), result banners, and all existing drawer/dialog components.
   - **Styles (`*.scss`, `*.css`)**:
     - NEVER output only the newly added class selectors.
     - ALWAYS retain all existing class rules, layout styles, themes, and media queries.
   - **TypeScript Logic (`*.ts`, `*.service.ts`, `*.component.ts`)**:
     - NEVER replace a component with just the new methods.
     - ALWAYS retain all existing imports, class properties, `@ViewChild` refs, lifecycle hooks (`ngOnInit`, `ngOnChanges`), constructor injections, and helper functions.

5. **Mandatory Immediate `git diff` Verification**:
   - After writing to any file, IMMEDIATELY run `git diff <path>` to review the line-by-line diff.
   - If any unintended deletions, wiped sections, or missing template blocks are detected, restore and correct them immediately before proceeding.

6. **Mandatory Post-Task Verification & Production Docker Builds (ALWAYS `--no-cache`)**:
   - Run `git status` prior to completing any task or reporting back to ensure no files were corrupted or accidentally overwritten.
   - Run local build checks (`npm run build -- --configuration production` in `frontend/`, `npm run build` in `candidate-frontend/`, or Gradle compile) to verify the code compiles cleanly with 0 errors.
   - **MANDATORY DOCKER BUILDS WITH `--no-cache`**: After task completion, ALWAYS execute clean Docker builds with `--no-cache` to ensure cached layers do not mask stale assets, missing files, or broken import paths:
     1. **Angular Admin UI**: `docker build --no-cache -t exam-frontend:latest ./frontend`
     2. **React Candidate Engine**: `docker build --no-cache -t candidate-frontend:latest ./candidate-frontend`
     3. **Backend Monolith / Service**: `docker build --no-cache -f backend/Dockerfile -t exam-monolith:latest ./backend` (when backend code changed).

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

#### 1a. Backslash Escaping — Critical Rules

> [!CAUTION]
> Incorrect backslash escaping is the single most common cause of broken LaTeX rendering.
> Follow these rules exactly depending on context.

| Context | Required escaping | Example |
|---|---|---|
| Human-readable `.md` / display strings | Single backslash | `$$\frac{a}{b}$$`, `$$\neq 0$$`, `$$\neg L$$` |
| JSON field value (API payload, seed script) | Double backslash (`\\`) | `"$$\\frac{a}{b}$$"`, `"$$\\neq 0$$"`, `"$$\\neg L$$"` |
| SQL string literal inside a migration file | Double backslash (`\\`) | `'$$\det(A) \\neq 0$$'` |
| Java/Kotlin string literal | Double backslash (`\\`) | `"$$\\frac{a}{b}$$"` |

**Why this matters for the renderer:** The MathRenderer receives the *parsed* JSON value (single backslash). It normalises any remaining over-escaped chains (e.g. `\\\\det` → `\det`) before passing to KaTeX. Never emit more than two consecutive backslashes (`\\`) before a LaTeX command in a JSON value — triple or quadruple escaping (`\\\\`) produces raw token output even after normalisation.

#### 1b. Newline Sequences — Critical Rules

> [!CAUTION]
> `\n` followed by a lowercase letter is a LaTeX command, NOT a line break.
> This is the second most common cause of broken rendering.

The MathRenderer converts `\n` to a real newline **only when it is not immediately followed by a lowercase letter**. This prevents `\neq`, `\neg`, `\rightarrow`, `\text`, `\tau`, `\theta` from being split.

**Correct separators in JSON strings:**

```json
"**Statements:**\n1. Statement one.\n2. Statement two.\n\n**Conclusions:**\nI. Conclusion one.\nII. Conclusion two."
```

- `\n1.` → safe (digit follows)
- `\nI.` → safe (uppercase letter follows)
- `\nII.` → safe (uppercase letter follows)
- `\n\n` → safe (another `\n` follows)
- `\neq` → **NOT** a newline — this is the LaTeX not-equal command
- `\neg` → **NOT** a newline — this is the LaTeX negation command

**Never use `\n` as a separator immediately before a lowercase LaTeX command word.** Always use a space or `\n\n` to separate text from such commands.

#### 1d. Chemistry Notation (`\ce{}` and `\pu{}`)

The MathRenderer loads the KaTeX **mhchem** contrib extension. Use `\ce{}` for chemical formulas and equations, `\pu{}` for physical units — both wrapped in `$$ ... $$`:

| Concept | JSON value | Rendered |
|---|---|---|
| Molecular formula | `"$$\\ce{H2SO4}$$"` | H₂SO₄ |
| Reaction equation | `"$$\\ce{2H2 + O2 -> 2H2O}$$"` | balanced equation with arrow |
| Ion | `"$$\\ce{Fe^{2+}}$$" | Fe²⁺ |
| Physical unit | `"$$\\pu{6.022e23 mol-1}$$"` | Avogadro's number |

No new npm dependency — mhchem ships inside the `katex` package. Does **not** support 2D structural diagrams (benzene rings etc.).

### 2. General Formatting Rules

- **Standard Markdown (GFM)**: Use `**bold**`, `*italic*`, `` `code` ``, fenced code blocks, and pipe tables.
- **Paragraph separation**: Use double newlines (`\n\n`) between headings, statement groups, and conclusion groups.
- **List items**: Use single newlines (`\n`) between ordered (`1. `, `2. `) or roman numeral (`I. `, `II. `) items.
- **Inline vs display math**: Formulas inside sentences or list items render inline. A formula on its own line with no surrounding text renders as a centred display block. Do not wrap list-item formulas in their own paragraph — keep them on the same line as the item text.

---

### 3. Reasoning & Syllogism Questions (Statements & Conclusions)

Structure multi-premise reasoning questions with clear Markdown headings and numbered/roman lists.

**Correct JSON string value (double-backslash escaped):**
```json
"**Statements:**\n1. All quantum computers capable of Shor's algorithm ($$Q$$) require coherent qubits with fidelity exceeding $$99.9\\%$$ ($$F$$).\n2. No noisy intermediate-scale quantum ($$NISQ$$) system achieves coherent qubit fidelity exceeding $$99.9\\%$$.\n3. System $$\\Psi$$ is a $$NISQ$$ system.\n\n**Conclusions:**\nI. System $$\\Psi$$ does not have coherent qubit fidelity exceeding $$99.9\\%$$.\nII. System $$\\Psi$$ is not capable of executing Shor's algorithm for large integers."
```

**What this renders as (human-readable):**
```
**Statements:**
1. All quantum computers capable of Shor's algorithm ($$Q$$) require coherent qubits with fidelity exceeding $$99.9\%$$ ($$F$$).
2. No noisy intermediate-scale quantum ($$NISQ$$) system achieves coherent qubit fidelity exceeding $$99.9\%$$.
3. System $$\Psi$$ is a $$NISQ$$ system.

**Conclusions:**
I. System $$\Psi$$ does not have coherent qubit fidelity exceeding $$99.9\%$$.
II. System $$\Psi$$ is not capable of executing Shor's algorithm for large integers.
```

**Key rules:**
- Separate `**Statements:**` and `**Conclusions:**` blocks with `\n\n` (double newline).
- Each numbered/roman item uses `\n` before the marker — safe because digits and uppercase letters are safe separators.
- Math variables like `$$\neg L$$`, `$$\det(A)$$`, `$$\neq 0$$` stay within `$$ ... $$` and are never split by a `\n`.

---

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
   - Always run through WSL Ubuntu 24.04 or native terminal:
     - Angular: `cd frontend && npm run build -- --configuration production` (or via WSL)
     - Vite/React: `cd candidate-frontend && npm run build` (or via WSL)

3. **Mandatory Post-Task Docker Builds (Always `--no-cache`)**:
   - **Step 1: UI Docker Builds (Run with `--no-cache`)**:
     ```bash
     # Angular Admin / Authoring UI
     docker build --no-cache -t exam-frontend:latest ./frontend

     # React Candidate Delivery UI
     docker build --no-cache -t candidate-frontend:latest ./candidate-frontend
     ```
   - **Step 2: Backend Docker Build (When backend code changed)**:
     ```bash
     # Build service or monolith container
     docker build --no-cache -f backend/Dockerfile -t exam-monolith:latest ./backend
     ```
