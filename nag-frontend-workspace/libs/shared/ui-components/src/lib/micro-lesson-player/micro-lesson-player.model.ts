export interface PlayerMicroLessonStep {
  stepNumber: number;
  title: string;
  explanation: string;
  formula?: string;
  tip?: string;
}

export interface PlayerStepByStepBreakdown {
  problemStatement?: string;
  questionStem?: string;
  steps: PlayerMicroLessonStep[];
  finalAnswer?: string;
}

export interface PlayerTrapAlert {
  title: string;
  description: string;
  commonMistake?: string;
  correctApproach?: string;
}

export interface PlayerFormulaEntry {
  name: string;
  latex: string;
  description: string;
  variables?: string[];
}

export interface PlayerQuickCheckItem {
  question: string;
  options?: string[];
  correctAnswer?: string;
  answer: string;
  explanation?: string;
}

export type LessonSlideType = 'INTRO' | 'CONCEPT' | 'FORMULA' | 'STEPS' | 'QUIZ';

export type TutorAvatarState = 'IDLE' | 'SPEAKING' | 'THINKING' | 'CELEBRATING';

export type LessonPlaybackMode = 'MANUAL' | 'AUTO';

export interface LessonTutorMediaHook {
  audioUrl?: string;
  videoUrl?: string;
  captionsUrl?: string;
  avatarMood?: TutorAvatarState;
  animationPreset?: 'slide' | 'zoom' | 'spotlight';
}

export interface LessonSlide {
  id: string;
  index: number;
  type: LessonSlideType;
  title: string;
  subtitle: string;
  icon: string;
  durationSeconds: number;
  narrationText: string;
  mediaHook?: LessonTutorMediaHook;
}

export interface QuizSelectionState {
  selectedOption: string;
  isCorrect: boolean;
}

export interface QuizSuccessStats {
  totalQuestions: number;
  answeredCount: number;
  correctCount: number;
  accuracyPercent: number;
  isAllAnswered: boolean;
  statusLabel: string;
}

export interface PlayerMicroLessonContent {
  coreConcept: string;
  trapAlert: string | PlayerTrapAlert;
  formulaCheatSheet: string | PlayerFormulaEntry[];
  stepByStepBreakdown: PlayerStepByStepBreakdown | PlayerMicroLessonStep[];
  problemStatement?: string;
  finalAnswer?: string;
  keyTakeaways?: string[];
  quickCheckItems?: PlayerQuickCheckItem[];
}

export interface PlayerSubtopicMicroLesson {
  id: string;
  subtopicId: number;
  subtopicName: string;
  topicName: string;
  subjectName: string;
  targetExam?: string;
  title?: string;
  approachName?: string;
  sequenceNo?: number;
  estimatedMinutes?: number;
  lessonType?: string;
  difficultyLevel?: string;
  status: string;
  contentVersion: number;
  languageCode?: string;
  fallbackToEnglish?: boolean;
  mediaHook?: LessonTutorMediaHook;
  content: PlayerMicroLessonContent;
}

export function extractStepsFromLesson(
  lesson?: PlayerSubtopicMicroLesson | null
): PlayerMicroLessonStep[] {
  if (!lesson) return [];
  const rawContent = lesson.content as any;
  const rawBd = rawContent?.stepByStepBreakdown ?? (lesson as any)?.stepByStepBreakdown;
  let sourceArray: any[] = [];

  if (Array.isArray(rawBd)) {
    sourceArray = rawBd;
  } else if (rawBd && typeof rawBd === 'object') {
    if (Array.isArray(rawBd.steps) && rawBd.steps.length > 0) {
      sourceArray = rawBd.steps;
    } else if (Array.isArray(rawBd.structuredSteps) && rawBd.structuredSteps.length > 0) {
      sourceArray = rawBd.structuredSteps;
    }
  }

  return sourceArray.map((st: any, idx: number) => {
    if (typeof st === 'string') {
      return {
        stepNumber: idx + 1,
        title: `Step ${idx + 1}`,
        explanation: st,
      };
    }
    return {
      stepNumber: st?.stepNumber ?? idx + 1,
      title: st?.title || `Step ${idx + 1}`,
      explanation: st?.explanation || '',
      formula: st?.formula,
      tip: st?.tip,
    };
  });
}

export function extractProblemStatementFromLesson(
  lesson?: PlayerSubtopicMicroLesson | null
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
  lesson?: PlayerSubtopicMicroLesson | null
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
  if (steps.length > 0) {
    const lastTip = steps[steps.length - 1].tip || '';
    if (lastTip.toLowerCase().startsWith('final answer:')) {
      return lastTip.substring('final answer:'.length).trim();
    }
  }
  return '';
}

export function createSlidesFromLesson(lesson: PlayerSubtopicMicroLesson): LessonSlide[] {
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
