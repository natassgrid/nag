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

export interface MicroLessonReviewItem {
  id: string;
  subtopicId: number;
  subtopicName: string;
  topicName: string;
  subjectName: string;
  targetExam: string;
  status: 'AI_GENERATED' | 'IN_REVIEW' | 'APPROVED' | 'PUBLISHED' | 'REJECTED';
  contentVersion: number;
  reviewerNotes?: string | null;
  content: MicroLessonContent;
  createdAt?: string;
  updatedAt?: string;
}

export interface ReviewMicroLessonRequest {
  action: 'APPROVE' | 'REJECT' | 'EDIT' | 'PUBLISH';
  reviewerNotes?: string;
  updatedContent?: MicroLessonContent;
}

export interface BatchGenerateMicroLessonsRequest {
  subjectId?: number;
  targetExam?: string;
  forceRegenerate?: boolean;
  limit?: number;
}

export interface GenerateMicroLessonRequest {
  subjectId?: number;
  subject: string;
  topicId?: number;
  topic: string;
  subtopicId?: number;
  subtopic: string;
  targetExam?: string;
  referenceText?: string;
  promptNotes?: string;
}

export interface BatchGenerateResponse {
  batchId: string;
  totalQueued: number;
  status: string;
  message: string;
}
