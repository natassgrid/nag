import { QuestionOption } from './question.model';

export interface ParagraphSetConfig {
  passageWordLength?: number;
  subQuestionCount?: number;
  passageTheme?: string;
}

export interface QuestionGenerationRequest {
  subject: string;
  topic: string;
  subtopic?: string;
  difficulty: 'EASY' | 'MEDIUM' | 'HARD' | string;
  cognitiveLevel:
    | 'REMEMBER'
    | 'UNDERSTAND'
    | 'APPLY'
    | 'ANALYZE'
    | 'EVALUATE'
    | 'CREATE'
    | string;
  questionType:
    | 'SINGLE_MCQ'
    | 'MULTI_MCQ'
    | 'NUMERICAL'
    | 'DESCRIPTIVE'
    | 'PARAGRAPH_SET'
    | string;
  generationType?: 'STANDALONE' | 'PARAGRAPH_SET' | string;
  paragraphSetConfig?: ParagraphSetConfig;
  count: number;
  avoidDuplicate?: boolean;
  autoSave?: boolean;
}

export interface GeneratedQuestionValidation {
  valid: boolean;
  errors: string[];
}

export interface GeneratedQuestionDuplicate {
  similarQuestionId?: string;
  similarity: number;
}

export interface GeneratedQuestion {
  content: string;
  answerKey?: string;
  explanation?: string;
  options?: QuestionOption[];
  difficulty: string;
  cognitiveLevel: string;
  questionType: string;
  validation?: GeneratedQuestionValidation;
  duplicate?: GeneratedQuestionDuplicate;
  savedQuestionId?: string;
}

export interface QuestionGenerationResponse {
  questions: GeneratedQuestion[];
  modelUsed: string;
  totalGenerated: number;
  totalValid: number;
  totalDuplicates: number;
}

export interface BatchItem {
  subject: string;
  topic: string;
  subtopic?: string;
  difficulty: string;
  cognitiveLevel: string;
  questionType: string;
  count: number;
}

export interface BatchGenerationRequest {
  items: BatchItem[];
  avoidDuplicates?: boolean;
}

export interface BatchGenerationJob {
  id: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
  totalRequested: number;
  totalGenerated: number;
  totalFailed: number;
  totalDuplicates: number;
  modelUsed?: string;
  createdAt?: string;
  completedAt?: string;
  errorMessage?: string;
  progress?: number;
}
