export interface WeakTopicRecommendation {
  topicName: string;
  currentAccuracy: number;
  subtopicsToRevise: string[];
}

export interface StudyPlanItem {
  day: number;
  topicName: string;
  estimatedMinutes: number;
}

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

export interface LearnerProfile {
  candidateId: string;
  totalPracticeSessions: number;
  overallAccuracy: number;
  weakTopics: string[];
  strongTopics: string[];
  topicAccuracyMap: Record<string, { accuracy: number; attempts: number }>;
}
