import {
  CreateMicroLessonRequest,
  FormulaEntry,
  MicroLessonReviewItem,
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
  { label: 'JEE Main & Advanced (JEE_MAIN)', value: 'JEE_MAIN' },
  { label: 'JEE Advanced (JEE_ADVANCED)', value: 'JEE_ADVANCED' },
  { label: 'NEET-UG Medical (NEET)', value: 'NEET' },
  { label: 'GATE Engineering (GATE)', value: 'GATE' },
  { label: 'UPSC Civil Services (UPSC_CSAT)', value: 'UPSC_CSAT' },
  { label: 'CAT Management (CAT)', value: 'CAT' },
  { label: 'SSC CGL (SSC_CGL)', value: 'SSC_CGL' },
  { label: 'General / Foundational (GENERAL)', value: 'GENERAL' },
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

export interface ParsedAuthoringLessonState {
  targetExam: string;
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

export function parseLessonForAuthoring(lesson: MicroLessonReviewItem): ParsedAuthoringLessonState {
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
  if (formulaEntries.length === 0) {
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
    targetExam: lesson.targetExam || 'JEE_MAIN',
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
  targetExam: string;
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
    targetExam: params.targetExam.trim() || 'JEE_MAIN',
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
