export interface EnrolledExam {
  id: string;
  code: string;
  title: string;
  scheduledDate: string;
  scheduledTime: string;
  durationMinutes: number;
  centerName: string;
  centerAddress: string;
  rollNumber: string;
  status: 'LIVE' | 'UPCOMING' | 'COMPLETED';
  admitCardReady: boolean;
}
