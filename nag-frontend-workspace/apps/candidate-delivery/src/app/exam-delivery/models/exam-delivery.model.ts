export type ExamDeliveryMode = 'LIVE' | 'PRACTICE' | 'PREVIEW';

export interface ExamOption {
  id: string;
  text: string;
  content?: string;
}

export interface ExamTranslation {
  languageCode: string;
  content: string;
  options?: ExamOption[];
}

export interface ExamItem {
  id: string;
  order: number;
  questionCode: string;
  content: string;
  stem?: string;
  options: ExamOption[];
  marks: number;
  negativeMarks: number;
  subject?: string;
  correctOptionId?: string;
  selectedOptionId?: string;
  isFlagged?: boolean;
  isVisited?: boolean;
  primaryLanguage?: string;
  fallbackToEnglish?: boolean;
  primaryTranslation?: ExamTranslation;
}

export interface ExamScoreSummary {
  score: number;
  totalMarks: number;
  correctCount: number;
  incorrectCount: number;
  unansweredCount: number;
}

export interface ExamSubmissionReceipt {
  signature: string;
  hash: string;
  timestamp: string;
  mode?: ExamDeliveryMode;
  score?: number;
  totalMarks?: number;
  correctCount?: number;
  incorrectCount?: number;
  unansweredCount?: number;
  totalQuestions?: number;
  accuracyPercent?: number;
  practiceSetName?: string;
  sessionId?: string;
}

export interface ExamSessionMetadata {
  sessionId: string;
  candidateId: string;
  title?: string;
}
