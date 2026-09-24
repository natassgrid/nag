import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export type ExamStatus = 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export interface ExamSchedule {
  id: string;
  examCode: string;
  title: string;
  subject: string;
  sessionDate: string;
  startTime: string;
  endTime: string;
  totalSlots: number;
  registeredCandidates: number;
  status: ExamStatus;
}

export interface ExamCenter {
  id: string;
  centerCode: string;
  name: string;
  city: string;
  state: string;
  capacity: number;
  activeNodes: number;
  proctorCount: number;
  status: 'ACTIVE' | 'INACTIVE' | 'AUDIT_PENDING';
}

export interface PaperSectionConfig {
  id: string;
  name: string;
  questionCount: number;
  marksPerQuestion: number;
}

export interface PaperGenerationConfig {
  examId: string;
  totalQuestions: number;
  difficultyDistribution: {
    easy: number;
    medium: number;
    hard: number;
  };
  sections: PaperSectionConfig[];
  shuffleQuestions: boolean;
  shuffleOptions: boolean;
  ledgerProofHash?: string;
}

export interface LedgerProofResult {
  proofHash: string;
  blockNumber: number;
  timestamp: string;
  merkleRoot: string;
  verified: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class ExaminationService {
  private readonly http = inject(HttpClient);

  readonly schedules = signal<ExamSchedule[]>([]);
  readonly centers = signal<ExamCenter[]>([]);
  readonly loading = signal<boolean>(false);
  readonly activeProof = signal<LedgerProofResult | null>(null);

  loadSchedules(): Observable<ExamSchedule[]> {
    this.loading.set(true);
    return this.http.get<ExamSchedule[]>('/api/v1/examinations/schedules').pipe(
      tap({
        next: (data) => {
          this.schedules.set(data || []);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      })
    );
  }

  loadCenters(): Observable<ExamCenter[]> {
    return this.http.get<ExamCenter[]>('/api/v1/examinations/centers').pipe(
      tap((data) => this.centers.set(data || []))
    );
  }

  createSchedule(schedule: Partial<ExamSchedule>): Observable<ExamSchedule> {
    return this.http.post<ExamSchedule>('/api/v1/examinations/schedules', schedule).pipe(
      tap((created) => this.schedules.update((prev) => [created, ...prev]))
    );
  }

  generatePaper(config: PaperGenerationConfig): Observable<{ paperId: string; proofHash: string }> {
    return this.http.post<{ paperId: string; proofHash: string }>(
      '/api/v1/examinations/papers/generate',
      config
    );
  }

  verifyLedgerProof(proofHash: string): Observable<LedgerProofResult> {
    return this.http
      .get<LedgerProofResult>(`/api/v1/examinations/ledger/verify/${proofHash}`)
      .pipe(tap((proof) => this.activeProof.set(proof)));
  }
}
