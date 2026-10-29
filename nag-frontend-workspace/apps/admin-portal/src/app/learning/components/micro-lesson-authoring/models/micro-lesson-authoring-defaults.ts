import {
  FormulaEntry,
  MicroLessonStep,
  QuickCheckItem,
  TrapAlert,
} from '../../../models/micro-lesson.model';

export interface TargetExamOption {
  label: string;
  value: string;
  category?: string;
}

export const DEFAULT_EXAM_OPTIONS: TargetExamOption[] = [
  { label: 'Universal / All Exams (Agnostic)', value: 'ALL' },
  { label: 'JEE Main & Advanced (JEE_MAIN)', value: 'JEE_MAIN' },
  { label: 'JEE Advanced (JEE_ADVANCED)', value: 'JEE_ADVANCED' },
  { label: 'NEET-UG Medical (NEET)', value: 'NEET' },
  { label: 'GATE Engineering (GATE)', value: 'GATE' },
  { label: 'UPSC Civil Services (UPSC_CSAT)', value: 'UPSC_CSAT' },
  { label: 'CAT Management (CAT)', value: 'CAT' },
  { label: 'SSC CGL (SSC_CGL)', value: 'SSC_CGL' },
  { label: 'General / Foundational (GENERAL)', value: 'GENERAL' },
];

export const LESSON_TYPE_OPTIONS = [
  { label: 'Core Concept (CONCEPT)', value: 'CONCEPT' },
  { label: 'Worked Examples (WORKED_EXAMPLES)', value: 'WORKED_EXAMPLES' },
  { label: 'Traps & Common Pitfalls (TRAPS_AND_PITFALLS)', value: 'TRAPS_AND_PITFALLS' },
  { label: 'Formula Cheat Sheet (CHEAT_SHEET)', value: 'CHEAT_SHEET' },
  { label: 'Visual Intuition (VISUAL)', value: 'VISUAL' },
  { label: 'Quick Recap (QUICK_RECAP)', value: 'QUICK_RECAP' },
  { label: 'Story & Analogy (STORY_ANALOGY)', value: 'STORY_ANALOGY' },
];

export const DIFFICULTY_LEVEL_OPTIONS = [
  { label: 'Foundation (Remedial / Beginner)', value: 'FOUNDATION' },
  { label: 'Core (Standard Curriculum)', value: 'CORE' },
  { label: 'Advanced (High-Order / Olympiad)', value: 'ADVANCED' },
];

export const AUTHORING_STATUS_OPTIONS = [
  { label: 'Approved (Ready to Publish)', value: 'APPROVED' },
  { label: 'Published (Live for Candidates)', value: 'PUBLISHED' },
  { label: 'In Review (Moderation Queue)', value: 'IN_REVIEW' },
  { label: 'Draft', value: 'DRAFT' },
];

export const DEFAULT_CORE_CONCEPT =
  'The **Work-Energy Theorem** states that the net work done by all external and internal forces acting on a system equals the change in its kinetic energy:\n\n$$W_{\\text{all}} = \\Delta K = \\frac{1}{2}mv_f^2 - \\frac{1}{2}mv_i^2$$';

export const DEFAULT_PROBLEM_STATEMENT =
  'A block of mass $$m = 4\\text{ kg}$$ is pushed across a rough horizontal surface ($$\\mu_k = 0.25$$) by a constant horizontal force $$F = 30\\text{ N}$$ over a displacement $$d = 5\\text{ m}$$ starting from rest. Find its final speed.';

export const DEFAULT_FINAL_ANSWER =
  '$$v_f = 5\\sqrt{2}\\text{ m/s} \\approx 7.07\\text{ m/s}$$';

export const DEFAULT_TRAP_ALERT: TrapAlert = {
  title: 'Sign Convention & Boundary Condition Pitfall',
  description:
    'Candidates frequently apply the standard formula without verifying whether the frame of reference is inertial or non-inertial.',
  commonMistake:
    'Neglecting pseudo-force terms when resolving equilibrium in accelerating frames.',
  correctApproach:
    'Always draw the complete free-body diagram and include $$-m\\vec{a}_{\\text{frame}}$$ before writing conservation equations.',
};

export const DEFAULT_FORMULA_ENTRIES: FormulaEntry[] = [
  {
    name: 'Work-Energy Theorem',
    latex: '$$W_{\\text{all}} = \\Delta K = \\frac{1}{2}mv_f^2 - \\frac{1}{2}mv_i^2$$',
    description: 'Total work done by all internal and external forces equals change in kinetic energy.',
    variables: ['W_all: Total work', 'm: Mass', 'v_f, v_i: Final & initial speed'],
  },
];

export const DEFAULT_STEP_ITEMS: MicroLessonStep[] = [
  {
    stepNumber: 1,
    title: 'Identify Given Parameters & System Boundaries',
    explanation: 'List known quantities in SI units and isolate the system of interest.',
    formula: '$$\\sum \\vec{F}_{\\text{ext}} = m\\vec{a}_{\\text{cm}}$$',
    tip: 'Verify whether external friction or impulse acts during the interval.',
  },
  {
    stepNumber: 2,
    title: 'Substitute & Solve for Target Variable',
    explanation: 'Apply the governing conservation law and simplify algebraically before plugging numbers.',
    formula: '$$v = \\sqrt{v_0^2 + 2as}$$',
    tip: 'Check dimensional consistency of the final expression.',
  },
];

export const DEFAULT_QUICK_CHECKS: QuickCheckItem[] = [
  {
    question:
      'Does the Work-Energy Theorem hold in the presence of non-conservative forces such as kinetic friction?',
    options: [
      'Yes, provided work done by friction is included in W_all',
      'No, it only applies to conservative fields',
      'Only when acceleration is zero',
      'Only for point masses in vacuum',
    ],
    answer: 'Yes, provided work done by friction is included in W_all',
    correctAnswer: 'Yes, provided work done by friction is included in W_all',
    explanation:
      'The Work-Energy Theorem $$W_{\\text{all}} = \\Delta K$$ is universally valid when work of all forces (conservative and non-conservative) is summed.',
  },
];

export const DEFAULT_KEY_TAKEAWAYS: string[] = [
  'Always verify assumptions before applying shortcut formulas.',
  'Include both conservative and non-conservative work terms in $$W_{\\text{all}} = \\Delta K$$.',
];

export interface MicroLessonWizardStep {
  id: string;
  stepNumber: number;
  title: string;
  subtitle: string;
  icon: string;
}

export const MICRO_LESSON_WIZARD_STEPS: MicroLessonWizardStep[] = [
  {
    id: 'metadata',
    stepNumber: 1,
    title: 'Metadata & Taxonomy',
    subtitle: 'Subject, topic, exam & duration',
    icon: 'tune',
  },
  {
    id: 'core-concept',
    stepNumber: 2,
    title: 'Part 1: Core Concept',
    subtitle: 'Foundational principle & KaTeX',
    icon: 'lightbulb',
  },
  {
    id: 'trap-alert',
    stepNumber: 3,
    title: 'Part 2: Trap Alert',
    subtitle: 'Exam pitfalls & correct approach',
    icon: 'warning_amber',
  },
  {
    id: 'formula-sheet',
    stepNumber: 4,
    title: 'Part 3: Formula Sheet',
    subtitle: 'KaTeX equations & variables',
    icon: 'functions',
  },
  {
    id: 'step-breakdown',
    stepNumber: 5,
    title: 'Part 4: Worked Steps',
    subtitle: 'Problem stem & derivation steps',
    icon: 'format_list_numbered',
  },
  {
    id: 'takeaways-preview',
    stepNumber: 6,
    title: 'Parts 5–6 & Preview',
    subtitle: 'Quick checks & candidate player',
    icon: 'visibility',
  },
];
