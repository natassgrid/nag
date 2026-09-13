# feat: Support Images, Diagrams & Visual Figures in Questions
## Issue [#100](https://github.com/natassgrid/nag/issues/100) — Implementation Plan & Status

> **Status:** Completed & Closed (Implemented)  
> **Last updated:** 2026-09-13  
> **Branch:** `feat/issue-100-image-diagram-support`

---

## 1. Problem Statement

The NAG question bank previously supported textual content and LaTeX mathematical expressions (`$$...$$`) only. Competitive examination domains (SSC CGL/CHSL, RRB NTPC, Banking PO/Clerk, JEE, NEET, State PSCs) rely heavily on diagrammatic and visual content:

| Domain | Visual Content Types |
|---|---|
| **Non-Verbal Reasoning** | Mirror/water images, paper folding/cutting, pattern completion, dice/cube net folding, embedded figures, counting figures, Venn diagrams |
| **Quantitative Aptitude & Geometry** | Circle theorems (tangents, secants), cyclic quadrilaterals, triangle similarity/congruence, coordinate geometry plots, mensuration 2D/3D composites |
| **Trigonometry** | Heights & distances (line-of-sight), elevation/depression angles, unit circles, trig curves |
| **General Science & Physics/Chemistry** | Ray optics diagrams (mirrors/lenses), electric circuit schematics, chemical apparatus, molecular/structural formulas |
| **Geography & History (GA)** | River basin maps, topographical contour maps, tectonic plates, architectural monument ID |

---

## 2. Current Architecture & Implementation

### 2.1 Question Data Model
- `content` — Encrypted `TEXT` column supporting plain text, HTML, LaTeX, SVG, and Markdown images (`![alt](url)`).
- `explanation` — Plain `TEXT` column supporting images, LaTeX, and SVGs.
- `options` — `JSONB` array of `QuestionOption` supporting `{id, text, imageUrl, imageAltText, isCorrect}`.
- `hasImages` flag — Added to Question entity and indexed (`idx_question_has_images`) for rapid visual question discovery and badge rendering.

### 2.2 Asset Service
- Media Asset Upload, download, search, soft-delete, archive/restore.
- `AssetType`: `IMAGE`, `SVG`, `AUDIO`, `VIDEO`, `DOCUMENT`.
- `ReferenceType`: `QUESTION`, `QUESTION_OPTION`, `PASSAGE`, `EXPLANATION`, `INSTRUCTION`.
- Asset picker dialog with dual-tab support (Asset Library browser and direct Drag & Drop Upload Now with automatic selection).

### 2.3 MathRenderer & Examination Candidate Interface
- `MathRenderer` renders LaTeX via KaTeX, passes through SVG markup, and attaches zoom triggers to all images.
- Full-screen `ImageZoomModal` lightbox for candidate visual inspection.
- 2×2 grid layout for visual 4-option image choices (A/B/C/D).

### 2.4 Machine Translation Protection
- `LatexPreservationUtil` protects Markdown image syntax (`![alt](url)`), HTML `<img>` tags, and inline SVGs during neural machine translation.
- `TranslatedQuestionPayload` preserves `imageUrl` unchanged while translating accessible `imageAltText`.

---

## 3. Acceptance Criteria

- [x] Questions can render diagrams in stem, option choices, and explanations.
- [x] Option choices support visual figures (A, B, C, D image options).
- [x] Machine translation engine preserves all image URLs and SVG markup.
- [x] Examination candidate interface supports responsive diagrams with zoom/expand modal.
- [x] Seed migrations include representative diagrammatic questions for Reasoning, Geometry, and Trigonometry.

---

## 4. Key Files & Components

### Backend
- `QuestionOption.java` & `ValidOptionValidator.java`: Multi-modal option validation (text, image, or mixed).
- `Question.java` & `V1_2__add_question_has_images_flag.sql`: `hasImages` column & metadata indexing.
- `LatexPreservationUtil.java`: Tokenization and preservation of `![alt](url)` and `<img>`.
- `TranslatedQuestionPayload.java`: Option image preservation during translation.
- `V2_27__seed_visual_reasoning_geometry.sql`: Seed visual questions for Reasoning, Geometry, Trigonometry, and Geography.

### Frontend Admin
- `question-form-dialog.component.ts` & `.html`: Image insertion in content, explanation, and options.
- `asset-picker-dialog.component.ts`, `.html`, `.scss`: Dual-tab library and direct upload dropzone.
- `error.interceptor.ts` & `notification.service.ts`: Error handling with 413 payload handling and HTML tag sanitization.

### Candidate Frontend
- `ImageZoomModal.tsx`: High-resolution diagram inspector with keyboard and touch support.
- `MathRenderer.tsx`: Diagram rendering and zoom trigger integration.
- `TakeExam.tsx`: 2×2 image option grid layout.

---

*References: [GitHub Issue #100](https://github.com/natassgrid/nag/issues/100)*
