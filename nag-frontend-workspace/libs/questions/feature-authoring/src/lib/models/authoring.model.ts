import { QuestionOption, DifficultyLevel, QuestionType } from '@nag-frontend-workspace/questions-data-access';

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
