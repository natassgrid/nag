export type QuestionType =
  | 'SINGLE_MCQ'
  | 'MULTIPLE_MCQ'
  | 'MULTI_MCQ'
  | 'MULTIPLE_CHOICE'
  | 'MULTIPLE_SELECT'
  | 'NUMERICAL'
  | 'TEXT'
  | 'DESCRIPTIVE'
  | 'PARAGRAPH_SET';

export type DifficultyLevel = 'EASY' | 'MEDIUM' | 'HARD' | 'EXPERT';

export type QuestionStatus =
  | 'DRAFT'
  | 'REVIEW'
  | 'IN_REVIEW'
  | 'APPROVED'
  | 'REJECTED';

export interface QuestionOption {
  id: string;
  text: string;
  isCorrect: boolean;
  imageUrl?: string;
  imageAltText?: string;
}

export interface Question {
  id: string;
  code?: string;
  content: string;
  type: QuestionType | string;
  difficulty: DifficultyLevel;
  status: QuestionStatus | string;
  subject?: string;
  topic?: string;
  subtopic?: string;
  subjectId?: string | number;
  topicId?: string | number;
  subtopicId?: string | number;
  marks: number;
  negativeMarks: number;
  options: QuestionOption[];
  answerKey?: string;
  explanation?: string;
  tags?: string[];
  passageId?: string;
  passageOrderIndex?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface QuestionFilter {
  query?: string;
  search?: string;
  subject?: string;
  subjectId?: string | number;
  topic?: string;
  topicId?: string | number;
  difficulty?: string;
  state?: string;
  status?: string;
  type?: string;
  sort?: string;
  order?: 'asc' | 'desc';
  page: number;
  size: number;
}

export interface VectorSearchResult {
  id: string;
  code: string;
  content: string;
  similarityScore: number;
  difficulty: DifficultyLevel;
}

export interface PagedQuestionsResponse {
  content: Question[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
