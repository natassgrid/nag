export interface PracticeSet {
  id: string;
  name: string;
  description: string | null;
  source: 'MANUAL' | 'AUTO_GENERATED' | 'EXAM_CLONE';
  durationMinutes: number;
  subjectSlug: string | null;
  published: boolean;
  totalQuestions: number;
  createdBy: string;
  createdAt: string;
}

export interface CreatePracticeSetRequest {
  name: string;
  description: string | null;
  durationMinutes: number;
  subjectSlug: string | null;
  questionIds: string[];
}

export interface UpdatePracticeSetRequest {
  name: string;
  description: string | null;
  durationMinutes: number;
  totalQuestions: number;
}

export interface PracticeSetFormState {
  name: string;
  description: string;
  durationMinutes: number;
  subjectSlug: string;
}
