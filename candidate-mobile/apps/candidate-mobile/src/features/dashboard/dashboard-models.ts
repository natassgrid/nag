/**
 * Candidate Mobile Dashboard Models
 * Ported from nag-frontend-workspace candidate-delivery dashboard module.
 */

export type DashboardExamStatus = 'LIVE' | 'UPCOMING' | 'COMPLETED' | 'SCHEDULED';
export type DashboardExamFilter = 'ALL' | 'LIVE' | 'UPCOMING' | 'PRACTICE' | 'COMPLETED';

export interface EnrolledExam {
  id: string;
  applicationId: string;
  code: string;
  title: string;
  conductingAuthority: string;
  scheduledDate: string; // YYYY-MM-DD
  scheduledTime: string; // e.g. '09:00 AM - 12:00 PM'
  durationMinutes: number;
  totalMarks: number;
  centerName: string;
  centerAddress: string;
  centerCity: string;
  centerState: string;
  shiftName: string;
  rollNumber: string;
  status: DashboardExamStatus;
  admitCardReady: boolean;
  daysRemaining: number;
  deliverySessionId?: string;
  isPractice: boolean;
  practiceAvailable: boolean;
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
  practiceCount: number;
}

export interface PracticePaperSummary {
  paperId: string;
  name: string;
  examId: string;
  examName?: string;
  shiftId?: string;
  status: string;
  isPractice: boolean;
  difficultyScore: number;
  totalQuestions: number;
  createdAt?: string;
}

export interface UpcomingAlert {
  id: string;
  examId: string;
  title: string;
  message: string;
  severity: 'CRITICAL' | 'WARNING' | 'INFO';
  actionLabel?: string;
  actionType?: 'LAUNCH_LIVE' | 'DOWNLOAD_ADMIT_CARD' | 'PRACTICE';
  date: string;
}

export interface DashboardState {
  enrolledExams: EnrolledExam[];
  kpis: DashboardKpiMetrics;
  activeFilter: DashboardExamFilter;
  alerts: UpcomingAlert[];
  selectedAdmitCard: DigitalAdmitCard | null;
  selectedPracticeExam: { exam: EnrolledExam; papers: PracticePaperSummary[] } | null;
  loading: boolean;
  error: string | null;
  candidateName: string;
}
