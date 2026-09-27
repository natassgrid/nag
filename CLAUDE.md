# CLAUDE.md - NAG Project Instructions, Modular Architecture & Build Setup

## Modular Code Architecture & Frontend Component Standards

### 1. Component Granularity & Decomposition Rules
- **No Monolithic Components**: Components should follow the Single Responsibility Principle (SRP). Any feature component with over 250 lines of HTML or 200 lines of TypeScript must be decomposed into focused, reusable sub-components.
- **Dedicated Sub-Component Directories**: Every sub-component must reside in its own named kebab-case subdirectory under `components/` (e.g., `components/centre-kpi-cards/`, `components/asset-toolbar/`, `components/education-item-card/`, `components/education-form-modal/`).
- **Strict File Triad Separation**: Every Angular component MUST have separate `.ts`, `.html`, and `.scss` files:
  - `templateUrl: './component-name.component.html'`
  - `styleUrl: './component-name.component.scss'`
  - Inline templates (`template: '...'`) and inline styles (`styles: [...]`) are strictly prohibited in feature sub-components.
- **Explicit `:host` Display**: Component stylesheets must define host styling (typically `:host { display: block; }` or `:host { display: contents; }`).
- **Standardized Feature & Sub-Panel Directory Structure**:
  ```
  feature-module/ (or complex sub-panel, e.g., education-details-panel/)
  ├── models/
  │   ├── feature.model.ts        # Domain models, form state interfaces, constants
  │   └── index.ts                # Barrel export for models
  ├── components/
  │   ├── feature-kpi-cards/      # Metric summaries & KPI counters
  │   │   ├── feature-kpi-cards.component.ts
  │   │   ├── feature-kpi-cards.component.html
  │   │   └── feature-kpi-cards.component.scss
  │   ├── feature-item-card/      # Individual item/card view
  │   │   ├── feature-item-card.component.ts
  │   │   ├── feature-item-card.component.html
  │   │   └── feature-item-card.component.scss
  │   ├── feature-empty-state/    # Contextual empty state placeholder & call-to-action
  │   │   ├── feature-empty-state.component.ts
  │   │   ├── feature-empty-state.component.html
  │   │   └── feature-empty-state.component.scss
  │   ├── feature-form-modal/     # Add / Edit form dialog or drawer
  │   │   ├── feature-form-modal.component.ts
  │   │   ├── feature-form-modal.component.html
  │   │   └── feature-form-modal.component.scss
  │   ├── feature-delete-dialog/  # Deletion confirmation dialog
  │   │   ├── feature-delete-dialog.component.ts
  │   │   ├── feature-delete-dialog.component.html
  │   │   └── feature-delete-dialog.component.scss
  │   └── index.ts                # Barrel export for all sub-components
  ├── feature.component.ts        # Container / Orchestrator component
  ├── feature.component.html      # Clean orchestrator template consuming sub-components
  ├── feature.component.scss      # Feature-level page styling
  └── index.ts                    # Public module barrel export
  ```

### 2. Modern Angular Best Practices (Angular 19+ / 20+)
- **Standalone Architecture**: All components, directives, and pipes must be standalone (`standalone: true`).
- **OnPush Change Detection**: Always specify `changeDetection: ChangeDetectionStrategy.OnPush` on container and presentational components for optimal change detection performance.
- **Signal-Based Reactivity**:
  - Use `signal<T>()` for mutable state.
  - Use `computed()` for derived state and metrics.
  - Use `input<T>()` and `input.required<T>()` for component inputs (replacing legacy `@Input()`).
  - Use `output<T>()` for component event emitters (replacing legacy `@Output()`).
- **Function-Based Dependency Injection**: Use `inject(ServiceName)` instead of constructor parameter injection.
- **Native Control Flow**:
  - Use `@if`, `@else if`, `@else` for conditional rendering.
  - Use `@for (item of items(); track item.id)` with explicit track keys.
  - Use `@switch` and `@case` for multi-branch rendering.
- **Reactive Forms with Signal Synergy**: Bind reactive forms (`[formGroup]="form()"`) cleanly into drawer and modal sub-components.

### 3. Defensive Programming & Null Safety
- **Signal Null-Safety**: Never call methods like `.toLowerCase()`, `.trim()`, `.includes()`, `.slice()`, or `.reduce()` directly on object properties that may be `null` or `undefined`.
- **Safe Fallbacks**: Always provide defensive defaults:
  ```typescript
  readonly filteredItems = computed(() => {
    const list = this.items() || [];
    const q = (this.searchQuery() || '').toLowerCase().trim();
    return list.filter(item => {
      if (!item) return false;
      return !q || (item.name && item.name.toLowerCase().includes(q));
    });
  });
  ```

---

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

6. **Mandatory Post-Task Verification & Workspace Builds**:
   - Run `git status` prior to completing any task or reporting back to ensure no files were corrupted or accidentally overwritten.
   - Run local build checks (`npm run build`, `npm run lint`, unit tests, or Gradle compile) to verify the code compiles cleanly with 0 errors.
   - **Nx Workspace Verification**:
     ```bash
     npx nx run-many -t build --skip-nx-cache
     ```
   - **MANDATORY DOCKER BUILDS**: After task completion, ALWAYS execute Docker builds:
     1. **Backend Docker Build**: Run Docker build for the backend base and modified backend service / monolith.
     2. **UI Docker Builds**: After backend build completes, run Docker builds for the UI applications (`frontend` and `candidate-frontend`).

---
