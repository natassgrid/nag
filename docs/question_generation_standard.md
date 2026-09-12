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
   - When constructing JSON within SQL migration files or raw JSON payloads, LaTeX backslashes (`\`) MUST be escaped as `\\` (e.g. `$$\frac{a}{b}$$`, `$$75(\\sqrt{3} - 1)\\text{ m}$$`).

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

---

## 5. Question Generation Context & Blueprint Assembly Reference

### 5.1 Blueprint Constraint Matching (`PaperAssemblyService`)
`PaperAssemblyService` queries the question bank by matching:
- `subject` (Exact case-sensitive match against `question_service.subject.name`)
- `topic` (Exact case-sensitive match against `question_service.topic.name`)
- `difficulty` (`EASY`, `MEDIUM`, `HARD`)
- `state = 'APPROVED'`

If the active pool contains fewer available questions than the rule quota (`needed`), paper assembly fails immediately with `InsufficientQuestionsException` and generates a gap report.

### 5.2 Context Reference: General Intelligence & Reasoning -> Statement and Conclusion (HARD)

When generating or seeding questions for **`Statement and Conclusion`** under **`General Intelligence and Reasoning`** at **`HARD`** difficulty:

1. **Logical Frameworks**:
   - **Multi-Premise Categorical Syllogisms**: Minimum 3–4 complex premises involving universal affirmatives ($$\forall x (P(x) \implies Q(x))$$), negative universals ($$P \cap Q = \emptyset$$), and existential particular quantifiers ($$\exists x (P(x) \land Q(x))$$).
   - **Conditional Reasoning & Contrapositives**: Strict implications ($$A \implies B$$), biconditionals ($$A \iff B$$), and disjunctive antecedents ($$(A \lor B) \implies C$$), testing candidate awareness of formal fallacies (Affirming the Consequent, Denying the Antecedent).
   - **Causal Necessity vs Sufficiency**: Scenarios distinguishing necessary conditions ($$\text{Effect} \implies \text{Cause}$$) from sufficient conditions ($$\text{Cause} \implies \text{Effect}$$), controlling for confounding variables and ecological fallacies.
   - **Modal & Quantified Logic**: "Only a few", "At least one", "None except", and boundary constraints.

2. **Subtopic Taxonomy**:
   - `Direct & Indirect Inferences`
   - `Logical Fallacies & Conditional Deductions`
   - `Cause, Effect and Assertion-Reasoning`
   - `Multi-Statement Analytical Conclusions`

3. **Cognitive Level Alignment**:
   - `HARD` items must be mapped to `ANALYZE` or `EVALUATE`.
   - Distractors must be plausible fallacies commonly made in unrigorous thinking (e.g. assuming correlation equals causation, fallacy of division/composition, or converses of one-way implications).

4. **Minimum Seeding Threshold**:
   - Each `(subject, topic, difficulty)` pool should maintain a minimum of **50 approved seed questions** in migrations (e.g., `V2_24__seed_statement_and_conclusion_hard.sql`) to prevent test/production assembly starvation under random sampling policies.
