export interface WeakTopicRecommendation {
  topicName?: string;
  topic?: string;
  subject?: string;
  subjectName?: string;
  currentAccuracy?: number;
  subtopicsToRevise?: string[];
  subtopics?: string[];
}

export interface StudyPlanItem {
  day: number;
  topicName: string;
  estimatedMinutes: number;
  completed?: boolean;
}

export type StudyPlanDay = StudyPlanItem;

export interface Recommendation {
  id: string;
  candidateId: string;
  triggerSessionId: string | null;
  status: 'PENDING' | 'GENERATED' | 'DISMISSED';
  generatedAt: string | null;
  weakTopicRecommendations: WeakTopicRecommendation[];
  studyPlanItems: StudyPlanItem[];
  suggestedPracticeSetIds: string[];
  motivationalMessage: string | null;
}

export type RecommendationResponse = Recommendation;

export interface LearnerProfile {
  candidateId: string;
  totalPracticeSessions: number;
  totalSessionsCompleted?: number;
  overallAccuracy: number;
  weakTopics: string[];
  strongTopics: string[];
  topicAccuracyMap: Record<string, { accuracy: number; attempts: number }>;
}
