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

export function createDefaultRubric(maxMarks: number): RubricCriterion[] {
  const p1 = Math.round(maxMarks * 0.4);
  const p2 = Math.round(maxMarks * 0.3);
  const p3 = Math.round(maxMarks * 0.2);
  const p4 = Math.max(1, maxMarks - p1 - p2 - p3);

  return [
    {
      id: 'crit-1',
      name: 'Conceptual Accuracy & Core Logic',
      description: 'Correctness of mathematical/algorithmic formulation and theoretical principles.',
      maxMarks: p1,
      awardedMarks: p1,
    },
    {
      id: 'crit-2',
      name: 'Methodological Rigor & Intermediate Steps',
      description: 'Sound step-by-step reasoning, derivation, and adherence to edge cases.',
      maxMarks: p2,
      awardedMarks: p2,
    },
    {
      id: 'crit-3',
      name: 'Efficiency & Optimization',
      description: 'Time/space complexity, resource utilization, or structural conciseness.',
      maxMarks: p3,
      awardedMarks: p3,
    },
    {
      id: 'crit-4',
      name: 'Presentation & Notation Quality',
      description: 'Clarity of variables, clean documentation, and standard scientific syntax.',
      maxMarks: p4,
      awardedMarks: p4,
    },
  ];
}
