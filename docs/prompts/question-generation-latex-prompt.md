# AI Question Generation Prompt & Knowledge Specification (LaTeX Math Standard)

This document provides the standardized **System Prompt**, **User Prompt template**, **JSON Schema**, and **LaTeX `$$...$$` syntax rules** for generating high-quality examination questions for the National Assessment Grid (NAG).

---

## 1. Core LaTeX Math Rule

> [!IMPORTANT]
> **Strict Delimiter Syntax:**
> - **ALL** mathematical formulas, equations, expressions, variables, fractions, square roots, matrices, exponents, and unit notations in **EVERY** field (`content`, `options[].text`, `answerKey`, and `explanation`) **MUST** use `$$ ... $$` delimiters.
> - **NEVER** use `\( ... \)` or `\[ ... \]` or single `$ ... $`.
>
> **Examples:**
> - Inline variable: `$$x$$`, `$$\theta$$`, `$$\alpha$$`
> - Fraction: `$$\frac{-b \pm \sqrt{b^2 - 4ac}}{2a}$$`
> - Exponent / Index: `$$x^3 + \frac{1}{x^3} = 110$$`
> - Integral: `$$\int_{0}^{\frac{\pi}{2}} \sin^2(x)\, dx = \frac{\pi}{4}$$`
> - Metric units with math: `$$75(\sqrt{3} - 1)\text{ m}$$`, `$$11.2\text{ km/s}$$`

---

## 2. Reusable System Prompt Template

```markdown
You are an expert examination question generator for national-level Indian competitive examinations (SSC CGL, IBPS PO, RRB NTPC, CTET, UPSC CSE, JEE/NEET).
You generate rigorous, high-quality questions formatted in strict JSON.

### Formatting & Syntax Rules:
1. Content and formulas:
   - For all mathematical, chemical, and physical formulas, expressions, numbers with units, and variables, you MUST enclose them in $$ ... $$ LaTeX syntax.
   - Example: "If $$x + \frac{1}{x} = 5$$, find the value of $$x^3 + \frac{1}{x^3}$$."
   - DO NOT use \( ... \) or \[ ... \] or single $.
2. Option structure:
   - For SINGLE_MCQ: Exactly 4 options with ids "A", "B", "C", "D". Exactly ONE option has "isCorrect": true, and the other three have "isCorrect": false. The "answerKey" must be the matching option ID ("A", "B", "C", or "D").
   - For MULTI_MCQ: Exactly 4 options (A, B, C, D) where 2 or more options have "isCorrect": true.
   - For NUMERICAL: "options" is null or empty array, and "answerKey" contains the numeric string value.
   - For DESCRIPTIVE: "options" is null, and "answerKey" contains the comprehensive model solution.
3. Language: English only.
4. Explanations:
   - Provide step-by-step mathematical or logical derivations in the "explanation" field using $$...$$ LaTeX syntax.
5. Novelty:
   - Generate unique and original questions; do not duplicate referenced context verbatim.

### Output Format:
Return ONLY a valid JSON array of question objects without markdown wrapping or commentary.
```

---

## 3. JSON Output Schema

```json
[
  {
    "content": "Question stem text with $$LaTeX$$ notation",
    "answerKey": "A",
    "explanation": "Step-by-step solution with $$LaTeX$$ equations",
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

## 4. Reusable User Prompt Template

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

## 5. Exemplar JSON Questions

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

### Example 2: Trigonometry & Heights and Distances
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

### Example 3: Statistics & Empirical Relations
```json
{
  "content": "In a moderately skewed frequency distribution, if the mean is $$28.4$$ and the median is $$27.2$$, what is the empirical mode of the distribution?",
  "answerKey": "A",
  "explanation": "Using Pearson's empirical formula relating measures of central tendency: $$\\text{Mode} = 3(\\text{Median}) - 2(\\text{Mean})$$. Substituting: $$\\text{Mode} = 3(27.2) - 2(28.4) = 81.6 - 56.8 = 24.8$$.",
  "options": [
    {"id": "A", "text": "$$24.8$$", "isCorrect": true},
    {"id": "B", "text": "$$25.6$$", "isCorrect": false},
    {"id": "C", "text": "$$26.4$$", "isCorrect": false},
    {"id": "D", "text": "$$29.6$$", "isCorrect": false}
  ],
  "difficulty": "EASY",
  "cognitiveLevel": "REMEMBER",
  "questionType": "SINGLE_MCQ",
  "chapter": "Measures of Central Tendency",
  "references": "NCERT Class 10 Statistics"
}
```

---

## 6. Seed Examination to Subject Mapping Matrix

| Examination Name | Exam ID | Mapped Subject(s) | Key Mathematical Topics |
|---|---|---|---|
| **SSC CGL Tier-1** | `e1000000-0000-0000-0000-000000000001` | `Quantitative Aptitude / Mathematical Abilities`, `General Intelligence and Reasoning`, `General Awareness`, `English Language and Comprehension` | Algebra, Geometry, Mensuration, Trigonometry, Number Systems, Profit & Loss, Time & Work |
| **IBPS PO Preliminary** | `e1000000-0000-0000-0000-000000000002` | `Quantitative Aptitude / Mathematical Abilities`, `Reasoning`, `English` | Compound Interest, Ratio & Proportion, Quadratic Inequalities, Data Interpretation |
| **UPSC CSE GS Paper-1** | `e1000000-0000-0000-0000-000000000003` | `General Studies`, `General Awareness` | Indian Polity, Geography, Physical Sciences, Environmental Ecology |
| **RRB NTPC CBT-1** | `e1000000-0000-0000-0000-000000000004` | `Mathematics`, `General Awareness`, `General Intelligence and Reasoning` | Arithmetic, Elementary Surds, Statistics, Speed & Distance |
| **CTET Paper-1** | `e1000000-0000-0000-0000-000000000005` | `Child Development and Pedagogy`, `Mathematics`, `Environmental Ecology and Biodiversity` | Basic Number Operations, Shapes & Spatial Understanding |
