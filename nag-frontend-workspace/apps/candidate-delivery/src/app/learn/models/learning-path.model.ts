export interface MilestoneItem {
  milestoneId: string;
  subtopicId: number;
  subtopicName: string;
  topicName: string;
  subjectName: string;
  status: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED';
  targetScore: number;
  microLessonId?: string | null;
  completedAt?: string | null;
}

export interface MilestonePhase {
  phaseNumber: number;
  name: string;
  focus: string;
  status: 'LOCKED' | 'ACTIVE' | 'IN_PROGRESS' | 'COMPLETED';
  durationWeeks: number;
  targetAccuracy: number;
  achievedAccuracy?: number | null;
  quizSessionId?: string | null;
  passThreshold: number;
  gatekeeperSessionId?: string | null;
  passed: boolean;
  items: MilestoneItem[];
}

export interface MilestoneGraph {
  phases: MilestonePhase[];
  currentPhase: number | string;
  targetExam: string;
  gatekeeperPassed: boolean;
}

export interface DynamicLearningPath {
  id: string;
  candidateId: string;
  status: 'ACTIVE' | 'IN_PROGRESS' | 'COMPLETED' | 'SUPERSEDED' | 'ARCHIVED';
  targetExam: string;
  createdAt: string;
  updatedAt?: string;
  milestoneGraph: MilestoneGraph;
  conceptualGaps?: Array<{ subtopicId?: number; subtopicName: string; accuracyPercent?: number }>;
  applicationGaps?: Array<{ subtopicId?: number; subtopicName: string; accuracyPercent?: number }>;
  gatekeeperPassed: boolean;
}

export interface GatekeeperStatusResponse {
  allowed?: boolean;
  gatekeeperPassed?: boolean;
  gatekeeperRequired?: boolean;
  clearedForFullMock?: boolean;
  currentPhase?: string;
  gatekeeperSessionId?: string;
  remainingMilestonesCount?: number;
  remedialSubtopics?: string[];
  active?: boolean;
  hasGaps?: boolean;
  conceptualGaps?: Array<{ subtopicId?: number; subtopicName: string; accuracyPercent?: number }>;
  applicationGaps?: Array<{ subtopicId?: number; subtopicName: string; accuracyPercent?: number }>;
  message?: string;
}

export interface SubjectMasterySummary {
  subjectName: string;
  totalSubtopics: number;
  conceptualGapsCount: number;
  applicationGapsCount: number;
  proficientCount: number;
  avgAccuracyPercent: number;
}

export interface LearningPathSummary {
  candidateId: string;
  activePathId?: string;
  examTarget?: string;
  status?: string;
  totalSubtopics: number;
  conceptualGapsCount: number;
  applicationGapsCount: number;
  proficientCount: number;
  masteryPercentage: number;
  subjectSummaries?: Record<string, SubjectMasterySummary>;
}

export interface StartAdaptiveQuizResponse {
  quizSessionId: string;
  targetAccuracy: number;
  questionCount: number;
  message?: string;
}

export interface StartGatekeeperResponse {
  gatekeeperSessionId: string;
  passThreshold: number;
  questionCount: number;
  message?: string;
}
