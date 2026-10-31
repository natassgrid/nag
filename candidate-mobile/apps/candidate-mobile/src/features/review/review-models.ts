/**
 * Candidate Mobile Review & Answer Key Challenge Models
 * Ported from nag-frontend-workspace candidate-delivery review module.
 */

export type ReviewStatusFilter = 'ALL' | 'CORRECT' | 'INCORRECT' | 'UNATTEMPTED' | 'CHALLENGED';

export type ChallengeReasonType =
  | 'INCORRECT_PROVISIONAL_KEY'
  | 'QUESTION_AMBIGUOUS_OR_INCORRECT'
  | 'MULTIPLE_CORRECT_OPTIONS'
  | 'NO_CORRECT_OPTION'
  | 'OUT_OF_SYLLABUS';

export interface ReviewOption {
  id: string;
  text: string;
  isCorrect: boolean;
}

export interface StandardBookCitation {
  bookTitle: string;
  author: string;
  editionOrYear?: string;
  pageNumber: string;
  publisher?: string;
}

export interface AnswerKeyChallengePayload {
  questionId: string;
  candidateId: string;
  claimedCorrectOptionId?: string;
  reasonType: ChallengeReasonType;
  justificationText: string;
  bookCitation: StandardBookCitation;
  supportingDocumentUrl?: string;
  feeAmount: number;
}

export interface SubmittedChallengeRecord {
  challengeId: string;
  questionId: string;
  questionNumber: number;
  reasonType: ChallengeReasonType;
  claimedCorrectOptionId?: string;
  justificationText: string;
  bookCitation: StandardBookCitation;
  feePaid: number;
  transactionReference: string;
  submittedAt: string;
  status: 'SUBMITTED' | 'UNDER_REVIEW' | 'ACCEPTED_REFUNDED' | 'REJECTED';
}

export interface ReviewQuestionItem {
  id: string;
  questionNumber: number;
  subject: string;
  topic?: string;
  content: string;
  explanation: string;
  options: ReviewOption[];
  userChoice?: string;
  provisionalAnswerKey: string;
  isCorrect: boolean;
  marksAwarded: number;
  negativeMarks: number;
  isChallenged?: boolean;
  challengeDetails?: SubmittedChallengeRecord;
}

export interface ReviewSummaryStats {
  totalQuestions: number;
  totalAttempted: number;
  correct: number;
  incorrect: number;
  unattempted: number;
  challengedCount: number;
  rawScore: number;
  maxMarks: number;
  accuracyRate: number;
  avgTimePerItem?: string;
}

export interface ReviewableExamAttempt {
  attemptId: string;
  examId: string;
  examTitle: string;
  examCode: string;
  examDate: string;
  shiftName: string;
  rollNumber: string;
  candidateName: string;
  challengeWindowClosesAt: string;
  challengeFeePerQuestion: number;
  questions: ReviewQuestionItem[];
  challenges: SubmittedChallengeRecord[];
}

export interface ReviewState {
  attempt: ReviewableExamAttempt;
  statusFilter: ReviewStatusFilter;
  currentIndex: number;
  selectedQuestionId: string | null;
  isChallengeModalOpen: boolean;
  activeChallengeQuestion: ReviewQuestionItem | null;
  submittedChallenges: SubmittedChallengeRecord[];
  stats: ReviewSummaryStats;
  searchQuery: string;
}
