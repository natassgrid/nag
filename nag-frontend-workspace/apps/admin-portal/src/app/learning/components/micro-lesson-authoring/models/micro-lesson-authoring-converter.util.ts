import {
  CreateMicroLessonRequest,
  FormulaEntry,
  MicroLessonReviewItem,
  MicroLessonStep,
  QuickCheckItem,
  TrapAlert,
} from '../../../models/micro-lesson.model';
import { resolveFormulaApplicability } from './formula-applicability.util';

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

  const rawTrap = lesson.content?.trapAlert || lesson.trap || '';
  let trapAlert: TrapAlert = {
    title: 'Exam Trap & Common Pitfall',
    description: typeof rawTrap === 'string' ? rawTrap : rawTrap?.description || '',
    commonMistake: typeof rawTrap === 'object' && rawTrap?.commonMistake ? rawTrap.commonMistake : '',
    correctApproach: typeof rawTrap === 'object' && rawTrap?.correctApproach ? rawTrap.correctApproach : '',
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
  } else if (Array.isArray(lesson.content?.formulaCheatSheet)) {
    formulaEntries = (lesson.content.formulaCheatSheet as any[]).map((f: any, idx: number) => ({
      name: f.name || `Formula ${idx + 1}`,
      latex: f.latex || f.expression || (typeof f === 'string' ? f : ''),
      description: f.description || f.notes || '',
      variables: f.variables ? [...f.variables] : [],
    }));
  } else if (typeof lesson.content?.formulaCheatSheet === 'string') {
    const blocks: string[] = lesson.content.formulaCheatSheet
      .split('\n\n')
      .map((s: string) => s.trim())
      .filter(Boolean);
    formulaEntries = blocks.map((b: string, idx: number) => {
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
      ? bd.steps.map((s: MicroLessonStep, idx: number) => ({
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
