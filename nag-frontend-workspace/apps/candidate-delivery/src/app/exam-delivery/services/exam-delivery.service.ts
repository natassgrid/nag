import { Injectable, computed, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {
  hashSha256,
  signSubmissionHash,
} from '@nag-frontend-workspace/shared-util-crypto';
import {
  ExamDeliveryMode,
  ExamItem,
  ExamScoreSummary,
  ExamSessionMetadata,
  ExamSubmissionReceipt,
} from '../models';
import {
  LIVE_QUESTIONS,
  PRACTICE_QUESTIONS,
  PREVIEW_QUESTIONS,
} from '../data';
import { PracticeService } from '../../practice/services/practice.service';
import { firstValueFrom } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class ExamDeliveryService {
  private readonly http = inject(HttpClient);
  private readonly practiceService = inject(PracticeService);

  readonly paperId = signal<string | null>(null);
  readonly examId = signal<string | null>(null);
  readonly practiceSessionId = signal<string | null>(null);
  readonly deliveryMode = signal<ExamDeliveryMode>('LIVE');
  readonly sessionMeta = signal<ExamSessionMetadata>({
    sessionId: 'NES-2026-A48',
    candidateId: '849202',
  });

  readonly sessionError = signal<string | null>(null);
  readonly isConcurrentConflict = signal<boolean>(false);
  readonly isLoading = signal<boolean>(false);

  readonly remainingSeconds = signal<number>(5400); // 90 minutes default
  private timerInterval: any = null;

  readonly questions = signal<ExamItem[]>([]);
  readonly currentIndex = signal<number>(0);
  readonly submissionReceipt = signal<ExamSubmissionReceipt | null>(null);

  readonly totalQuestions = computed(() => this.questions().length);

  readonly currentItem = computed(() => {
    const list = this.questions();
    const idx = this.currentIndex();
    return list[idx] || null;
  });

  readonly formattedTime = computed(() => {
    const s = this.remainingSeconds();
    const hrs = Math.floor(s / 3600);
    const mins = Math.floor((s % 3600) / 60);
    const secs = s % 60;
    return `${hrs.toString().padStart(2, '0')}:${mins
      .toString()
      .padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  });

  readonly countAnswered = computed(
    () => this.questions().filter((q) => q.selectedOptionId).length
  );

  readonly countFlagged = computed(
    () => this.questions().filter((q) => q.isFlagged).length
  );

  readonly countUnvisited = computed(
    () => this.questions().filter((q) => !q.isVisited && !q.selectedOptionId).length
  );

  readonly isWarningTimer = computed(
    () => this.remainingSeconds() < 300 && this.deliveryMode() !== 'PREVIEW'
  );

  initialize(
    mode: ExamDeliveryMode = 'LIVE',
    examId: string | null = null,
    paperId: string | null = null,
    sessionId: string | null = null
  ): void {
    this.deliveryMode.set(mode);
    this.examId.set(examId);
    this.paperId.set(paperId);
    this.practiceSessionId.set(sessionId);
    this.currentIndex.set(0);
    this.submissionReceipt.set(null);
    this.sessionError.set(null);
    this.isConcurrentConflict.set(false);

    if (mode === 'PRACTICE') {
      const resolvedSessionId = sessionId || (paperId ? `PRACTICE-${paperId.substring(0, 8)}` : 'MOCK-SESSION-SIM-2026');
      this.sessionMeta.set({
        sessionId: resolvedSessionId,
        candidateId: 'PRACTICE-CANDIDATE',
        title: 'Official Practice Mock Assessment',
      });
      this.remainingSeconds.set(3600); // 60 minutes for practice

      if (paperId) {
        this.practiceService.getSet(paperId).subscribe({
          next: (set) => {
            if (set) {
              this.sessionMeta.update((meta) => ({
                ...meta,
                title: set.name || meta.title,
              }));
              if (set.durationMinutes) {
                this.remainingSeconds.set(set.durationMinutes * 60);
              }
            }
          },
          error: () => {}
        });
      }

      if (sessionId) {
        this.practiceService.getSessionQuestions(sessionId).subscribe({
          next: (list) => {
            if (Array.isArray(list) && list.length > 0) {
              const mapped: ExamItem[] = list.map((q: any, idx: number) => {
                let parsedOptions = [];
                if (typeof q.optionsJson === 'string') {
                  try {
                    parsedOptions = JSON.parse(q.optionsJson);
                  } catch (e) {
                    parsedOptions = [];
                  }
                } else if (Array.isArray(q.options)) {
                  parsedOptions = q.options;
                }
                return {
                  id: String(q.id),
                  order: q.order || idx + 1,
                  questionCode: q.questionCode || `PRAC-Q${idx + 1}`,
                  content: q.content || '',
                  options: parsedOptions.map((opt: any) => ({
                    id: String(opt.id || opt.optionId || opt.key),
                    text: opt.text || opt.content || opt.value || '',
                  })),
                  marks: q.marks || 2,
                  negativeMarks: q.negativeMarks || 0.5,
                  subject: q.subject,
                  correctOptionId: q.correctOptionId,
                  isVisited: idx === 0,
                  primaryLanguage: q.primaryLanguage,
                  fallbackToEnglish: q.fallbackToEnglish,
                  primaryTranslation: q.primaryTranslation,
                };
              });
              this.questions.set(mapped);
              return;
            }
            this.fallbackToMockQuestions(paperId || sessionId);
          },
          error: () => {
            this.fallbackToMockQuestions(paperId || sessionId);
          }
        });
      } else {
        this.fallbackToMockQuestions(paperId);
      }
    } else if (mode === 'PREVIEW') {
      this.questions.set(JSON.parse(JSON.stringify(PREVIEW_QUESTIONS)));
      this.sessionMeta.set({
        sessionId: 'PREVIEW-SAMPLE-2026',
        candidateId: 'SAMPLE-GUEST',
        title: 'Examination Structure Preview',
      });
      this.remainingSeconds.set(5400);
    } else {
      this.sessionMeta.set({
        sessionId: 'NES-2026-A48',
        candidateId: '849202',
        title: 'National Examination Session',
      });
      this.remainingSeconds.set(5400);

      if (examId) {
        this.startLiveSession(examId, paperId);
      } else {
        this.questions.set(JSON.parse(JSON.stringify(LIVE_QUESTIONS)));
      }
    }
  }

  startLiveSession(examId: string, paperId?: string | null, terminateExisting = false): void {
    this.isLoading.set(true);
    this.sessionError.set(null);
    this.isConcurrentConflict.set(false);

    this.http
      .post<any>('/api/v1/sessions/start', {
        examId,
        paperId: paperId || undefined,
        terminateExisting,
      })
      .subscribe({
        next: (res) => {
          this.isLoading.set(false);
          if (res) {
            this.sessionMeta.set({
              sessionId: res.sessionId || 'LIVE-SESSION-ACTIVE',
              candidateId: res.candidateId || '849202',
              title: res.examTitle || 'National Examination Session',
            });
            if (res.durationSeconds) {
              this.remainingSeconds.set(res.durationSeconds);
            }
            if (Array.isArray(res.questions) && res.questions.length > 0) {
              const mapped: ExamItem[] = res.questions.map((q: any, idx: number) => ({
                id: String(q.id),
                order: idx + 1,
                questionCode: q.code || `Q-${idx + 1}`,
                content: q.content || '',
                options: (q.options || []).map((opt: any) => ({
                  id: String(opt.id),
                  text: opt.text || '',
                })),
                marks: q.marks || 2,
                negativeMarks: q.negativeMarks || 0.5,
                subject: q.subject,
                correctOptionId: q.correctOptionId,
                isVisited: idx === 0,
                primaryLanguage: q.primaryLanguage,
                fallbackToEnglish: q.fallbackToEnglish,
                primaryTranslation: q.primaryTranslation,
              }));
              this.questions.set(mapped);
              return;
            }
          }
          this.questions.set(JSON.parse(JSON.stringify(LIVE_QUESTIONS)));
        },
        error: (err) => {
          this.isLoading.set(false);
          if (err?.status === 409) {
            this.isConcurrentConflict.set(true);
            this.sessionError.set(
              'A session for another examination is already active. Please terminate existing sessions before starting.'
            );
          }
          this.questions.set(JSON.parse(JSON.stringify(LIVE_QUESTIONS)));
        },
      });
  }

  terminateActiveAndStart(examId: string, paperId?: string | null): void {
    this.startLiveSession(examId, paperId, true);
  }

  private fallbackToMockQuestions(targetId: string | null): void {
    if (targetId) {
      this.http
        .get<any>(`/api/v1/sessions/paper/${targetId}/questions`)
        .subscribe({
          next: (res) => {
            const list = res?.data ?? res;
            if (Array.isArray(list) && list.length > 0) {
              const mapped: ExamItem[] = list.map((q: any, idx: number) => ({
                id: String(q.id),
                order: idx + 1,
                questionCode: q.code || `PRAC-Q${idx + 1}`,
                content: q.content || q.text || '',
                options: (q.options || []).map((opt: any) => ({
                  id: String(opt.id || opt.optionId),
                  text: opt.text || opt.content || '',
                })),
                marks: q.marks || 2,
                negativeMarks: q.negativeMarks || 0.5,
                subject: q.subject,
                correctOptionId: q.correctOptionId,
                isVisited: idx === 0,
                primaryLanguage: q.primaryLanguage,
                fallbackToEnglish: q.fallbackToEnglish,
                primaryTranslation: q.primaryTranslation,
              }));
              this.questions.set(mapped);
              return;
            }
            this.questions.set(JSON.parse(JSON.stringify(PRACTICE_QUESTIONS)));
          },
          error: () => {
            this.questions.set(JSON.parse(JSON.stringify(PRACTICE_QUESTIONS)));
          },
        });
    } else {
      this.questions.set(JSON.parse(JSON.stringify(PRACTICE_QUESTIONS)));
    }
  }

  startTimer(onExpire?: () => void): void {
    this.stopTimer();
    this.timerInterval = setInterval(() => {
      this.remainingSeconds.update((val) => {
        if (val <= 1) {
          this.stopTimer();
          if (onExpire) {
            onExpire();
          }
          return 0;
        }
        return val - 1;
      });
    }, 1000);
  }

  stopTimer(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
  }

  selectOption(item: ExamItem, optionId: string): void {
    this.questions.update((list) =>
      list.map((q) =>
        q.id === item.id ? { ...q, selectedOptionId: optionId, isVisited: true } : q
      )
    );

    const sId = this.practiceSessionId();
    if (this.deliveryMode() === 'PRACTICE' && sId) {
      this.practiceService.saveResponse(sId, {
        questionId: item.id,
        selectedOptionIds: `[\"${optionId}\"]`,
        enteredValue: null,
        timeSpentMs: 15000,
        markedForReview: !!item.isFlagged,
      }).subscribe({
        error: () => {
          // Response saved in memory fallback
        }
      });
    }
  }

  clearResponse(item: ExamItem): void {
    this.questions.update((list) =>
      list.map((q) => (q.id === item.id ? { ...q, selectedOptionId: undefined } : q))
    );

    const sId = this.practiceSessionId();
    if (this.deliveryMode() === 'PRACTICE' && sId) {
      this.practiceService.saveResponse(sId, {
        questionId: item.id,
        selectedOptionIds: null,
        enteredValue: null,
        timeSpentMs: 10000,
        markedForReview: !!item.isFlagged,
      }).subscribe({
        error: () => {}
      });
    }
  }

  toggleFlag(item: ExamItem): void {
    this.questions.update((list) =>
      list.map((q) => (q.id === item.id ? { ...q, isFlagged: !q.isFlagged } : q))
    );

    const sId = this.practiceSessionId();
    if (this.deliveryMode() === 'PRACTICE' && sId && item.selectedOptionId) {
      this.practiceService.saveResponse(sId, {
        questionId: item.id,
        selectedOptionIds: `[\"${item.selectedOptionId}\"]`,
        enteredValue: null,
        timeSpentMs: 15000,
        markedForReview: !item.isFlagged,
      }).subscribe({
        error: () => {}
      });
    }
  }

  goToQuestion(index: number): void {
    const list = this.questions();
    if (index >= 0 && index < list.length) {
      this.currentIndex.set(index);
      this.questions.update((items) =>
        items.map((q, i) => (i === index ? { ...q, isVisited: true } : q))
      );
    }
  }

  nextQuestion(): void {
    this.goToQuestion(this.currentIndex() + 1);
  }

  prevQuestion(): void {
    this.goToQuestion(this.currentIndex() - 1);
  }

  calculateScoreSummary(): ExamScoreSummary {
    const currentQuestions = this.questions();
    let correctCount = 0;
    let incorrectCount = 0;
    let unansweredCount = 0;
    let calculatedScore = 0;
    let totalMarks = 0;

    for (const q of currentQuestions) {
      totalMarks += q.marks;
      if (q.selectedOptionId) {
        if (q.correctOptionId && q.selectedOptionId === q.correctOptionId) {
          correctCount++;
          calculatedScore += q.marks;
        } else {
          incorrectCount++;
          calculatedScore -= q.negativeMarks;
        }
      } else {
        unansweredCount++;
      }
    }

    return {
      score: Math.max(0, calculatedScore),
      totalMarks,
      correctCount,
      incorrectCount,
      unansweredCount,
    };
  }

  async sealAndSubmit(): Promise<ExamSubmissionReceipt> {
    const mode = this.deliveryMode();
    const currentQuestions = this.questions();
    let scoreSummary = this.calculateScoreSummary();
    const sId = this.practiceSessionId();
    let evalAccuracy: number | undefined;
    let evalSetName: string | undefined;

    if (mode === 'PRACTICE' && sId) {
      try {
        const evalResult = await firstValueFrom(this.practiceService.submitSession(sId));
        if (evalResult) {
          scoreSummary = {
            score: evalResult.obtainedMarks,
            totalMarks: evalResult.totalMarks,
            correctCount: evalResult.correctCount,
            incorrectCount: evalResult.incorrectCount,
            unansweredCount: evalResult.skippedCount,
          };
          evalAccuracy = evalResult.accuracyPercent;
          evalSetName = evalResult.practiceSetName ?? undefined;
        }
      } catch (e) {
        // Fallback to locally calculated summary if backend submit call fails
      }
    }

    const payload = JSON.stringify({
      candidateId: this.sessionMeta().candidateId,
      examId: this.examId() || 'NES-2026-S1',
      mode,
      score: scoreSummary.score,
      answers: currentQuestions.map((q) => ({
        id: q.id,
        selected: q.selectedOptionId || null,
      })),
      timestamp: new Date().toISOString(),
    });

    const shaHash = await hashSha256(payload);
    const simulatedKey = 'candidate-session-private-key-2026';
    const sig = await signSubmissionHash(shaHash, simulatedKey);

    const calculatedAccuracy = currentQuestions.length > 0
      ? Math.round((scoreSummary.correctCount / currentQuestions.length) * 1000) / 10
      : 0;

    const receipt: ExamSubmissionReceipt = {
      signature: sig,
      hash: shaHash,
      timestamp: new Date().toLocaleString(),
      mode,
      score: scoreSummary.score,
      totalMarks: scoreSummary.totalMarks,
      correctCount: scoreSummary.correctCount,
      incorrectCount: scoreSummary.incorrectCount,
      unansweredCount: scoreSummary.unansweredCount,
      totalQuestions: currentQuestions.length,
      accuracyPercent: evalAccuracy ?? calculatedAccuracy,
      practiceSetName: evalSetName || this.sessionMeta().title,
      sessionId: sId || undefined,
    };

    this.submissionReceipt.set(receipt);
    return receipt;
  }

  clearReceipt(): void {
    this.submissionReceipt.set(null);
  }
}
