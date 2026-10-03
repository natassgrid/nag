export interface ExamSection {
  name: string;
  durationMinutes: number;
  totalQuestions: number;
  totalMarks: number;
  cutoffMarks?: number;
}

export interface CatalogExam {
  id: string;
  code: string;
  title: string;
  conductingAuthority: string;
  category: 'ENGINEERING' | 'CIVIL_SERVICES' | 'BANKING' | 'DEFENSE' | 'MEDICAL' | 'GENERAL' | string;
  examinationType?: string;
  examinationMode?: string;
  durationMinutes: number;
  totalMarks: number;
  negativeMarkingEnabled: boolean;
  negativeMarkingValue: number;
  navigationPolicy?: string;
  calculatorPolicy?: string;
  reviewFlagEnabled?: boolean;
  applicationDeadline: string;
  examDate: string;
  feeAmount: number;
  eligibility: string;
  totalSeats: number;
  vacanciesCount?: number;
  sections?: ExamSection[];
  status: 'OPEN' | 'CLOSING_SOON' | 'CLOSED' | 'PUBLISHED';
  applied: boolean;
  isPractice?: boolean;
  applicationId?: string;
  hallTicketNumber?: string;
}

export interface PublicCentre {
  id: string;
  centreName: string;
  region: string;
  state: string;
  district: string;
  city: string;
  building?: string;
  totalCapacity?: number;
}

export interface ApplyExamPayload {
  firstChoiceCentreId: string;
  secondChoiceCentreId?: string;
  thirdChoiceCentreId?: string;
  pwdRequired?: boolean;
  scribeRequired?: boolean;
  preferredShiftId?: string;
}

export interface ApplicationReceipt {
  applicationId: string;
  applicationNumber: string;
  examId: string;
  examTitle: string;
  examCode: string;
  candidateName: string;
  candidateEmail: string;
  category: string;
  appliedAt: string;
  feePaid: number;
  firstChoiceCentreName: string;
  secondChoiceCentreName?: string;
  thirdChoiceCentreName?: string;
  pwdAssistance: boolean;
  status: string;
}

export interface CatalogFilterState {
  searchQuery: string;
  category: string;
  statusFilter: string;
  sortBy: 'DATE_ASC' | 'FEE_ASC' | 'FEE_DESC' | 'TITLE_ASC';
}
