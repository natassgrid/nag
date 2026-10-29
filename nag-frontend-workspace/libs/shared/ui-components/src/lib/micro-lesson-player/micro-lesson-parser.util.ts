import {
  FormulaEntry,
  LessonSlide,
  MicroLessonContent,
  MicroLessonStep,
  StepByStepBreakdown,
  StepItem,
  SubtopicMicroLesson,
  TrapAlert,
} from './micro-lesson-core.model';

/**
 * Normalizes LaTeX math strings by stripping accidental "Name: " prefixes
 * and guaranteeing $$...$$ display math delimiters if absent.
 */
export function formatLatex(latex: string): string {
  let trimmed = (latex || '').trim();
  if (!trimmed) return '';
  const colonIdx = trimmed.indexOf(':');
  const dollarIdx = trimmed.indexOf('$');
  const slashIdx = trimmed.indexOf('\\');
  if (
    colonIdx > 0 &&
    (dollarIdx > colonIdx || (dollarIdx === -1 && slashIdx > colonIdx))
  ) {
    trimmed = trimmed.substring(colonIdx + 1).trim();
  }
  if (trimmed.includes('$$') || trimmed.includes('$')) return trimmed;
  return `$$${trimmed}$$`;
}

/**
 * Parses formula entries from either structured formula array, string array (cheatSheet), or raw string.
 * Avoids splitting on colon if colon is inside LaTeX math delimiters.
 */
export function parseFormulaEntries(
  rawFormulas: any,
  rawCheatSheet?: any
): { formulaEntries?: FormulaEntry[]; formulaCheatSheet: string } {
  let formulaEntries: FormulaEntry[] | undefined = undefined;
  let formulaCheatSheet = '';

  const rawFormulaArray = Array.isArray(rawFormulas) && rawFormulas.length > 0
    ? rawFormulas
    : null;

  if (rawFormulaArray) {
    const parsed: FormulaEntry[] = rawFormulaArray.map((f: any, idx: number) => ({
      name: f.name || f.title || f.namePart || `Formula ${idx + 1}`,
      latex: f.latex || f.formula || f.expression || f.value || '',
      description: f.description || f.note || f.usage || '',
      variables: Array.isArray(f.variables) ? f.variables : [],
    }));
    formulaEntries = parsed;
    formulaCheatSheet = Array.isArray(rawCheatSheet) && rawCheatSheet.length > 0
      ? rawCheatSheet.join('\n\n')
      : parsed
          .map((f: FormulaEntry) => (f.name ? `${f.name}: ${f.latex}` : f.latex))
          .join('\n\n');
  } else if (Array.isArray(rawCheatSheet)) {
    formulaCheatSheet = rawCheatSheet.join('\n\n');
    formulaEntries = rawCheatSheet
      .filter((item: any) => typeof item === 'string' && item.trim().length > 0)
      .map((item: string, idx: number) => {
        const trimmed = item.trim();
        const colonIdx = trimmed.indexOf(':');
        const firstDollarIdx = trimmed.indexOf('$');
        const firstSlashIdx = trimmed.indexOf('\\');
        const hasColon = colonIdx > 0;
        const colonBeforeDollar = firstDollarIdx === -1 || colonIdx < firstDollarIdx;
        const colonBeforeSlash = firstSlashIdx === -1 || colonIdx < firstSlashIdx;

        let name = `Formula ${idx + 1}`;
        let latex = trimmed;
        let desc = '';

        if (hasColon && colonBeforeDollar && colonBeforeSlash) {
          const namePart = trimmed.substring(0, colonIdx).trim();
          const latexPart = trimmed.substring(colonIdx + 1).trim();
          if (namePart.length > 0 && !namePart.includes('$') && !namePart.includes('\\')) {
            name = namePart;
            latex = latexPart;
            desc = namePart;
          }
        }
        return {
          name,
          latex,
          description: desc,
          variables: [],
        };
      });
  } else if (typeof rawFormulas === 'string' && rawFormulas.trim()) {
    formulaCheatSheet = rawFormulas.trim();
  }

  return { formulaEntries, formulaCheatSheet };
}

/**
 * Normalizes trap alert representation into string description and structured TrapAlert.
 */
export function parseTrapAlert(
  rawTrap: any,
  nestedStructuredTrap?: any
): { trapAlert: string; structuredTrapAlert?: TrapAlert } {
  let trapAlert = '';
  let structuredTrapAlert: TrapAlert | undefined = undefined;

  if (typeof rawTrap === 'string') {
    trapAlert = rawTrap;
    if (rawTrap.trim().length > 0) {
      structuredTrapAlert = {
        title: 'Exam Trap & Common Pitfall',
        description: rawTrap,
        commonMistake: rawTrap,
        correctApproach: 'Apply the governing formula and verify boundary conditions carefully.',
      };
    }
  } else if (rawTrap && typeof rawTrap === 'object') {
    structuredTrapAlert = {
      title: rawTrap.title || 'Exam Trap & Common Pitfall',
      description: rawTrap.description || '',
      commonMistake: rawTrap.commonMistake,
      correctApproach: rawTrap.correctApproach,
    };
    trapAlert = rawTrap.description || rawTrap.commonMistake || rawTrap.title || '';
  }

  if (!structuredTrapAlert && nestedStructuredTrap && typeof nestedStructuredTrap === 'object') {
    structuredTrapAlert = {
      title: nestedStructuredTrap.title || 'Exam Trap & Common Pitfall',
      description: nestedStructuredTrap.description || trapAlert,
      commonMistake: nestedStructuredTrap.commonMistake,
      correctApproach: nestedStructuredTrap.correctApproach,
    };
    if (!trapAlert) {
      trapAlert = structuredTrapAlert.description || '';
    }
  }

  return { trapAlert, structuredTrapAlert };
}

/**
 * Normalizes step-by-step breakdown from diverse backend and form shapes.
 */
export function parseStepBreakdown(
  rawBreakdown: any,
  rawProblemStatement?: string,
  rawQuestionStem?: string,
  rawFinalAnswer?: string
): StepByStepBreakdown {
  const topBreakdownObj = rawBreakdown && !Array.isArray(rawBreakdown) ? rawBreakdown : null;

  const problemStatement =
    topBreakdownObj?.problemStatement ||
    topBreakdownObj?.questionStem ||
    rawProblemStatement ||
    rawQuestionStem ||
    '';

  const finalAnswer = topBreakdownObj?.finalAnswer || rawFinalAnswer || undefined;

  let rawStepsSource: any[] = [];
  if (Array.isArray(rawBreakdown)) {
    rawStepsSource = rawBreakdown;
  } else if (topBreakdownObj) {
    if (Array.isArray(topBreakdownObj.structuredSteps) && topBreakdownObj.structuredSteps.length > 0) {
      rawStepsSource = topBreakdownObj.structuredSteps;
    } else if (Array.isArray(topBreakdownObj.steps)) {
      rawStepsSource = topBreakdownObj.steps;
    }
  }

  const steps: StepItem[] = rawStepsSource.map((st: any, idx: number) => {
    if (typeof st === 'string') {
      return {
        stepNumber: idx + 1,
        title: `Step ${idx + 1}`,
        explanation: st,
      };
    }
    return {
      stepNumber: Number(st?.stepNumber ?? idx + 1),
      title: st?.title || `Step ${idx + 1}`,
      explanation: st?.explanation || '',
      formula: st?.formula || undefined,
      tip: st?.tip || undefined,
    };
  });

  return {
    problemStatement: problemStatement || undefined,
    questionStem: problemStatement || undefined,
    steps,
    finalAnswer,
  };
}

export function extractStepsFromLesson(
  lesson?: SubtopicMicroLesson | null
): StepItem[] {
  if (!lesson) return [];
  const rawContent = lesson.content as any;
  const rawBd = rawContent?.stepByStepBreakdown ?? (lesson as any)?.stepByStepBreakdown;
  return parseStepBreakdown(rawBd).steps;
}

export function extractProblemStatementFromLesson(
  lesson?: SubtopicMicroLesson | null
): string {
  if (!lesson) return '';
  const rawContent = lesson.content as any;
  const rawBd = rawContent?.stepByStepBreakdown;
  const topBd = (lesson as any)?.stepByStepBreakdown;
  return (
    (rawBd && !Array.isArray(rawBd) && (rawBd.problemStatement || rawBd.questionStem)) ||
    rawContent?.problemStatement ||
    rawContent?.questionStem ||
    (topBd && !Array.isArray(topBd) && (topBd.problemStatement || topBd.questionStem)) ||
    (lesson as any)?.problemStatement ||
    lesson.subtopicName ||
    ''
  );
}

export function extractFinalAnswerFromLesson(
  lesson?: SubtopicMicroLesson | null
): string {
  if (!lesson) return '';
  const rawContent = lesson.content as any;
  const rawBd = rawContent?.stepByStepBreakdown;
  const topBd = (lesson as any)?.stepByStepBreakdown;
  const explicit =
    (rawBd && !Array.isArray(rawBd) && rawBd.finalAnswer) ||
    rawContent?.finalAnswer ||
    (topBd && !Array.isArray(topBd) && topBd.finalAnswer) ||
    (lesson as any)?.finalAnswer ||
    '';
  if (explicit) return explicit;

  const steps = extractStepsFromLesson(lesson);
  for (let i = steps.length - 1; i >= 0; i--) {
    const tip = steps[i].tip || '';
    const match = tip.match(/^final\s+answer\s*:\s*(.+)$/i);
    if (match?.[1]) {
      return match[1].trim();
    }
  }
  return '';
}

export function createSlidesFromLesson(lesson: SubtopicMicroLesson): LessonSlide[] {
  const subtopic = lesson.subtopicName || 'Micro-Tutorial';
  const subject = lesson.subjectName || 'Subject';
  const topic = lesson.topicName || 'Topic';
  const approach = lesson.approachName || lesson.lessonType || 'Conceptual Foundation';
  const steps = extractStepsFromLesson(lesson);
  const stepCount = steps.length;
  const quizCount = lesson.content?.quickCheckItems?.length || 1;
  const formulaCount = Array.isArray(lesson.content?.formulaCheatSheet)
    ? lesson.content.formulaCheatSheet.length
    : lesson.content?.formulaCheatSheet
    ? 1
    : 0;

  return [
    {
      id: `${lesson.id || 'lesson'}-slide-intro`,
      index: 0,
      type: 'INTRO',
      title: subtopic,
      subtitle: `${subject} • ${topic} (${approach})`,
      icon: 'school',
      durationSeconds: 6,
      narrationText: `Welcome to the interactive tutorial on ${subtopic} under ${subject}, ${topic}.`,
      mediaHook: { avatarMood: 'SPEAKING', animationPreset: 'zoom' },
    },
    {
      id: `${lesson.id || 'lesson'}-slide-concept`,
      index: 1,
      type: 'CONCEPT',
      title: 'Core Concept & Exam Trap',
      subtitle: 'Foundational principle and negative-marking pitfalls',
      icon: 'lightbulb',
      durationSeconds: 10,
      narrationText: `Core Concept for ${subtopic}: ${lesson.content?.coreConcept || ''}`,
      mediaHook: { avatarMood: 'SPEAKING', animationPreset: 'slide' },
    },
    {
      id: `${lesson.id || 'lesson'}-slide-formula`,
      index: 2,
      type: 'FORMULA',
      title: 'Formula & Identity Cheat Sheet',
      subtitle: `${formulaCount} governing identity card${formulaCount === 1 ? '' : 's'}`,
      icon: 'functions',
      durationSeconds: 8,
      narrationText: `Review the governing formulas and key relations for ${subtopic}.`,
      mediaHook: { avatarMood: 'THINKING', animationPreset: 'spotlight' },
    },
    {
      id: `${lesson.id || 'lesson'}-slide-steps`,
      index: 3,
      type: 'STEPS',
      title: 'Interactive Step-by-Step Solver',
      subtitle: stepCount > 0 ? `Reveal ${stepCount} logical step${stepCount === 1 ? '' : 's'} in sequence` : 'Guided walkthrough',
      icon: 'format_list_numbered',
      durationSeconds: Math.max(8, stepCount * 4),
      narrationText: `Let's solve the problem step by step for ${subtopic}.`,
      mediaHook: { avatarMood: 'SPEAKING', animationPreset: 'slide' },
    },
    {
      id: `${lesson.id || 'lesson'}-slide-quiz`,
      index: 4,
      type: 'QUIZ',
      title: 'Quick Check & Mastery Stat',
      subtitle: `${quizCount} interactive check${quizCount === 1 ? '' : 's'} to verify mastery`,
      icon: 'quiz',
      durationSeconds: 12,
      narrationText: `Select the best option below to verify your mastery of ${subtopic}.`,
      mediaHook: { avatarMood: 'CELEBRATING', animationPreset: 'zoom' },
    },
  ];
}
