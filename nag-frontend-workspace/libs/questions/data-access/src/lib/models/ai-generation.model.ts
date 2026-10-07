import { QuestionOption } from './question.model';

export interface ParagraphConfig {
  passageWordLength?: number;
  subQuestionCount?: number;
  passageTheme?: string;
}

/** @deprecated Use ParagraphConfig — renamed to match backend field name */
export type ParagraphSetConfig = ParagraphConfig;

export interface QuestionGenerationRequest {
  /** Numeric FK to the subject (from taxonomy API). When present, backend uses id lookup — no auto-create. */
  subjectId?: number;
  /** Numeric FK to the topic (from taxonomy API). When present, backend uses id lookup — no auto-create. */
  topicId?: number;
  /** Numeric FK to the subtopic (from taxonomy API, optional). */
  subtopicId?: number;

  subject: string;
  topic: string;
  subtopic?: string;
  rawTextInput?: string;
  description?: string;
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
    | 'ASSERTION_REASON'
    | 'MATRIX_MATCH'
    | string;
  /** Matches backend field name `paragraphConfig` (not `paragraphSetConfig`). */
  paragraphConfig?: ParagraphConfig;
  count: number;
  avoidDuplicate?: boolean;
  autoSave?: boolean;

  /** Pipeline execution mode: AUTO | FAST | MULTI_AGENT */
  executionMode?: 'AUTO' | 'FAST' | 'MULTI_AGENT' | string;
  /** Target competitive exam code, e.g. UPSC_CSE, JEE_ADV, GATE, NEET */
  targetExam?: string;
  /** Generation quality rubric: STANDARD | EXAM_READY */
  generationQuality?: 'STANDARD' | 'EXAM_READY' | string;
  requireCriticReview?: boolean;
  sampleQuestions?: string[];
  sampleFileUrl?: string;
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
  criticScore?: number;
  criticFeedback?: string[];
}

export interface QuestionGenerationResponse {
  questions: GeneratedQuestion[];
  modelUsed: string;
  totalGenerated: number;
  totalValid: number;
  totalDuplicates: number;
  /** Execution path resolved by the triage router: FAST or MULTI_AGENT */
  executionMode?: string;
  triageRationale?: string;
}

export interface BatchItem {
  /** Numeric FK to the subject (from taxonomy API). When present, no auto-create on backend. */
  subjectId?: number;
  /** Numeric FK to the topic (from taxonomy API). When present, no auto-create on backend. */
  topicId?: number;
  /** Numeric FK to the subtopic (optional). */
  subtopicId?: number;

  subject: string;
  topic: string;
  subtopic?: string;
  rawTextInput?: string;
  description?: string;
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
