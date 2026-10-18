export interface MicroLessonStep {
  stepNumber: number;
  title: string;
  explanation: string;
  formula?: string;
  tip?: string;
}

export interface StepByStepBreakdown {
  problemStatement: string;
  questionStem?: string;
  steps: MicroLessonStep[];
  finalAnswer?: string;
}

export interface TrapAlert {
  title: string;
  description: string;
  commonMistake?: string;
  correctApproach?: string;
}

export interface FormulaEntry {
  name: string;
  latex: string;
  description: string;
  variables?: string[];
}

export interface QuickCheckItem {
  question: string;
  options?: string[];
  answer: string;
  correctAnswer?: string;
  explanation?: string;
}

export interface MicroLessonContent {
  coreConcept: string;
  trapAlert: string;
  structuredTrapAlert?: TrapAlert;
  formulaCheatSheet: string;
  formulaEntries?: FormulaEntry[];
  stepByStepBreakdown?: StepByStepBreakdown;
  keyTakeaways?: string[];
  quickCheckItems?: QuickCheckItem[];
}

export type MicroLessonType =
  | 'CONCEPT'
  | 'WORKED_EXAMPLES'
  | 'TRAPS_AND_PITFALLS'
  | 'CHEAT_SHEET'
  | 'VISUAL'
  | 'QUICK_RECAP'
  | 'STORY_ANALOGY';

export type MicroLessonDifficulty = 'FOUNDATION' | 'CORE' | 'ADVANCED';

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

  let trapAlert = '';
  let structuredTrapAlert: TrapAlert | undefined = undefined;
  const rawTrap = raw.trap || raw.trapAlert || raw.content?.trapAlert;
  if (typeof rawTrap === 'string') {
    trapAlert = rawTrap;
  } else if (rawTrap && typeof rawTrap === 'object') {
    structuredTrapAlert = {
      title: rawTrap.title || 'Exam Trap & Common Pitfall',
      description: rawTrap.description || '',
      commonMistake: rawTrap.commonMistake,
      correctApproach: rawTrap.correctApproach,
    };
    trapAlert = rawTrap.description || rawTrap.commonMistake || rawTrap.title || '';
  }
  const structuredTrapSource =
    raw.stepByStepBreakdown?.structuredTrapAlert ||
    (raw.content?.trapAlert && typeof raw.content.trapAlert === 'object' ? raw.content.trapAlert : null);
  if (!structuredTrapAlert && structuredTrapSource) {
    structuredTrapAlert = {
      title: structuredTrapSource.title || 'Exam Trap & Common Pitfall',
      description: structuredTrapSource.description || trapAlert,
      commonMistake: structuredTrapSource.commonMistake,
      correctApproach: structuredTrapSource.correctApproach,
    };
  }

  let formulaCheatSheet = '';
  let formulaEntries: FormulaEntry[] | undefined = undefined;
  const rawFormulaArray = Array.isArray(raw.formulaCheatSheet)
    ? raw.formulaCheatSheet
    : Array.isArray(raw.content?.formulaCheatSheet)
      ? raw.content.formulaCheatSheet
      : null;

  if (rawFormulaArray && rawFormulaArray.length > 0) {
    const parsedFormulas: FormulaEntry[] = rawFormulaArray.map((f: any, idx: number) => ({
      name: f.name || f.title || `Formula ${idx + 1}`,
      latex: f.latex || f.formula || '',
      description: f.description || f.usage || '',
      variables: Array.isArray(f.variables) ? f.variables : [],
    }));
    formulaEntries = parsedFormulas;
    formulaCheatSheet =
      Array.isArray(raw.cheatSheet) && raw.cheatSheet.length > 0
        ? raw.cheatSheet.join('\n\n')
        : parsedFormulas
            .map((f: FormulaEntry) => (f.name ? `${f.name}: ${f.latex}` : f.latex))
            .join('\n\n');
  } else if (Array.isArray(raw.cheatSheet)) {
    formulaCheatSheet = raw.cheatSheet.join('\n\n');
    formulaEntries = raw.cheatSheet
      .filter((item: any) => typeof item === 'string' && item.trim().length > 0)
      .map((item: string, idx: number) => {
        const trimmed = item.trim();
        const colonIdx = trimmed.indexOf(':');
        const firstDollarIdx = trimmed.indexOf('$');
        const firstSlashIdx = trimmed.indexOf('\\');
        const hasColon = colonIdx > 0;
        const colonBeforeDollar = firstDollarIdx === -1 || colonIdx < firstDollarIdx;
        const colonBeforeSlash = firstSlashIdx === -1 || colonIdx < firstSlashIdx;

        if (hasColon && colonBeforeDollar && colonBeforeSlash) {
          const namePart = trimmed.substring(0, colonIdx).trim();
          const latexPart = trimmed.substring(colonIdx + 1).trim();
          if (namePart.length > 0 && !namePart.includes('$') && !namePart.includes('\\')) {
            return {
              name: namePart,
              latex: latexPart,
              description: namePart,
            };
          }
        }
        return {
          name: `Formula ${idx + 1}`,
          latex: trimmed,
          description: '',
        };
      });
  } else if (typeof raw.formulaCheatSheet === 'string') {
    formulaCheatSheet = raw.formulaCheatSheet;
  } else if (typeof raw.content?.formulaCheatSheet === 'string') {
    formulaCheatSheet = raw.content.formulaCheatSheet;
  }

  let breakdown: StepByStepBreakdown | undefined = undefined;
  const rawBreakdown = raw.stepByStepBreakdown || raw.content?.stepByStepBreakdown;
  if (rawBreakdown) {
    if (Array.isArray(rawBreakdown)) {
      const steps: MicroLessonStep[] = rawBreakdown.map((st: any, idx: number) => ({
        stepNumber: st.stepNumber ?? idx + 1,
        title: st.title || `Step ${idx + 1}`,
        explanation: st.explanation || (typeof st === 'string' ? st : ''),
        formula: st.formula,
        tip: st.tip,
      }));
      breakdown = {
        problemStatement: raw.content?.problemStatement || '',
        steps,
        finalAnswer: raw.content?.finalAnswer,
      };
    } else {
      const problemStatement =
        rawBreakdown.questionStem ||
        rawBreakdown.problemStatement ||
        raw.content?.problemStatement ||
        '';
      const stepSource =
        Array.isArray(rawBreakdown.structuredSteps) && rawBreakdown.structuredSteps.length > 0
          ? rawBreakdown.structuredSteps
          : Array.isArray(raw.content?.stepByStepBreakdown) && raw.content.stepByStepBreakdown.length > 0
            ? raw.content.stepByStepBreakdown
            : Array.isArray(rawBreakdown.steps)
              ? rawBreakdown.steps
              : [];
      const steps: MicroLessonStep[] = stepSource.map((st: any, idx: number) => {
        if (typeof st === 'string') {
          return {
            stepNumber: idx + 1,
            title: `Step ${idx + 1}`,
            explanation: st,
          };
        }
        return {
          stepNumber: st.stepNumber ?? idx + 1,
          title: st.title || `Step ${idx + 1}`,
          explanation: st.explanation || '',
          formula: st.formula,
          tip: st.tip,
        };
      });
      breakdown = {
        problemStatement,
        steps,
        finalAnswer: rawBreakdown.finalAnswer || raw.content?.finalAnswer,
      };
    }
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
    stepByStepBreakdown: breakdown,
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

export interface MicroLessonLanguage {
  code: string;
  name: string;
  nativeName: string;
  script: string;
}

export const SUPPORTED_MICROLESSON_LANGUAGES: MicroLessonLanguage[] = [
  { code: 'hi', name: 'Hindi', nativeName: 'हिन्दी', script: 'Devanagari' },
  { code: 'bn', name: 'Bengali', nativeName: 'বাংলা', script: 'Bengali' },
  { code: 'te', name: 'Telugu', nativeName: 'తెలుగు', script: 'Telugu' },
  { code: 'mr', name: 'Marathi', nativeName: 'मराठी', script: 'Devanagari' },
  { code: 'ta', name: 'Tamil', nativeName: 'தமிழ்', script: 'Tamil' },
  { code: 'gu', name: 'Gujarati', nativeName: 'ગુજરાતી', script: 'Gujarati' },
  { code: 'kn', name: 'Kannada', nativeName: 'ಕನ್ನಡ', script: 'Kannada' },
  { code: 'ml', name: 'Malayalam', nativeName: 'മലയാളം', script: 'Malayalam' },
  { code: 'or', name: 'Odia', nativeName: 'ଓଡ଼ିଆ', script: 'Odia' },
  { code: 'pa', name: 'Punjabi', nativeName: 'ਪੰਜਾਬੀ', script: 'Gurmukhi' },
  { code: 'as', name: 'Assamese', nativeName: 'অসমীয়া', script: 'Bengali' },
  { code: 'ur', name: 'Urdu', nativeName: 'اردو', script: 'Perso-Arabic' },
  { code: 'sa', name: 'Sanskrit', nativeName: 'संस्कृतम्', script: 'Devanagari' },
  { code: 'en', name: 'English', nativeName: 'English', script: 'Latin' },
];

