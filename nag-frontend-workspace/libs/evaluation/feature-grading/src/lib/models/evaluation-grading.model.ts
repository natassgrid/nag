export type EvaluationActiveTab = 'grading' | 'disputes';

export interface EvaluationKpiStats {
  pendingTasksCount: number;
  openDisputesCount: number;
  meanCohortScore?: string;
  scoringReliability?: string;
}

export interface GradeSubmissionPayload {
  taskId: string;
  awardedMarks: number;
  evaluatorComments: string;
}
