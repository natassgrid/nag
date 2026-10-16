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

export interface SubtopicMicroLesson {
  id: string;
  subtopicId: number;
  subtopicName: string;
  topicName: string;
  subjectName: string;
  targetExam: string;
  status: 'AI_GENERATED' | 'IN_REVIEW' | 'APPROVED' | 'PUBLISHED' | 'REJECTED';
  contentVersion: number;
  content: MicroLessonContent;
  createdAt?: string;
  updatedAt?: string;
}
