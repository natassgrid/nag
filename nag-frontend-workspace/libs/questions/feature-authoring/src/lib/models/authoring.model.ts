import {
  QuestionOption,
  DifficultyLevel,
  QuestionType,
  Subject,
  Topic,
  Subtopic,
} from '@nag-frontend-workspace/questions-data-access';

export type AuthoringMode = 'STANDALONE' | 'PASSAGE';

export interface AuthoringSubQuestion {
  id?: string;
  passageOrderIndex: number;
  content: string;
  questionType: QuestionType;
  difficulty: DifficultyLevel;
  cognitiveLevel: string;
  marks: number;
  negativeMarks: number;
  explanation: string;
  options: QuestionOption[];
}

export interface CognitiveLevelOption {
  value: string;
  label: string;
}

export const COGNITIVE_LEVELS: CognitiveLevelOption[] = [
  { value: 'REMEMBER', label: 'Remember / Recall' },
  { value: 'UNDERSTAND', label: 'Understand / Conceptual' },
  { value: 'APPLY', label: 'Apply / Application' },
  { value: 'ANALYZE', label: 'Analyze / Critical Thinking' },
  { value: 'EVALUATE', label: 'Evaluate / Judgment' },
  { value: 'CREATE', label: 'Create / Synthesis' },
];

export interface QuestionWizardStep {
  id: string;
  stepNumber: number;
  title: string;
  subtitle: string;
  icon: string;
}

export const STANDALONE_WIZARD_STEPS: QuestionWizardStep[] = [
  {
    id: 'taxonomy',
    stepNumber: 1,
    title: 'Taxonomy & Scoring',
    subtitle: 'Subject, topic, Bloom level & marks',
    icon: 'tune',
  },
  {
    id: 'problem',
    stepNumber: 2,
    title: 'Problem Statement',
    subtitle: 'Question stem with LaTeX & symbols',
    icon: 'edit_note',
  },
  {
    id: 'options',
    stepNumber: 3,
    title: 'Options & Answer Key',
    subtitle: 'Choices and correct response key',
    icon: 'checklist',
  },
  {
    id: 'review',
    stepNumber: 4,
    title: 'Explanation & Preview',
    subtitle: 'Solution derivation & live preview',
    icon: 'visibility',
  },
];

export const PASSAGE_WIZARD_STEPS: QuestionWizardStep[] = [
  {
    id: 'passage-context',
    stepNumber: 1,
    title: 'Taxonomy & Passage',
    subtitle: 'Classification & reading comprehension text',
    icon: 'auto_stories',
  },
  {
    id: 'subquestions',
    stepNumber: 2,
    title: 'Linked Sub-Questions',
    subtitle: 'Compose 2 to 6 passage items',
    icon: 'quiz',
  },
  {
    id: 'passage-review',
    stepNumber: 3,
    title: 'Full Set Preview',
    subtitle: 'Review passage & items before saving',
    icon: 'visibility',
  },
];

export interface QuestionWizardState {
  mode: AuthoringMode;
  editingQuestionId: string | null;
  selectedSubjectId: number | null;
  selectedTopicId: number | null;
  selectedSubtopicId: number | null;
  cognitiveLevel: string;
  type: QuestionType;
  difficulty: DifficultyLevel;
  marks: number;
  negativeMarks: number;
  content: string;
  explanation: string;
  options: QuestionOption[];
  passageTitle: string;
  passageContent: string;
  subQuestions: AuthoringSubQuestion[];
  activeSubQuestionIndex: number;
}

export interface QuestionWizardDialogData extends QuestionWizardState {
  subjects: Subject[];
  topics: Topic[];
  subtopics: Subtopic[];
  initialStep?: number;
}

export interface QuestionWizardDialogResult {
  action: 'save' | 'switch-to-inpage' | 'close';
  state: QuestionWizardState;
  subjects: Subject[];
  topics: Topic[];
  subtopics: Subtopic[];
}

