import {
  FormulaEntry,
  MicroLessonContent,
  MicroLessonDifficulty,
  MicroLessonStep,
  MicroLessonType,
  QuickCheckItem,
  StepByStepBreakdown,
  TrapAlert,
  parseFormulaEntries,
  parseStepBreakdown,
  parseTrapAlert,
} from '@nag-frontend-workspace/shared-ui-components';

export * from '@nag-frontend-workspace/shared-ui-components';

export interface MicroLessonReviewItem {
  id: string;
  subjectId?: number;
  topicId?: number;
  subtopicId?: number;
  subject?: string;
  topic?: string;
  subtopic?: string;
  subtopicName: string;
  topicName: string;
  subjectName: string;
  targetExam?: string;
  lessonType?: MicroLessonType | string;
  difficultyLevel?: MicroLessonDifficulty | string;
  sequenceNo?: number;
  estimatedMinutes?: number;
  status: 'AI_GENERATED' | 'IN_REVIEW' | 'APPROVED' | 'PUBLISHED' | 'REJECTED' | 'DRAFT';
  contentVersion: number;
  reviewerNotes?: string | null;
  content: MicroLessonContent;
  coreConcept?: string;
  trap?: string;
  cheatSheet?: string[];
  createdAt?: string;
  updatedAt?: string;
  languageCode?: string;
  fallbackToEnglish?: boolean;
  translations?: MicroLessonTranslation[];
}

export type MicroLessonAdminItem = MicroLessonReviewItem;

export interface CreateMicroLessonRequest {
  subjectId?: number;
  subject?: string;
  subjectName?: string;
  topicId?: number;
  topic?: string;
  topicName?: string;
  subtopicId?: number;
  subtopic?: string;
  subtopicName?: string;
  targetExam?: string;
  lessonType?: MicroLessonType | string;
  difficultyLevel?: MicroLessonDifficulty | string;
  sequenceNo?: number;
  estimatedMinutes?: number;
  status?: string;
  coreConcept: string;
  trap?: string;
  trapAlert?: string | TrapAlert;
  cheatSheet?: string[];
  formulaCheatSheet?: string | FormulaEntry[];
  stepByStepBreakdown?: {
    questionStem?: string;
    problemStatement?: string;
    steps: (string | MicroLessonStep)[];
    structuredSteps?: MicroLessonStep[];
    finalAnswer?: string;
  };
  keyTakeaways?: string[];
  quickCheckItems?: QuickCheckItem[];
  reviewerNotes?: string;
  reviewComments?: string;
}

export type UpdateMicroLessonRequest = CreateMicroLessonRequest;

export interface ReviewMicroLessonRequest {
  action: 'APPROVE' | 'REJECT' | 'EDIT' | 'PUBLISH';
  reviewerNotes?: string;
  updatedContent?: MicroLessonContent;
  status?: string;
  comments?: string;
  coreConcept?: string;
  trap?: string;
  cheatSheet?: string[];
  stepByStepBreakdown?: any;
}

export interface BatchGenerateMicroLessonsRequest {
  subjectId?: number;
  topicId?: number;
  subtopicIds?: number[];
  targetExam?: string;
  forceRegenerate?: boolean;
  limit?: number;
  maxCount?: number;
}

export interface GenerateMicroLessonRequest {
  subjectId?: number;
  subject: string;
  topicId?: number;
  topic: string;
  subtopicId?: number;
  subtopic: string;
  targetExam?: string;
  lessonType?: string;
  difficultyLevel?: string;
  referenceText?: string;
  promptNotes?: string;
}

export interface BatchGenerateResponse {
  batchId?: string;
  status: 'QUEUED' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED' | 'CANCELLED' | string;
  totalQueued: number;
  totalGenerated: number;
  totalFailed: number;
  percentComplete?: number;
  currentSubtopic?: string;
  generatedSubtopics?: string[];
  failedSubtopics?: string[];
  message?: string;
}

export function adaptToReviewItem(raw: any): MicroLessonReviewItem {
  if (!raw) return raw;

  const subject = raw.subject || raw.subjectName || 'General';
  const topic = raw.topic || raw.topicName || 'General';
  const subtopic = raw.subtopic || raw.subtopicName || 'General';

  const coreConcept = raw.coreConcept || raw.content?.coreConcept || '';

  // 1. Trap Alert normalization via shared parser
  const { trapAlert, structuredTrapAlert } = parseTrapAlert(
    raw.trap || raw.trapAlert || raw.content?.trapAlert,
    raw.stepByStepBreakdown?.structuredTrapAlert ||
      (raw.content?.trapAlert && typeof raw.content.trapAlert === 'object' ? raw.content.trapAlert : null)
  );

  // 2. Formula Cheat Sheet normalization via shared parser
  const { formulaEntries, formulaCheatSheet } = parseFormulaEntries(
    raw.formulaCheatSheet || raw.content?.formulaCheatSheet,
    raw.cheatSheet || raw.content?.cheatSheet
  );

  // 3. Step-by-Step Breakdown normalization via shared parser
  const rawBreakdown = raw.stepByStepBreakdown || raw.content?.stepByStepBreakdown;
  let breakdown: StepByStepBreakdown | undefined = undefined;
  if (rawBreakdown) {
    breakdown = parseStepBreakdown(
      rawBreakdown,
      raw.content?.problemStatement || raw.problemStatement,
      raw.content?.questionStem || raw.questionStem,
      raw.content?.finalAnswer || raw.finalAnswer
    );
  }

  const keyTakeaways: string[] | undefined =
    raw.keyTakeaways || raw.content?.keyTakeaways || undefined;
  const rawQuickChecks = raw.quickCheckItems || raw.content?.quickCheckItems;
  const quickCheckItems: QuickCheckItem[] | undefined = Array.isArray(rawQuickChecks)
    ? rawQuickChecks.map((qc: any) => ({
        question: qc.question || '',
        options: qc.options || [],
        answer: qc.answer || qc.correctAnswer || '',
        correctAnswer: qc.correctAnswer || qc.answer || '',
        explanation: qc.explanation || '',
      }))
    : undefined;

  const content: MicroLessonContent = {
    coreConcept,
    trapAlert,
    structuredTrapAlert,
    formulaCheatSheet,
    formulaEntries,
    stepByStepBreakdown: breakdown || { steps: [] },
    keyTakeaways,
    quickCheckItems,
  };

  return {
    ...raw,
    subject,
    topic,
    subtopic,
    subjectName: subject,
    topicName: topic,
    subtopicName: subtopic,
    targetExam: raw.targetExam || '',
    lessonType: raw.lessonType || 'CONCEPT',
    difficultyLevel: raw.difficultyLevel || 'CORE',
    sequenceNo: raw.sequenceNo ?? 1,
    estimatedMinutes: raw.estimatedMinutes ?? 5,
    coreConcept,
    trap: trapAlert,
    reviewerNotes: raw.reviewerNotes || raw.reviewComments || '',
    languageCode: raw.languageCode || 'en',
    fallbackToEnglish: !!raw.fallbackToEnglish,
    translations: raw.translations || [],
    content,
  };
}

export interface MicroLessonStats {
  total: number;
  aiGenerated: number;
  inReview: number;
  approved: number;
  published: number;
  rejected: number;
}

export interface MicroLessonFilterOptions {
  subjects: string[];
  topics: string[];
  statuses: string[];
}

export interface BulkDeleteResponse {
  deletedCount: number;
}

export interface MicroLessonTranslation {
  id: string;
  microlessonId: string;
  tenantId?: string;
  languageCode: string;
  coreConcept: string;
  trap: string;
  cheatSheet: string[];
  stepByStepBreakdown: any;
  content?: any;
  sourceVersion: number;
  status: 'PUBLISHED' | 'APPROVED' | 'IN_REVIEW' | 'DRAFT' | 'REJECTED';
  translatorId?: string;
  reviewerId?: string;
  reviewComments?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface MicroLessonBatchTranslationRequest {
  sourceLanguage?: string;
  targetLanguage: string;
  targetStatus?: string;
  subject?: string;
  topic?: string;
  targetExam?: string;
  lessonIds?: string[];
  overwriteExisting?: boolean;
  batchSize?: number;
  throttleDelayMs?: number;
  maxConcurrency?: number;
}

export interface MicroLessonBatchTranslationJob {
  id: string;
  jobId?: string;
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
  sourceLanguage: string;
  targetLanguage: string;
  targetStatus?: string;
  subjectFilter?: string;
  topicFilter?: string;
  targetExamFilter?: string;
  overwriteExisting?: boolean;
  totalLessons: number;
  processedLessons: number;
  successfulLessons: number;
  failedLessons: number;
  progressPercentage: number;
  failedLessonIds?: string[];
  batchSize?: number;
  throttleDelayMs?: number;
  maxConcurrency?: number;
  startedAt?: string;
  completedAt?: string;
  errorMessage?: string;
  createdAt?: string;
  updatedAt?: string;
}
