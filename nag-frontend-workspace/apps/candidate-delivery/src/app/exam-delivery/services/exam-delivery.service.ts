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
import { I18nService } from '@nag-frontend-workspace/shared-util-i18n';
import { firstValueFrom } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class ExamDeliveryService {
  private readonly http = inject(HttpClient);
  private readonly practiceService = inject(PracticeService);
  private readonly i18nService = inject(I18nService, { optional: true });

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
    sessionId: string | null = null,
    lang?: string | null
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

      const targetLang = (lang || this.i18nService?.currentLanguage() || 'en').toLowerCase();

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
        this.practiceService.getSessionQuestions(sessionId, targetLang).subscribe({
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
                const isEn = targetLang === 'en';
                const hasPrimary = !!q.primaryTranslation;
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
                  primaryLanguage: q.primaryLanguage || targetLang,
                  fallbackToEnglish: q.fallbackToEnglish !== undefined
                    ? q.fallbackToEnglish
                    : (!isEn && !hasPrimary),
                  primaryTranslation: q.primaryTranslation,
                  translations: q.translations,
                };
              });
              this.questions.set(mapped);
              return;
            }
            this.fallbackToMockQuestions(paperId || sessionId, targetLang);
          },
          error: () => {
            this.fallbackToMockQuestions(paperId || sessionId, targetLang);
          }
        });
      } else {
        this.fallbackToMockQuestions(paperId, targetLang);
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

  private fallbackToMockQuestions(targetId: string | null, targetLang = 'en'): void {
    if (targetId) {
      this.http
        .get<any>(`/api/v1/sessions/paper/${targetId}/questions?lang=${targetLang}`)
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
                primaryLanguage: q.primaryLanguage || targetLang,
                fallbackToEnglish: q.fallbackToEnglish !== undefined
                  ? q.fallbackToEnglish
                  : (targetLang !== 'en' && !q.primaryTranslation),
                primaryTranslation: q.primaryTranslation,
                translations: q.translations,
              }));
              this.questions.set(mapped);
              return;
            }
            this.loadMockPracticeQuestions(targetLang);
          },
          error: () => {
            this.loadMockPracticeQuestions(targetLang);
          },
        });
    } else {
      this.loadMockPracticeQuestions(targetLang);
    }
  }

  private loadMockPracticeQuestions(targetLang = 'en'): void {
    const list: ExamItem[] = JSON.parse(JSON.stringify(PRACTICE_QUESTIONS));
    const isEn = targetLang === 'en';
    const mapped = list.map((q) => {
      const translation = q.translations?.[targetLang];
      return {
        ...q,
        primaryLanguage: targetLang,
        fallbackToEnglish: isEn ? false : !translation,
        primaryTranslation: translation,
      };
    });
    this.questions.set(mapped);
  }

  setLanguage(lang: string): void {
    const targetLang = (lang || 'en').toLowerCase();
    this.questions.update((list) =>
      list.map((q) => {
        if (targetLang === 'en') {
          return {
            ...q,
            primaryLanguage: 'en',
            fallbackToEnglish: false,
            primaryTranslation: undefined,
          };
        }
        const translation = q.translations?.[targetLang];
        if (translation) {
          return {
            ...q,
            primaryLanguage: targetLang,
            fallbackToEnglish: false,
            primaryTranslation: translation,
          };
        }
        return {
          ...q,
          primaryLanguage: targetLang,
          fallbackToEnglish: true,
          primaryTranslation: undefined,
        };
      })
    );
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
        timeSpentMs: 15000,
        markedForReview: false,
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
    if (this.deliveryMode() === 'PRACTICE' && sId) {
      this.practiceService.saveResponse(sId, {
        questionId: item.id,
        selectedOptionIds: item.selectedOptionId ? `[\"${item.selectedOptionId}\"]` : null,
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
