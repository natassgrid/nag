# NAG Explain --- Requirements & System Design

**Project:** Next-generation Assessment Grid (NAG)\
**Module:** NAG Explain / Educational Reasoning & Explanation Engine\
**Document Type:** Software Requirements Specification + Architecture &
Design\
**Status:** Proposed\
**Version:** 1.0\
**Target:** Open-source MVP → production-ready platform capability

------------------------------------------------------------------------

## 1. Executive Summary

NAG Explain is an educational explanation engine that transforms an
assessment question and its verified solution into a structured,
student-friendly explanation that can be rendered through multiple
synchronized representations:

-   Natural-language explanation
-   Mathematical notation
-   Symbols and expressions
-   Tables
-   Logical/reasoning graphs
-   Static diagrams
-   Interactive diagrams
-   Lightweight text-based animations
-   Hints
-   Common-mistake explanations
-   Verification steps

The central design principle is:

> **One verified reasoning model → many synchronized educational
> representations.**

The system must not depend on an LLM directly generating HTML, SVG,
JavaScript animation code, or arbitrary executable code. Instead, the AI
produces a constrained **Reasoning Proposal**, which is validated,
normalized, and converted into a canonical **Explanation IR
(Intermediate Representation)**.

The Explanation IR becomes the contract between the reasoning engine and
presentation layer.

------------------------------------------------------------------------

# 2. Goals

## 2.1 Primary Goals

1.  Explain answers rather than merely provide answers.
2.  Make reasoning visible step-by-step.
3.  Support mathematics, logical reasoning, and diagram-based
    explanations.
4.  Generate multiple representations from one underlying reasoning
    model.
5.  Validate mathematical and logical transformations wherever possible.
6.  Provide deterministic rendering from a safe declarative IR.
7.  Support interactive and lightweight animated explanations.
8.  Allow students to ask "Why?", "How?", and "Give me a hint".
9.  Support different explanation depths based on student level.
10. Integrate naturally with the existing NAG question/assessment
    platform.
11. Keep the core explanation model open, extensible, and
    vendor-neutral.
12. Reuse mature open-source visualization and mathematics libraries
    rather than rebuilding them.

## 2.2 Secondary Goals

-   Teacher/content-authoring support
-   Explanation versioning
-   Explanation quality evaluation
-   Misconception tracking
-   Explanation analytics
-   Localization
-   Accessibility
-   Future voice narration
-   Future adaptive tutoring

------------------------------------------------------------------------

# 3. Non-Goals for MVP

The MVP will not attempt to:

-   Build a general-purpose AI tutor.
-   Replace a full symbolic mathematics system.
-   Build a general-purpose drawing application.
-   Build a full video production engine.
-   Generate arbitrary executable JavaScript from an LLM.
-   Automatically solve every possible competitive-exam question.
-   Guarantee correctness for domains where no deterministic validator
    exists.

------------------------------------------------------------------------

# 4. Target Users

## 4.1 Student

Needs:

-   clear answer
-   step-by-step reasoning
-   visual explanation
-   hints
-   verification
-   ability to replay explanation
-   ability to ask why

## 4.2 Teacher / Content Author

Needs:

-   review generated explanations
-   edit explanation steps
-   add diagrams
-   add hints
-   identify misconceptions
-   publish approved explanations

## 4.3 Question Author

Needs:

-   associate concepts and expected solution
-   provide authoritative answer
-   optionally provide reasoning constraints

## 4.4 Platform Administrator

Needs:

-   configure explanation providers
-   configure AI models
-   manage safety policies
-   monitor explanation quality
-   manage versions

## 4.5 Developer

Needs:

-   stable IR schema
-   renderer APIs
-   extension points
-   deterministic behavior
-   testability

------------------------------------------------------------------------

# 5. Core Design Principle

The architecture is based on four distinct layers:

``` text
Question
   |
   v
Reasoning
   |
   v
Explanation IR
   |
   +----> Text Renderer
   +----> Math Renderer
   +----> Diagram Renderer
   +----> Animation Renderer
   +----> Accessibility Renderer
```

The LLM must never be the final authority for correctness.

Preferred pipeline:

``` text
Question
   |
   v
Question Analyzer
   |
   v
AI Reasoning Planner
   |
   v
Reasoning Proposal
   |
   v
Deterministic Validation
   |
   v
Explanation Compiler
   |
   v
Canonical Explanation IR
   |
   +------------------------------+
   |              |               |
   v              v               v
Text/Math      Diagram        Animation
Renderer       Renderer       Renderer
   |              |               |
   +--------------+---------------+
                  |
                  v
            Student Player
```

------------------------------------------------------------------------

# 6. Functional Requirements

## FR-001 Question Input

The system shall accept:

-   plain text questions
-   structured NAG questions
-   multiple-choice questions
-   numerical questions
-   mathematical expressions
-   image-based questions where supported by an upstream OCR/vision
    service
-   questions with known answers
-   questions with authoritative solution steps

## FR-002 Question Classification

The system shall classify a question into one or more domains:

-   Mathematics
-   Logical reasoning
-   Quantitative aptitude
-   Geometry
-   Algebra
-   Arithmetic
-   Probability
-   Statistics
-   Data interpretation
-   Physics
-   Chemistry
-   Biology
-   General reasoning

It shall also identify:

-   concepts
-   entities
-   quantities
-   units
-   relationships
-   constraints
-   unknowns
-   expected answer type

## FR-003 Reasoning Generation

The system shall generate a structured reasoning proposal containing:

-   interpretation
-   facts
-   assumptions
-   rules
-   intermediate results
-   transformations
-   calculations
-   conclusions
-   verification strategy

## FR-004 Reasoning Validation

The system shall validate:

-   mathematical calculations
-   equation transformations
-   unit consistency
-   logical consistency
-   references to known facts
-   final answer against authoritative answer where available

Invalid reasoning shall not be published automatically.

## FR-005 Explanation Generation

The system shall generate:

1.  Short answer
2.  Step-by-step explanation
3.  Detailed explanation
4.  Hints
5.  Verification
6.  Common mistakes
7.  Why alternatives are wrong, for MCQs

## FR-006 Multiple Representations

A single explanation step shall be capable of producing:

-   text
-   equation
-   symbol
-   table
-   diagram
-   graph
-   highlight
-   animation
-   verification

## FR-007 Interactive Diagrams

The system shall support:

-   points
-   lines
-   arrows
-   circles
-   rectangles
-   triangles
-   axes
-   grids
-   labels
-   graphs
-   trees
-   relationship diagrams
-   flow diagrams

## FR-008 Animation

The system shall support declarative actions:

-   SHOW
-   HIDE
-   HIGHLIGHT
-   FOCUS
-   MOVE
-   DRAW
-   TRANSFORM
-   REVEAL
-   EMPHASIZE
-   PAUSE
-   RESET

Animations shall reference semantic objects rather than arbitrary screen
coordinates whenever possible.

## FR-009 Student Interaction

Students shall be able to:

-   step forward
-   step backward
-   replay
-   pause
-   request a hint
-   reveal the next step
-   ask "Why?"
-   ask "How?"
-   request a simpler explanation
-   request a more detailed explanation

## FR-010 Misconception Support

An explanation may contain:

-   misconception identifier
-   incorrect reasoning
-   why it is wrong
-   corrected reasoning

## FR-011 Explanation Versioning

Explanations shall be versioned.

Example:

``` text
question-version
explanation-version
renderer-version
reasoning-engine-version
```

## FR-012 Human Review

Generated explanations shall support workflow states:

``` text
DRAFT
VALIDATING
NEEDS_REVIEW
APPROVED
PUBLISHED
DEPRECATED
REJECTED
```

## FR-013 Localization

The explanation model shall separate language content from
mathematical/visual semantics.

The same reasoning should be renderable in:

-   English
-   Hindi
-   other supported NAG languages

without rebuilding the reasoning model.

## FR-014 Accessibility

The system shall support:

-   screen-reader-friendly math
-   text alternatives for diagrams
-   keyboard navigation
-   reduced-motion mode
-   accessible step descriptions
-   sufficient semantic labels

------------------------------------------------------------------------

# 7. Explanation Model

## 7.1 Explanation

``` text
Explanation
|
+-- Problem
+-- Concepts
+-- Facts
+-- Assumptions
+-- Reasoning Graph
+-- Steps
+-- Representations
+-- Verification
+-- Hints
+-- Misconceptions
+-- Metadata
```

## 7.2 Reasoning Step

Every step should be atomic.

``` text
ReasoningStep
|
+-- id
+-- type
+-- inputs
+-- operation
+-- outputs
+-- justification
+-- representations
+-- verification
+-- misconception
```

Supported initial types:

``` text
GIVEN
DEFINE
OBSERVE
IDENTIFY
APPLY_RULE
SUBSTITUTE
CALCULATE
TRANSFORM
COMPARE
ELIMINATE
INFER
CONCLUDE
VERIFY
```

------------------------------------------------------------------------

# 8. Explanation IR

The Explanation IR is the most important NAG-owned component.

Example:

``` json
{
  "schemaVersion": "1.0",
  "explanationId": "exp-001",
  "questionId": "q-001",

  "problem": {
    "statement": "If 3x + 5 = 20, find x."
  },

  "concepts": [
    "linear-equation",
    "inverse-operation"
  ],

  "facts": [],

  "steps": [
    {
      "id": "s1",
      "type": "SUBTRACT",
      "operation": {
        "target": "both-sides",
        "value": 5
      },
      "before": "3x + 5 = 20",
      "after": "3x = 15",
      "justification": {
        "text": "Subtracting the same value from both sides preserves equality."
      },
      "representations": [
        "TEXT",
        "MATH",
        "HIGHLIGHT",
        "ANIMATION"
      ]
    },
    {
      "id": "s2",
      "type": "DIVIDE",
      "operation": {
        "target": "both-sides",
        "value": 3
      },
      "before": "3x = 15",
      "after": "x = 5",
      "representations": [
        "TEXT",
        "MATH",
        "ANIMATION"
      ]
    }
  ],

  "verification": {
    "type": "SUBSTITUTION",
    "expression": "3 * 5 + 5 = 20",
    "expected": true
  }
}
```

------------------------------------------------------------------------

# 9. Reasoning Graph

The canonical reasoning representation shall support graph
relationships.

``` text
Fact A
  |
  v
Rule
  |
  +----> Intermediate Result A
  |
  +----> Intermediate Result B
                |
                v
            Conclusion
```

Each node should have:

``` text
id
type
content
source
confidence
validationStatus
```

Each edge should have:

``` text
from
to
relationship
justification
```

Relationships may include:

``` text
DEPENDS_ON
DERIVED_FROM
CONTRADICTS
SUPPORTS
REQUIRES
ELIMINATES
VERIFIES
```

------------------------------------------------------------------------

# 10. Mathematical Representation

The system should distinguish between display mathematics and semantic
mathematics.

Preferred internal representation:

``` text
Semantic Math AST
       |
       +----> LaTeX
       +----> MathML
       +----> MathJSON-compatible structure
       +----> Verification engine
```

Example:

``` json
{
  "type": "divide",
  "numerator": {
    "symbol": "d"
  },
  "denominator": {
    "symbol": "t"
  }
}
```

renders as:

``` text
v = d / t
```

This is preferable to storing only a string.

------------------------------------------------------------------------

# 11. Diagram Model

Diagram IR shall be semantic.

Example:

``` json
{
  "type": "geometry",
  "objects": [
    {
      "id": "A",
      "type": "POINT",
      "position": [0, 3],
      "label": "A"
    },
    {
      "id": "B",
      "type": "POINT",
      "position": [-2, 0],
      "label": "B"
    },
    {
      "id": "C",
      "type": "POINT",
      "position": [2, 0],
      "label": "C"
    }
  ],
  "relationships": [
    {
      "type": "LINE",
      "from": "A",
      "to": "B"
    },
    {
      "type": "LINE",
      "from": "A",
      "to": "C"
    },
    {
      "type": "LINE",
      "from": "B",
      "to": "C"
    }
  ]
}
```

------------------------------------------------------------------------

# 12. Animation IR

Animations shall be declarative.

Example:

``` json
{
  "timeline": [
    {
      "at": 0,
      "action": "SHOW",
      "target": "equation"
    },
    {
      "at": 1000,
      "action": "HIGHLIGHT",
      "target": "term-5"
    },
    {
      "at": 2000,
      "action": "TRANSFORM",
      "target": "equation",
      "operation": "SUBTRACT",
      "value": 5
    },
    {
      "at": 3500,
      "action": "REVEAL",
      "target": "result"
    }
  ]
}
```

Semantic animation is preferred:

``` text
MOVE(point=A, to=B)
```

over:

``` text
MOVE(x=134, y=87)
```

------------------------------------------------------------------------

# 13. Renderer Architecture

## 13.1 Text Renderer

Input:

``` text
Explanation IR
```

Output:

``` text
Accessible HTML / React components
```

## 13.2 Math Renderer

Recommended libraries:

-   MathLive
-   KaTeX

Responsibilities:

-   render expressions
-   editable expressions where required
-   accessibility
-   math input
-   conversion between semantic representation and display notation

## 13.3 Calculation Engine

Recommended:

-   Math.js for browser/server calculations
-   SymPy service for advanced symbolic verification

## 13.4 Diagram Renderer

Recommended:

-   JSXGraph for mathematical/geometry interaction
-   SVG for simple diagrams
-   Mermaid for logical/flow diagrams
-   React Flow for reasoning graphs

## 13.5 Animation Renderer

Recommended MVP:

-   SVG
-   CSS transitions
-   React Spring

Optional:

-   Manim for server-generated mathematical videos

------------------------------------------------------------------------

# 14. Recommended Open-Source Components

  -----------------------------------------------------------------------
  Component               Purpose                 Recommendation
  ----------------------- ----------------------- -----------------------
  MathLive                Math input/rendering    Adopt

  KaTeX                   Fast math rendering     Adopt

  Math.js                 Numerical/symbolic      Adopt
                          calculation             

  SymPy                   Advanced symbolic       Optional service
                          verification            

  JSXGraph                Interactive geometry    Adopt

  Mermaid                 Logical/flow diagrams   Adopt

  React Flow              Reasoning graph UI      Adopt

  Excalidraw              Teacher whiteboard      Later
                          authoring               

  React Spring            Browser animation       Adopt

  Manim Community         High-quality math       Later
                          animation/video         

  Mathigon                Architectural           Study; review licenses
                          reference/inspiration   before reuse
  -----------------------------------------------------------------------

NAG should avoid reimplementing these capabilities unless a specific
product requirement demands it.

------------------------------------------------------------------------

# 15. System Architecture

``` text
                         NAG Platform
                              |
                +-------------+-------------+
                |                           |
         Question Service             Learning UI
                |                           |
                +-------------+-------------+
                              |
                       Explanation API
                              |
                 +------------+-------------+
                 |                          |
                 v                          v
        Question Analyzer          Explanation Repository
                 |
                 v
        Reasoning Orchestrator
                 |
       +---------+----------+
       |                    |
       v                    v
  AI Reasoner        Knowledge/Rules
       |                    |
       +---------+----------+
                 |
                 v
         Reasoning Proposal
                 |
                 v
        Validation Pipeline
                 |
       +---------+----------+
       |         |          |
       v         v          v
     Math     Logic      Domain
   Validator Validator   Validator
       |         |          |
       +---------+----------+
                 |
                 v
         Explanation Compiler
                 |
                 v
          Canonical IR
                 |
        +--------+--------+---------+
        |        |        |         |
        v        v        v         v
      Text     Math    Diagram   Animation
    Renderer  Renderer Renderer  Renderer
        |        |        |         |
        +--------+--------+---------+
                 |
                 v
            Student Player
```

------------------------------------------------------------------------

# 16. Backend Architecture

Recommended technology:

``` text
Java 21
Spring Boot
Spring AI
PostgreSQL
Redis
OpenTelemetry
Docker
Kubernetes
```

For MVP, keep the explanation engine as a module within an existing NAG
service rather than immediately creating multiple microservices.

Suggested package structure:

``` text
com.nag.explain
|
+-- api
|   +-- ExplanationController
|   +-- HintController
|   +-- ValidationController
|
+-- application
|   +-- ExplanationService
|   +-- ReasoningService
|   +-- ValidationService
|   +-- CompilationService
|
+-- domain
|   +-- Explanation
|   +-- ReasoningGraph
|   +-- ReasoningStep
|   +-- MathExpression
|   +-- Diagram
|   +-- Animation
|
+-- ai
|   +-- ReasoningPlanner
|   +-- PromptRepository
|   +-- ModelRouter
|
+-- validation
|   +-- MathValidator
|   +-- LogicValidator
|   +-- AnswerValidator
|
+-- repository
|   +-- ExplanationRepository
|   +-- TemplateRepository
|   +-- FeedbackRepository
|
+-- compiler
|   +-- ExplanationCompiler
|
+-- renderer
|   +-- RendererRegistry
```

------------------------------------------------------------------------

# 17. AI Architecture

The AI layer shall use structured output.

Preferred flow:

``` text
System Prompt
+
Question
+
Known Answer
+
Domain Rules
+
Explanation Policy
       |
       v
LLM
       |
       v
Strict JSON Reasoning Proposal
       |
       v
JSON Schema Validation
       |
       v
Domain Validation
```

The LLM shall not be allowed to:

-   execute arbitrary code
-   produce arbitrary JavaScript
-   directly manipulate DOM
-   create unrestricted SVG
-   invoke external systems without tools
-   bypass validation

------------------------------------------------------------------------

# 18. AI Model Strategy

The system should support model abstraction.

``` text
ModelRouter
|
+-- Local Ollama
+-- AWS Bedrock
+-- Google
+-- OpenAI-compatible provider
```

The model should be selected based on:

-   domain
-   difficulty
-   latency
-   cost
-   context size
-   required reasoning quality

For MVP, smaller models may handle:

-   classification
-   formatting
-   simple explanation

Stronger models should handle:

-   complex reasoning
-   ambiguous questions
-   advanced explanations

Deterministic engines remain responsible for verification.

------------------------------------------------------------------------

# 19. Explanation Policies

An explanation policy controls pedagogical behavior.

Example:

``` json
{
  "level": "BEGINNER",
  "showFormula": true,
  "showEveryCalculation": true,
  "showWhy": true,
  "showVerification": true,
  "showMisconceptions": true,
  "animation": "LIGHT"
}
```

Levels:

``` text
BEGINNER
INTERMEDIATE
ADVANCED
EXAM
EXPERT
```

------------------------------------------------------------------------

# 20. Hint Engine

Hints should be progressive.

Example:

``` text
Hint 1:
"What information do we already know?"

Hint 2:
"Which formula connects distance and time?"

Hint 3:
"Try speed = distance / time."

Hint 4:
"Substitute 120 for distance and 2 for time."
```

The hint engine must avoid revealing the full answer prematurely.

------------------------------------------------------------------------

# 21. "Why?" Engine

Every reasoning step should optionally expose its justification.

Example:

``` text
Step:
3x + 5 = 20

Action:
Subtract 5 from both sides.

Why?
Because performing the same operation on both sides preserves equality.
```

The system should be able to recursively explain:

``` text
Why subtract 5?
    |
    v
We want to isolate 3x.
    |
    v
Why?
    |
    v
The next operation is division by 3.
```

A maximum explanation depth must prevent infinite recursion.

------------------------------------------------------------------------

# 22. MCQ Explanation

For multiple-choice questions:

``` text
Question
 |
 +-- Option A -> eliminate -> reason
 |
 +-- Option B -> eliminate -> reason
 |
 +-- Option C -> correct -> proof
 |
 +-- Option D -> eliminate -> reason
```

This should be represented explicitly rather than as prose only.

------------------------------------------------------------------------

# 23. Example: Logical Reasoning

Question:

> A is taller than B. B is taller than C. Who is shortest?

Canonical representation:

``` text
Facts:
A > B
B > C

Inference:
A > B > C

Conclusion:
C is shortest.
```

Diagram:

``` text
A  ██████████
B  ███████
C  ████
```

Reasoning graph:

``` text
A > B ----+
           |
B > C ----+--> A > B > C --> C shortest
```

Animation:

``` text
A > B
   +
B > C
   |
   v
A > B > C
   |
   v
C is shortest
```

------------------------------------------------------------------------

# 24. Example: Algebra

Question:

``` text
3x + 5 = 20
```

Explanation:

``` text
Step 1:
Subtract 5 from both sides.

3x + 5 - 5 = 20 - 5

3x = 15

Step 2:
Divide both sides by 3.

3x / 3 = 15 / 3

x = 5
```

Verification:

``` text
3(5) + 5 = 20
15 + 5 = 20
20 = 20 ✓
```

------------------------------------------------------------------------

# 25. Example: Geometry

Question:

> Why is the sum of the angles of a triangle 180°?

The Explanation IR may contain:

``` text
Objects:
A, B, C

Relationships:
AB
BC
CA

Construction:
Line through A parallel to BC

Reasoning:
alternate-interior-angle relationship

Conclusion:
A + B + C = 180°
```

JSXGraph can render the geometry and animation can progressively
construct the parallel line.

------------------------------------------------------------------------

# 26. Rendering Contract

The frontend should not know how the explanation was generated.

It receives:

``` http
GET /api/explanations/{questionId}
```

and receives:

``` json
{
  "schemaVersion": "1.0",
  "content": {},
  "steps": [],
  "representations": {},
  "verification": {}
}
```

React uses a renderer registry:

``` typescript
RendererRegistry.register("TEXT", TextRenderer);
RendererRegistry.register("MATH", MathRenderer);
RendererRegistry.register("DIAGRAM", DiagramRenderer);
RendererRegistry.register("REASONING_GRAPH", ReasoningGraphRenderer);
RendererRegistry.register("ANIMATION", AnimationRenderer);
```

This makes the system extensible.

------------------------------------------------------------------------

# 27. Frontend Components

``` text
ExplanationPlayer
|
+-- ExplanationHeader
+-- ProblemView
+-- ConceptView
+-- StepNavigator
|   +-- Previous
|   +-- Next
|   +-- Replay
|
+-- StepRenderer
|   +-- TextRenderer
|   +-- MathRenderer
|   +-- DiagramRenderer
|   +-- GraphRenderer
|   +-- AnimationRenderer
|
+-- HintPanel
+-- WhyPanel
+-- VerificationPanel
+-- MisconceptionPanel
```

------------------------------------------------------------------------

# 28. Animation Player

The animation player should support:

``` text
PLAY
PAUSE
RESET
NEXT
PREVIOUS
SPEED 0.5x / 1x / 1.5x / 2x
REDUCED_MOTION
```

Animation should never be required to understand the solution.

Text and static representations must remain sufficient.

------------------------------------------------------------------------

# 29. Persistence Model

Recommended PostgreSQL tables:

``` text
explanation
----------------
id
tenant_id
question_id
version
status
difficulty
language
schema_version
content_json
created_by
created_at
updated_at
published_at
```

``` text
explanation_step
----------------
id
explanation_id
sequence
step_type
content_json
validation_status
```

``` text
explanation_validation
----------------
id
explanation_id
validator
status
score
details_json
created_at
```

``` text
explanation_feedback
----------------
id
explanation_id
step_id
user_id
feedback_type
comment
created_at
```

``` text
explanation_template
----------------
id
tenant_id
domain
template_type
content_json
version
status
```

------------------------------------------------------------------------

# 30. Multi-Tenancy

NAG Explain shall follow the existing NAG multi-tenant architecture.

Every persisted entity must include:

``` text
tenant_id
```

Tenant-specific configuration may include:

-   explanation policies
-   allowed models
-   language
-   domain templates
-   review workflow
-   AI provider
-   token limits
-   animation policy

------------------------------------------------------------------------

# 31. API Design

## Generate Explanation

``` http
POST /api/v1/explanations
```

Request:

``` json
{
  "questionId": "q-001",
  "mode": "STEP_BY_STEP",
  "language": "en",
  "studentLevel": "INTERMEDIATE"
}
```

## Get Explanation

``` http
GET /api/v1/explanations/{id}
```

## Get Step

``` http
GET /api/v1/explanations/{id}/steps/{stepId}
```

## Generate Hint

``` http
POST /api/v1/explanations/{id}/hints
```

## Explain Why

``` http
POST /api/v1/explanations/{id}/steps/{stepId}/why
```

## Validate

``` http
POST /api/v1/explanations/{id}/validate
```

## Review

``` http
POST /api/v1/explanations/{id}/review
```

## Publish

``` http
POST /api/v1/explanations/{id}/publish
```

------------------------------------------------------------------------

# 32. Security Requirements

The renderer must treat Explanation IR as untrusted input.

Controls:

-   JSON schema validation
-   allow-listed node types
-   allow-listed animation actions
-   no arbitrary JavaScript
-   no arbitrary HTML
-   SVG sanitization
-   URL allow-listing
-   maximum object counts
-   maximum animation duration
-   maximum explanation depth
-   prompt-injection defenses
-   tenant isolation

LLM output must never be directly rendered as executable code.

------------------------------------------------------------------------

# 33. Performance Requirements

MVP targets:

-   cached explanation retrieval: \< 200 ms server-side
-   static rendering: \< 500 ms client-side for typical explanations
-   first explanation generation: target \< 10 seconds depending on
    model
-   animation should maintain smooth browser playback
-   diagrams should remain responsive on standard student devices

Large explanations should support lazy loading.

------------------------------------------------------------------------

# 34. Observability

Use OpenTelemetry.

Track:

``` text
explanation.request
explanation.generation
explanation.validation
explanation.render
explanation.hint
explanation.why
explanation.replay
```

Metrics:

``` text
generation_latency
validation_latency
validation_failure_rate
explanation_publish_rate
student_completion_rate
hint_usage
why_usage
replay_count
explanation_error_rate
renderer_error_rate
```

------------------------------------------------------------------------

# 35. Quality Metrics

An explanation should be evaluated on:

``` text
Correctness
Completeness
Clarity
Pedagogical quality
Visual usefulness
Conciseness
Age appropriateness
Accessibility
Verification coverage
```

Possible score:

``` text
Explanation Quality Score =
  correctness * 0.35
+ reasoning completeness * 0.20
+ pedagogical clarity * 0.20
+ verification * 0.15
+ representation quality * 0.10
```

Weights should be configurable.

------------------------------------------------------------------------

# 36. Testing Strategy

## Unit Tests

Test:

-   IR parsing
-   schema validation
-   mathematical transformations
-   graph construction
-   animation compilation
-   renderer selection

## Property Tests

For mathematical transformations:

``` text
If transformation is valid:
    evaluate(before) == evaluate(after)
```

where appropriate.

## Integration Tests

Test:

``` text
Question
 -> AI Proposal
 -> Validation
 -> Explanation IR
 -> API
 -> React renderer
```

## Golden Tests

Maintain approved JSON explanations and compare generated IR.

## Visual Regression Tests

For:

-   equations
-   diagrams
-   animations
-   reasoning graphs

## Security Tests

Test malicious IR such as:

``` text
<script>
javascript:
data:
external SVG
```

and oversized recursive structures.

------------------------------------------------------------------------

# 37. Content Authoring Workflow

``` text
Question
   |
Generate explanation
   |
AI Draft
   |
Validation
   |
Teacher Review
   |
Edit
   |
Preview
   |
Approve
   |
Publish
```

Teachers should be able to modify:

-   text
-   reasoning steps
-   hints
-   diagram labels
-   animation timing
-   misconceptions

without changing the underlying question.

------------------------------------------------------------------------

# 38. Explanation Templates

Templates should exist for common problem families.

Examples:

``` text
ALGEBRA_LINEAR_EQUATION
PERCENTAGE_CHANGE
RATIO_PROPORTION
AVERAGE
TIME_SPEED_DISTANCE
NUMBER_SERIES
SYLLOGISM
BLOOD_RELATION
DIRECTION_SENSE
RANKING
SEATING_ARRANGEMENT
PROBABILITY
TRIANGLE_GEOMETRY
DATA_INTERPRETATION
```

Templates should provide:

-   expected reasoning structure
-   available operations
-   validation rules
-   common misconceptions
-   preferred visual representation

------------------------------------------------------------------------

# 39. Domain Plugin Architecture

New domains should be pluggable.

``` text
ExplanationDomain
|
+-- classify()
+-- buildReasoningModel()
+-- validate()
+-- recommendRepresentations()
+-- generateHints()
+-- detectMisconceptions()
```

Example:

``` java
public interface ExplanationDomain {

    boolean supports(Question question);

    ReasoningProposal generateReasoning(Question question);

    ValidationResult validate(ReasoningProposal proposal);

    RepresentationPlan planRepresentations(
        ReasoningModel reasoning
    );
}
```

------------------------------------------------------------------------

# 40. Renderer Plugin Architecture

``` java
public interface ExplanationRenderer {

    String type();

    RenderedRepresentation render(
        ExplanationIR explanation,
        RenderContext context
    );
}
```

Potential renderer types:

``` text
TEXT
MATH
TABLE
SVG
GEOMETRY
REASONING_GRAPH
MERMAID
ANIMATION
AUDIO
```

------------------------------------------------------------------------

# 41. Explanation Compiler

The compiler transforms:

``` text
Reasoning Proposal
       |
       v
Normalization
       |
       v
Validation
       |
       v
Pedagogical Planning
       |
       v
Representation Planning
       |
       v
Canonical Explanation IR
```

It should:

-   remove redundant steps
-   normalize expressions
-   assign stable IDs
-   build dependencies
-   detect unsupported operations
-   attach validation metadata
-   select renderers
-   create animation events

------------------------------------------------------------------------

# 42. Deterministic vs AI Responsibilities

  Responsibility                        AI                Deterministic
  -------------------------- ------------- ----------------------------
  Question classification          Primary          Optional validation
  Concept identification           Primary                        Rules
  Reasoning proposal               Primary                          ---
  Arithmetic                        Assist                  **Primary**
  Symbolic verification             Assist                  **Primary**
  Logical graph validation          Assist   **Primary where possible**
  Text explanation             **Primary**                Policy checks
  Diagram planning                 Primary                     Renderer
  SVG rendering                         No                  **Primary**
  Animation rendering                   No                  **Primary**
  Safety validation                 Assist                  **Primary**

------------------------------------------------------------------------

# 43. Example End-to-End Flow

Question:

``` text
A train travels 120 km in 2 hours.
What is its average speed?
```

### Step 1 --- Analyze

``` text
distance = 120 km
time = 2 h
unknown = speed
concept = speed
```

### Step 2 --- Reason

``` text
speed = distance / time
```

### Step 3 --- Substitute

``` text
speed = 120 / 2
```

### Step 4 --- Calculate

``` text
speed = 60 km/h
```

### Step 5 --- Verify

``` text
60 × 2 = 120 km
```

### Step 6 --- Represent

``` text
Text:
Average speed is distance divided by time.

Math:
v = d/t

Diagram:
A ---------------- B
       120 km
       → 2 h

Animation:
distance appears
→ time appears
→ formula appears
→ substitution
→ answer
→ verification
```

------------------------------------------------------------------------

# 44. MVP Scope

## Phase 1 --- Foundation

Implement:

-   Explanation IR
-   JSON Schema
-   reasoning steps
-   text renderer
-   math renderer
-   deterministic validation
-   basic hints
-   basic why explanations
-   REST API
-   persistence

Supported domains:

1.  Algebra
2.  Arithmetic
3.  Number series
4.  Basic logical reasoning
5.  Percentage/ratio

## Phase 2 --- Visual

Implement:

-   SVG diagram IR
-   JSXGraph integration
-   reasoning graph
-   Mermaid
-   animation timeline
-   React animation player

## Phase 3 --- Authoring

Implement:

-   teacher editor
-   explanation editing
-   diagram authoring
-   review workflow
-   versioning
-   publishing

## Phase 4 --- Advanced AI

Implement:

-   misconception detection
-   adaptive explanations
-   explanation difficulty
-   personalized hints
-   multi-language explanations
-   model routing
-   feedback learning

## Phase 5 --- Advanced Media

Optional:

-   Manim video generation
-   narration
-   audio synchronization
-   richer simulations
-   physics visualization

------------------------------------------------------------------------

# 45. Suggested Repository Structure

``` text
nag/
|
+-- services/
|   +-- question-service/
|   +-- explanation-service/
|
+-- libraries/
|   +-- explanation-ir/
|   +-- reasoning-core/
|   +-- math-core/
|   +-- validation-core/
|
+-- frontend/
|   +-- explanation-player/
|   +-- explanation-authoring/
|
+-- schemas/
|   +-- explanation-ir/
|   +-- animation-ir/
|   +-- diagram-ir/
|
+-- docs/
|   +-- explanation/
|       +-- requirements.md
|       +-- architecture.md
|       +-- ir-spec.md
```

For a monorepo, the IR schema should be treated as a first-class public
contract.

------------------------------------------------------------------------

# 46. Versioning Strategy

Every IR schema must have:

``` text
major.minor
```

Example:

``` text
1.0
1.1
2.0
```

Rules:

-   minor versions must remain backward compatible
-   major versions may introduce breaking changes
-   old explanations must remain renderable
-   renderer compatibility must be tracked

------------------------------------------------------------------------

# 47. Caching

Cache at multiple levels:

``` text
Question
   |
   +-- Reasoning Proposal cache
   |
   +-- Explanation IR cache
   |
   +-- Rendered representation cache
```

Cache key should include:

``` text
tenant
question version
explanation policy
language
model version
IR version
```

------------------------------------------------------------------------

# 48. Offline / Local AI Support

Because NAG is open source, the architecture should support local
models.

Example:

``` text
Spring AI
   |
   +-- Ollama
   +-- AWS Bedrock
   +-- OpenAI-compatible
   +-- Google
```

The Explanation IR remains provider-independent.

------------------------------------------------------------------------

# 49. Open-Source Strategy

The following should be NAG-owned:

``` text
Explanation IR
Reasoning Graph Model
Explanation Compiler
Pedagogical Planner
Domain Templates
Validation Integration
Renderer Contracts
Student Explanation Player
```

Existing projects should be reused for:

``` text
Math rendering
Symbolic mathematics
Geometry
Graphs
Animation
Whiteboard
```

This keeps NAG's unique contribution focused and maintainable.

------------------------------------------------------------------------

# 50. Future Research Direction

Potential future feature:

## Universal Reasoning Representation

Represent knowledge as:

``` text
Entities
+
Facts
+
Rules
+
Constraints
+
Transformations
+
Evidence
+
Verification
```

This could support:

``` text
Math
Logic
Physics
Chemistry
Programming
Economics
Data Interpretation
```

The long-term architecture becomes:

``` text
                  Universal Reasoning Model
                           |
       +-------------------+-------------------+
       |                   |                   |
       v                   v                   v
     Text                Math              Visual
       |                   |                   |
       +-------------------+-------------------+
                           |
                      Animation
                           |
                           v
                    Student Learning
```

------------------------------------------------------------------------

# 51. Success Criteria

The MVP is successful if NAG can take a supported question and produce:

1.  Correct answer
2.  Correct reasoning steps
3.  Human-readable explanation
4.  Mathematical notation where applicable
5.  At least one useful visual representation
6.  Deterministic verification
7.  Progressive hints
8.  "Why?" explanation for each major step
9.  Replayable step-by-step presentation
10. Human-reviewable structured IR

A sample target:

``` text
Question
   ↓
< 10 sec generation
   ↓
Validated Explanation IR
   ↓
Text + Math + Diagram
   ↓
Interactive student explanation
```

------------------------------------------------------------------------

# 52. Architectural Principles

1.  **AI proposes; deterministic systems verify.**
2.  **IR is the source of truth for presentation.**
3.  **Semantic objects are preferred over coordinates.**
4.  **Rendering must be deterministic.**
5.  **Every reasoning step should be independently explainable.**
6.  **Animations are optional representations, not the reasoning
    itself.**
7.  **Accessibility must be built into the IR.**
8.  **Human review must be supported.**
9.  **Domain logic must be pluggable.**
10. **LLM providers must remain replaceable.**
11. **Tenant isolation must be preserved.**
12. **Open-source dependencies should be preferred over custom
    implementations.**
13. **The Explanation IR should be versioned as a public contract.**
14. **Never trust raw model output.**
15. **The system should teach the reasoning process, not merely expose
    the answer.**

------------------------------------------------------------------------

# 53. Final Recommended Technology Stack

``` text
Backend
--------
Java 21
Spring Boot
Spring AI
PostgreSQL
Redis
OpenTelemetry

AI
--
AWS Bedrock
Ollama
OpenAI-compatible providers
Structured JSON output

Mathematics
-----------
MathLive
KaTeX
Math.js
SymPy

Visualization
-------------
JSXGraph
SVG
Mermaid
React Flow

Animation
---------
SVG
CSS
React Spring

Optional Video
--------------
Manim Community

Frontend
--------
React
TypeScript
NAG Design System

Deployment
----------
Docker
Kubernetes
GitLab CI/CD
```

------------------------------------------------------------------------

# 54. Recommended Initial Implementation

Do not start by integrating every library.

Build this vertical slice first:

``` text
Question
   ↓
Spring AI
   ↓
Structured Reasoning Proposal
   ↓
NAG Explanation Compiler
   ↓
Explanation IR
   ↓
Math.js validation
   ↓
React
   ├── Text
   ├── KaTeX/MathLive
   └── simple SVG
```

Implement approximately 20--30 representative questions.

Then add:

``` text
JSXGraph
React Flow
Animation IR
```

Only after the IR proves stable should additional renderers be
introduced.

------------------------------------------------------------------------

# 55. Most Important Product Decision

The strategic differentiator should be:

> **NAG Explain is not an LLM answer generator. It is a structured
> reasoning-to-explanation engine.**

The LLM is one possible reasoning assistant.

The durable NAG asset is:

``` text
                 Question
                    |
                    v
             Reasoning Model
                    |
                    v
             Explanation IR
                    |
       +------------+-------------+
       |            |             |
       v            v             v
      Text         Math        Visual
       |            |             |
       +------------+-------------+
                    |
                    v
                Animation
                    |
                    v
              Student Tutor
```

This architecture allows NAG to replace the AI model, math engine,
diagram library, or renderer independently without redesigning the
educational content model.

------------------------------------------------------------------------

# 56. Immediate Next Deliverables

Recommended engineering sequence:

### D1 --- Explanation IR Specification

Create:

``` text
explanation-ir.schema.json
reasoning-step.schema.json
math-expression.schema.json
diagram.schema.json
animation.schema.json
```

### D2 --- Java Domain Model

Implement:

``` text
Explanation
ReasoningGraph
ReasoningStep
MathExpression
Diagram
AnimationTimeline
VerificationResult
```

### D3 --- Explanation Compiler

``` text
ReasoningProposal
        ↓
Validation
        ↓
Normalization
        ↓
ExplanationIR
```

### D4 --- First React Player

Support:

``` text
TEXT
MATH
HIGHLIGHT
STEP
VERIFY
```

### D5 --- First Five Domains

``` text
Algebra
Arithmetic
Number Series
Syllogism
Percentage/Ratio
```

### D6 --- Visual Layer

``` text
SVG
JSXGraph
React Flow
Animation IR
```

### D7 --- Authoring and Review

``` text
Generate
Review
Edit
Validate
Approve
Publish
```

------------------------------------------------------------------------

# 57. Conclusion

There is no need for NAG to build a new mathematical rendering engine,
geometry engine, graph engine, or animation framework.

The strongest architecture is to combine mature open-source components
while making the **NAG Explanation IR + Reasoning Compiler + Pedagogical
Planner** the central innovation.

The resulting system can evolve from:

``` text
AI-generated solution
```

to:

``` text
AI-assisted reasoning
        ↓
verified reasoning graph
        ↓
pedagogically structured explanation
        ↓
text + symbols + mathematics + diagrams
        ↓
interactive animation
        ↓
adaptive student tutoring
```

That gives NAG a reusable educational infrastructure component rather
than another chatbot.
