import { Injectable, computed, signal } from '@angular/core';
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

@Injectable({
  providedIn: 'root',
})
export class ExamDeliveryService {
  readonly examId = signal<string | null>(null);
  readonly deliveryMode = signal<ExamDeliveryMode>('LIVE');
  readonly sessionMeta = signal<ExamSessionMetadata>({
    sessionId: 'NES-2026-A48',
    candidateId: '849202',
  });

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

  initialize(mode: ExamDeliveryMode = 'LIVE', examId: string | null = null): void {
    this.deliveryMode.set(mode);
    this.examId.set(examId);
    this.currentIndex.set(0);
    this.submissionReceipt.set(null);

    if (mode === 'PRACTICE') {
      this.questions.set(JSON.parse(JSON.stringify(PRACTICE_QUESTIONS)));
      this.sessionMeta.set({
        sessionId: 'MOCK-SESSION-SIM-2026',
        candidateId: 'PRACTICE-CANDIDATE',
        title: 'Official Practice Mock Assessment',
      });
      this.remainingSeconds.set(3600); // 60 minutes for practice
    } else if (mode === 'PREVIEW') {
      this.questions.set(JSON.parse(JSON.stringify(PREVIEW_QUESTIONS)));
      this.sessionMeta.set({
        sessionId: 'PREVIEW-SAMPLE-2026',
        candidateId: 'SAMPLE-GUEST',
        title: 'Examination Structure Preview',
      });
      this.remainingSeconds.set(5400);
    } else {
      this.questions.set(JSON.parse(JSON.stringify(LIVE_QUESTIONS)));
      this.sessionMeta.set({
        sessionId: 'NES-2026-A48',
        candidateId: '849202',
        title: 'National Examination Session',
      });
      this.remainingSeconds.set(5400);
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
  }

  clearResponse(item: ExamItem): void {
    this.questions.update((list) =>
      list.map((q) => (q.id === item.id ? { ...q, selectedOptionId: undefined } : q))
    );
  }

  toggleFlag(item: ExamItem): void {
    this.questions.update((list) =>
      list.map((q) => (q.id === item.id ? { ...q, isFlagged: !q.isFlagged } : q))
    );
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
    const scoreSummary = this.calculateScoreSummary();

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
    };

    this.submissionReceipt.set(receipt);
    return receipt;
  }

  clearReceipt(): void {
    this.submissionReceipt.set(null);
  }
}
