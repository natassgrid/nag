# feat: Support Images, Diagrams & Visual Figures in Questions
## Issue [#100](https://github.com/natassgrid/nag/issues/100) — Implementation Plan

> **Last updated:** 2026-09-13  
> **Author:** Implementation Planning

---

## 1. Problem Statement

The NAG question bank currently supports textual content and LaTeX mathematical expressions (`$$...$$`) only. However, many competitive examination domains (SSC CGL/CHSL, RRB NTPC, Banking PO/Clerk, JEE, NEET, State PSCs) rely heavily on diagrammatic and visual content:

| Domain | Visual Content Types |
|---|---|
| **Non-Verbal Reasoning** | Mirror/water images, paper folding/cutting, pattern completion, dice/cube net folding, embedded figures, counting figures, Venn diagrams |
| **Quantitative Aptitude & Geometry** | Circle theorems (tangents, secants), cyclic quadrilaterals, triangle similarity/congruence, coordinate geometry plots, mensuration 2D/3D composites |
| **Trigonometry** | Heights & distances (line-of-sight), elevation/depression angles, unit circles, trig curves |
| **General Science & Physics/Chemistry** | Ray optics diagrams (mirrors/lenses), electric circuit schematics, chemical apparatus, molecular/structural formulas |
| **Geography & History (GA)** | River basin maps, topographical contour maps, tectonic plates, architectural monument ID |

---

## 2. Current Architecture Analysis

### 2.1 Question Data Model

**`Question` entity** ([`Question.java`](file:///C:/Users/sheel/IdeaProjects/nag/backend/question-bank-service/src/main/java/com/examplatform/questionbank/domain/Question.java)):
- `content` — encrypted `TEXT` column (already supports plain text, HTML, LaTeX, SVG per schema comment)
- `explanation` — plain `TEXT` column
- `options` — `JSONB` array of [`QuestionOption`](file:///C:/Users/sheel/IdeaProjects/nag/backend/question-bank-service/src/main/java/com/examplatform/questionbank/dto/QuestionOption.java) `{id, text, isCorrect}`

**`QuestionOption` DTO** has NO `imageUrl` field — only `id`, `text`, and `correct`.

**DB schema** ([`V1__create_question_schema.sql`](file:///C:/Users/sheel/IdeaProjects/nag/backend/question-bank-service/src/main/resources/db/migration/question/V1__create_question_schema.sql)) comments that `content` "supports: plain text, HTML, LaTeX ($$..$$), SVG" — but the **option JSONB schema** has no image field and the **frontend doesn't render images inside options**.

### 2.2 Asset Service

The [`asset-service`](file:///C:/Users/sheel/IdeaProjects/nag/backend/asset-service) is **already implemented** and supports:
- Upload, download, list, soft-delete, archive/restore
- `AssetType`: `IMAGE`, `AUDIO`, `VIDEO`, `DOCUMENT`
- `ReferenceType`: `QUESTION`, `PASSAGE`, `EXPLANATION`, `INSTRUCTION`
- `MediaAsset` stores `width`, `height`, `dpi`, `altText`, `storageLocation`, `storageProvider`
- Storage via `StorageProvider` abstraction (currently `LocalFileSystemStorageProvider`)

**Gaps identified:**
- No `S3StorageProvider` for production object storage (MinIO/AWS S3)
- No `SVG` asset type enum variant
- No `QUESTION_OPTION` reference type for option-level image linking
- The Angular asset picker ([`asset-picker-dialog.component.ts`](file:///C:/Users/sheel/IdeaProjects/nag/frontend/src/app/features/assets/asset-picker-dialog.component.ts)) is not wired into the question form's option fields
- `AssetUploadResponse` does not return a `publicUrl` for direct embedding

### 2.3 MathRenderer (Candidate Frontend)

[`MathRenderer.tsx`](file:///C:/Users/sheel/IdeaProjects/nag/candidate-frontend/src/components/MathRenderer.tsx) already:
- Extracts `<svg>...</svg>` blocks as opaque placeholders (`extractSvgBlocks`) before Markdown processing — **SVG pass-through works**
- Passes Markdown `![alt](url)` through `marked` — **standard Markdown images render** via `dangerouslySetInnerHTML`
- Does **NOT** add a zoom/enlarge modal for images

**Gaps:** Image options (A/B/C/D as images) not rendered as a 2×2 grid. No zoom modal. `MathRenderer` needs a companion `ImageZoomModal` component.

### 2.4 Translation Engine

[`LatexPreservationUtil.java`](file:///C:/Users/sheel/IdeaProjects/nag/backend/question-bank-service/src/main/java/com/examplatform/questionbank/translation/service/LatexPreservationUtil.java) already:
- Preserves `<svg>...</svg>` blocks via `PRESERVED_PATTERN`
- Does **NOT** preserve Markdown image syntax `![...](...)` or HTML `<img>` tags
- Does **NOT** preserve `imageUrl` values in JSONB option objects

[`TranslatedQuestionPayload`](file:///C:/Users/sheel/IdeaProjects/nag/backend/question-bank-service/src/main/java/com/examplatform/questionbank/translation/domain/TranslatedQuestionPayload.java) stores only `{content, options:[{id, text}], explanation}` — **`imageUrl` is not carried through translation**.

---

## 3. Acceptance Criteria (from Issue)

- [ ] Questions can render diagrams in stem, option choices, and explanations.
- [ ] Option choices support visual figures (A, B, C, D image options).
- [ ] Machine translation engine preserves all image URLs and SVG markup.
- [ ] Examination candidate interface supports responsive diagrams with zoom/expand modal.
- [ ] Seed migrations include representative diagrammatic questions for Reasoning, Geometry, and Trigonometry.

---

## 4. Proposed Changes

---

### 4.1 Backend — `asset-service`

#### [MODIFY] [`AssetType.java`](file:///C:/Users/sheel/IdeaProjects/nag/backend/asset-service/src/main/java/com/examplatform/asset/domain/enums/AssetType.java)
Add `SVG` enum variant:

```java
public enum AssetType {
    IMAGE,
    SVG,      // NEW — inline or file-based vector graphics
    AUDIO,
    VIDEO,
    DOCUMENT
}
```

#### [MODIFY] [`ReferenceType.java`](file:///C:/Users/sheel/IdeaProjects/nag/backend/asset-service/src/main/java/com/examplatform/asset/domain/enums/ReferenceType.java)
Add `QUESTION_OPTION` for linking assets to individual answer choices:

```java
public enum ReferenceType {
    QUESTION,
    QUESTION_OPTION,   // NEW — for option-level image assets
    PASSAGE,
    EXPLANATION,
    INSTRUCTION
}
```

#### [NEW] `S3StorageProvider.java`
Implement `StorageProvider` interface backed by AWS S3 or MinIO:
- Upload via `PutObjectRequest`
- Download via `GetObjectRequest`
- Generate pre-signed CDN URLs with configurable TTL
- Activated by `app.storage.provider=s3` profile; local filesystem remains default for dev

#### [MODIFY] `AssetService.java`
- Validate SVG uploads (content-type `image/svg+xml`); sanitize SVG (strip `<script>` and `on*` event handlers)
- Return `publicUrl` (CDN/pre-signed or local path) in `AssetUploadResponse` for direct embedding
- Add `getPublicUrl(UUID assetId)` method for use by question-bank-service

#### [MODIFY] `AssetUploadResponse.java`
Add `publicUrl` field to be returned on upload completion.

---

### 4.2 Backend — `question-bank-service`

#### [MODIFY] [`QuestionOption.java`](file:///C:/Users/sheel/IdeaProjects/nag/backend/question-bank-service/src/main/java/com/examplatform/questionbank/dto/QuestionOption.java)
Add optional `imageUrl` and `imageAltText` fields:

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidOption   // NEW class-level constraint (see below)
public class QuestionOption {

    private String id;

    // Now nullable — pure-image options have no text
    private String text;

    @JsonProperty("isCorrect")
    private boolean correct;

    /** CDN/storage URL of a raster image or SVG for this option */
    private String imageUrl;

    /** Accessible alt text (translatable); required when imageUrl is set */
    private String imageAltText;
}
```

> [!IMPORTANT]
> `@NotBlank` on `text` must be relaxed. A new class-level `@ValidOption` constraint enforces:
> `(text != null && !text.isBlank()) || (imageUrl != null && !imageUrl.isBlank())`

#### [NEW] `ValidOption.java` + `ValidOptionValidator.java`
Custom `ConstraintValidator` enforcing the above rule at class level.

#### [NEW] DB Migration — `V1_2__add_question_has_images_flag.sql`

```sql
ALTER TABLE question_service.question
    ADD COLUMN IF NOT EXISTS has_images BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_question_has_images
    ON question_service.question(has_images)
    WHERE has_images = TRUE;

COMMENT ON COLUMN question_service.question.has_images
    IS 'Set TRUE when content, explanation, or any option contains image/SVG media.';
```

#### [MODIFY] [`Question.java`](file:///C:/Users/sheel/IdeaProjects/nag/backend/question-bank-service/src/main/java/com/examplatform/questionbank/domain/Question.java)
Add `hasImages` boolean field.

#### [MODIFY] `QuestionService.java` (write path)
Auto-detect `hasImages` by scanning `content`, `explanation`, and `options[*].imageUrl` for `<img`, `<svg`, `![`, or non-blank `imageUrl`.

#### [MODIFY] [`LatexPreservationUtil.java`](file:///C:/Users/sheel/IdeaProjects/nag/backend/question-bank-service/src/main/java/com/examplatform/questionbank/translation/service/LatexPreservationUtil.java)
Extend `PRESERVED_PATTERN` to protect:
1. **Markdown image syntax** `![alt](url)` — add `!\[[^\]]*\]\([^)]*\)` to the pattern
2. **HTML `<img>` tags** — add `<img[^>]*>` to the pattern

For Markdown images, split into two sub-tokens: URL is fully preserved (opaque), alt text is tokenized separately for translation.

#### [MODIFY] [`TranslatedQuestionPayload.java`](file:///C:/Users/sheel/IdeaProjects/nag/backend/question-bank-service/src/main/java/com/examplatform/questionbank/translation/domain/TranslatedQuestionPayload.java)
Add `imageUrl` and `imageAltText` to `TranslatedOption`:

```java
public record TranslatedOption(
    String id,
    String text,
    String imageUrl,       // preserved verbatim (never translated)
    String imageAltText    // translatable
) {}
```

#### [MODIFY] `TranslationWorkflowService.java`
When building `TranslatedOption` objects, copy `imageUrl` from source option and pass `imageAltText` through the translation engine.

#### [NEW] DB Seed — `V2_27__seed_visual_reasoning_geometry.sql`
Add seed questions with inline SVG for:
- **Non-Verbal Reasoning**: mirror images, paper folding (4 SVG image options A/B/C/D)
- **Geometry**: circle-with-tangents, right-triangle altitude diagram
- **Physical Geography**: topographical contour map, tectonic plate boundary diagram

All seeded rows set `has_images = TRUE`.

---

### 4.3 Frontend (Admin) — `frontend` (Angular)

#### [MODIFY] [`question-form-dialog.component.ts`](file:///C:/Users/sheel/IdeaProjects/nag/frontend/src/app/features/questions/question-form-dialog.component.ts) + `.html`
- Add **"Insert Image"** button in the content editor that opens `AssetPickerDialogComponent`; on selection, inserts `![alt](assetUrl)` at cursor
- Per option (A–D), add **"Add Image"** button → opens asset picker → populates `option.imageUrl` + `option.imageAltText`
- When `imageUrl` is set on an option, show thumbnail preview alongside text field
- Add **"Image-only option"** checkbox that clears `text` and makes `imageUrl` required

#### [MODIFY] `question-list.component.html`
- Add 📷 icon badge in question row when `hasImages = true`

#### [MODIFY] Translation review preview modal
- Render `imageUrl` options as thumbnail images in the preview
- Editable `imageAltText` field in the translated option form

---

### 4.4 Candidate Frontend — `candidate-frontend` (Vite/React)

#### [NEW] `ImageZoomModal.tsx`
Full-screen lightbox component:
```tsx
interface ImageZoomModalProps {
  src: string;
  alt: string;
  isOpen: boolean;
  onClose: () => void;
}
```
- Renders image at up to 90vw × 85vh with CSS transform zoom + touch events
- Accessible: `role="dialog"`, `aria-modal`, focus trap, `Escape` to close
- Tailwind dark overlay backdrop

#### [MODIFY] [`MathRenderer.tsx`](file:///C:/Users/sheel/IdeaProjects/nag/candidate-frontend/src/components/MathRenderer.tsx)
- Post-process final HTML to add `data-zoom-src` and `onClick` trigger to every `<img>` element
- Add `max-width: 100%; height: auto` CSS for responsive images

#### [MODIFY] [`TakeExam.tsx`](file:///C:/Users/sheel/IdeaProjects/nag/candidate-frontend/src/pages/TakeExam.tsx)
Detect `option.imageUrl` presence; render **2×2 grid** layout for image options:

```
┌──────────────┬──────────────┐
│  (A) [img]   │  (B) [img]   │
├──────────────┼──────────────┤
│  (C) [img]   │  (D) [img]   │
└──────────────┴──────────────┘
```
- Each cell is radio-selectable
- Clicking image opens `ImageZoomModal`
- Mixed mode (text + image): thumbnail left, text right

#### [MODIFY] `types/api.ts`
Add `imageUrl?: string` and `imageAltText?: string` to `QuestionOption` interface.

---

## 5. Architecture Flow

```
AUTHORING FLOW
──────────────
Content Author (Angular Admin)
  │
  ├─► Upload SVG/PNG via AssetPickerDialog
  │     └─► POST /api/v1/assets          [asset-service]
  │           └─► Returns assetId + publicUrl
  │
  ├─► Insert ![alt](publicUrl) into content  OR  set option.imageUrl
  │
  └─► POST/PUT /api/v1/questions          [question-bank-service]
        └─► Service sets has_images=true, validates option structure
              └─► Registers AssetReference(QUESTION / QUESTION_OPTION)

TRANSLATION FLOW
──────────────
LatexPreservationUtil.mask()
  ├─► Preserves $$...$$, \[...\], <svg>...</svg>   (existing)
  ├─► Preserves ![alt](url) → splits alt (translatable) / url (preserved)  (NEW)
  └─► Preserves <img src="..."> entire tag                                  (NEW)

IndicTrans2 translates only natural language text

LatexPreservationUtil.unmask() → restores all tokens

DELIVERY FLOW
──────────────
Candidate (React TakeExam.tsx)
  │
  ├─► MathRenderer renders content:
  │     ├─► LaTeX → KaTeX HTML  (existing)
  │     ├─► SVG pass-through    (existing)
  │     └─► ![...](url) → <img> + zoom trigger  (ENHANCED)
  │
  └─► Option rendering:
        ├─► text-only options → radio list  (existing)
        └─► image options → 2×2 grid + ImageZoomModal  (NEW)
```

---

## 6. Open Questions / Design Decisions

> [!IMPORTANT]
> **Q1: S3 vs MinIO vs Local for production images**  
> The asset-service storage abstraction supports multiple providers. Should the initial implementation ship with the MinIO-compatible S3 provider (works in Docker Compose dev), or remain with `LocalFileSystemStorageProvider` until a dedicated object storage environment is provisioned?

> [!IMPORTANT]
> **Q2: SVG Sanitization strategy**  
> Inline SVG in `content` goes through `dangerouslySetInnerHTML` in the candidate frontend. Should SVG be sanitized server-side (Java XML sanitizer before storage) or client-side before render? **Recommend server-side** to prevent stored XSS.

> [!WARNING]
> **Q3: `@NotBlank` relaxation on `QuestionOption.text` is a breaking API change**  
> Existing validation tests will fail. A custom class-level `@ValidOption` constraint must be added (enforcing text OR imageUrl is present), and integration tests must be updated.

> [!NOTE]
> **Q4: Embedding / Similarity for image-only options**  
> `EmbeddingService` currently embeds text for similarity detection. Questions with pure-image options have no text embedding for those options. Should `imageAltText` be included in the embedding input string as a fallback?

> [!NOTE]
> **Q5: Zoom on mobile — custom vs library**  
> Pinch-to-zoom requires touch event handling. Consider `yet-another-react-lightbox` or `react-medium-image-zoom` to reduce maintenance overhead vs a custom implementation. Evaluate against bundle size impact.

---

## 7. File Change Summary

### Backend — `asset-service`

| File | Change Type | Description |
|---|---|---|
| [`AssetType.java`](file:///C:/Users/sheel/IdeaProjects/nag/backend/asset-service/src/main/java/com/examplatform/asset/domain/enums/AssetType.java) | MODIFY | Add `SVG` variant |
| [`ReferenceType.java`](file:///C:/Users/sheel/IdeaProjects/nag/backend/asset-service/src/main/java/com/examplatform/asset/domain/enums/ReferenceType.java) | MODIFY | Add `QUESTION_OPTION` variant |
| `S3StorageProvider.java` | NEW | AWS S3 / MinIO storage provider |
| [`AssetService.java`](file:///C:/Users/sheel/IdeaProjects/nag/backend/asset-service/src/main/java/com/examplatform/asset/service/AssetService.java) | MODIFY | SVG sanitization, `getPublicUrl()` |
| [`AssetUploadResponse.java`](file:///C:/Users/sheel/IdeaProjects/nag/backend/asset-service/src/main/java/com/examplatform/asset/dto/AssetUploadResponse.java) | MODIFY | Add `publicUrl` field |

### Backend — `question-bank-service`

| File | Change Type | Description |
|---|---|---|
| [`QuestionOption.java`](file:///C:/Users/sheel/IdeaProjects/nag/backend/question-bank-service/src/main/java/com/examplatform/questionbank/dto/QuestionOption.java) | MODIFY | Add `imageUrl`, `imageAltText`; custom `@ValidOption` |
| `ValidOption.java` | NEW | Custom constraint annotation |
| `ValidOptionValidator.java` | NEW | `ConstraintValidator` implementation |
| [`Question.java`](file:///C:/Users/sheel/IdeaProjects/nag/backend/question-bank-service/src/main/java/com/examplatform/questionbank/domain/Question.java) | MODIFY | Add `hasImages` field |
| `QuestionService.java` | MODIFY | Auto-detect & set `hasImages` on write |
| `V1_2__add_question_has_images_flag.sql` | NEW | DB migration |
| [`LatexPreservationUtil.java`](file:///C:/Users/sheel/IdeaProjects/nag/backend/question-bank-service/src/main/java/com/examplatform/questionbank/translation/service/LatexPreservationUtil.java) | MODIFY | Extend `PRESERVED_PATTERN` for `![](url)` and `<img>` |
| [`TranslatedQuestionPayload.java`](file:///C:/Users/sheel/IdeaProjects/nag/backend/question-bank-service/src/main/java/com/examplatform/questionbank/translation/domain/TranslatedQuestionPayload.java) | MODIFY | Add `imageUrl`, `imageAltText` to `TranslatedOption` |
| `TranslationWorkflowService.java` | MODIFY | Copy `imageUrl`, translate `imageAltText` |
| `V2_27__seed_visual_reasoning_geometry.sql` | NEW | Visual question seeds |

### Frontend Admin — Angular

| File | Change Type | Description |
|---|---|---|
| [`question-form-dialog.component.ts`](file:///C:/Users/sheel/IdeaProjects/nag/frontend/src/app/features/questions/question-form-dialog.component.ts) | MODIFY | Asset picker for content & options |
| [`question-form-dialog.component.html`](file:///C:/Users/sheel/IdeaProjects/nag/frontend/src/app/features/questions/question-form-dialog.component.html) | MODIFY | "Add Image" button + thumbnail preview per option |
| `question-list.component.html` | MODIFY | 📷 badge for `hasImages` questions |
| Translation review modal | MODIFY | Thumbnail rendering + `imageAltText` edit field |

### Candidate Frontend — Vite/React

| File | Change Type | Description |
|---|---|---|
| `ImageZoomModal.tsx` | NEW | Full-screen lightbox with zoom |
| [`MathRenderer.tsx`](file:///C:/Users/sheel/IdeaProjects/nag/candidate-frontend/src/components/MathRenderer.tsx) | MODIFY | Post-process `<img>` tags with zoom trigger |
| [`TakeExam.tsx`](file:///C:/Users/sheel/IdeaProjects/nag/candidate-frontend/src/pages/TakeExam.tsx) | MODIFY | 2×2 image option grid + `ImageZoomModal` wiring |
| `types/api.ts` | MODIFY | Add `imageUrl?`, `imageAltText?` to `QuestionOption` |

---

## 8. Verification Plan

### Automated Tests

```bash
# Backend unit tests
wsl -d Ubuntu-24.04 -e bash -lic "cd /mnt/c/Users/sheel/IdeaProjects/nag && ./gradlew :backend:question-bank-service:test --tests '*QuestionOption*' --tests '*LatexPreservation*' --tests '*Translation*'"

# Asset service integration tests
wsl -d Ubuntu-24.04 -e bash -lic "cd /mnt/c/Users/sheel/IdeaProjects/nag && ./gradlew :backend:asset-service:test"

# Frontend builds
wsl -d Ubuntu-24.04 -e bash -lic "cd /mnt/c/Users/sheel/IdeaProjects/nag/frontend && npm run build"
wsl -d Ubuntu-24.04 -e bash -lic "cd /mnt/c/Users/sheel/IdeaProjects/nag/candidate-frontend && npm run build"
```

**New test cases needed:**

| Test | Location |
|---|---|
| `ValidOptionValidatorTest` — text+image / image-only / neither (fail) | `question-bank-service/test` |
| `LatexPreservationUtilTest` — `mask/unmask` with `![alt](url)` and `<img>` | `question-bank-service/test` |
| `TranslationWorkflowServiceTest` — `imageUrl` preserved, `imageAltText` translated | `question-bank-service/test` |
| `AssetServiceTest` — SVG sanitization rejects `<script>` tags | `asset-service/test` |
| `ImageZoomModal.spec.tsx` — opens/closes, keyboard accessible | `candidate-frontend` |

### Manual Verification Steps

1. **Upload SVG via Admin UI** → asset-service stores it → paste `imageUrl` into question content → save → confirm 📷 badge in question list.
2. **Image Options** → create Non-Verbal Reasoning question with 4 SVG image options → verify 2×2 grid in TakeExam → click image → zoom modal opens.
3. **Translation preservation** → auto-translate a question with `![fig](url)` → verify URL unchanged, only alt text translated.
4. **Seed questions** → run Flyway `V2_27` migration → confirm visual questions load in candidate exam mode.
5. **Mobile responsive** → 375px viewport → 2×2 grid no overflow → pinch-to-zoom works.
6. **Accessibility** → screen reader announces option images via `imageAltText`; `aria-modal` focus trap on zoom modal.

---

## 9. Implementation Phases

| Phase | Scope | Estimated Effort |
|---|---|---|
| **Phase 1** (Core) | `QuestionOption.imageUrl`, `has_images` DB migration, `LatexPreservationUtil` image protection, `TranslatedOption.imageUrl` carry-through | ~3 days |
| **Phase 2** (Delivery UI) | `ImageZoomModal.tsx`, `MathRenderer` zoom wiring, `TakeExam` 2×2 grid | ~2 days |
| **Phase 3** (Authoring UI) | Admin question form asset picker integration, option image thumbnails | ~2 days |
| **Phase 4** (Asset Service) | `S3StorageProvider`, SVG sanitization, `AssetType.SVG`, `publicUrl` | ~2 days |
| **Phase 5** (Seeds & QA) | `V2_27` seed migration with visual questions, full QA pass | ~2 days |

**Total estimated: ~11 developer-days**

---

*References: [GitHub Issue #100](https://github.com/natassgrid/nag/issues/100)*
