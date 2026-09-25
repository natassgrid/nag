import { Question, QuestionOption, QuestionType } from './question.model';

export interface SubQuestionRequest {
  id?: string;
  passageOrderIndex?: number;
  content: string;
  difficulty?: 'EASY' | 'MEDIUM' | 'HARD' | 'EXPERT' | string;
  cognitiveLevel?: 'REMEMBER' | 'UNDERSTAND' | 'APPLY' | 'ANALYZE' | 'EVALUATE' | 'CREATE' | string;
  questionType?: QuestionType | string;
  options: QuestionOption[];
  answerKey?: string;
  explanation?: string;
  marks?: number;
  negativeMarks?: number;
}

export interface PassageRequest {
  title?: string;
  content: string;
  contentFormat?: string;
  subjectId: number;
  topicId?: number;
  subject?: string;
  topic?: string;
  subtopic?: string;
  hasImages?: boolean;
  subQuestions: SubQuestionRequest[];
}

export interface PassageResponse {
  id: string;
  title?: string;
  content: string;
  contentFormat?: string;
  subjectId: number;
  topicId?: number;
  subject?: string;
  topic?: string;
  subtopic?: string;
  hasImages?: boolean;
  state: string;
  authorId?: string;
  reviewerId?: string;
  subQuestions: Question[];
  subQuestionCount?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface PagedPassagesResponse {
  content: PassageResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
