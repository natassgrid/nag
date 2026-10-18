export type ExamStatus = 'DRAFT' | 'PUBLISHED' | 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export interface ExamSection {
  id?: string;
  subjectId?: number;
  subject?: string;
  name: string;
  orderIndex?: number;
  questionCount: number;
  marksPerQuestion: number;
  negativeMarks?: number;
  durationMinutes?: number;
}

export interface ExamSyllabusNode {
  id?: string;
  nodeType: 'SUBJECT' | 'TOPIC' | 'SUBTOPIC';
  nodeId: number;
  weightagePct?: number;
  name?: string;
  code?: string;
}

export interface ExaminationResponse {
  id: string;
  name: string;
  code?: string;
  conductingAuthority?: string;
  category?: string;
  examinationType?: string;
  academicYear?: string;
  examinationMode?: string;
  durationMinutes: number;
  totalMarks: number;
  negativeMarkingEnabled: boolean;
  negativeMarkingValue: number;
  navigationPolicy: string;
  calculatorPolicy: string;
  reviewFlagEnabled: boolean;
  isPractice?: boolean;
  blueprintId?: string;
  parentExaminationId?: string;
  sections: ExamSection[];
  syllabusNodes?: ExamSyllabusNode[];
  status: string;
  createdAt: string;
  updatedAt?: string;
}

export interface CreateExamRequest {
  name: string;
  code?: string;
  conductingAuthority?: string;
  category?: string;
  examinationType?: string;
  academicYear?: string;
  examinationMode?: string;
  durationMinutes: number;
  totalMarks: number;
  negativeMarkingEnabled: boolean;
  negativeMarkingValue: number;
  navigationPolicy: string;
  calculatorPolicy: string;
  reviewFlagEnabled: boolean;
  isPractice?: boolean;
  blueprintId?: string;
  parentExaminationId?: string;
  sections: ExamSection[];
  syllabusNodes?: ExamSyllabusNode[];
}

