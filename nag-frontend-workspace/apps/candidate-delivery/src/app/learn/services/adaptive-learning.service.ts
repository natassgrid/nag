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
    return this.http.get<any[]>('/api/v1/learner/exams').pipe(
      map((res) => {
        if (!res || !Array.isArray(res) || res.length === 0) {
          return this.publishedExams();
        }
        return res.map((item) => this.normalizeExamSummary(item));
      }),
      tap((normalized) => {
        if (normalized && normalized.length > 0) {
          this.publishedExams.set(normalized);
        }
      }),
      catchError(() => of(this.publishedExams()))
    );
  }

  private normalizeExamSummary(raw: any): LearnerExamSummary {
    const matchingSeed = SEED_PUBLISHED_EXAMS.find(
      (s) => s.id === raw.id || s.code === raw.code
    );

    const title = raw.title || raw.name || matchingSeed?.title || 'Competitive Examination';
    const sectionsCount = raw.sectionsCount ?? matchingSeed?.syllabusSummary?.subjectCount ?? 0;
    const nodesCount = raw.syllabusNodesCount ?? matchingSeed?.syllabusSummary?.subtopicCount ?? 0;

    return {
      id: raw.id,
      code: raw.code || 'EXAM',
      title,
      name: raw.name || title,
      description: raw.description || matchingSeed?.description || '',
      conductingAuthority: raw.conductingAuthority || matchingSeed?.conductingAuthority || 'Exam Authority',
      category: raw.category || matchingSeed?.category || 'General',
      totalQuestions: raw.totalQuestions ?? matchingSeed?.totalQuestions ?? 100,
      totalMarks: raw.totalMarks ?? matchingSeed?.totalMarks ?? 200,
      durationMinutes: raw.durationMinutes ?? matchingSeed?.durationMinutes ?? 60,
      blueprintId: raw.blueprintId,
      sectionsCount,
      syllabusNodesCount: nodesCount,
      syllabusSummary: raw.syllabusSummary || matchingSeed?.syllabusSummary || {
        subjectCount: sectionsCount,
        topicCount: Math.max(1, Math.round(nodesCount / 2)),
        subtopicCount: nodesCount,
        subjects: [],
      },
    };
  }

  getEnrollments(): Observable<LearnerEnrollment[]> {
    return this.http.get<any[]>('/api/v1/learner/enrollments').pipe(
      map((list) =>
        Array.isArray(list) ? list.map((item) => this.normalizeEnrollment(item)) : []
      ),
      catchError(() => of(this.activeEnrollment() ? [this.activeEnrollment()!] : []))
    );
  }

  getActiveEnrollment(): Observable<LearnerEnrollment | null> {
    return this.http.get<any>('/api/v1/learner/enrollments/active').pipe(
      map((res) => (res ? this.normalizeEnrollment(res) : null)),
      tap((res) => {
        if (res) this.activeEnrollment.set(res);
      }),
      catchError(() => of(this.activeEnrollment()))
    );
  }

  createEnrollment(req: CreateEnrollmentRequest): Observable<LearnerEnrollment> {
    const exam =
      this.publishedExams().find((e) => e.id === req.examinationId) || this.publishedExams()[0];
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
      readinessScore: 0,
    };

    return this.http.post<any>('/api/v1/learner/enrollments', req).pipe(
      map((res) => (res ? this.normalizeEnrollment(res, fallbackEnrollment) : fallbackEnrollment)),
      tap((res) => this.activeEnrollment.set(res)),
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
    const fallbackSession: DiagnosticSession = {
      ...SEED_DIAGNOSTIC_SESSION,
      enrollmentId,
      examinationId: active?.examinationId || SEED_DIAGNOSTIC_SESSION.examinationId,
      examinationTitle: active?.examinationTitle || SEED_DIAGNOSTIC_SESSION.examinationTitle,
      examinationCode: active?.examinationCode,
      neutralPriorPercent: 50,
    };

    return this.http
      .post<any>(`/api/v1/learner/enrollments/${enrollmentId}/diagnostic`, {})
      .pipe(
        map((res) => this.normalizeDiagnosticSession(res, fallbackSession)),
        catchError(() => of(fallbackSession))
      );
  }

  submitDiagnostic(
    enrollmentId: string,
    req: DiagnosticSubmitRequest
  ): Observable<DiagnosticSubmitResult> {
    return this.http
      .post<any>(`/api/v1/learner/enrollments/${enrollmentId}/diagnostic/submit`, req)
      .pipe(
        map((res) => this.normalizeDiagnosticResult(res, enrollmentId, false)),
        tap((res) => {
          this.activeEnrollment.update((curr) =>
            curr ? { ...curr, diagnosticCompleted: true, readinessScore: res.initialReadinessScore } : null
          );
          this.progress.update((p) =>
            p ? { ...p, readinessScore: res.initialReadinessScore } : null
          );
        }),
        catchError(() => {
          const fallback = this.computeClientSideDiagnosticResult(enrollmentId, req);
          this.activeEnrollment.update((curr) =>
            curr ? { ...curr, diagnosticCompleted: true, readinessScore: fallback.initialReadinessScore } : null
          );
          return of(fallback);
        })
      );
  }

  skipDiagnostic(enrollmentId: string): Observable<DiagnosticSubmitResult> {
    const skippedResult: DiagnosticSubmitResult = {
      ...SEED_DIAGNOSTIC_RESULT,
      enrollmentId,
      status: 'SKIPPED',
      score: 0,
      accuracyPercent: 50,
      initialReadinessScore: 50,
      neutralPriorPercent: 50,
      baselineLabel: 'Neutral Baseline (50% Prior)',
      strengths: [],
      weaknesses: ['Diagnostic skipped: subtopic mastery initialized with neutral prior.'],
    };

    return this.http
      .post<any>(`/api/v1/learner/enrollments/${enrollmentId}/diagnostic/submit`, {
        answers: [],
        isSkip: true,
      })
      .pipe(
        map((res) => this.normalizeDiagnosticResult(res, enrollmentId, true)),
        tap((res) => {
          const score = res.initialReadinessScore || 50;
          this.activeEnrollment.update((curr) =>
            curr ? { ...curr, diagnosticCompleted: true, readinessScore: score } : null
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

  private normalizeEnrollment(raw: any, fallback?: LearnerEnrollment): LearnerEnrollment {
    const exam = this.publishedExams().find(
      (e) => e.id === raw?.examinationId || e.code === raw?.examinationCode
    );
    const diagnosticStatus = raw?.diagnosticStatus || '';
    const diagnosticCompleted =
      raw?.diagnosticCompleted ??
      (diagnosticStatus === 'COMPLETED' || diagnosticStatus === 'SKIPPED');
    const readinessScore =
      raw?.readinessScore ??
      (diagnosticStatus === 'SKIPPED' ? 50 : fallback?.readinessScore ?? 0);

    return {
      id: String(raw?.id || fallback?.id || `enr-${Date.now()}`),
      candidateId: String(raw?.candidateId || fallback?.candidateId || this.profile()?.candidateId || 'cand-001'),
      examinationId: String(raw?.examinationId || fallback?.examinationId || ''),
      examName: raw?.examName || raw?.examinationTitle || exam?.title,
      examinationTitle:
        raw?.examinationTitle ||
        raw?.examName ||
        exam?.title ||
        fallback?.examinationTitle ||
        'Target Examination',
      examinationCode:
        raw?.examinationCode ||
        exam?.code ||
        fallback?.examinationCode ||
        'GENERAL_EXAM',
      targetDate: raw?.targetDate ?? fallback?.targetDate ?? null,
      status: raw?.status || fallback?.status || 'ACTIVE',
      diagnosticStatus,
      diagnosticSessionId: raw?.diagnosticSessionId,
      startedAt: raw?.startedAt || raw?.enrolledAt || fallback?.startedAt || new Date().toISOString(),
      diagnosticCompleted,
      readinessScore,
    };
  }

  private normalizeDiagnosticSession(
    raw: any,
    fallback: DiagnosticSession
  ): DiagnosticSession {
    if (!raw || !Array.isArray(raw.questions) || raw.questions.length === 0) {
      return fallback;
    }
    const questions = raw.questions.map((q: any) => ({
      id: String(q.id || q.questionId || ''),
      questionId: String(q.questionId || q.id || ''),
      stem: q.stem || q.content || '',
      content: q.content || q.stem || '',
      options: Array.isArray(q.options)
        ? q.options.map((o: any) => ({ id: String(o.id), text: String(o.text) }))
        : [],
      subjectId: Number(q.subjectId ?? 1),
      subjectName: q.subjectName || 'General Subject',
      topicId: Number(q.topicId ?? 10),
      topicName: q.topicName || 'Core Topic',
      subtopicId: Number(q.subtopicId ?? 101),
      subtopicName: q.subtopicName || 'Core Subtopic',
      difficulty: q.difficulty || 'CORE',
    }));

    return {
      sessionId: String(raw.sessionId || fallback.sessionId),
      enrollmentId: String(raw.enrollmentId || fallback.enrollmentId),
      examinationId: String(raw.examinationId || fallback.examinationId),
      examinationTitle:
        raw.examinationTitle ||
        this.activeEnrollment()?.examinationTitle ||
        fallback.examinationTitle,
      examinationCode:
        raw.examinationCode ||
        this.activeEnrollment()?.examinationCode ||
        fallback.examinationCode,
      status: raw.status || 'IN_PROGRESS',
      totalQuestions: raw.totalQuestions ?? questions.length,
      estimatedMinutes: raw.estimatedMinutes ?? Math.max(5, questions.length * 2),
      neutralPriorPercent: raw.neutralPriorPercent ?? 50,
      questions,
    };
  }

  private normalizeDiagnosticResult(
    raw: any,
    enrollmentId: string,
    isSkip = false
  ): DiagnosticSubmitResult {
    if (!raw) {
      return {
        ...SEED_DIAGNOSTIC_RESULT,
        enrollmentId,
        initialReadinessScore: isSkip ? 50 : SEED_DIAGNOSTIC_RESULT.initialReadinessScore,
        neutralPriorPercent: 50,
        baselineLabel: isSkip ? 'Neutral Baseline (50% Prior)' : 'Calibrated Diagnostic Baseline',
      };
    }

    const totalQuestions = Number(raw.totalQuestions ?? SEED_DIAGNOSTIC_RESULT.totalQuestions);
    const correctCount = Number(raw.correctCount ?? raw.correctAnswers ?? raw.score ?? 0);
    const accuracyPercent =
      raw.accuracyPercent !== undefined
        ? Math.round(Number(raw.accuracyPercent))
        : totalQuestions > 0
          ? Math.round((correctCount * 100) / totalQuestions)
          : isSkip
            ? 50
            : 0;
    const initialReadinessScore =
      raw.initialReadinessScore !== undefined
        ? Number(raw.initialReadinessScore)
        : isSkip
          ? 50
          : accuracyPercent;
    const breakdownKeys =
      raw.subtopicBreakdown && typeof raw.subtopicBreakdown === 'object'
        ? Object.keys(raw.subtopicBreakdown).length
        : 0;

    return {
      sessionId: raw.sessionId,
      enrollmentId: String(raw.enrollmentId || enrollmentId),
      status: raw.status || (isSkip ? 'SKIPPED' : 'COMPLETED'),
      score: Number(raw.score ?? correctCount),
      totalQuestions,
      correctCount,
      correctAnswers: correctCount,
      accuracyPercent,
      seededSubtopicsCount: Number(
        raw.seededSubtopicsCount ?? (breakdownKeys > 0 ? breakdownKeys : Math.max(totalQuestions, 3))
      ),
      initialReadinessScore,
      strengths: Array.isArray(raw.strengths) ? raw.strengths : [],
      weaknesses:
        Array.isArray(raw.weaknesses) && raw.weaknesses.length > 0
          ? raw.weaknesses
          : isSkip
            ? ['Diagnostic skipped: subtopic mastery initialized with neutral prior.']
            : [],
      baselineLabel:
        raw.baselineLabel ||
        (isSkip ? 'Neutral Baseline (50% Prior)' : 'Calibrated Diagnostic Baseline'),
      neutralPriorPercent: Number(raw.neutralPriorPercent ?? 50),
      subtopicBreakdown: raw.subtopicBreakdown,
      completedAt: raw.completedAt,
    };
  }

  private computeClientSideDiagnosticResult(
    enrollmentId: string,
    req: DiagnosticSubmitRequest
  ): DiagnosticSubmitResult {
    const answers = req?.answers || [];
    if (answers.length === 0) {
      return {
        ...SEED_DIAGNOSTIC_RESULT,
        enrollmentId,
        baselineLabel: 'Calibrated Diagnostic Baseline',
        neutralPriorPercent: 50,
      };
    }
    const questions = SEED_DIAGNOSTIC_SESSION.questions;
    const strengths: string[] = [];
    const weaknesses: string[] = [];
    let correct = 0;

    for (const ans of answers) {
      const q = questions.find((item) => item.id === ans.questionId);
      const isCorr = ans.selectedOptionId === 'opt-1' || ans.selectedOptionId === 'A';
      if (isCorr) {
        correct++;
        if (q?.subtopicName && !strengths.includes(q.subtopicName)) {
          strengths.push(q.subtopicName);
        }
      } else if (q?.subtopicName && !weaknesses.includes(q.subtopicName)) {
        weaknesses.push(q.subtopicName);
      }
    }

    const total = Math.max(answers.length, questions.length);
    const accuracy = Math.round((correct * 100) / total);
    return {
      enrollmentId,
      score: correct,
      totalQuestions: total,
      correctCount: correct,
      correctAnswers: correct,
      accuracyPercent: accuracy,
      seededSubtopicsCount: total,
      initialReadinessScore: accuracy,
      strengths,
      weaknesses,
      baselineLabel: 'Calibrated Diagnostic Baseline',
      neutralPriorPercent: 50,
    };
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
