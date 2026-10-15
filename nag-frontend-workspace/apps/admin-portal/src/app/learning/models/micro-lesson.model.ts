export interface MicroLessonStep {
  stepNumber: number;
  title: string;
  explanation: string;
  formula?: string;
}

export interface StepByStepBreakdown {
  problemStatement: string;
  steps: MicroLessonStep[];
  finalAnswer?: string;
}

export interface MicroLessonContent {
  coreConcept: string;
  trapAlert: string;
  formulaCheatSheet: string;
  stepByStepBreakdown?: StepByStepBreakdown;
}

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
  targetExam: string;
  status: 'AI_GENERATED' | 'IN_REVIEW' | 'APPROVED' | 'PUBLISHED' | 'REJECTED';
  contentVersion: number;
  reviewerNotes?: string | null;
  content: MicroLessonContent;
  coreConcept?: string;
  trap?: string;
  cheatSheet?: string[];
  createdAt?: string;
  updatedAt?: string;
}

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
  referenceText?: string;
  promptNotes?: string;
}

export interface BatchGenerateResponse {
  batchId: string;
  totalQueued: number;
  status: string;
  message: string;
}

export function adaptToReviewItem(raw: any): MicroLessonReviewItem {
  if (!raw) return raw;

  const subject = raw.subject || raw.subjectName || 'General';
  const topic = raw.topic || raw.topicName || 'General';
  const subtopic = raw.subtopic || raw.subtopicName || 'General';

  const coreConcept = raw.coreConcept || raw.content?.coreConcept || '';
  const trapAlert = raw.trap || raw.trapAlert || raw.content?.trapAlert || '';

  let formulaCheatSheet = '';
  if (Array.isArray(raw.cheatSheet)) {
    formulaCheatSheet = raw.cheatSheet.join('\n\n');
  } else if (typeof raw.formulaCheatSheet === 'string') {
    formulaCheatSheet = raw.formulaCheatSheet;
  } else if (raw.content?.formulaCheatSheet) {
    formulaCheatSheet = raw.content.formulaCheatSheet;
  }

  let breakdown: StepByStepBreakdown | undefined = undefined;
  const rawBreakdown = raw.stepByStepBreakdown || raw.content?.stepByStepBreakdown;
  if (rawBreakdown) {
    const problemStatement = rawBreakdown.questionStem || rawBreakdown.problemStatement || '';
    let steps: MicroLessonStep[] = [];
    if (Array.isArray(rawBreakdown.steps)) {
      steps = rawBreakdown.steps.map((st: any, idx: number) => {
        if (typeof st === 'string') {
          return {
            stepNumber: idx + 1,
            title: `Step ${idx + 1}`,
            explanation: st,
          };
        }
        return {
          stepNumber: st.stepNumber ?? (idx + 1),
          title: st.title || `Step ${idx + 1}`,
          explanation: st.explanation || '',
          formula: st.formula,
        };
      });
    }
    breakdown = {
      problemStatement,
      steps,
      finalAnswer: rawBreakdown.finalAnswer,
    };
  }

  const content: MicroLessonContent = {
    coreConcept,
    trapAlert,
    formulaCheatSheet,
    stepByStepBreakdown: breakdown,
  };

  return {
    ...raw,
    subject,
    topic,
    subtopic,
    subjectName: subject,
    topicName: topic,
    subtopicName: subtopic,
    coreConcept,
    trap: trapAlert,
    reviewerNotes: raw.reviewerNotes || raw.reviewComments || '',
    content,
  };
}
