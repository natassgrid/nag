import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, forkJoin, map, of, tap } from 'rxjs';
import {
  CatalogExam,
  PublicCentre,
  ApplyExamPayload,
  ApplicationReceipt,
} from '../models';

export const DEFAULT_MOCK_EXAMS: CatalogExam[] = [
  {
    id: 'exam-1',
    code: 'NES-2026-S1',
    title: 'National Eligibility Screening (Computer Science & AI)',
    conductingAuthority: 'National Testing Agency / Ministry of Education',
    category: 'ENGINEERING',
    examinationType: 'COMPUTER_BASED_TEST',
    examinationMode: 'CENTRE_PROCTORED',
    durationMinutes: 180,
    totalMarks: 300,
    negativeMarkingEnabled: true,
    negativeMarkingValue: 0.25,
    navigationPolicy: 'FREE_FORWARD_BACKWARD',
    calculatorPolicy: 'SCIENTIFIC_VIRTUAL',
    reviewFlagEnabled: true,
    applicationDeadline: '2026-10-15',
    examDate: '2026-10-28',
    feeAmount: 650,
    eligibility: 'B.Tech / MCA / M.Sc in CS, IT, AI or equivalent with min 60% aggregate',
    totalSeats: 250000,
    vacanciesCount: 12500,
    sections: [
      { name: 'Core Computer Science & Algorithms', durationMinutes: 60, totalQuestions: 40, totalMarks: 120 },
      { name: 'Machine Learning & Applied AI', durationMinutes: 60, totalQuestions: 35, totalMarks: 100 },
      { name: 'Quantitative Aptitude & Logic', durationMinutes: 60, totalQuestions: 30, totalMarks: 80 },
    ],
    status: 'OPEN',
    applied: true,
    isPractice: false,
  },
  {
    id: 'mock-nes-101',
    code: 'MOCK-NES-2026-P1',
    title: 'NES 2026 Official Practice Mock Assessment (CS & AI)',
    conductingAuthority: 'National Assessment Grid / Academic Council',
    category: 'ENGINEERING',
    examinationType: 'COMPUTER_BASED_TEST',
    examinationMode: 'OPEN_PRACTICE',
    durationMinutes: 90,
    totalMarks: 150,
    negativeMarkingEnabled: true,
    negativeMarkingValue: 0.25,
    navigationPolicy: 'FREE_FORWARD_BACKWARD',
    calculatorPolicy: 'SCIENTIFIC_VIRTUAL',
    reviewFlagEnabled: true,
    applicationDeadline: '2026-12-31',
    examDate: '2026-10-02',
    feeAmount: 0,
    eligibility: 'Free Open Mock Simulation for all registered & prospective candidates',
    totalSeats: 1000000,
    vacanciesCount: 0,
    sections: [
      { name: 'Practice Algorithms & Problem Solving', durationMinutes: 45, totalQuestions: 20, totalMarks: 75 },
      { name: 'Practice Mathematics & Applied Logic', durationMinutes: 45, totalQuestions: 20, totalMarks: 75 },
    ],
    status: 'OPEN',
    applied: true,
    isPractice: true,
  },
  {
    id: 'exam-2',
    code: 'GATE-DPI-2026',
    title: 'Graduate Assessment for Open DPI Engineering',
    conductingAuthority: 'Digital India Corporation / MeitY',
    category: 'ENGINEERING',
    examinationType: 'COMPUTER_BASED_TEST',
    examinationMode: 'CENTRE_PROCTORED',
    durationMinutes: 180,
    totalMarks: 100,
    negativeMarkingEnabled: true,
    negativeMarkingValue: 0.33,
    navigationPolicy: 'FREE_FORWARD_BACKWARD',
    calculatorPolicy: 'STANDARD_VIRTUAL',
    reviewFlagEnabled: true,
    applicationDeadline: '2026-10-20',
    examDate: '2026-11-15',
    feeAmount: 850,
    eligibility: 'Bachelor degree in Engineering / Technology / Architecture or final year students',
    totalSeats: 150000,
    vacanciesCount: 8400,
    sections: [
      { name: 'General Aptitude & Reasoning', durationMinutes: 45, totalQuestions: 15, totalMarks: 25 },
      { name: 'Engineering Mathematics', durationMinutes: 45, totalQuestions: 15, totalMarks: 25 },
      { name: 'DPI Protocol Architecture & Systems', durationMinutes: 90, totalQuestions: 35, totalMarks: 50 },
    ],
    status: 'OPEN',
    applied: false,
    isPractice: false,
  },
  {
    id: 'mock-gate-102',
    code: 'MOCK-DPI-P1',
    title: 'Open Protocols Practice Simulation Test',
    conductingAuthority: 'Digital India Corporation / MeitY',
    category: 'ENGINEERING',
    examinationType: 'COMPUTER_BASED_TEST',
    examinationMode: 'OPEN_PRACTICE',
    durationMinutes: 60,
    totalMarks: 50,
    negativeMarkingEnabled: true,
    negativeMarkingValue: 0.33,
    navigationPolicy: 'FREE_FORWARD_BACKWARD',
    calculatorPolicy: 'STANDARD_VIRTUAL',
    reviewFlagEnabled: true,
    applicationDeadline: '2026-12-31',
    examDate: '2026-10-02',
    feeAmount: 0,
    eligibility: 'Interactive mock test designed to familiarize candidates with digital systems',
    totalSeats: 500000,
    vacanciesCount: 0,
    sections: [
      { name: 'Aptitude & Protocol Systems Mock', durationMinutes: 60, totalQuestions: 25, totalMarks: 50 },
    ],
    status: 'OPEN',
    applied: false,
    isPractice: true,
  },
  {
    id: 'exam-3',
    code: 'CSE-PRE-2026',
    title: 'Civil Services Preliminary Screening (GS & CSAT)',
    conductingAuthority: 'Union Public Service Commission (UPSC)',
    category: 'CIVIL_SERVICES',
    examinationType: 'OFFLINE_OMR',
    examinationMode: 'CENTRE_PROCTORED',
    durationMinutes: 240,
    totalMarks: 400,
    negativeMarkingEnabled: true,
    negativeMarkingValue: 0.33,
    navigationPolicy: 'LINEAR_SECTION_TIMED',
    calculatorPolicy: 'NONE',
    reviewFlagEnabled: false,
    applicationDeadline: '2026-11-05',
    examDate: '2026-11-28',
    feeAmount: 100,
    eligibility: 'Any Recognized Bachelor Degree from a UGC recognized university',
    totalSeats: 1000000,
    vacanciesCount: 1100,
    sections: [
      { name: 'Paper I - General Studies', durationMinutes: 120, totalQuestions: 100, totalMarks: 200 },
      { name: 'Paper II - CSAT Aptitude', durationMinutes: 120, totalQuestions: 80, totalMarks: 200, cutoffMarks: 66 },
    ],
    status: 'OPEN',
    applied: false,
    isPractice: false,
  },
  {
    id: 'exam-4',
    code: 'RBI-GRADE-B-2026',
    title: 'Reserve Bank Officer Recruitment Phase I (DR-General)',
    conductingAuthority: 'Reserve Bank of India Services Board',
    category: 'BANKING',
    examinationType: 'COMPUTER_BASED_TEST',
    examinationMode: 'CENTRE_PROCTORED',
    durationMinutes: 120,
    totalMarks: 200,
    negativeMarkingEnabled: true,
    negativeMarkingValue: 0.25,
    navigationPolicy: 'FREE_FORWARD_BACKWARD',
    calculatorPolicy: 'NONE',
    reviewFlagEnabled: true,
    applicationDeadline: '2026-10-18',
    examDate: '2026-11-10',
    feeAmount: 850,
    eligibility: 'Graduation with min 60% marks (50% for SC/ST/PwBD) or equivalent technical degree',
    totalSeats: 80000,
    vacanciesCount: 320,
    sections: [
      { name: 'General Awareness', durationMinutes: 25, totalQuestions: 80, totalMarks: 80 },
      { name: 'Reasoning Ability', durationMinutes: 45, totalQuestions: 60, totalMarks: 60 },
      { name: 'English Language', durationMinutes: 25, totalQuestions: 30, totalMarks: 30 },
      { name: 'Quantitative Aptitude', durationMinutes: 25, totalQuestions: 30, totalMarks: 30 },
    ],
    status: 'CLOSING_SOON',
    applied: false,
    isPractice: false,
  },
  {
    id: 'exam-5',
    code: 'NDA-NA-2026',
    title: 'National Defence Academy & Naval Academy Exam',
    conductingAuthority: 'Union Public Service Commission / Ministry of Defence',
    category: 'DEFENSE',
    examinationType: 'COMPUTER_BASED_TEST',
    examinationMode: 'CENTRE_PROCTORED',
    durationMinutes: 300,
    totalMarks: 900,
    negativeMarkingEnabled: true,
    negativeMarkingValue: 0.33,
    navigationPolicy: 'LINEAR_SECTION_TIMED',
    calculatorPolicy: 'NONE',
    reviewFlagEnabled: false,
    applicationDeadline: '2026-11-12',
    examDate: '2026-12-05',
    feeAmount: 100,
    eligibility: '12th Class pass of the 10+2 pattern of School Education with Physics and Mathematics',
    totalSeats: 400000,
    vacanciesCount: 400,
    sections: [
      { name: 'Mathematics', durationMinutes: 150, totalQuestions: 120, totalMarks: 300 },
      { name: 'General Ability Test (GAT)', durationMinutes: 150, totalQuestions: 150, totalMarks: 600 },
    ],
    status: 'OPEN',
    applied: false,
    isPractice: false,
  },
  {
    id: 'exam-6',
    code: 'NEET-PG-DPI-2026',
    title: 'National Eligibility cum Entrance Test (Postgraduate Assessment)',
    conductingAuthority: 'National Board of Examinations in Medical Sciences (NBEMS)',
    category: 'MEDICAL',
    examinationType: 'COMPUTER_BASED_TEST',
    examinationMode: 'CENTRE_PROCTORED',
    durationMinutes: 210,
    totalMarks: 800,
    negativeMarkingEnabled: true,
    negativeMarkingValue: 0.25,
    navigationPolicy: 'FREE_FORWARD_BACKWARD',
    calculatorPolicy: 'NONE',
    reviewFlagEnabled: true,
    applicationDeadline: '2026-10-30',
    examDate: '2026-11-20',
    feeAmount: 1250,
    eligibility: 'MBBS Degree recognized by NMC with 1-year compulsory rotating internship',
    totalSeats: 220000,
    vacanciesCount: 45000,
    sections: [
      { name: 'Pre-Clinical Sciences', durationMinutes: 60, totalQuestions: 50, totalMarks: 200 },
      { name: 'Para-Clinical Sciences', durationMinutes: 60, totalQuestions: 50, totalMarks: 200 },
      { name: 'Clinical Sciences', durationMinutes: 90, totalQuestions: 100, totalMarks: 400 },
    ],
    status: 'OPEN',
    applied: false,
    isPractice: false,
  },
];

export const DEFAULT_MOCK_CENTRES: PublicCentre[] = [
  {
    id: 'c-delhi-01',
    centreName: 'NAG Assessment Hub — IIT Delhi Campus',
    region: 'North',
    state: 'Delhi',
    district: 'New Delhi',
    city: 'Delhi NCR',
    building: 'Block IV, Academic Complex, Hauz Khas',
    totalCapacity: 1200,
  },
  {
    id: 'c-mumbai-01',
    centreName: 'National Examination Center — VJTI Matunga',
    region: 'West',
    state: 'Maharashtra',
    district: 'Mumbai City',
    city: 'Mumbai',
    building: 'Computing Annex, H R Mahajani Marg',
    totalCapacity: 850,
  },
  {
    id: 'c-bengaluru-01',
    centreName: 'Digital India Assessment Complex — Electronic City',
    region: 'South',
    state: 'Karnataka',
    district: 'Bengaluru Urban',
    city: 'Bengaluru',
    building: 'Phase 1, Cyber Park Sector 3',
    totalCapacity: 1500,
  },
  {
    id: 'c-hyderabad-01',
    centreName: 'Telangana State Cyber Examination Hub — HITEC City',
    region: 'South',
    state: 'Telangana',
    district: 'Hyderabad',
    city: 'Hyderabad',
    building: 'Knowledge Corridor, Madhapur',
    totalCapacity: 1000,
  },
  {
    id: 'c-chennai-01',
    centreName: 'Southern Regional Testing Center — IIT Madras Research Park',
    region: 'South',
    state: 'Tamil Nadu',
    district: 'Chennai',
    city: 'Chennai',
    building: 'Kanagam Road, Taramani',
    totalCapacity: 900,
  },
  {
    id: 'c-kolkata-01',
    centreName: 'Eastern Testing Complex — Salt Lake Sector V',
    region: 'East',
    state: 'West Bengal',
    district: 'North 24 Parganas',
    city: 'Kolkata',
    building: 'Block EP & GP, Bidhan Nagar',
    totalCapacity: 800,
  },
  {
    id: 'c-lucknow-01',
    centreName: 'Central Assessment Hub — IET Lucknow',
    region: 'North',
    state: 'Uttar Pradesh',
    district: 'Lucknow',
    city: 'Lucknow',
    building: 'Sitapur Road Campus',
    totalCapacity: 750,
  },
];

@Injectable({
  providedIn: 'root',
})
export class CandidateBrowseService {
  private readonly http = inject(HttpClient);
  private readonly examsUrl = '/api/v1/examinations';

  readonly catalog = signal<CatalogExam[]>(DEFAULT_MOCK_EXAMS);
  readonly centres = signal<PublicCentre[]>(DEFAULT_MOCK_CENTRES);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  /**
   * Load public examination catalog from backend API with fallback to default catalogue.
   * Also merges applied status for the authenticated candidate.
   */
  loadPublicCatalog(search?: string, category?: string): Observable<CatalogExam[]> {
    this.loading.set(true);
    this.error.set(null);

    let params = new HttpParams().set('page', '0').set('size', '50');
    if (search && search.trim()) {
      params = params.set('search', search.trim());
    }
    if (category && category.trim() && category !== 'ALL') {
      params = params.set('category', category.trim());
    }

    const publicExams$ = this.http
      .get<any>(`${this.examsUrl}/public`, { params })
      .pipe(
        map((res) => {
          const payload = res?.data ?? res;
          const items = Array.isArray(payload?.content)
            ? payload.content
            : Array.isArray(payload)
            ? payload
            : [];
          return items;
        }),
        catchError(() => of([]))
      );

    const myExams$ = this.http
      .get<any>(`${this.examsUrl}/my-exams`)
      .pipe(
        map((res) => {
          const payload = res?.data ?? res;
          return Array.isArray(payload) ? payload : [];
        }),
        catchError(() => of([]))
      );

    return forkJoin({ publicExams: publicExams$, myApplications: myExams$ }).pipe(
      map(({ publicExams, myApplications }) => {
        const appliedExamIds = new Set<string>(
          myApplications.map((app: any) => String(app.examId || app.id))
        );

        if (publicExams.length > 0) {
          const mapped: CatalogExam[] = publicExams.map((item: any) => ({
            id: String(item.id),
            code: item.code || 'EXAM',
            title: item.name || item.title || 'National Examination',
            conductingAuthority: item.conductingAuthority || 'National Testing Body',
            category: item.category || 'GENERAL',
            examinationType: item.examinationType || 'COMPUTER_BASED_TEST',
            examinationMode: item.examinationMode || 'CENTRE_PROCTORED',
            durationMinutes: item.durationMinutes || 180,
            totalMarks: item.totalMarks || 100,
            negativeMarkingEnabled: !!item.negativeMarkingEnabled,
            negativeMarkingValue: item.negativeMarkingValue ?? 0.25,
            navigationPolicy: item.navigationPolicy || 'FREE_FORWARD_BACKWARD',
            calculatorPolicy: item.calculatorPolicy || 'NONE',
            reviewFlagEnabled: item.reviewFlagEnabled ?? true,
            applicationDeadline: item.applicationDeadline || '2026-11-15',
            examDate: item.examDate || '2026-12-01',
            feeAmount: item.feeAmount ?? 500,
            eligibility: item.eligibility || 'Graduation in relevant discipline',
            totalSeats: item.totalSeats || 50000,
            vacanciesCount: item.vacanciesCount || 1000,
            sections: item.sections || [],
            status: item.status || 'OPEN',
            applied: appliedExamIds.has(String(item.id)),
            isPractice: !!(item.isPractice || item.practice),
          }));

          return mapped;
        }

        // Return fallback mock exams with live applied mapping
        return DEFAULT_MOCK_EXAMS.map((exam) => ({
          ...exam,
          applied: appliedExamIds.has(exam.id) || exam.applied,
        }));
      }),
      tap({
        next: (exams) => {
          this.catalog.set(exams);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(err?.message || 'Failed to load examination catalog');
          this.loading.set(false);
        },
      })
    );
  }

  /**
   * Load public centres directory from backend API.
   */
  loadPublicCentres(): Observable<PublicCentre[]> {
    return this.http.get<any>(`${this.examsUrl}/centres/public`).pipe(
      map((res) => {
        const payload = res?.data ?? res;
        return Array.isArray(payload) && payload.length > 0 ? payload : DEFAULT_MOCK_CENTRES;
      }),
      catchError(() => of(DEFAULT_MOCK_CENTRES)),
      tap((centres) => this.centres.set(centres))
    );
  }

  /**
   * Apply for an examination.
   */
  applyForExam(examId: string, payload: ApplyExamPayload): Observable<ApplicationReceipt> {
    return this.http
      .post<any>(`${this.examsUrl}/${examId}/apply`, payload)
      .pipe(
        map((res) => {
          const data = res?.data ?? res;
          return {
            applicationId: data.id || data.applicationId || `APP-${Date.now()}`,
            applicationNumber: data.applicationNumber || `NAG-${Math.floor(100000 + Math.random() * 900000)}`,
            examId: examId,
            examTitle: data.examTitle || 'National Assessment Examination',
            examCode: data.examCode || 'EXAM-2026',
            candidateName: data.candidateName || 'Candidate',
            candidateEmail: data.candidateEmail || 'candidate@example.gov.in',
            category: data.category || 'General',
            appliedAt: data.appliedAt || new Date().toISOString(),
            feePaid: data.feePaid ?? 0,
            firstChoiceCentreName: data.centreName || 'Allocated Centre Hub',
            pwdAssistance: !!payload.pwdRequired,
            status: data.status || 'CONFIRMED',
          };
        }),
        tap(() => {
          this.catalog.update((exams) =>
            exams.map((e) => (e.id === examId ? { ...e, applied: true } : e))
          );
        })
      );
  }
}
