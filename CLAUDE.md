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
| SQL string literal inside a migration file | Double backslash (`\\`) | `'$$\\det(A) \\neq 0$$'` |
| Java/Kotlin string literal | Double backslash (`\\`) | `"$$\\frac{a}{b}$$"` |

**Why this matters for the renderer:** The MathRenderer receives the *parsed* JSON value (single backslash). It normalises over-escaped chains (e.g. `\\\\det` → `\det`) before passing to KaTeX. Never emit more than two consecutive backslashes (`\\`) before a LaTeX command in a JSON value — triple or quadruple escaping (`\\\\`) produces raw token output even after normalisation.

#### 1b. Newline Sequences — Critical Rules

> [!CAUTION]
> `\n` followed by a lowercase letter is a LaTeX command, NOT a line break.
> This is the second most common cause of broken rendering.

The MathRenderer converts `\n` to a real newline **only when it is not immediately followed by a lowercase letter**. This prevents `\neq`, `\neg`, `\rightarrow`, `\text`, `\tau`, `\theta` from being split at the `\n`.

**Safe separators in JSON strings:**

```json
"**Statements:**\n1. Statement one.\n2. Statement two.\n\n**Conclusions:**\nI. Conclusion one.\nII. Conclusion two."
```

| Sequence | Safe? | Why |
|---|---|---|
| `\n1.` | ✅ safe | digit follows |
| `\nI.` | ✅ safe | uppercase letter follows |
| `\nII.` | ✅ safe | uppercase letter follows |
| `\n\n` | ✅ safe | another `\n` follows |
| `\neq` | ❌ NOT a newline | `eq` is lowercase → LaTeX `\neq` command |
| `\neg` | ❌ NOT a newline | `eg` is lowercase → LaTeX `\neg` command |
| `\nrightarrow` | ❌ NOT a newline | `rightarrow` is lowercase → LaTeX command |

**Rule:** Never use `\n` as a separator immediately before a lowercase LaTeX command word. Keep math commands inside `$$ ... $$` delimiters where they belong.

#### 1c. Percentage Symbols

- `%` is a LaTeX comment character. Always write `\%` inside math delimiters.
- Human-readable: `$$99.9\%$$`
- JSON/SQL: `"$$99.9\\%$$"`

---

### 2. Markdown & Paragraph Formatting

- **Standard Markdown (GFM)**: Use `**bold**`, `*italic*`, `` `code` ``, fenced code blocks, and pipe tables.
- **Paragraph separation**: Use `\n\n` between headings, statement groups, and conclusion groups.
- **List items**: Use `\n` before each `1.`/`2.` or `I.`/`II.` marker — safe because digits and uppercase letters never start LaTeX command names.
- **Inline vs display math**: Formulas on list-item lines render inline (no unwanted block breaks). A formula alone on its own line renders as a centred display block. Always keep math expressions on the same line as the surrounding item text.

---

### 3. Reasoning & Syllogism Questions (Statements & Conclusions)

**Correct JSON string value (double-backslash escaped, ready for API/SQL):**
```json
"**Statements:**\n1. All quantum computers capable of Shor's algorithm ($$Q$$) require coherent qubits with fidelity exceeding $$99.9\\%$$ ($$F$$).\n2. No noisy intermediate-scale quantum ($$NISQ$$) system achieves coherent qubit fidelity exceeding $$99.9\\%$$.\n3. System $$\\Psi$$ is a $$NISQ$$ system.\n\n**Conclusions:**\nI. System $$\\Psi$$ does not have coherent qubit fidelity exceeding $$99.9\\%$$.\nII. System $$\\Psi$$ is not capable of executing Shor's algorithm for large integers."
```

**What this renders as:**
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
- Separate `**Statements:**` and `**Conclusions:**` blocks with `\n\n`.
- Each numbered/roman item uses `\n` before the marker.
- Math commands like `$$\neg L$$`, `$$\det(A)$$`, `$$\neq 0$$` stay inside `$$ ... $$` — they are never placed bare in the text where `\n` processing could split them.

---

### 4. Option Structure & Integrity

- **`SINGLE_MCQ`**: Exactly 4 options (`A`, `B`, `C`, `D`). Exactly ONE option with `isCorrect: true`, exactly THREE with `isCorrect: false`. `answerKey` MUST equal the correct option ID (`"A"`, `"B"`, `"C"`, or `"D"`).
- **`MULTI_MCQ`**: Exactly 4 options (`A`, `B`, `C`, `D`) with 2 or more having `isCorrect: true`.
- **`NUMERICAL`**: `options` is `null` or `[]`, `answerKey` contains the numeric string value.
- **`DESCRIPTIVE`**: `options` is `null`, `answerKey` contains the comprehensive model answer/scoring rubric.

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
