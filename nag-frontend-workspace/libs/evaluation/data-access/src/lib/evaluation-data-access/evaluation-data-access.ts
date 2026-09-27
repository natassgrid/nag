import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export type GradingStatus = 'PENDING' | 'GRADED' | 'DISPUTED';
export type DisputeStatus = 'OPEN' | 'RESOLVED' | 'REJECTED';

export interface GradingTask {
  id: string;
  examId: string;
  candidateRoll: string;
  questionCode: string;
  questionContent: string;
  candidateResponse: string;
  maxMarks: number;
  awardedMarks?: number;
  comments?: string;
  status: GradingStatus;
}

export interface DisputeRecord {
  id: string;
  candidateRoll: string;
  examCode: string;
  questionCode: string;
  reason: string;
  candidateArgument: string;
  reviewerRemarks?: string;
  status: DisputeStatus;
  createdAt: string;
}

export interface EvaluationAnalytics {
  totalGraded: number;
  pendingGrading: number;
  disputedCount: number;
  meanScore: number;
  standardDeviation: number;
}

@Injectable({
  providedIn: 'root',
})
export class EvaluationService {
  private readonly http = inject(HttpClient);

  readonly tasks = signal<GradingTask[]>([]);
  readonly disputes = signal<DisputeRecord[]>([]);
  readonly analytics = signal<EvaluationAnalytics | null>(null);
  readonly loading = signal<boolean>(false);

  loadPendingTasks(): Observable<GradingTask[]> {
    this.loading.set(true);
    return this.http.get<GradingTask[]>('/api/v1/evaluation/tasks/pending').pipe(
      tap({
        next: (data) => {
          this.tasks.set(data || []);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      })
    );
  }

  submitGrade(taskId: string, marks: number, comments: string): Observable<GradingTask> {
    return this.http
      .post<GradingTask>(`/api/v1/evaluation/tasks/${taskId}/grade`, {
        awardedMarks: marks,
        comments,
      })
      .pipe(
        tap((updated) => {
          this.tasks.update((list) => list.map((t) => (t.id === taskId ? updated : t)));
        })
      );
  }

  loadDisputes(): Observable<DisputeRecord[]> {
    return this.http.get<DisputeRecord[]>('/api/v1/evaluation/disputes').pipe(
      tap((data) => this.disputes.set(data || []))
    );
  }

  resolveDispute(
    disputeId: string,
    status: DisputeStatus,
    remarks: string
  ): Observable<DisputeRecord> {
    return this.http
      .post<DisputeRecord>(`/api/v1/evaluation/disputes/${disputeId}/resolve`, {
        status,
        remarks,
      })
      .pipe(
        tap((updated) => {
          this.disputes.update((list) => list.map((d) => (d.id === disputeId ? updated : d)));
        })
      );
  }

  loadAnalytics(): Observable<EvaluationAnalytics> {
    return this.http.get<EvaluationAnalytics>('/api/v1/evaluation/analytics/summary').pipe(
      tap((data) => this.analytics.set(data))
    );
  }
}
