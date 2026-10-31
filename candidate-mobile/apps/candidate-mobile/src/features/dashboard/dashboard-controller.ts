/**
 * Candidate Mobile Dashboard Controller
 * Manages KPI summary counters, enrolled examinations filtering,
 * digital admit cards, and practice mock launches.
 */

import {
  EnrolledExam,
  DigitalAdmitCard,
  DashboardKpiMetrics,
  PracticePaperSummary,
  UpcomingAlert,
  DashboardExamFilter,
  DashboardState,
} from './dashboard-models';
import { DOCKER_API_PATHS } from '@nag/candidate-core';
import { defaultDockerApiService } from '../../services/docker-api.service';

export {
  EnrolledExam,
  DigitalAdmitCard,
  DashboardKpiMetrics,
  PracticePaperSummary,
  UpcomingAlert,
  DashboardExamFilter,
  DashboardState,
};

export const DEFAULT_ENROLLED_EXAMS: EnrolledExam[] = [
  {
    id: 'exam-1',
    applicationId: 'APP-NES-2026-849202',
    code: 'NES-2026-S1',
    title: 'National Eligibility Screening (Computer Science & AI)',
    conductingAuthority: 'National Testing Agency',
    scheduledDate: '2026-09-28',
    scheduledTime: '09:00 AM - 12:00 PM',
    durationMinutes: 180,
    totalMarks: 300,
    centerName: 'Zone 4 - Center 102 (IIT Delhi Campus)',
    centerAddress: 'Academic Complex Block IV, Hauz Khas, New Delhi - 110016',
    centerCity: 'New Delhi',
    centerState: 'Delhi NCR',
    shiftName: 'Morning Shift (Shift 1)',
    rollNumber: '849202',
    status: 'LIVE',
    admitCardReady: true,
    daysRemaining: 0,
    deliverySessionId: 'sess-nes-2026-live-01',
    isPractice: false,
    practiceAvailable: true,
  },
  {
    id: 'mock-nes-101',
    applicationId: 'MOCK-APP-NES-849202',
    code: 'MOCK-NES-2026-P1',
    title: 'NES 2026 Official Practice Mock Assessment (CS & AI)',
    conductingAuthority: 'NextGen Assessment Grid / Academic Council',
    scheduledDate: '2026-10-02',
    scheduledTime: 'Anytime On-Demand Practice',
    durationMinutes: 90,
    totalMarks: 150,
    centerName: 'Candidate Self-Proctored Browser Runtime',
    centerAddress: 'Online Practice Simulation Environment',
    centerCity: 'National Grid Web',
    centerState: 'Online',
    shiftName: 'Simulated Practice Session',
    rollNumber: 'MOCK-849202',
    status: 'UPCOMING',
    admitCardReady: false,
    daysRemaining: 0,
    deliverySessionId: 'sess-mock-nes-practice-01',
    isPractice: true,
    practiceAvailable: true,
  },
  {
    id: 'exam-2',
    applicationId: 'APP-GATE-2026-310948',
    code: 'GATE-DPI-2026',
    title: 'Graduate Assessment for Open DPI Engineering',
    conductingAuthority: 'Digital India Corporation / MeitY',
    scheduledDate: '2026-10-15',
    scheduledTime: '02:00 PM - 05:00 PM',
    durationMinutes: 180,
    totalMarks: 100,
    centerName: 'Zone 1 - Center 044 (Bangalore Tech Hub)',
    centerAddress: 'Phase 1, Cyber Park Sector 3, Electronic City, Bangalore - 560100',
    centerCity: 'Bangalore',
    centerState: 'Karnataka',
    shiftName: 'Afternoon Shift (Shift 2)',
    rollNumber: '310948',
    status: 'UPCOMING',
    admitCardReady: true,
    daysRemaining: 18,
    deliverySessionId: 'sess-gate-dpi-upcoming-02',
    isPractice: false,
    practiceAvailable: true,
  },
  {
    id: 'exam-3',
    applicationId: 'APP-RBI-2026-559104',
    code: 'RBI-GRADE-B-2026',
    title: 'Reserve Bank Officer Recruitment Phase I (DR-General)',
    conductingAuthority: 'Reserve Bank of India Services Board',
    scheduledDate: '2026-11-10',
    scheduledTime: '09:30 AM - 11:30 AM',
    durationMinutes: 120,
    totalMarks: 200,
    centerName: 'National Examination Center — VJTI Matunga',
    centerAddress: 'Computing Annex, H R Mahajani Marg, Mumbai - 400019',
    centerCity: 'Mumbai',
    centerState: 'Maharashtra',
    shiftName: 'Morning Shift (Shift 1)',
    rollNumber: '559104',
    status: 'UPCOMING',
    admitCardReady: false,
    daysRemaining: 44,
    isPractice: false,
    practiceAvailable: true,
  },
  {
    id: 'exam-prev-1',
    applicationId: 'APP-CSE-2025-104928',
    code: 'CSE-PRE-2025',
    title: 'Civil Services Preliminary Screening (GS & CSAT)',
    conductingAuthority: 'Union Public Service Commission (UPSC)',
    scheduledDate: '2025-11-20',
    scheduledTime: '09:30 AM - 04:30 PM',
    durationMinutes: 240,
    totalMarks: 400,
    centerName: 'Central Assessment Hub — IET Lucknow',
    centerAddress: 'Sitapur Road Campus, Lucknow - 226021',
    centerCity: 'Lucknow',
    centerState: 'Uttar Pradesh',
    shiftName: 'Full Day Dual Shift',
    rollNumber: '104928',
    status: 'COMPLETED',
    admitCardReady: true,
    daysRemaining: 0,
    isPractice: false,
    practiceAvailable: false,
  },
];

export const DEFAULT_PRACTICE_PAPERS_MAP: Record<string, PracticePaperSummary[]> = {
  'exam-1': [
    {
      paperId: 'pp-nes-cs-01',
      name: 'NES 2026 Official Mock Paper A (Full Syllabus)',
      examId: 'exam-1',
      examName: 'National Eligibility Screening (Computer Science & AI)',
      status: 'APPROVED',
      isPractice: true,
      difficultyScore: 7.2,
      totalQuestions: 75,
      createdAt: '2026-08-10',
    },
    {
      paperId: 'pp-nes-cs-02',
      name: 'NES 2026 Sectional Sprint — Machine Intelligence & Logic',
      examId: 'exam-1',
      examName: 'National Eligibility Screening (Computer Science & AI)',
      status: 'APPROVED',
      isPractice: true,
      difficultyScore: 8.5,
      totalQuestions: 40,
      createdAt: '2026-08-22',
    },
  ],
  'exam-2': [
    {
      paperId: 'pp-gate-dpi-01',
      name: 'GATE-DPI Mock Test 1 (Distributed Systems & Cryptography)',
      examId: 'exam-2',
      examName: 'Graduate Assessment for Open DPI Engineering',
      status: 'APPROVED',
      isPractice: true,
      difficultyScore: 6.8,
      totalQuestions: 65,
      createdAt: '2026-09-01',
    },
  ],
  'mock-nes-101': [
    {
      paperId: 'pp-mock-101',
      name: 'NES Practice Simulation Starter Set',
      examId: 'mock-nes-101',
      examName: 'NES 2026 Official Practice Mock Assessment',
      status: 'APPROVED',
      isPractice: true,
      difficultyScore: 5.0,
      totalQuestions: 25,
      createdAt: '2026-09-15',
    },
  ],
};

export class DashboardController {
  private state: DashboardState;
  private readonly listeners = new Set<(state: DashboardState) => void>();
  private readonly practicePaperStore: Record<string, PracticePaperSummary[]>;

  constructor(
    initialExams: EnrolledExam[] = DEFAULT_ENROLLED_EXAMS,
    candidateName = 'Aryan Sharma',
    customPracticePapers = DEFAULT_PRACTICE_PAPERS_MAP
  ) {
    this.practicePaperStore = { ...customPracticePapers };
    const kpis = this.calculateKpis(initialExams);
    const alerts = this.generateAlerts(initialExams);

    this.state = {
      enrolledExams: [...initialExams],
      kpis,
      activeFilter: 'ALL',
      alerts,
      selectedAdmitCard: null,
      selectedPracticeExam: null,
      loading: false,
      error: null,
      candidateName,
    };

    if (typeof window !== 'undefined' && typeof (window as any).NagReactMount === 'function') {
      void this.syncFromDocker();
    }
  }

  public async syncFromDocker(): Promise<void> {
    try {
      const res = await defaultDockerApiService.requestDocker<any>(
        DOCKER_API_PATHS.EXAMINATIONS.MY_EXAMS,
        { method: 'GET' }
      );
      const items = Array.isArray(res)
        ? res
        : Array.isArray(res?.content)
        ? res.content
        : Array.isArray(res?.data)
        ? res.data
        : [];

      if (items.length > 0) {
        const mapped: EnrolledExam[] = items.map((item: any, idx: number) => ({
          id: item.id || item.examId || `exam-${idx}`,
          applicationId: item.applicationId || item.applicationNumber || `APP-${item.id || idx}`,
          code: item.code || item.examCode || `EXAM-${idx}`,
          title: item.title || item.examTitle || item.name || 'Examination',
          conductingAuthority: item.conductingAuthority || item.conductingBody || 'National Assessment Grid',
          scheduledDate: item.scheduledDate || item.examDate || new Date().toISOString().split('T')[0],
          scheduledTime: item.scheduledTime || '09:00 AM - 12:00 PM',
          durationMinutes: typeof item.durationMinutes === 'number' ? item.durationMinutes : 180,
          totalMarks: typeof item.totalMarks === 'number' ? item.totalMarks : 100,
          centerName: item.centerName || 'Allocated Examination Center',
          centerAddress: item.centerAddress || 'Center Address',
          centerCity: item.centerCity || 'City',
          centerState: item.centerState || 'State',
          shiftName: item.shiftName || 'Morning Shift',
          rollNumber: item.rollNumber || String(100000 + idx),
          status: item.status || 'UPCOMING',
          admitCardReady: item.admitCardReady ?? true,
          daysRemaining: item.daysRemaining ?? 0,
          deliverySessionId: item.deliverySessionId,
          isPractice: Boolean(item.isPractice),
          practiceAvailable: item.practiceAvailable ?? true,
        }));
        this.state.enrolledExams = mapped;
        this.state.kpis = this.calculateKpis(mapped);
        this.state.alerts = this.generateAlerts(mapped);
        this.notify();
      }
    } catch {
      // Non-blocking fallback to local seed enrolled exams
    }
  }

  public getState(): DashboardState {
    return { ...this.state };
  }

  public subscribe(listener: (state: DashboardState) => void): () => void {
    this.listeners.add(listener);
    listener(this.getState());
    return () => {
      this.listeners.delete(listener);
    };
  }

  private notify(): void {
    const currentState = this.getState();
    for (const listener of this.listeners) {
      listener(currentState);
    }
  }

  public calculateKpis(exams: EnrolledExam[]): DashboardKpiMetrics {
    const safeExams = exams || [];
    const totalRegistered = safeExams.length;
    const liveCount = safeExams.filter((e) => e.status === 'LIVE').length;
    const upcomingCount = safeExams.filter(
      (e) => !e.isPractice && (e.status === 'UPCOMING' || e.status === 'SCHEDULED')
    ).length;
    const completedCount = safeExams.filter((e) => e.status === 'COMPLETED').length;
    const practiceCount = safeExams.filter((e) => e.isPractice || e.practiceAvailable).length;

    return {
      totalRegistered,
      liveCount,
      upcomingCount,
      scorecardsCount: completedCount > 0 ? completedCount : 1,
      practiceCount,
    };
  }

  private generateAlerts(exams: EnrolledExam[]): UpcomingAlert[] {
    const alerts: UpcomingAlert[] = [];
    const safeExams = exams || [];

    for (const exam of safeExams) {
      if (exam.status === 'LIVE') {
        alerts.push({
          id: `alert-live-${exam.id}`,
          examId: exam.id,
          title: 'Live Assessment Session Active',
          message: `${exam.title} is currently active. Please report to the testing terminal immediately.`,
          severity: 'CRITICAL',
          actionLabel: 'Launch Assessment',
          actionType: 'LAUNCH_LIVE',
          date: exam.scheduledDate,
        });
      } else if (exam.admitCardReady && exam.status === 'UPCOMING') {
        alerts.push({
          id: `alert-admit-${exam.id}`,
          examId: exam.id,
          title: 'Official Hall Ticket / Admit Card Released',
          message: `Admit card for ${exam.code} is available for offline download & QR verification.`,
          severity: 'WARNING',
          actionLabel: 'Download Admit Card',
          actionType: 'DOWNLOAD_ADMIT_CARD',
          date: exam.scheduledDate,
        });
      }
    }

    return alerts;
  }

  public setFilter(filter: DashboardExamFilter): void {
    this.state.activeFilter = filter;
    this.notify();
  }

  public getFilteredExams(): EnrolledExam[] {
    const { enrolledExams, activeFilter } = this.state;
    const list = enrolledExams || [];

    switch (activeFilter) {
      case 'LIVE':
        return list.filter((e) => e.status === 'LIVE');
      case 'UPCOMING':
        return list.filter(
          (e) => !e.isPractice && (e.status === 'UPCOMING' || e.status === 'SCHEDULED')
        );
      case 'PRACTICE':
        return list.filter((e) => e.isPractice || e.practiceAvailable);
      case 'COMPLETED':
        return list.filter((e) => e.status === 'COMPLETED');
      case 'ALL':
      default:
        return list;
    }
  }

  public openAdmitCard(examId: string): DigitalAdmitCard | null {
    const exam = this.state.enrolledExams.find((e) => e.id === examId);
    if (!exam) return null;

    const qrPayload = JSON.stringify({
      nagId: exam.id,
      roll: exam.rollNumber,
      app: exam.applicationId,
      sig: `SHA256:${exam.rollNumber.split('').reverse().join('')}NAG2026`,
      issuedAt: Date.now(),
    });

    const card: DigitalAdmitCard = {
      applicationId: exam.applicationId || `APP-${exam.id}`,
      examId: exam.id,
      examCode: exam.code,
      examTitle: exam.title,
      conductingAuthority: exam.conductingAuthority || 'National Testing Agency',
      candidateName: this.state.candidateName,
      rollNumber: exam.rollNumber,
      candidateCategory: 'General (GEN-UR)',
      scheduledDate: exam.scheduledDate,
      reportingTime: '07:30 AM',
      gateClosingTime: '08:30 AM',
      examTime: exam.scheduledTime,
      durationMinutes: exam.durationMinutes,
      centerName: exam.centerName,
      centerAddress: exam.centerAddress,
      centerCode: `${exam.centerCity.toUpperCase().substring(0, 3)}-CTR-102`,
      qrVerificationToken: qrPayload,
      instructions: [
        'Candidates must present this Printed Digital Admit Card with an authorized Government Photo ID (Aadhaar / Passport / Voter ID).',
        'Entry gates strictly close 30 minutes before the scheduled exam start time. Late arrivals will not be permitted.',
        'No electronic gadgets, smart watches, calculators, or bags are permitted inside the examination hall.',
        'Biometric facial verification and OTP attendance logging will be executed at the allocated terminal desk.',
      ],
    };

    this.state.selectedAdmitCard = card;
    this.notify();
    return card;
  }

  public closeAdmitCard(): void {
    this.state.selectedAdmitCard = null;
    this.notify();
  }

  public getPracticePapers(examId: string): PracticePaperSummary[] {
    return this.practicePaperStore[examId] || [];
  }

  public selectPracticeExam(examId: string): void {
    const exam = this.state.enrolledExams.find((e) => e.id === examId);
    if (!exam) return;

    const papers = this.getPracticePapers(examId);
    this.state.selectedPracticeExam = { exam, papers };
    this.notify();
  }

  public closePracticeDialog(): void {
    this.state.selectedPracticeExam = null;
    this.notify();
  }

  public startLiveExam(examId: string): { exam: EnrolledExam; sessionId: string } | null {
    const exam = this.state.enrolledExams.find((e) => e.id === examId);
    if (!exam || exam.status !== 'LIVE') return null;

    const sessionId = exam.deliverySessionId || `sess-live-${exam.id}`;
    return { exam, sessionId };
  }

  public startPracticePaper(
    examId: string,
    paperId?: string
  ): { examId: string; paperId: string } | null {
    const exam = this.state.enrolledExams.find((e) => e.id === examId);
    if (!exam) return null;

    const papers = this.getPracticePapers(examId);
    const targetPaperId = paperId || (papers.length > 0 ? papers[0].paperId : `default-mock-${examId}`);
    this.state.selectedPracticeExam = null;
    this.notify();

    return { examId, paperId: targetPaperId };
  }

  public viewScorecard(examId: string): string | null {
    const exam = this.state.enrolledExams.find((e) => e.id === examId);
    if (!exam || exam.status !== 'COMPLETED') return null;
    return `result-${exam.id}`;
  }

  public addEnrolledExam(exam: EnrolledExam): void {
    this.state.enrolledExams = [exam, ...this.state.enrolledExams];
    this.state.kpis = this.calculateKpis(this.state.enrolledExams);
    this.state.alerts = this.generateAlerts(this.state.enrolledExams);
    this.notify();
  }

  public refreshExams(): void {
    this.state.kpis = this.calculateKpis(this.state.enrolledExams);
    this.state.alerts = this.generateAlerts(this.state.enrolledExams);
    this.notify();
  }
}
