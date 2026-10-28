import {
  CreateMicroLessonRequest,
  FormulaEntry,
  MicroLessonReviewItem,
  MicroLessonStep,
  QuickCheckItem,
  TrapAlert,
} from '../../../models/micro-lesson.model';
import {
  Subject,
  Subtopic,
  Topic,
} from '@nag-frontend-workspace/questions-data-access';

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

export type FormulaApplicabilityMode =
  | 'MANDATORY'
  | 'OPTIONAL_NUMERICAL'
  | 'OPTIONAL_NOT_APPLICABLE';

export interface SubjectTopicFormulaPolicy {
  mode: FormulaApplicabilityMode;
  required: boolean;
  badgeLabel: string;
  reason: string;
}

const MANDATORY_SUBJECT_PATTERNS = [
  'mathematics',
  'math',
  'quantitative aptitude',
  'numerical ability',
  'arithmetic',
  'general science',
  'physics',
  'chemistry',
  'statistics',
  'data interpretation',
];

const MANDATORY_NUMERICAL_TOPIC_PATTERNS = [
  'numerical operation',
  'number series',
  'symbolic operation',
  'mathematical step operation',
  'inequalit',
  'everyday science',
  'physics',
  'chemistry',
  'probability',
  'permutation',
  'combination',
  'mensuration',
  'trigonometry',
  'calculus',
  'algebra',
  'geometry',
  'arithmetic',
  'time and work',
  'time, speed',
  'profit and loss',
  'simple and compound interest',
  'ratio and proportion',
  'percentage',
  'data interpretation',
  'tabular analysis',
  'bar & line',
  'pie chart',
  'caselet',
];

const OPTIONAL_NUMERICAL_TOPIC_PATTERNS = [
  // Banking & Economics quantitative topics
  'monetary policy',
  'repo',
  'crr',
  'slr',
  'capital adequacy',
  'basel',
  'crar',
  'bond',
  'yield',
  'priority sector',
  'national income',
  'gdp',
  'inflation',
  'fiscal',
  'tax',
  'economic',
  // Computer Science quantitative/formal topics
  'algorithm',
  'complexity',
  'big-o',
  'asymptotic',
  'computer organization',
  'architecture',
  'cpu',
  'pipelin',
  'cache',
  'memory',
  'operating system',
  'scheduling',
  'paging',
  'network',
  'subnet',
  'cidr',
  'boolean',
  'logic gate',
  'relational algebra',
  // Ecology & Biology quantitative topics
  'ecosystem dynamics',
  'energy flow',
  'lindeman',
  'trophic',
  'genetics',
  'hardy-weinberg',
  'mendel',
  'population',
  'cardiac',
  'respiration',
  'photosynthesis',
  // Reasoning symbolic/formal topics
  'coding and decoding',
  'coding-decoding',
  'venn diagram',
  'syllogism',
  'statement and conclusion',
  'direction',
  'distance',
  'clock',
  'calendar',
  'dice',
  'cube',
  'ranking',
  'blood relation',
  'series',
  // Hindi Sandhi / Geography / CDP metrics
  'संधि',
  'sandhi',
  'समास',
  'latitude',
  'longitude',
  'time zone',
  'iq',
  'intelligence',
];

const NON_APPLICABLE_SUBJECT_PATTERNS = [
  'english',
  'general hindi',
  'hindi',
  'public administration',
  'governance',
  'child development',
  'pedagogy',
  'history',
  'polity',
  'geography',
  'reasoning',
  'general studies',
  'general awareness',
  'banking',
  'environmental',
  'ecology',
  'computer science',
  'biology',
  'life science',
];

/**
 * Classifies whether Part 3 (KaTeX Formula Cheat Sheet Builder) is mandatory,
 * optional-numerical, or optional-not-applicable based on the selected subject,
 * topic, subtopic, and lesson type.
 */
export function resolveFormulaApplicability(
  subjectName?: string | null,
  topicName?: string | null,
  subtopicName?: string | null,
  lessonType?: string | null
): SubjectTopicFormulaPolicy {
  const subj = (subjectName || '').trim().toLowerCase();
  const top = (topicName || '').trim().toLowerCase();
  const sub = (subtopicName || '').trim().toLowerCase();
  const type = (lessonType || '').trim().toUpperCase();
  const combinedTopic = `${top} ${sub}`.trim();

  if (type === 'CHEAT_SHEET') {
    return {
      mode: 'MANDATORY',
      required: true,
      badgeLabel: 'Required (Formula Cheat Sheet Lesson)',
      reason:
        'Part 3 is required because the lesson type is explicitly set to Formula Cheat Sheet (CHEAT_SHEET).',
    };
  }

  // Biology under General Science or standalone Biology is optional unless numerical topic
  const isBiologyTopic =
    subj.includes('biology') ||
    subj.includes('life science') ||
    top.includes('biology') ||
    top.includes('life science') ||
    top.includes('botany') ||
    top.includes('zoology');

  if (!isBiologyTopic) {
    const isMandatorySubject = MANDATORY_SUBJECT_PATTERNS.some((p) => subj.includes(p));
    if (isMandatorySubject) {
      return {
        mode: 'MANDATORY',
        required: true,
        badgeLabel: 'Required (Math / Science / Numerical)',
        reason: `Part 3 KaTeX Formula Cheat Sheet is mandatory for ${subjectName || 'Mathematics, Science & Numerical'} lessons.`,
      };
    }
  }

  const isMandatoryNumericalTopic = MANDATORY_NUMERICAL_TOPIC_PATTERNS.some((p) =>
    combinedTopic.includes(p)
  );
  if (isMandatoryNumericalTopic && !isBiologyTopic) {
    return {
      mode: 'MANDATORY',
      required: true,
      badgeLabel: 'Required (Numerical / Science Topic)',
      reason: `Part 3 KaTeX Formula Cheat Sheet is mandatory for numerical/scientific topic "${topicName || subtopicName}".`,
    };
  }

  const isOptionalNumericalTopic = OPTIONAL_NUMERICAL_TOPIC_PATTERNS.some((p) =>
    combinedTopic.includes(p)
  );
  if (isOptionalNumericalTopic || isBiologyTopic) {
    return {
      mode: 'OPTIONAL_NUMERICAL',
      required: false,
      badgeLabel: 'Optional (Symbolic / Quantitative Topic)',
      reason: `Part 3 is optional for ${subjectName || 'this subject'} (${topicName || 'this topic'}). Add KaTeX equations or symbolic rules only if helpful, or skip Part 3.`,
    };
  }

  if (subj && NON_APPLICABLE_SUBJECT_PATTERNS.some((p) => subj.includes(p))) {
    return {
      mode: 'OPTIONAL_NOT_APPLICABLE',
      required: false,
      badgeLabel: 'Optional — Not Applicable for Subject',
      reason: `KaTeX formulas are not applicable for ${subjectName}${topicName ? ' — ' + topicName : ''}. Part 3 can be safely left empty and will be hidden from candidates.`,
    };
  }

  if (!subj) {
    return {
      mode: 'MANDATORY',
      required: true,
      badgeLabel: 'Required (Math / Science / Numerical)',
      reason: 'Part 3 is required for Mathematics, Science, and Numerical topics, and optional for non-formulaic subjects.',
    };
  }

  return {
    mode: 'OPTIONAL_NOT_APPLICABLE',
    required: false,
    badgeLabel: 'Optional for Non-Math/Science Subject',
    reason: `Part 3 is optional for ${subjectName}. Include KaTeX formulas only if the topic involves mathematical or scientific expressions.`,
  };
}

export interface ParsedAuthoringLessonState {
  targetExam: string;
  lessonType: string;
  difficultyLevel: string;
  sequenceNo: number;
  estimatedMinutes: number;
  status: string;
  subtopicCustomName: string;
  coreConcept: string;
  trapAlert: TrapAlert;
  formulaEntries: FormulaEntry[];
  problemStatement: string;
  steps: MicroLessonStep[];
  finalAnswer: string;
  keyTakeaways: string[];
  quickCheckItems: QuickCheckItem[];
  reviewerNotes: string;
}

export function parseLessonForAuthoring(
  lesson: MicroLessonReviewItem,
  formulaRequired?: boolean
): ParsedAuthoringLessonState {
  const coreConcept = lesson.content?.coreConcept || lesson.coreConcept || '';

  let trapAlert: TrapAlert = {
    title: 'Exam Trap & Common Pitfall',
    description: lesson.content?.trapAlert || lesson.trap || '',
    commonMistake: '',
    correctApproach: '',
  };
  if (lesson.content?.structuredTrapAlert) {
    trapAlert = {
      title: lesson.content.structuredTrapAlert.title || 'Exam Trap & Common Pitfall',
      description: lesson.content.structuredTrapAlert.description || trapAlert.description,
      commonMistake: lesson.content.structuredTrapAlert.commonMistake || '',
      correctApproach: lesson.content.structuredTrapAlert.correctApproach || '',
    };
  }

  let formulaEntries: FormulaEntry[] = [];
  if (lesson.content?.formulaEntries && lesson.content.formulaEntries.length > 0) {
    formulaEntries = lesson.content.formulaEntries.map((f) => ({
      name: f.name || '',
      latex: f.latex || '',
      description: f.description || '',
      variables: f.variables ? [...f.variables] : [],
    }));
  } else if (lesson.content?.formulaCheatSheet) {
    const blocks = lesson.content.formulaCheatSheet
      .split('\n\n')
      .map((s) => s.trim())
      .filter(Boolean);
    formulaEntries = blocks.map((b, idx) => {
      const colonIdx = b.indexOf(':');
      const firstDollarIdx = b.indexOf('$');
      const firstSlashIdx = b.indexOf('\\');
      const hasColon = colonIdx > 0;
      const colonBeforeDollar = firstDollarIdx === -1 || colonIdx < firstDollarIdx;
      const colonBeforeSlash = firstSlashIdx === -1 || colonIdx < firstSlashIdx;

      if (hasColon && colonBeforeDollar && colonBeforeSlash) {
        const namePart = b.substring(0, colonIdx).trim();
        const latexPart = b.substring(colonIdx + 1).trim();
        if (namePart.length > 0 && !namePart.includes('$') && !namePart.includes('\\')) {
          return {
            name: namePart,
            latex: latexPart,
            description: '',
            variables: [],
          };
        }
      }
      return {
        name: `Formula ${idx + 1}`,
        latex: b,
        description: '',
        variables: [],
      };
    });
  }

  const resolvedPolicy = resolveFormulaApplicability(
    lesson.subjectName || lesson.subject,
    lesson.topicName || lesson.topic,
    lesson.subtopicName || lesson.subtopic,
    lesson.lessonType
  );
  const shouldRequireFormula =
    formulaRequired !== undefined ? formulaRequired : resolvedPolicy.required;

  if (formulaEntries.length === 0 && shouldRequireFormula) {
    formulaEntries = [{ name: 'Formula 1', latex: '', description: '', variables: [] }];
  }

  const bd = lesson.content?.stepByStepBreakdown;
  const problemStatement = bd?.problemStatement || bd?.questionStem || '';
  const finalAnswer = bd?.finalAnswer || '';
  const steps: MicroLessonStep[] =
    bd?.steps && bd.steps.length > 0
      ? bd.steps.map((s, idx) => ({
          stepNumber: idx + 1,
          title: s.title || `Step ${idx + 1}`,
          explanation: s.explanation || '',
          formula: s.formula || '',
          tip: s.tip || '',
        }))
      : [{ stepNumber: 1, title: 'Step 1', explanation: '', formula: '', tip: '' }];

  const keyTakeaways =
    lesson.content?.keyTakeaways && lesson.content.keyTakeaways.length > 0
      ? [...lesson.content.keyTakeaways]
      : [''];

  const quickCheckItems: QuickCheckItem[] =
    lesson.content?.quickCheckItems && lesson.content.quickCheckItems.length > 0
      ? lesson.content.quickCheckItems.map((qc) => ({
          question: qc.question || '',
          options: qc.options ? [...qc.options] : [],
          answer: qc.answer || qc.correctAnswer || '',
          correctAnswer: qc.correctAnswer || qc.answer || '',
          explanation: qc.explanation || '',
        }))
      : [{ question: '', options: [], answer: '', correctAnswer: '', explanation: '' }];

  return {
    targetExam: lesson.targetExam || '',
    lessonType: lesson.lessonType || 'CONCEPT',
    difficultyLevel: lesson.difficultyLevel || 'CORE',
    sequenceNo: lesson.sequenceNo ?? 1,
    estimatedMinutes: lesson.estimatedMinutes ?? 5,
    status: lesson.status || 'APPROVED',
    subtopicCustomName: lesson.subtopicName || lesson.subtopic || '',
    coreConcept,
    trapAlert,
    formulaEntries,
    problemStatement,
    steps,
    finalAnswer,
    keyTakeaways,
    quickCheckItems,
    reviewerNotes: lesson.reviewerNotes || '',
  };
}

export function buildCreateOrUpdateRequest(params: {
  subjectId: number | null;
  subjectName: string;
  topicId: number | null;
  topicName: string;
  subtopicId: number | null;
  subtopicName: string;
  targetExam?: string;
  lessonType?: string;
  difficultyLevel?: string;
  sequenceNo?: number;
  estimatedMinutes?: number;
  status: string;
  coreConcept: string;
  trapAlert: TrapAlert;
  formulaEntries: FormulaEntry[];
  problemStatement: string;
  steps: MicroLessonStep[];
  finalAnswer: string;
  keyTakeaways: string[];
  quickCheckItems: QuickCheckItem[];
  reviewerNotes: string;
}): CreateMicroLessonRequest {
  const trapParts: string[] = [];
  if (params.trapAlert.title?.trim() && params.trapAlert.title.trim() !== 'Exam Trap & Common Pitfall') {
    trapParts.push(`**${params.trapAlert.title.trim()}**`);
  }
  if (params.trapAlert.description?.trim()) {
    trapParts.push(params.trapAlert.description.trim());
  }
  if (
    params.trapAlert.commonMistake?.trim() &&
    params.trapAlert.commonMistake.trim() !== params.trapAlert.description?.trim()
  ) {
    trapParts.push(`**Common Pitfall:** ${params.trapAlert.commonMistake.trim()}`);
  }
  if (params.trapAlert.correctApproach?.trim()) {
    trapParts.push(`**Correct Approach:** ${params.trapAlert.correctApproach.trim()}`);
  }
  const trapString = trapParts.join('\n\n') || params.trapAlert.description?.trim() || 'Avoid common sign and boundary errors.';

  const validFormulas = (params.formulaEntries || []).filter(
    (f) => f && (f.latex?.trim() || f.name?.trim())
  );
  const cheatSheetStrings = validFormulas.map((f, idx) => {
    const name = f.name?.trim();
    const latex = f.latex?.trim() || '';
    const formattedLatex =
      latex && !latex.includes('$$') && !latex.includes('$') ? `$$${latex}$$` : latex;
    const isGenericName = !name || name.toLowerCase() === `formula ${idx + 1}`.toLowerCase();
    return isGenericName ? formattedLatex : `${name}: ${formattedLatex}`;
  });

  const validSteps = (params.steps || [])
    .filter((s) => s && (s.explanation?.trim() || s.title?.trim() || s.formula?.trim()))
    .map((s, idx) => ({
      stepNumber: idx + 1,
      title: s.title?.trim() || `Step ${idx + 1}`,
      explanation: s.explanation?.trim() || '',
      formula: s.formula?.trim() || undefined,
      tip: s.tip?.trim() || undefined,
    }));

  const stepStrings = validSteps.map((s) => {
    let line = s.title && !s.explanation.startsWith(s.title)
      ? `${s.title}: ${s.explanation}`
      : s.explanation;
    if (s.formula && !line.includes(s.formula)) {
      const f = !s.formula.includes('$$') && !s.formula.includes('$') ? `$$${s.formula}$$` : s.formula;
      line = `${line} ${f}`.trim();
    }
    return line;
  });

  const cleanTakeaways = (params.keyTakeaways || [])
    .map((t) => (t || '').trim())
    .filter(Boolean);

  const cleanQuickChecks = (params.quickCheckItems || [])
    .filter((q) => q && q.question?.trim())
    .map((q) => ({
      question: q.question.trim(),
      options: (q.options || []).map((o) => o.trim()).filter(Boolean),
      answer: (q.answer || q.correctAnswer || '').trim(),
      correctAnswer: (q.correctAnswer || q.answer || '').trim(),
      explanation: q.explanation?.trim() || undefined,
    }));

  return {
    subjectId: params.subjectId ?? 1,
    subject: params.subjectName.trim() || 'General',
    subjectName: params.subjectName.trim() || 'General',
    topicId: params.topicId ?? 1,
    topic: params.topicName.trim() || 'General',
    topicName: params.topicName.trim() || 'General',
    subtopicId: params.subtopicId ?? undefined,
    subtopic: params.subtopicName.trim() || 'General',
    subtopicName: params.subtopicName.trim() || 'General',
    targetExam: params.targetExam?.trim() || undefined,
    lessonType: params.lessonType || 'CONCEPT',
    difficultyLevel: params.difficultyLevel || 'CORE',
    sequenceNo: params.sequenceNo ?? 1,
    estimatedMinutes: params.estimatedMinutes ?? 5,
    status: params.status || 'APPROVED',
    coreConcept: params.coreConcept.trim(),
    trap: trapString,
    trapAlert: {
      title: params.trapAlert.title?.trim() || 'Exam Trap & Common Pitfall',
      description: params.trapAlert.description?.trim() || trapString,
      commonMistake: params.trapAlert.commonMistake?.trim() || undefined,
      correctApproach: params.trapAlert.correctApproach?.trim() || undefined,
    },
    cheatSheet: cheatSheetStrings,
    formulaCheatSheet: validFormulas,
    stepByStepBreakdown: {
      questionStem: params.problemStatement.trim(),
      problemStatement: params.problemStatement.trim(),
      steps: stepStrings,
      structuredSteps: validSteps,
      finalAnswer: params.finalAnswer.trim() || undefined,
    },
    keyTakeaways: cleanTakeaways,
    quickCheckItems: cleanQuickChecks,
    reviewerNotes: params.reviewerNotes.trim() || undefined,
    reviewComments: params.reviewerNotes.trim() || undefined,
  };
}

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

export interface MicroLessonWizardState {
  editingLessonId: string | null;
  selectedSubjectId: number | null;
  selectedTopicId: number | null;
  selectedSubtopicId: number | null;
  customSubtopicName: string;
  targetExam: string;
  lessonType: string;
  difficultyLevel: string;
  sequenceNo: number;
  estimatedMinutes: number;
  status: string;
  coreConcept: string;
  trapAlert: TrapAlert;
  formulaEntries: FormulaEntry[];
  problemStatement: string;
  steps: MicroLessonStep[];
  finalAnswer: string;
  keyTakeaways: string[];
  quickCheckItems: QuickCheckItem[];
  reviewerNotes: string;
}

export interface MicroLessonWizardDialogData extends MicroLessonWizardState {
  subjects: Subject[];
  topics: Topic[];
  subtopics: Subtopic[];
  examOptions: TargetExamOption[];
  initialStep?: number;
}

export interface MicroLessonWizardDialogResult {
  action: 'save' | 'switch-to-inpage' | 'close';
  state: MicroLessonWizardState;
  subjects: Subject[];
  topics: Topic[];
  subtopics: Subtopic[];
}

