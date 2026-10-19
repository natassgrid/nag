export type PaperExtractionStatus =
  | 'PENDING'
  | 'PROCESSING'
  | 'COMPLETED'
  | 'PARTIAL_FAILED'
  | 'FAILED'
  | 'CANCELLED';

export type ExtractedQuestionStatus =
  | 'EXTRACTED'
  | 'NEEDS_REVIEW'
  | 'APPROVED'
  | 'REJECTED'
  | 'PROMOTED'
  | 'DUPLICATE';

export interface ExtractedQuestionOption {
  id: string; // 'A', 'B', 'C', 'D', 'E', 'F'
  text: string;
  correct?: boolean;
  imageUrl?: string;
  imageAltText?: string;
}

export interface ExtractedQuestion {
  id: string;
  createdAt?: string;
  updatedAt?: string;
  tenantId?: string;
  version?: number;
  paperMetadataId: string;
  questionNumber: number;
  sourcePageNumber?: number;
  sectionName?: string;
  questionType: string; // 'SINGLE_MCQ', 'MULTI_MCQ', 'NUMERICAL', 'DESCRIPTIVE'
  content: string;
  options: ExtractedQuestionOption[];
  answerKey?: string;
  explanation?: string;
  passageText?: string;
  translations?: Record<string, { content?: string; options?: ExtractedQuestionOption[]; explanation?: string; [key: string]: any }>;
  hasImages?: boolean;
  hasTables?: boolean;
  hasFormulas?: boolean;
  mediaUrls?: string[];
  suggestedSubjectId?: number;
  suggestedTopicId?: number;
  suggestedSubtopicId?: number;
  suggestedDifficulty?: string;
  suggestedCognitiveLevel?: string;
  extractionConfidence?: number;
  parsingTier?: string;
  rawLlmPayload?: Record<string, any>;
  status: ExtractedQuestionStatus;
  duplicateOfQuestionId?: string;
  promotedQuestionId?: string;
  reviewerId?: string;
  reviewComments?: string;
}

export interface QuestionPaperMetadata {
  id: string;
  createdAt?: string;
  updatedAt?: string;
  tenantId?: string;
  version?: number;
  examinationName: string;
  examYear: number;
  department?: string;
  paperCode?: string;
  bookletSerialNo?: string;
  subjectName?: string;
  defaultSubjectId?: number;
  defaultTopicId?: number;
  defaultSubtopicId?: number;
  languages?: string[];
  totalPages: number;
  totalQuestionsExpected?: number;
  durationMinutes?: number;
  maxMarks?: number;
  negativeMarkingRatio?: number;
  sectionsMetadata?: Record<string, any>[];
  fileAssetId?: string;
  originalFilename?: string;
  fileStoragePath?: string;
  fileSha256?: string;
  extractionStatus: PaperExtractionStatus;
  modelUsed?: string;
  pagesProcessed: number;
  questionsExtracted: number;
  questionsApproved: number;
  questionsPromoted: number;
  failedPages?: Record<string, any>[];
  errorMessage?: string;
  uploadedBy?: string;
  startedAt?: string;
  completedAt?: string;
  progressPercentage?: number;
}

export interface PaperUploadRequest {
  examinationName: string;
  examYear: number;
  department?: string;
  paperCode?: string;
  bookletSerialNo?: string;
  subjectName?: string;
  defaultSubjectId?: number;
  defaultTopicId?: number;
  defaultSubtopicId?: number;
  languages?: string[];
  totalQuestionsExpected?: number;
  durationMinutes?: number;
  maxMarks?: number;
  negativeMarkingRatio?: number;
  forceReExtract?: boolean;
  fileAssetId?: string;
}

export interface BulkAnswerKeyRequest {
  answersByQuestionNumber: Record<number, string>;
  explanationsByQuestionNumber?: Record<number, string>;
  autoApproveMatched?: boolean;
}

export interface PromoteQuestionsRequest {
  extractedQuestionIds?: string[];
  fallbackSubjectId?: number;
  fallbackTopicId?: number;
  fallbackSubtopicId?: number;
  targetQuestionState?: string;
  createTranslations?: boolean;
}

export interface PromoteQuestionsResponse {
  paperMetadataId: string;
  totalRequested: number;
  totalPromoted: number;
  totalSkipped: number;
  totalFailed: number;
  promotedQuestionIds: string[];
  failureReasons?: Record<string, string>;
}

export interface UpdateExtractedQuestionRequest {
  questionNumber?: number;
  sectionName?: string;
  questionType?: string;
  content?: string;
  options?: ExtractedQuestionOption[];
  answerKey?: string;
  explanation?: string;
  passageText?: string;
  translations?: Record<string, Record<string, any>>;
  suggestedSubjectId?: number;
  suggestedTopicId?: number;
  suggestedSubtopicId?: number;
  suggestedDifficulty?: string;
  suggestedCognitiveLevel?: string;
  status?: ExtractedQuestionStatus;
  reviewComments?: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages?: number;
  size?: number;
  number?: number;
  first?: boolean;
  last?: boolean;
  empty?: boolean;
}

export interface PaperKpiStats {
  totalPapers: number;
  pagesProcessed: number;
  questionsExtracted: number;
  questionsApproved: number;
  questionsPromoted: number;
  processingPapers: number;
}
