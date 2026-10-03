export type PracticeSessionMode = 'TIMED' | 'UNTIMED' | 'SECTION_WISE';
export type PracticeSessionStatus = 'CREATED' | 'IN_PROGRESS' | 'SUBMITTED' | 'ABANDONED';

export interface PracticeSet {
  id: string;
  name: string;
  description: string;
  source: string;
  durationMinutes: number;
  subjectSlug: string;
  published: boolean;
  totalQuestions: number;
}

export interface PracticeSession {
  id: string;
  practiceSetId: string;
  mode: PracticeSessionMode;
  status: PracticeSessionStatus;
  startedAt: string;
  totalQuestions: number;
  durationMinutes: number;
}

export interface QuestionResult {
  questionId: string;
  candidateAnswer: string | null;
  correctAnswer: string | null;
  correct: boolean;
  marksAwarded: number;
  timeSpentMs: number;
  markedForReview: boolean;
  content?: string | null;
  optionsJson?: string | null;
  explanation?: string | null;
  topic?: string | null;
  subject?: string | null;
}

export interface PracticeResult {
  sessionId: string;
  correctCount: number;
  incorrectCount: number;
  skippedCount: number;
  obtainedMarks: number;
  totalMarks: number;
  accuracyPercent: number;
  topicWiseBreakdown: Record<string, Record<string, number>> | null;
  difficultyBreakdown: Record<string, number[]> | null;
  timingBreakdown: Record<string, number> | null;
  questionResults: QuestionResult[];
  practiceSetName?: string | null;
  mode?: string | null;
}

export interface PracticeHistoryItem {
  sessionId: string;
  practiceSetId: string;
  practiceSetName: string | null;
  submittedAt: string | null;
  obtainedMarks: number;
  totalMarks: number;
  accuracyPercent: number;
  totalQuestions: number;
  correctCount: number;
  incorrectCount: number;
}

export interface StartSessionRequest {
  practiceSetId: string;
  mode: PracticeSessionMode;
}

export interface SaveResponseRequest {
  questionId: string;
  selectedOptionIds: string | null;
  enteredValue: string | null;
  timeSpentMs: number;
  markedForReview: boolean;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
