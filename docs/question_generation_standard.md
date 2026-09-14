# NAG Question Generation Knowledge Base & Standard Operating Procedure

## 1. Mathematical Notation & LaTeX Rules

### 1.1 Delimiter Standard
- **ALL** mathematical formulas, equations, expressions, symbols, fractions, powers, square roots, matrices, angles, and numbers with units MUST be enclosed in `$$ ... $$` delimiters.
- **FORBIDDEN DELIMITERS**: `\( ... \)`, `\[ ... \]`, and single `$ ... $` are strictly prohibited.

### 1.2 Field Ubiquity
Apply `$$ ... $$` across **ALL** fields:
- `content` (question stem)
- `options[].text` (every single option)
- `answerKey` (if numerical/algebraic)
- `explanation` (detailed solution derivation)

### 1.3 Backslash Escaping by Context

> [!CAUTION]
> Incorrect backslash escaping is the single most common cause of broken LaTeX rendering in the candidate and admin UIs.

| Context | Required escaping | Example |
|---|---|---|
| Human-readable `.md` / documentation | Single backslash | `$$\frac{a}{b}$$`, `$$\neq 0$$`, `$$\neg L$$` |
| JSON field value (API payload / seed JSON) | Double backslash (`\\`) | `"$$\\frac{a}{b}$$"`, `"$$\\neq 0$$"`, `"$$\\neg L$$"` |
| SQL string literal in Flyway migration | Double backslash (`\\`) | `'$$\\det(A) \\neq 0$$'` |
| Java / Kotlin string literal | Double backslash (`\\`) | `"$$\\frac{a}{b}$$"` |

**Renderer behaviour:** The MathRenderer receives the *parsed* JSON value (single backslash). It iteratively collapses any remaining over-escaped chains (e.g. `\\\\det` → `\\det` → `\det`) before handing off to KaTeX. Never emit more than two consecutive backslashes (`\\`) before a LaTeX command inside a JSON value — triple/quadruple escaping (`\\\\`) will survive the normalisation pass and produce raw token output for candidates.

### 1.4 Newline Sequences — Critical Rules

> [!CAUTION]
> `\n` followed by a **lowercase** letter is interpreted as a LaTeX command, NOT a line break.
> This is the second most common source of broken rendering.

The MathRenderer converts `\n` to a real newline **only when the character immediately after is not a lowercase ASCII letter**. This protects commands like `\neq`, `\neg`, `\rightarrow`, `\text`, `\tau`, `\theta`, `\times` from being silently split.

**Safe vs unsafe `\n` usage in JSON strings:**

| Sequence | Interpretation | Safe to use as separator? |
|---|---|---|
| `\n1.` | line break before digit | ✅ yes |
| `\nI.` | line break before uppercase | ✅ yes |
| `\nII.` | line break before uppercase | ✅ yes |
| `\n\n` | blank line / paragraph break | ✅ yes |
| `\n ` | line break before space | ✅ yes |
| `\neq` | LaTeX not-equal command | ❌ NOT a newline |
| `\neg` | LaTeX negation command | ❌ NOT a newline |
| `\nrightarrow` | LaTeX right-arrow command | ❌ NOT a newline |
| `\tau` | LaTeX tau (but starts with `\t`) | ❌ `\t` is protected too |

**Rule:** Math commands must always stay inside `$$ ... $$` delimiters. Never place a bare LaTeX command (e.g. `\neq`, `\neg`) outside delimiters where `\n`/`\t` unescaping could corrupt it.

### 1.5 Percentage Symbols
- `%` is a LaTeX comment character. Always write `\%` inside math delimiters.
- Human-readable: `$$99.9\%$$`
- JSON / SQL: `"$$99.9\\%$$"`

---

## 2. Markdown & Paragraph Formatting Rules

### 2.1 GFM Markdown
Use standard GitHub Flavored Markdown: `**bold**`, `*italic*`, `` `code` ``, fenced code blocks, and pipe tables.

### 2.2 Newline Handling

- **Paragraph separation**: Use double newlines (`\n\n`) between headings, statement groups, conclusion groups, tables, and paragraphs.
- **List items**: Use single newlines (`\n`) between ordered (`1. `, `2. `) or roman numeral (`I. `, `II. `) items.
- **Safe separators**: Digits (`1.`, `2.`) and uppercase letters (`I.`, `II.`, `A.`) after `\n` are always safe — they never form LaTeX command names.

### 2.3 Inline vs Display Math

- A formula **on a list-item line** (e.g. `1. ... $$\neq 0$$ ...`) renders **inline** — no block break.
- A formula **alone on its own line** with no other text renders as a **centred display block**.
- Always keep math expressions on the same line as the surrounding item text to avoid unwanted block-mode rendering that breaks list flow.

### 2.4 Reasoning & Syllogism Structure (Statements & Conclusions)

**Correct JSON string value (double-backslash escaped, ready for API/SQL):**

```json
"**Statements:**\n1. All quantum computers capable of Shor's algorithm ($$Q$$) require coherent qubits with fidelity exceeding $$99.9\\%$$ ($$F$$).\n2. No noisy intermediate-scale quantum ($$NISQ$$) system achieves coherent qubit fidelity exceeding $$99.9\\%$$.\n3. System $$\\Psi$$ is a $$NISQ$$ system.\n\n**Conclusions:**\nI. System $$\\Psi$$ does not have coherent qubit fidelity exceeding $$99.9\\%$$.\nII. System $$\\Psi$$ is not capable of executing Shor's algorithm for large integers."
```

**Human-readable equivalent (what gets rendered):**
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
1. `**Statements:**` and `**Conclusions:**` sections are separated by `\n\n`.
2. Each numbered/roman item is preceded by `\n` — safe because digit and uppercase never form LaTeX commands.
3. Math commands (`\neg`, `\det`, `\neq`) are always inside `$$ ... $$` — never bare in the text.
4. Parenthetical math like `($$\det(A) \neq 0$$)` keeps both the parentheses and the formula on the **same line** as the statement text so the formula renders inline, not as a centred block that breaks the sentence.

### 2.5 Data Interpretation Tables

Use standard Markdown pipe tables:
```text
| Department | 2022 ($$\times 10^3$$) | 2023 ($$\times 10^3$$) | Growth ($$\%$$) |
|------------|------------------------|------------------------|-----------------|
| Physics    | 120                    | 150                    | $$+25\%$$       |
| Chemistry  | 90                     | 108                    | $$+20\%$$       |
```

---

## 3. Question Classification & Bloom's Taxonomy

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

## 4. Option & Answer Key Integrity Rules

### SINGLE_MCQ
- Exactly 4 options with IDs `"A"`, `"B"`, `"C"`, `"D"`.
- Exactly ONE option has `"isCorrect": true`.
- Exactly THREE options have `"isCorrect": false`.
- The `answerKey` MUST match the correct option letter (`"A"`, `"B"`, `"C"`, or `"D"`).

### MULTI_MCQ (MSQ)
- Exactly 4 options (`"A"`, `"B"`, `"C"`, `"D"`).
- At least 2 options have `"isCorrect": true`.

### NUMERICAL
- `options` is `null` or `[]`.
- `answerKey` contains the exact numeric string value (e.g., `"4.5"` or `"-12"`).

### DESCRIPTIVE
- `options` is `null`.
- `answerKey` contains the comprehensive model answer / scoring rubric.

---

## 5. Primary Key & Database Rules

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

## 6. Question Generation Context & Blueprint Assembly Reference

### 6.1 Blueprint Constraint Matching (`PaperAssemblyService`)
`PaperAssemblyService` queries the question bank by matching:
- `subject` (Exact case-sensitive match against `question_service.subject.name`)
- `topic` (Exact case-sensitive match against `question_service.topic.name`)
- `difficulty` (`EASY`, `MEDIUM`, `HARD`)
- `state = 'APPROVED'`

If the active pool contains fewer available questions than the rule quota (`needed`), paper assembly fails immediately with `InsufficientQuestionsException` and generates a gap report.

### 6.2 Context Reference: General Intelligence & Reasoning → Statement and Conclusion (HARD)

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

### 6.3 LaTeX Examples for Logic & Reasoning Questions

When writing statement-and-conclusion questions involving logic symbols, use the following patterns. The JSON column shows the exact string to store; the rendered column shows how it appears to candidates.

| Concept | JSON value (in content field) | Rendered |
|---|---|---|
| Not-equal | `$$\\neq 0$$` | $$\neq 0$$ |
| Negation | `$$\\neg L$$` | $$\neg L$$ |
| Determinant | `$$\\det(A)$$` | $$\det(A)$$ |
| Implication | `$$A \\implies B$$` | $$A \implies B$$ |
| Biconditional | `$$A \\iff B$$` | $$A \iff B$$ |
| For all | `$$\\forall x$$` | $$\forall x$$ |
| There exists | `$$\\exists x$$` | $$\exists x$$ |
| Intersection | `$$P \\cap Q = \\emptyset$$` | $$P \cap Q = \emptyset$$ |
| Percent | `$$99.9\\%$$` | $$99.9\%$$ |

**Common pitfall — parenthetical formulas at end of sentence:**

❌ Wrong (formula split onto next line, renders broken):
```
"1. A matrix $$A$$ is invertible if and only if its determinant is non-zero\n($$\\det(A) \\neq 0$$)."
```

✅ Correct (formula stays inline on the same line):
```
"1. A matrix $$A$$ is invertible if and only if its determinant is non-zero ($$\\det(A) \\neq 0$$)."
```

---

## 7. Chemistry Notation (mhchem)

The MathRenderer in both `candidate-frontend` and `frontend` loads the KaTeX **mhchem** contrib extension, which adds `\ce{}` (chemical equations) and `\pu{}` (physical units) support. No additional npm package is required — mhchem ships inside the `katex` package.

### 7.1 `\ce{}` — Chemical Equations

Wrap any chemical expression inside `$$\ce{ ... }$$`:

| What to write (JSON value) | Rendered output |
|---|---|
| `"$$\\ce{H2SO4}$$"` | Sulfuric acid — H₂SO₄ with correct subscripts |
| `"$$\\ce{2H2 + O2 -> 2H2O}$$"` | Balanced equation with reaction arrow |
| `"$$\\ce{Fe^{2+}}$$"` | Iron(II) ion with superscript charge |
| `"$$\\ce{CaCO3 -> CaO + CO2}$$"` | Decomposition reaction |
| `"$$\\ce{Na+ + Cl- -> NaCl}$$"` | Ionic equation |
| `"$$\\ce{H2O_{(l)}}$$"` | With state symbol |
| `"$$\\ce{^{235}_{92}U}$$"` | Isotope notation |

### 7.2 `\pu{}` — Physical Units

| What to write (JSON value) | Rendered output |
|---|---|
| `"$$\\pu{6.022e23 mol-1}$$"` | Avogadro's number |
| `"$$\\pu{8.314 J mol-1 K-1}$$"` | Gas constant |
| `"$$\\pu{1.6e-19 C}$$"` | Elementary charge |

### 7.3 Escaping Rules for Chemistry

Same rules as all other LaTeX — single backslash in human-readable text, double backslash in JSON/SQL:

```json
"The molar mass of $$\\ce{H2SO4}$$ is $$98 \\pu{g mol-1}$$."
```

### 7.4 What mhchem Does NOT Support

- **2D structural diagrams** (benzene rings, skeletal formulas, Newman projections) — these require a separate library (SmilesDrawer, RDKit.js). Out of scope for this platform.
- Use `\ce{}` for molecular formulae and reaction equations only.
