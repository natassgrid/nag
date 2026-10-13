export interface MicroLessonStep {
  stepNumber: number;
  title: string;
  explanation: string;
  formula?: string;
}

export interface StepByStepBreakdown {
  problemStatement: string;
  steps: MicroLessonStep[];
}

export interface MicroLessonContent {
  coreConcept: string;
  trapAlert: string;
  formulaCheatSheet: string;
  stepByStepBreakdown: StepByStepBreakdown;
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
