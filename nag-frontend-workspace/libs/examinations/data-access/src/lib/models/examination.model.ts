export type ExamStatus = 'DRAFT' | 'PUBLISHED' | 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export interface ExamSection {
  name: string;
  questionCount: number;
  marksPerQuestion: number;
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
  sections: ExamSection[];
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
  sections: ExamSection[];
}
