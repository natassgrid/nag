export interface MicroLessonStep {
  stepNumber: number;
  title: string;
  explanation: string;
  formula?: string;
  tip?: string;
}

export type StepItem = MicroLessonStep;

export interface StepByStepBreakdown {
  problemStatement?: string;
  questionStem?: string;
  steps: StepItem[];
  finalAnswer?: string;
}

export interface TrapAlert {
  title: string;
  description: string;
  commonMistake?: string;
  correctApproach?: string;
}

export interface FormulaEntry {
  name: string;
  latex: string;
  description: string;
  variables?: string[];
}

export interface QuickCheckItem {
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

export interface MicroLessonContent {
  coreConcept: string;
  trapAlert: string | TrapAlert;
  formulaCheatSheet: string | FormulaEntry[];
  stepByStepBreakdown: StepByStepBreakdown | StepItem[];
  problemStatement?: string;
  questionStem?: string;
  finalAnswer?: string;
  keyTakeaways?: string[];
  quickCheckItems?: QuickCheckItem[];
}

export interface LessonVariantRef {
  id: string;
  lessonType: 'CONCEPT' | 'WORKED_EXAMPLES' | 'TRAPS_AND_PITFALLS' | 'CHEAT_SHEET' | 'VISUAL' | 'QUICK_RECAP';
  difficultyLevel: 'FOUNDATION' | 'CORE' | 'ADVANCED';
  sequenceNo?: number;
  title?: string;
  approachName?: string;
  estimatedMinutes?: number;
}

export interface SubtopicMicroLesson {
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
  lessonType?: 'CONCEPT' | 'WORKED_EXAMPLES' | 'TRAPS_AND_PITFALLS' | 'CHEAT_SHEET' | 'VISUAL' | 'QUICK_RECAP';
  difficultyLevel?: 'FOUNDATION' | 'CORE' | 'ADVANCED';
  availableVariants?: LessonVariantRef[];
  status: 'AI_GENERATED' | 'IN_REVIEW' | 'APPROVED' | 'PUBLISHED' | 'REJECTED';
  contentVersion: number;
  languageCode?: string;
  fallbackToEnglish?: boolean;
  mediaHook?: LessonTutorMediaHook;
  content: MicroLessonContent;
  createdAt?: string;
  updatedAt?: string;
}

export interface MicroTutorialApproach {
  id: string;
  sequenceNo: number;
  title: string;
  approachName: string;
  description: string;
  icon: string;
  badge: string;
  lessonType: 'CONCEPT' | 'WORKED_EXAMPLES' | 'TRAPS_AND_PITFALLS' | 'CHEAT_SHEET' | 'VISUAL' | 'QUICK_RECAP';
  difficultyLevel: 'FOUNDATION' | 'CORE' | 'ADVANCED';
  estimatedMinutes: number;
  lesson: SubtopicMicroLesson;
}

export interface MicroLessonLanguage {
  code: string;
  name: string;
  nativeName: string;
  script: string;
}

export const SUPPORTED_MICROLESSON_LANGUAGES: MicroLessonLanguage[] = [
  { code: 'en', name: 'English', nativeName: 'English', script: 'Latin' },
  { code: 'hi', name: 'Hindi', nativeName: 'हिन्दी', script: 'Devanagari' },
  { code: 'bn', name: 'Bengali', nativeName: 'বাংলা', script: 'Bengali' },
  { code: 'te', name: 'Telugu', nativeName: 'తెలుగు', script: 'Telugu' },
  { code: 'mr', name: 'Marathi', nativeName: 'मराठी', script: 'Devanagari' },
  { code: 'ta', name: 'Tamil', nativeName: 'தமிழ்', script: 'Tamil' },
  { code: 'gu', name: 'Gujarati', nativeName: 'ગુજરાતી', script: 'Gujarati' },
  { code: 'kn', name: 'Kannada', nativeName: 'ಕನ್ನಡ', script: 'Kannada' },
  { code: 'ml', name: 'Malayalam', nativeName: 'മലയാളം', script: 'Malayalam' },
  { code: 'or', name: 'Odia', nativeName: 'ଓଡ଼ିଆ', script: 'Odia' },
  { code: 'pa', name: 'Punjabi', nativeName: 'ਪੰਜਾਬੀ', script: 'Gurmukhi' },
  { code: 'as', name: 'Assamese', nativeName: 'অসমীয়া', script: 'Bengali' },
  { code: 'ur', name: 'Urdu', nativeName: 'اردو', script: 'Perso-Arabic' },
];

export function extractStepsFromLesson(lesson: SubtopicMicroLesson | null | undefined): StepItem[] {
  const b = lesson?.content?.stepByStepBreakdown;
  if (!b) return [];
  const rawSteps = Array.isArray(b)
    ? b
    : Array.isArray(b.steps)
    ? b.steps
    : [];
  return rawSteps.map((s, idx) => ({
    stepNumber: Number(s?.stepNumber || idx + 1),
    title: s?.title || `Step ${idx + 1}`,
    explanation: s?.explanation || '',
    formula: s?.formula || undefined,
    tip: s?.tip || undefined,
  }));
}

export function extractProblemStatementFromLesson(lesson: SubtopicMicroLesson | null | undefined): string {
  const b = lesson?.content?.stepByStepBreakdown;
  if (b && !Array.isArray(b)) {
    const stmt = b.problemStatement || b.questionStem;
    if (stmt) return stmt;
  }
  return lesson?.content?.problemStatement || lesson?.content?.questionStem || lesson?.subtopicName || '';
}

export function extractFinalAnswerFromLesson(lesson: SubtopicMicroLesson | null | undefined): string {
  const b = lesson?.content?.stepByStepBreakdown;
  if (b && !Array.isArray(b) && b.finalAnswer) {
    return b.finalAnswer;
  }
  if (lesson?.content?.finalAnswer) {
    return lesson.content.finalAnswer;
  }
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
  const stepCount = extractStepsFromLesson(lesson).length;
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


