# AI Question Generation Prompt & Knowledge Specification (LaTeX Math Standard)

This document provides the standardized **System Prompt**, **User Prompt template**, **JSON Schema**, **LaTeX `$$...$$` syntax rules**, and **Markdown newline formatting guidelines** for generating high-quality examination questions for the National Assessment Grid (NAG).

---

## 1. Core LaTeX Math & Percentage Rules

> [!IMPORTANT]
> **Strict Delimiter Syntax:**
> - **ALL** mathematical formulas, equations, expressions, variables, fractions, square roots, matrices, exponents, percentages, and unit notations in **EVERY** field (`content`, `options[].text`, `answerKey`, and `explanation`) **MUST** use `$$ ... $$` delimiters.
> - **NEVER** use `\( ... \)` or `\[ ... \]` or single `$ ... $`.
> - **Percentage Signs in LaTeX**: Because `%` denotes a LaTeX comment, inside `$$ ... $$` you MUST write `\%` or `\text{%}` (e.g. `$$99.9\%$$`, `$$25\%$$`).
>
> **Examples:**
> - Inline variable: `$$x$$`, `$$\theta$$`, `$$\alpha$$`
> - Fraction: `$$\frac{-b \pm \sqrt{b^2 - 4ac}}{2a}$$`
> - Exponent / Index: `$$x^3 + \frac{1}{x^3} = 110$$`
> - Percentages: `$$99.9\%$$`, `$$75.5\%$$`
> - Integral: `$$\int_{0}^{\frac{\pi}{2}} \sin^2(x)\, dx = \frac{\pi}{4}$$`
> - Metric units with math: `$$75(\sqrt{3} - 1)\text{ m}$$`, `$$11.2\text{ km/s}$$`

---

## 2. Markdown, Tables & Newline Formatting Rules

1. **Newlines**:
   - In JSON strings, literal newlines are encoded as `\n`.
   - Separate major sections (e.g. `**Statements:**` and `**Conclusions:**`) with double newlines (`\n\n`) for clean paragraph rendering.
   - Separate individual numbered items with single newlines (`\n1. ...\n2. ...`).
2. **Markdown Bold & Structure**:
   - Use `**Statements:**` and `**Conclusions:**` for syllogisms and logical reasoning stems.
3. **Data Interpretation Tables**:
   - Format tabular data using Markdown pipe tables with double newline separation from the question prompt.

---

## 3. Reusable System Prompt Template

```markdown
You are an expert examination question generator for national-level Indian competitive examinations (SSC CGL, IBPS PO, RRB NTPC, CTET, UPSC CSE, JEE/NEET).
You generate rigorous, high-quality questions formatted in strict JSON.

### Formatting & Syntax Rules:
1. Content and formulas:
   - For all mathematical, chemical, and physical formulas, expressions, numbers with units, percentages, and variables, you MUST enclose them in $$ ... $$ LaTeX syntax.
   - For percentage symbols in LaTeX math mode, always use \% (e.g. $$99.9\%$$).
   - Example: "If $$x + \frac{1}{x} = 5$$, find the value of $$x^3 + \frac{1}{x^3}$$."
   - DO NOT use \( ... \) or \[ ... \] or single $.
2. Markdown & Newlines:
   - Format multi-line prompts (such as statements and conclusions, passages, or tables) using standard GitHub Flavored Markdown.
   - Use double newlines (\n\n) between headings and paragraphs, and single newlines (\n) between numbered list items.
3. Option structure:
   - For SINGLE_MCQ: Exactly 4 options with ids "A", "B", "C", "D". Exactly ONE option has "isCorrect": true, and the other three have "isCorrect": false. The "answerKey" must be the matching option ID ("A", "B", "C", or "D").
   - For MULTI_MCQ: Exactly 4 options (A, B, C, D) where 2 or more options have "isCorrect": true.
   - For NUMERICAL: "options" is null or empty array, and "answerKey" contains the numeric string value.
   - For DESCRIPTIVE: "options" is null, and "answerKey" contains the comprehensive model solution.
4. Language: English only.
5. Explanations:
   - Provide step-by-step mathematical or logical derivations in the "explanation" field using $$...$$ LaTeX syntax and clear line breaks.
6. Novelty:
   - Generate unique and original questions; do not duplicate referenced context verbatim.

### Output Format:
Return ONLY a valid JSON array of question objects without markdown wrapping or commentary.
```

---

## 4. JSON Output Schema

```json
[
  {
    "content": "Question stem text with $$LaTeX$$ notation and \\n\\n markdown line breaks",
    "answerKey": "A",
    "explanation": "Step-by-step solution with $$LaTeX$$ equations and \\n derivations",
    "options": [
      {"id": "A", "text": "$$\\text{Option A value}$$", "isCorrect": true},
      {"id": "B", "text": "$$\\text{Option B value}$$", "isCorrect": false},
      {"id": "C", "text": "$$\\text{Option C value}$$", "isCorrect": false},
      {"id": "D", "text": "$$\\text{Option D value}$$", "isCorrect": false}
    ],
    "difficulty": "EASY | MEDIUM | HARD",
    "cognitiveLevel": "REMEMBER | UNDERSTAND | APPLY | ANALYZE | EVALUATE | CREATE",
    "questionType": "SINGLE_MCQ | MULTI_MCQ | NUMERICAL | DESCRIPTIVE",
    "chapter": "Optional chapter name",
    "references": "Source / Syllabus benchmark (e.g., NCERT Class 10 / SSC CGL 2024)"
  }
]
```

---

## 5. Reusable User Prompt Template

```markdown
Generate {count} question(s) with the following specifications:
- Subject: {subject}
- Topic: {topic}
- Subtopic: {subtopic}
- Difficulty: {EASY | MEDIUM | HARD}
- Cognitive Level: {REMEMBER | UNDERSTAND | APPLY | ANALYZE | EVALUATE | CREATE}
- Question Type: {SINGLE_MCQ | MULTI_MCQ | NUMERICAL | DESCRIPTIVE}

Reference Context / Benchmark (Do NOT duplicate):
{referenceContext}

Generate the questions now as a valid JSON array:
```

---

## 6. Exemplar JSON Questions

### Example 1: Quantitative Aptitude (Algebra / Cubic Identity)
```json
{
  "content": "If $$x + \\frac{1}{x} = 5$$, what is the value of $$x^3 + \\frac{1}{x^3}$$?",
  "answerKey": "A",
  "explanation": "We know $$\\left(x + \\frac{1}{x}\\right)^3 = x^3 + \\frac{1}{x^3} + 3\\left(x + \\frac{1}{x}\\right)$$. Substituting $$x + \\frac{1}{x} = 5$$ gives: $$5^3 = x^3 + \\frac{1}{x^3} + 3(5) \\implies 125 = x^3 + \\frac{1}{x^3} + 15 \\implies x^3 + \\frac{1}{x^3} = 110$$.",
  "options": [
    {"id": "A", "text": "$$110$$", "isCorrect": true},
    {"id": "B", "text": "$$125$$", "isCorrect": false},
    {"id": "C", "text": "$$140$$", "isCorrect": false},
    {"id": "D", "text": "$$115$$", "isCorrect": false}
  ],
  "difficulty": "MEDIUM",
  "cognitiveLevel": "APPLY",
  "questionType": "SINGLE_MCQ",
  "chapter": "Polynomials and Algebraic Identities",
  "references": "SSC CGL Tier-1 / NCERT Class 9"
}
```

### Example 2: General Intelligence & Reasoning (Statements & Conclusions with LaTeX & Newlines)
```json
{
  "content": "**Statements:**\\n1. All quantum computers capable of Shor's algorithm ($$Q$$) require coherent qubits with fidelity exceeding $$99.9\\%$$ ($$F$$).\\n2. No noisy intermediate-scale quantum ($$NISQ$$) system achieves coherent qubit fidelity exceeding $$99.9\\%$$.\\n3. System $$\\Psi$$ is a $$NISQ$$ system.\\n\\n**Conclusions:**\\nI. System $$\\Psi$$ does not have coherent qubit fidelity exceeding $$99.9\\%$$.\\nII. System $$\\Psi$$ is not capable of executing Shor's algorithm for large integers.",
  "answerKey": "C",
  "explanation": "1. From Statement 2 and Statement 3: Since $$\\Psi$$ is a $$NISQ$$ system, and no $$NISQ$$ system achieves fidelity $$> 99.9\\%$$, it follows directly that $$\\Psi$$ has fidelity $$\\le 99.9\\%$$. Thus Conclusion I holds.\\n2. From Statement 1, capability of Shor's algorithm ($$Q$$) requires fidelity $$> 99.9\\%$$ ($$F$$), i.e., $$Q \\implies F$$. By contrapositive, $$\\neg F \\implies \\neg Q$$. Since $$\\Psi$$ has $$\\neg F$$, it cannot be capable of Shor's algorithm ($$\\neg Q$$). Thus Conclusion II also holds.\\nBoth Conclusions I and II follow.",
  "options": [
    {"id": "A", "text": "Only Conclusion I follows", "isCorrect": false},
    {"id": "B", "text": "Only Conclusion II follows", "isCorrect": false},
    {"id": "C", "text": "Both Conclusion I and Conclusion II follow", "isCorrect": true},
    {"id": "D", "text": "Neither Conclusion I nor Conclusion II follows", "isCorrect": false}
  ],
  "difficulty": "HARD",
  "cognitiveLevel": "ANALYZE",
  "questionType": "SINGLE_MCQ",
  "chapter": "Logical Deductions and Syllogisms",
  "references": "General Intelligence and Reasoning / SSC CGL Tier-1"
}
```

### Example 3: Trigonometry & Heights and Distances
```json
{
  "content": "From the top of a $$75\\text{ m}$$ high lighthouse above sea level, the angles of depression of two ships are $$30^\\circ$$ and $$45^\\circ$$. If one ship is directly behind the other on the same side of the lighthouse, what is the distance between the two ships?",
  "answerKey": "A",
  "explanation": "Let height $$h = 75\\text{ m}$$. Distance to the nearer ship: $$d_1 = \\frac{h}{\\tan(45^\\circ)} = 75\\text{ m}$$. Distance to the farther ship: $$d_2 = \\frac{h}{\\tan(30^\\circ)} = 75\\sqrt{3}\\text{ m}$$. Distance between ships $$= d_2 - d_1 = 75\\sqrt{3} - 75 = 75(\\sqrt{3} - 1)\\text{ m}$$.",
  "options": [
    {"id": "A", "text": "$$75(\\sqrt{3} - 1)\\text{ m}$$", "isCorrect": true},
    {"id": "B", "text": "$$75(\\sqrt{3} + 1)\\text{ m}$$", "isCorrect": false},
    {"id": "C", "text": "$$\\frac{75}{\\sqrt{3}}\\text{ m}$$", "isCorrect": false},
    {"id": "D", "text": "$$150(\\sqrt{3} - 1)\\text{ m}$$", "isCorrect": false}
  ],
  "difficulty": "MEDIUM",
  "cognitiveLevel": "APPLY",
  "questionType": "SINGLE_MCQ",
  "chapter": "Applications of Trigonometry",
  "references": "NCERT Class 10 Trigonometry"
}
```

---

## 7. Seed Examination to Subject Mapping Matrix

| Examination Name | Exam ID | Mapped Subject(s) | Key Mathematical Topics |
|---|---|---|---|
| **SSC CGL Tier-1** | `e1000000-0000-0000-0000-000000000001` | `Quantitative Aptitude / Mathematical Abilities`, `General Intelligence and Reasoning`, `General Awareness`, `English Language and Comprehension` | Algebra, Geometry, Mensuration, Trigonometry, Number Systems, Profit & Loss, Time & Work |
| **IBPS PO Preliminary** | `e1000000-0000-0000-0000-000000000002` | `Quantitative Aptitude / Mathematical Abilities`, `Reasoning`, `English` | Compound Interest, Ratio & Proportion, Quadratic Inequalities, Data Interpretation |
| **UPSC CSE GS Paper-1** | `e1000000-0000-0000-0000-000000000003` | `General Studies`, `General Awareness` | Indian Polity, Geography, Physical Sciences, Environmental Ecology |
| **RRB NTPC CBT-1** | `e1000000-0000-0000-0000-000000000004` | `Mathematics`, `General Awareness`, `General Intelligence and Reasoning` | Arithmetic, Elementary Surds, Statistics, Speed & Distance |
| **CTET Paper-1** | `e1000000-0000-0000-0000-000000000005` | `Child Development and Pedagogy`, `Mathematics`, `Environmental Ecology and Biodiversity` | Basic Number Operations, Shapes & Spatial Understanding |
