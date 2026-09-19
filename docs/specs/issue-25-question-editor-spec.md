# NAG Question Editor & Chemistry Rendering Specification

> **Document ID:** SPEC-025-126  
> **Related Issues:** [#25 (Question Editor)](https://github.com/natassgrid/nag/issues/25) | [#126 (SmilesDrawer 2.0 Chemical Structures)](https://github.com/natassgrid/nag/issues/126)  
> **Status:** Implemented & Verified  
> **Target Modules:** `frontend/` (Admin/Author SPA), `candidate-frontend/` (Candidate Exam App), `question-bank-service` (Backend)  

---

## 1. Executive Summary & Problem Statement

Develop a **standalone, extensible rich text editor** (`ExamEditorComponent`) providing a secure, plugin-based editor for authoring examination content, mathematical formulas, and chemical structures.

Simultaneously integrate **SmilesDrawer 2.0 (`smiles-drawer`)** across the editor, question bank service, and candidate delivery applications to support authoring, dynamic client-side rendering, and zoomable visualization of **2D skeletal chemical structures and ring diagrams** (benzene rings, stereochemical wedges/dashes, heterocycles, polymers) directly from standard **SMILES notation**.

### Current State vs. Target State Gap Analysis

| Concern | Current State | Target State |
|---|---|---|
| **Editor engine** | Quill.js (`ngx-quill`) | Slate.js / DOM ContentEditable with Plugin Architecture |
| **Persistence format** | Raw HTML string | Structured Markdown string with `$$...$$` and `<smiles>...</smiles>` |
| **Security** | XSS-prone HTML | Schema-validated markdown/text and sanitized tokens |
| **Architecture** | Coupled legacy quill | Standalone `ExamEditorComponent` implementing `ControlValueAccessor` |
| **Plugin system** | Unwired interfaces | Dynamic plugin registry with lifecycle hooks |
| **Math support** | String regex on `$$...$$` | First-class `MathInlinePlugin` (KaTeX) & Math input dialog |
| **2D Chemistry support (Issue #126)** | Static images uploaded via Asset Service | First-class `ChemicalStructurePlugin` (`<smiles>...</smiles>`) rendered dynamically via SmilesDrawer 2.0 Canvas/SVG |
| **Consumer adoption** | Only partial in question dialog | All forms: Question dialog, Translation dialog, Passage Comprehension dialog |

---

## 2. Editor Usage Map — All Consumer Sites

The `<exam-editor>` is integrated into the following locations:

| # | Component | Field(s) | Widget Mode | Toolbar Capabilities |
|---|---|---|---|---|
| 1 | `question-form-dialog` | `content` (question stem) | `<exam-editor mode="full">` | Full toolbar (Math, SMILES, Marks, Formatting) |
| 2 | `question-form-dialog` | `explanation` | `<exam-editor mode="full">` | Full toolbar |
| 3 | `question-form-dialog` | Option A–D text | `<exam-editor mode="inline">` | Inline marks & Math/SMILES insertion |
| 4 | `question-translation-dialog` | `translatedContent` (target stem) | `<exam-editor mode="full">` | Full toolbar |
| 5 | `question-translation-dialog` | `translatedExplanation` | `<exam-editor mode="full">` | Full toolbar |
| 6 | `question-translation-dialog` | Option translation text per option | `<exam-editor mode="inline">` | Inline marks & Math/SMILES insertion |
| 7 | `passage-form-dialog` | `content` (stimulus/passage text) | `<exam-editor mode="full">` | Full toolbar (Passage reading context, math, chemistry) |
| 8 | `passage-form-dialog` | Sub-question prompt / stem | `<exam-editor mode="full">` | Full toolbar |
| 9 | `passage-form-dialog` | Sub-question Option A–D text | `<exam-editor mode="inline">` | Inline marks & Math/SMILES insertion |
| 10 | `passage-form-dialog` | Sub-question explanation | `<exam-editor mode="full">` | Full toolbar |

---

## 3. Editor Modes

The editor component supports three distinct modes via the `[mode]` input:

```typescript
export type ExamEditorMode = 'full' | 'inline' | 'readonly';
```

| Mode | Toolbar Capabilities | Min Height | Primary Use Case |
|---|---|---|---|
| `full` | Headings (H1-H3), lists, align, indent, math, chemical structures (SMILES), colors, media | `200px` | Question stem, explanations, passage texts |
| `inline` | Text marks only (bold, italic, underline, sub/superscript, math, inline chemical formula) | `48px` | MCQ Option text (A–D), option translations |
| `readonly` | Hidden toolbar (read-only document rendering) | `auto` | Side-by-side translation preview comparison |

---

## 4. Document Model & Schema Validation

The document model uses a hierarchical tree structure serialized to Markdown strings:

```typescript
export interface ExamElement {
  type: ExamElementType;
  children: (ExamElement | ExamText)[];
  align?: 'left' | 'center' | 'right' | 'justify';
  level?: number;
  url?: string;
  formula?: string;
  smiles?: string;
}

export interface ExamText {
  text: string;
  bold?: boolean;
  italic?: boolean;
  underline?: boolean;
  subscript?: boolean;
  superscript?: boolean;
  color?: string;
}

export type ExamElementType =
  | 'paragraph'
  | 'heading'
  | 'bullet-list'
  | 'numbered-list'
  | 'list-item'
  | 'block-quote'
  | 'table'
  | 'table-row'
  | 'table-cell'
  | 'image'
  | 'math-inline'
  | 'math-display'
  | 'chemical-structure';    // Issue #126

export type ExamDocument = ExamElement[];
```

---

## 5. SmilesDrawer 2.0 Integration (Issue #126 Implementation)

### 5.1 SMILES Notation Authoring in Editor
1. The toolbar includes a **Chemical Structure (`science` / benzene)** icon.
2. Clicking opens an interactive modal/popover with:
   - **SMILES input box** (e.g. `c1ccccc1` for Benzene, `CC(=O)O` for Acetic Acid, `C1CCCCC1` for Cyclohexane).
   - **Common template quick-picks**: Benzene ring, Cyclohexane, Pyridine, Amino acids, Steroid backbone.
   - **Real-time 2D preview** rendered on keypress using `SmilesDrawer.Drawer`.
3. Inserting places a `chemical-structure` void node inside the document.

### 5.2 Serialization & Deserialization (`serializer.ts`)
To ensure backwards-compatible question persistence and human readability:
- **On Save (Document Tree ➔ String):**
  `chemical-structure` node ➔ `<smiles>c1ccccc1</smiles>` or ````smiles\nc1ccccc1\n````.
- **On Load (String ➔ Document Tree):**
  `<smiles>...</smiles>` tags are parsed into `chemical-structure` nodes.

### 5.3 Candidate Frontend (`candidate-frontend`) & Admin Frontend Rendering
- **`candidate-frontend`** (`MathRenderer.tsx` & `PassagePanel.tsx`):
  - Parses `<smiles>...</smiles>` and ```smiles code blocks.
  - Dynamically renders clean, responsive vector drawings using `smiles-drawer`.
  - Attaches click-to-zoom triggering `ImageZoomModal.tsx` for inspecting intricate stereochemistry and large reaction schemes.
- **`frontend`** (`math-renderer.component.ts`):
  - Equivalent Angular component renders `<smiles>` canvas using SmilesDrawer.

### 5.4 Backend Translation Protection (`question-bank-service`)
- [`LatexPreservationUtil.java`](file:///F:/code/IdeaProjects/nag/backend/question-bank-service/src/main/java/com/examplatform/questionbank/translation/service/LatexPreservationUtil.java):
  - Add regex placeholder matching `<smiles>([\s\S]*?)</smiles>` and ````smiles([\s\S]*?)````.
  - Protects chemical notation from being translated or modified by IndicTrans2 neural translation models.

---

## 6. Implementation & Verification Status

1. **Phase 1: Standalone Editor Component**
   - Implemented `ExamEditorComponent` with `ControlValueAccessor` interface.
   - Wired plugins (marks, headings, lists, math-inline, chemical-structure).
2. **Phase 2: SmilesDrawer 2.0 Chemistry Plugin (#126)**
   - Added `smiles-drawer` dependency to `frontend` and `candidate-frontend`.
   - Updated `MathRenderer.tsx` (candidate-frontend) with zoom support.
   - Updated `math-renderer.component.ts` (frontend) with canvas SMILES rendering.
   - Updated `LatexPreservationUtil.java` and verified with `LatexPreservationUtilTest.java`.
3. **Phase 3: Form Dialogs Integration**
   - Integrated `<exam-editor>` in `question-form-dialog.component.ts` (stem, explanation, options).
   - Integrated `<exam-editor>` in `question-translation-dialog.component.ts` (translated stem, explanation, option translations).
   - Integrated `<exam-editor>` in `passage-form-dialog.component.ts` (passage content, sub-question prompts, sub-question options, sub-question explanations).
4. **Phase 4: Builds & Verification**
   - Frontend build (`npm run build`) succeeded with 0 errors.
   - Candidate frontend build (`npm run build`) succeeded with 0 errors.
   - Backend tests (`./gradlew :backend:question-bank-service:test`) passed.
