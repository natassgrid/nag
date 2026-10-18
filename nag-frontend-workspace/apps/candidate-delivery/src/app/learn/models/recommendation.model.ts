export interface WeakTopicRecommendation {
  topicName?: string;
  topic?: string;
  subject?: string;
  subjectName?: string;
  currentAccuracy?: number;
  subtopicsToRevise?: string[];
  subtopics?: string[];
  subtopicId?: number;
}

export interface StudyPlanItem {
  day: number;
  topicName: string;
  estimatedMinutes: number;
  tasks?: string[];
  focusTopic?: string;
  durationMinutes?: number;
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

export interface RecommendationLearnerProfile {
  candidateId: string;
  totalPracticeSessions: number;
  totalSessionsCompleted?: number;
  overallAccuracy: number;
  weakTopics: string[];
  strongTopics: string[];
  topicAccuracyMap: Record<string, { accuracy: number; attempts: number }>;
}

export type LegacyLearnerProfile = RecommendationLearnerProfile;
