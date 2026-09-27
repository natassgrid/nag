export interface SelectOption<T = string> {
  value: T;
  label: string;
}

export const DEFAULT_AI_SUBJECTS: string[] = [
  'Mathematics',
  'Physics',
  'Chemistry',
  'Computer Science',
  'General Knowledge & Indian History',
  'Biology & Life Sciences',
  'Logical Reasoning & Aptitude',
];

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
];

export const DEFAULT_SUBJECT_TOPICS: Record<string, { topic: string; subtopic: string }> = {
  Mathematics: { topic: 'Linear Algebra & Matrices', subtopic: 'Eigenvalues and Eigenvectors' },
  Physics: { topic: 'Electromagnetism & Waves', subtopic: 'Gauss Law and Flux' },
  Chemistry: { topic: 'Organic Chemistry', subtopic: 'Electrophilic Aromatic Substitution' },
  'Computer Science': { topic: 'Algorithms & Data Structures', subtopic: 'Graph Traversal & Shortest Path' },
  'General Knowledge & Indian History': { topic: 'Indian Constitution', subtopic: 'Fundamental Rights & Directive Principles' },
  'Biology & Life Sciences': { topic: 'Cell Biology & Genetics', subtopic: 'Mendelian Inheritance' },
  'Logical Reasoning & Aptitude': { topic: 'Deductive Logic', subtopic: 'Syllogisms and Venn Diagrams' },
};
