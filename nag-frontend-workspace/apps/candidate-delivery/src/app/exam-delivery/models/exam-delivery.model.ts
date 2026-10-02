export interface ExamOption {
  id: string;
  text: string;
}

export interface ExamItem {
  id: string;
  order: number;
  questionCode: string;
  content: string;
  options: ExamOption[];
  marks: number;
  negativeMarks: number;
  subject?: string;
  correctOptionId?: string;
  selectedOptionId?: string;
  isFlagged?: boolean;
  isVisited?: boolean;
}

export interface ExamSubmissionReceipt {
  signature: string;
  hash: string;
  timestamp: string;
  mode?: 'LIVE' | 'PRACTICE' | 'PREVIEW';
  score?: number;
  totalMarks?: number;
  correctCount?: number;
  incorrectCount?: number;
  unansweredCount?: number;
}

export interface ExamSessionMetadata {
  sessionId: string;
  candidateId: string;
  title?: string;
}
