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
  answer: string;
  explanation?: string;
}

export interface MicroLessonContent {
  coreConcept: string;
  trapAlert: string | TrapAlert;
  formulaCheatSheet: string | FormulaEntry[];
  stepByStepBreakdown: StepByStepBreakdown;
  keyTakeaways?: string[];
  quickCheckItems?: QuickCheckItem[];
}

export interface LessonVariantRef {
  id: string;
  lessonType: 'CONCEPT' | 'WORKED_EXAMPLES' | 'TRAPS_AND_PITFALLS' | 'CHEAT_SHEET' | 'VISUAL' | 'QUICK_RECAP';
  difficultyLevel: 'FOUNDATION' | 'CORE' | 'ADVANCED';
  title?: string;
}

export interface SubtopicMicroLesson {
  id: string;
  subtopicId: number;
  subtopicName: string;
  topicName: string;
  subjectName: string;
  targetExam?: string;
  lessonType?: 'CONCEPT' | 'WORKED_EXAMPLES' | 'TRAPS_AND_PITFALLS' | 'CHEAT_SHEET' | 'VISUAL' | 'QUICK_RECAP';
  difficultyLevel?: 'FOUNDATION' | 'CORE' | 'ADVANCED';
  availableVariants?: LessonVariantRef[];
  status: 'AI_GENERATED' | 'IN_REVIEW' | 'APPROVED' | 'PUBLISHED' | 'REJECTED';
  contentVersion: number;
  languageCode?: string;
  fallbackToEnglish?: boolean;
  content: MicroLessonContent;
  createdAt?: string;
  updatedAt?: string;
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

