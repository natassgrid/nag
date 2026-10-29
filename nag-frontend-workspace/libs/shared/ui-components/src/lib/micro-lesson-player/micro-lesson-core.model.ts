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

export type LessonType =
  | 'CONCEPT'
  | 'WORKED_EXAMPLES'
  | 'TRAPS_AND_PITFALLS'
  | 'CHEAT_SHEET'
  | 'VISUAL'
  | 'QUICK_RECAP'
  | 'STORY_ANALOGY';

export type MicroLessonType = LessonType;

export type LessonDifficultyLevel = 'FOUNDATION' | 'CORE' | 'ADVANCED';

export type MicroLessonDifficulty = LessonDifficultyLevel;

export interface LessonVariantRef {
  id: string;
  lessonType: LessonType | string;
  difficultyLevel: LessonDifficultyLevel | string;
  sequenceNo?: number;
  title?: string;
  approachName?: string;
  estimatedMinutes?: number;
}

export interface MicroLessonContent {
  coreConcept: string;
  trapAlert: string | TrapAlert;
  structuredTrapAlert?: TrapAlert;
  formulaCheatSheet: string | FormulaEntry[];
  formulaEntries?: FormulaEntry[];
  stepByStepBreakdown: StepByStepBreakdown | StepItem[];
  problemStatement?: string;
  questionStem?: string;
  finalAnswer?: string;
  keyTakeaways?: string[];
  quickCheckItems?: QuickCheckItem[];
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
  lessonType?: LessonType | string;
  difficultyLevel?: LessonDifficultyLevel | string;
  availableVariants?: LessonVariantRef[];
  status: 'AI_GENERATED' | 'IN_REVIEW' | 'APPROVED' | 'PUBLISHED' | 'REJECTED' | 'DRAFT' | string;
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
  lessonType: LessonType;
  difficultyLevel: LessonDifficultyLevel;
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

// Aliases for Player-prefixed types for 100% backward compatibility
export type PlayerMicroLessonStep = MicroLessonStep;
export type PlayerStepByStepBreakdown = StepByStepBreakdown;
export type PlayerTrapAlert = TrapAlert;
export type PlayerFormulaEntry = FormulaEntry;
export type PlayerQuickCheckItem = QuickCheckItem;
export type PlayerMicroLessonContent = MicroLessonContent;
export type PlayerSubtopicMicroLesson = SubtopicMicroLesson;
