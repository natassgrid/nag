export type ReviewStatusFilter = 'ALL' | 'CORRECT' | 'INCORRECT' | 'UNATTEMPTED';

export interface ReviewOption {
  id: string;
  text: string;
  isCorrect: boolean;
}

export interface ReviewQuestionItem {
  id: string;
  subject: string;
  content: string;
  explanation: string;
  options: ReviewOption[];
  userChoice?: string;
  isCorrect: boolean;
}

export interface ReviewStats {
  correct: number;
  incorrect: number;
  unattempted: number;
  avgTimePerItem?: string;
}
