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
  selectedOptionId?: string;
  isFlagged?: boolean;
  isVisited?: boolean;
}

export interface ExamSubmissionReceipt {
  signature: string;
  hash: string;
  timestamp: string;
}

export interface ExamSessionMetadata {
  sessionId: string;
  candidateId: string;
}
