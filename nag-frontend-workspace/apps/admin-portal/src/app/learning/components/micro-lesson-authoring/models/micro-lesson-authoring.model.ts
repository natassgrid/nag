import {
  FormulaEntry,
  MicroLessonStep,
  QuickCheckItem,
  TrapAlert,
} from '../../../models/micro-lesson.model';
import {
  Subject,
  Subtopic,
  Topic,
} from '@nag-frontend-workspace/questions-data-access';
import { TargetExamOption } from './micro-lesson-authoring-defaults';

export * from './formula-applicability.util';
export * from './micro-lesson-authoring-defaults';
export * from './micro-lesson-authoring-converter.util';

export interface MicroLessonWizardState {
  editingLessonId: string | null;
  selectedSubjectId: number | null;
  selectedTopicId: number | null;
  selectedSubtopicId: number | null;
  customSubtopicName: string;
  targetExam: string;
  lessonType: string;
  difficultyLevel: string;
  sequenceNo: number;
  estimatedMinutes: number;
  status: string;
  coreConcept: string;
  trapAlert: TrapAlert;
  formulaEntries: FormulaEntry[];
  problemStatement: string;
  steps: MicroLessonStep[];
  finalAnswer: string;
  keyTakeaways: string[];
  quickCheckItems: QuickCheckItem[];
  reviewerNotes: string;
}

export interface MicroLessonWizardDialogData extends MicroLessonWizardState {
  subjects: Subject[];
  topics: Topic[];
  subtopics: Subtopic[];
  examOptions: TargetExamOption[];
  initialStep?: number;
}

export interface MicroLessonWizardDialogResult {
  action: 'save' | 'switch-to-inpage' | 'close';
  state: MicroLessonWizardState;
  subjects: Subject[];
  topics: Topic[];
  subtopics: Subtopic[];
}
