export interface EnrolledExam {
  id: string;
  applicationId?: string;
  code: string;
  title: string;
  conductingAuthority?: string;
  scheduledDate: string;
  scheduledTime: string;
  durationMinutes: number;
  totalMarks?: number;
  centerName: string;
  centerAddress: string;
  centerCity?: string;
  centerState?: string;
  shiftName?: string;
  rollNumber: string;
  status: 'LIVE' | 'UPCOMING' | 'COMPLETED' | 'SCHEDULED';
  admitCardReady: boolean;
  daysRemaining?: number;
  deliverySessionId?: string;
}

export interface DigitalAdmitCard {
  applicationId: string;
  examId: string;
  examCode: string;
  examTitle: string;
  conductingAuthority: string;
  candidateName: string;
  rollNumber: string;
  candidateCategory: string;
  scheduledDate: string;
  reportingTime: string;
  gateClosingTime: string;
  examTime: string;
  durationMinutes: number;
  centerName: string;
  centerAddress: string;
  centerCode: string;
  qrVerificationToken: string;
  photoUrl?: string;
  signatureUrl?: string;
  instructions: string[];
}

export interface DashboardKpiMetrics {
  totalRegistered: number;
  liveCount: number;
  upcomingCount: number;
  scorecardsCount: number;
}
