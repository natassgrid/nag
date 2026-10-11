export interface SelectOption<T = string> {
  value: T;
  label: string;
}

export const DEFAULT_AI_DIFFICULTIES: string[] = ['EASY', 'MEDIUM', 'HARD'];

export const AI_COGNITIVE_LEVELS: SelectOption[] = [
  { value: 'REMEMBER', label: 'Remember (Recall facts & basic concepts)' },
  { value: 'UNDERSTAND', label: 'Understand (Explain ideas or concepts)' },
  { value: 'APPLY', label: 'Apply (Use information in new situations)' },
  { value: 'ANALYZE', label: 'Analyze (Draw connections among ideas)' },
  { value: 'EVALUATE', label: 'Evaluate (Justify a stand or decision)' },
  { value: 'CREATE', label: 'Create (Produce new or original work)' },
];

export const AI_QUESTION_TYPES: SelectOption[] = [
  { value: 'SINGLE_MCQ', label: 'Single Choice MCQ' },
  { value: 'MULTI_MCQ', label: 'Multiple Correct (MSQ)' },
  { value: 'NUMERICAL', label: 'Numerical / Decimal Answer' },
  { value: 'DESCRIPTIVE', label: 'Descriptive / Long Answer' },
  { value: 'ASSERTION_REASON', label: 'Assertion & Reason' },
  { value: 'PARAGRAPH_SET', label: 'Paragraph / Reading Comprehension Set' },
];

export const AI_EXECUTION_MODES: SelectOption[] = [
  { value: 'AUTO', label: 'Auto (Triage-based routing)' },
  { value: 'FAST', label: 'Fast (Single lightweight model, <2s)' },
  { value: 'MULTI_AGENT', label: 'Multi-Agent (Deep review committee)' },
];

export const AI_GENERATION_QUALITY: SelectOption[] = [
  { value: 'STANDARD', label: 'Standard' },
  { value: 'EXAM_READY', label: 'Exam-Ready (Strict psychometric rubric)' },
];
