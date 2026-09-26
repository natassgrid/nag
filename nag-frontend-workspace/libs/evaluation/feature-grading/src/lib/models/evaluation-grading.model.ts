export type EvaluationActiveTab = 'grading' | 'disputes';

export interface EvaluationKpiStats {
  pendingTasksCount: number;
  openDisputesCount: number;
  meanCohortScore?: string;
  scoringReliability?: string;
}

export interface RubricCriterion {
  id: string;
  name: string;
  description: string;
  maxMarks: number;
  awardedMarks: number;
  weightage?: number;
}

export interface InlineComment {
  id: string;
  lineNumber?: number;
  text: string;
  author: string;
  timestamp: string;
  tag?: 'ACCURACY' | 'METHODOLOGY' | 'SYNTAX_LOGIC' | 'FORMATTING';
}

export interface ModerationSignOff {
  moderatorName: string;
  moderatorRole: string;
  signedAt: string;
  verdict: 'APPROVED' | 'REQUIRES_REVALUATION' | 'ESCALATED';
  moderationNotes?: string;
}

export interface GradeSubmissionPayload {
  taskId: string;
  awardedMarks: number;
  evaluatorComments: string;
  rubricBreakdown?: RubricCriterion[];
  inlineComments?: InlineComment[];
  moderationSignOff?: ModerationSignOff;
}
