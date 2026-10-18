import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, of, map } from 'rxjs';
import {
  LearnerProfile,
  LearnerExamSummary,
  LearnerEnrollment,
  CreateEnrollmentRequest,
  DiagnosticSession,
  DiagnosticSubmitRequest,
  DiagnosticSubmitResult,
  NextActivity,
  ActivityCompletionRequest,
  LearnerProgress,
} from '../models/learner.model';
import {
  SEED_LEARNER_PROFILE,
  SEED_PUBLISHED_EXAMS,
  SEED_ACTIVE_ENROLLMENT,
  SEED_DIAGNOSTIC_SESSION,
  SEED_DIAGNOSTIC_RESULT,
  SEED_NEXT_ACTIVITY,
  SEED_LEARNER_PROGRESS,
} from '../data/learner-seed.data';

@Injectable({ providedIn: 'root' })
export class AdaptiveLearningService {
  private readonly http = inject(HttpClient);

  readonly profile = signal<LearnerProfile | null>(SEED_LEARNER_PROFILE);
  readonly activeEnrollment = signal<LearnerEnrollment | null>(SEED_ACTIVE_ENROLLMENT);
  readonly publishedExams = signal<LearnerExamSummary[]>(SEED_PUBLISHED_EXAMS);
  readonly nextActivity = signal<NextActivity | null>(SEED_NEXT_ACTIVITY);
  readonly progress = signal<LearnerProgress | null>(SEED_LEARNER_PROGRESS);

  readonly hasCompletedProfile = computed<boolean>(() => {
    const p = this.profile();
    return !!(p && p.completedAt && p.educationLevel && p.preferredLanguage);
  });

  readonly hasActiveEnrollment = computed<boolean>(() => {
    return !!this.activeEnrollment();
  });

  readonly readinessScore = computed<number>(() => {
    return this.progress()?.readinessScore ?? this.activeEnrollment()?.readinessScore ?? 0;
  });

  getProfile(): Observable<LearnerProfile | null> {
    return this.http.get<LearnerProfile>('/api/v1/learner/profile').pipe(
      tap((res) => {
        if (res) this.profile.set(res);
      }),
      catchError(() => of(this.profile()))
    );
  }

  updateProfile(data: Partial<LearnerProfile>): Observable<LearnerProfile> {
    const current = this.profile() || SEED_LEARNER_PROFILE;
    const updated: LearnerProfile = {
      ...current,
      ...data,
      completedAt: data.completedAt !== undefined ? data.completedAt : new Date().toISOString(),
    };

    return this.http.put<LearnerProfile>('/api/v1/learner/profile', updated).pipe(
      tap((res) => this.profile.set(res || updated)),
      catchError(() => {
        this.profile.set(updated);
        return of(updated);
      })
    );
  }

  getPublishedExams(): Observable<LearnerExamSummary[]> {
    return this.http.get<LearnerExamSummary[]>('/api/v1/learner/exams').pipe(
      tap((res) => {
        if (res && res.length > 0) this.publishedExams.set(res);
      }),
      catchError(() => of(this.publishedExams()))
    );
  }

  getEnrollments(): Observable<LearnerEnrollment[]> {
    return this.http.get<LearnerEnrollment[]>('/api/v1/learner/enrollments').pipe(
      catchError(() => of(this.activeEnrollment() ? [this.activeEnrollment()!] : []))
    );
  }

  getActiveEnrollment(): Observable<LearnerEnrollment | null> {
    return this.http.get<LearnerEnrollment>('/api/v1/learner/enrollments/active').pipe(
      tap((res) => {
        if (res) this.activeEnrollment.set(res);
      }),
      catchError(() => of(this.activeEnrollment()))
    );
  }

  createEnrollment(req: CreateEnrollmentRequest): Observable<LearnerEnrollment> {
    const exam = this.publishedExams().find((e) => e.id === req.examinationId) || this.publishedExams()[0];
    const fallbackEnrollment: LearnerEnrollment = {
      id: `enr-${Date.now()}`,
      candidateId: this.profile()?.candidateId || 'cand-001',
      examinationId: req.examinationId,
      examinationTitle: exam ? exam.title : 'Competitive Examination',
      examinationCode: exam ? exam.code : 'EXAM-GEN',
      targetDate: req.targetDate || null,
      status: 'ACTIVE',
      startedAt: new Date().toISOString(),
      diagnosticCompleted: false,
      readinessScore: 40,
    };

    return this.http.post<LearnerEnrollment>('/api/v1/learner/enrollments', req).pipe(
      tap((res) => this.activeEnrollment.set(res || fallbackEnrollment)),
      catchError(() => {
        this.activeEnrollment.set(fallbackEnrollment);
        return of(fallbackEnrollment);
      })
    );
  }

  switchTargetExam(examinationId: string, targetDate?: string): Observable<LearnerEnrollment> {
    return this.createEnrollment({ examinationId, targetDate });
  }

  startDiagnostic(enrollmentId: string): Observable<DiagnosticSession> {
    const active = this.activeEnrollment();
    const session: DiagnosticSession = {
      ...SEED_DIAGNOSTIC_SESSION,
      enrollmentId,
      examinationId: active?.examinationId || SEED_DIAGNOSTIC_SESSION.examinationId,
      examinationTitle: active?.examinationTitle || SEED_DIAGNOSTIC_SESSION.examinationTitle,
    };

    return this.http
      .post<DiagnosticSession>(`/api/v1/learner/enrollments/${enrollmentId}/diagnostic`, {})
      .pipe(catchError(() => of(session)));
  }

  submitDiagnostic(
    enrollmentId: string,
    req: DiagnosticSubmitRequest
  ): Observable<DiagnosticSubmitResult> {
    return this.http
      .post<DiagnosticSubmitResult>(`/api/v1/learner/enrollments/${enrollmentId}/diagnostic/submit`, req)
      .pipe(
        tap((res) => {
          this.activeEnrollment.update((curr) =>
            curr ? { ...curr, diagnosticCompleted: true, readinessScore: res.initialReadinessScore } : null
          );
        }),
        catchError(() => {
          this.activeEnrollment.update((curr) =>
            curr ? { ...curr, diagnosticCompleted: true, readinessScore: SEED_DIAGNOSTIC_RESULT.initialReadinessScore } : null
          );
          return of(SEED_DIAGNOSTIC_RESULT);
        })
      );
  }

  skipDiagnostic(enrollmentId: string): Observable<DiagnosticSubmitResult> {
    const skippedResult: DiagnosticSubmitResult = {
      ...SEED_DIAGNOSTIC_RESULT,
      score: 0,
      accuracyPercent: 50,
      initialReadinessScore: 50,
      weaknesses: ['Diagnostic skipped: subtopic mastery initialized with neutral prior.'],
    };

    return this.http
      .post<DiagnosticSubmitResult>(`/api/v1/learner/enrollments/${enrollmentId}/diagnostic/submit`, {
        answers: [],
      })
      .pipe(
        tap(() => {
          this.activeEnrollment.update((curr) =>
            curr ? { ...curr, diagnosticCompleted: true, readinessScore: 50 } : null
          );
        }),
        catchError(() => {
          this.activeEnrollment.update((curr) =>
            curr ? { ...curr, diagnosticCompleted: true, readinessScore: 50 } : null
          );
          return of(skippedResult);
        })
      );
  }

  getNextActivity(enrollmentId: string): Observable<NextActivity> {
    return this.http
      .get<NextActivity>(`/api/v1/learning/enrollments/${enrollmentId}/next-activity`)
      .pipe(
        tap((act) => this.nextActivity.set(act)),
        catchError(() => of(SEED_NEXT_ACTIVITY))
      );
  }

  completeActivity(
    enrollmentId: string,
    activityId: string,
    req: ActivityCompletionRequest
  ): Observable<{ success: boolean; updatedReadiness: number }> {
    return this.http
      .post<{ success: boolean; updatedReadiness: number }>(
        `/api/v1/learning/enrollments/${enrollmentId}/activities/${activityId}/complete`,
        req
      )
      .pipe(
        tap((res) => {
          if (res?.updatedReadiness) {
            this.activeEnrollment.update((e) =>
              e ? { ...e, readinessScore: res.updatedReadiness } : null
            );
            this.progress.update((p) =>
              p ? { ...p, readinessScore: res.updatedReadiness } : null
            );
          }
        }),
        catchError(() => {
          const newScore = Math.min(100, (this.readinessScore() || 70) + 2);
          this.activeEnrollment.update((e) =>
            e ? { ...e, readinessScore: newScore } : null
          );
          return of({ success: true, updatedReadiness: newScore });
        })
      );
  }

  getProgress(enrollmentId: string): Observable<LearnerProgress> {
    return this.http
      .get<LearnerProgress>(`/api/v1/learning/enrollments/${enrollmentId}/progress`)
      .pipe(
        tap((p) => this.progress.set(p)),
        catchError(() => of(SEED_LEARNER_PROGRESS))
      );
  }
}

export { AdaptiveLearningService as LearnerOnboardingService };
