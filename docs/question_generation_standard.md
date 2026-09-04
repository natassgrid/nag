# NAG Question Generation Knowledge Base & Standard Operating Procedure

## 1. Mathematical Notation & LaTeX Rules
1. **Delimiter Standard**:
   - **ALL** mathematical formulas, equations, expressions, symbols, fractions, powers, square roots, matrices, angles, and numbers with units MUST be enclosed in `$$ ... $$` delimiters.
   - **FORBIDDEN DELIMITERS**: `\( ... \)`, `\[ ... \]`, and single `$ ... $` are strictly prohibited.
2. **Field Ubiquity**:
   - Apply `$$ ... $$` across **ALL** fields:
     - `content` (question stem)
     - `options[].text` (every single option)
     - `answerKey` (if numerical/algebraic)
     - `explanation` (detailed solution derivation)
3. **Escaping Inside JSON & SQL Literals**:
   - When constructing JSON within SQL migration files or raw JSON payloads, LaTeX backslashes (`\`) MUST be escaped as `\\` (e.g. `$$\\frac{a}{b}$$`, `$$75(\\\\sqrt{3} - 1)\\\\text{ m}$$`).

---

## 2. Question Classification & Bloom's Taxonomy

### Difficulty Levels
- `EASY`: Direct recall, standard formulas, single-step operations.
- `MEDIUM`: Multi-step reasoning, combination of 2 concepts, intermediate arithmetic.
- `HARD`: Multi-layered synthesis, edge cases, rigorous proofs, complex geometrical/calculus transformations.

### Revised Bloom's Taxonomy Cognitive Levels
1. `REMEMBER`: State definitions, recognize facts, cite articles or formulas.
2. `UNDERSTAND`: Explain meaning, interpret graphs, classify concepts.
3. `APPLY`: Calculate, solve equations, apply theorems to new scenarios.
4. `ANALYZE`: Breakdown arguments, distinguish cases, determine relationships.
5. `EVALUATE`: Justify conclusions, critique methodologies, verify solutions.
6. `CREATE`: Design alternative approaches, synthesize composite problems.

---

## 3. Option & Answer Key Integrity Rules

### SINGLE_MCQ:
- Exactly 4 options with IDs `"A"`, `"B"`, `"C"`, `"D"`.
- Exactly ONE option has `"isCorrect": true`.
- Exactly THREE options have `"isCorrect": false`.
- The `answerKey` MUST match the correct option letter (`"A"`, `"B"`, `"C"`, or `"D"`).

### MULTI_MCQ (MSQ):
- Exactly 4 options (`"A"`, `"B"`, `"C"`, `"D"`).
- At least 2 options have `"isCorrect": true`.

### NUMERICAL:
- `options` is `null` or `[]`.
- `answerKey` contains the exact numeric string value (e.g., `"4.5"` or `"-12"`).

### DESCRIPTIVE:
- `options` is `null`.
- `answerKey` contains the comprehensive model answer / scoring rubric.

---

## 4. Primary Key & Database Rules

1. **UUID Syntax**:
   - Primary key `id` and foreign keys `tenant_id`, `author_id`, `reviewer_id` must use strictly valid hexadecimal UUIDs: `[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}`.
   - Non-hex characters (like `g-z`) are invalid in PostgreSQL UUID types.
2. **Partitioning**:
   - `question` table is composite-keyed on `(id, subject_id)` and hash-partitioned on `subject_id`.
   - All inserts must provide `subject_id` matching an existing record in `question_service.subject`.
3. **State Management**:
   - Seed questions must default to `state = 'APPROVED'`.
   - LLM auto-generated questions default to `state = 'DRAFT'`.
