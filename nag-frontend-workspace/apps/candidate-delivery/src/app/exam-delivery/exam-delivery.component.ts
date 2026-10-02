import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  OnDestroy,
  inject,
  signal,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import {
  hashSha256,
  signSubmissionHash,
} from '@nag-frontend-workspace/shared-util-crypto';
import {
  I18nService,
  SUPPORTED_LANGUAGES,
} from '@nag-frontend-workspace/shared-util-i18n';
import { NotificationService } from '@nag-frontend-workspace/shared-ui-components';
import {
  ExamItem,
  ExamSubmissionReceipt,
  ExamSessionMetadata,
} from './models';
import {
  ExamRuntimeHeaderComponent,
  ExamQuestionCardComponent,
  ExamQuestionPaletteComponent,
  ExamSubmissionModalComponent,
} from './components';

export * from './models';

@Component({
  selector: 'app-exam-delivery',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatIconModule,
    MatButtonModule,
    ExamRuntimeHeaderComponent,
    ExamQuestionCardComponent,
    ExamQuestionPaletteComponent,
    ExamSubmissionModalComponent,
  ],
  templateUrl: './exam-delivery.component.html',
  styleUrl: './exam-delivery.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExamDeliveryComponent implements OnInit, OnDestroy {
  readonly route = inject(ActivatedRoute);
  readonly i18nService = inject(I18nService);
  private readonly notificationService = inject(NotificationService);
  readonly supportedLanguages = SUPPORTED_LANGUAGES;

  readonly examId = signal<string | null>(null);
  readonly deliveryMode = signal<'LIVE' | 'PRACTICE' | 'PREVIEW'>('LIVE');

  readonly sessionMeta = signal<ExamSessionMetadata>({
    sessionId: 'NES-2026-A48',
    candidateId: '849202',
  });

  readonly remainingSeconds = signal<number>(5400); // 90 minutes
  private timerInterval: any = null;

  readonly questions = signal<ExamItem[]>([
    {
      id: 'q-1',
      order: 1,
      questionCode: 'CS-ALGO-101',
      content:
        'What is the tightest worst-case asymptotic time complexity of building a Max-Heap from an unsorted array of $n$ elements using Floyd\'s algorithm?\\n\\n$$\\sum_{h=0}^{\\lfloor \\lg n \\rfloor} \\left\\lceil \\frac{n}{2^{h+1}} \\right\\rceil O(h) = O(n)$$',
      options: [
        { id: 'opt-a', text: '$O(n \\log n)$' },
        { id: 'opt-b', text: '$O(n)$' },
        { id: 'opt-c', text: '$O(\\log n)$' },
        { id: 'opt-d', text: '$O(n^2)$' },
      ],
      marks: 4,
      negativeMarks: 1,
      isVisited: true,
    },
    {
      id: 'q-2',
      order: 2,
      questionCode: 'CS-MATH-202',
      content:
        'Compute the determinant of the $2 \\times 2$ covariance matrix given by:\\n\\n$$\\mathbf{\\Sigma} = \\begin{pmatrix} 4 & 2 \\\\ 2 & 3 \\end{pmatrix}$$',
      options: [
        { id: 'opt-a', text: '$8$' },
        { id: 'opt-b', text: '$12$' },
        { id: 'opt-c', text: '$10$' },
        { id: 'opt-d', text: '$16$' },
      ],
      marks: 4,
      negativeMarks: 1,
    },
    {
      id: 'q-3',
      order: 3,
      questionCode: 'CS-SYS-305',
      content:
        'In an operating system with a 32-bit virtual address space and a 4 KB page size, calculate the number of entries in a single-level page table.',
      options: [
        { id: 'opt-a', text: '$2^{10} = 1,024$' },
        { id: 'opt-b', text: '$2^{20} = 1,048,576$' },
        { id: 'opt-c', text: '$2^{12} = 4,096$' },
        { id: 'opt-d', text: '$2^{32} = 4,294,967,296$' },
      ],
      marks: 4,
      negativeMarks: 1,
    },
  ]);

  readonly currentIndex = signal<number>(0);

  readonly currentItem = computed(() => {
    const list = this.questions();
    const idx = this.currentIndex();
    return list[idx] || null;
  });

  readonly submissionReceipt = signal<ExamSubmissionReceipt | null>(null);

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

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      const modeParam = (params['mode'] || '').toUpperCase();
      if (modeParam === 'PRACTICE') {
        this.deliveryMode.set('PRACTICE');
        this.sessionMeta.set({
          sessionId: 'MOCK-SESSION-SIM-2026',
          candidateId: 'PRACTICE-CANDIDATE',
        });
      } else if (modeParam === 'PREVIEW') {
        this.deliveryMode.set('PREVIEW');
      } else {
        this.deliveryMode.set('LIVE');
      }

      if (params['examId']) {
        this.examId.set(params['examId']);
      }
    });

    this.timerInterval = setInterval(() => {
      this.remainingSeconds.update((val) => {
        if (val <= 1) {
          clearInterval(this.timerInterval);
          this.autoSubmit();
          return 0;
        }
        return val - 1;
      });
    }, 1000);
  }

  ngOnDestroy(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
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
    if (index >= 0 && index < this.questions().length) {
      this.currentIndex.set(index);
      this.questions.update((list) =>
        list.map((q, i) => (i === index ? { ...q, isVisited: true } : q))
      );
    }
  }

  nextQuestion(): void {
    this.goToQuestion(this.currentIndex() + 1);
  }

  prevQuestion(): void {
    this.goToQuestion(this.currentIndex() - 1);
  }

  async confirmSubmission(): Promise<void> {
    const answered = this.countAnswered();
    const total = this.questions().length;
    const isMock = this.deliveryMode() === 'PRACTICE';
    const confirmed = await this.notificationService.confirm({
      title: isMock ? 'Complete Practice Mock Session?' : 'Finalize and Submit Exam Responses?',
      message: `You have answered ${answered} of ${total} questions.\n${isMock ? 'Your practice score and instant review will be generated.' : 'Once submitted, your responses will be cryptographically hashed, sealed, and cannot be modified.'}`,
      confirmText: isMock ? 'Submit Practice Mock' : 'Submit & Seal Exam',
      cancelText: 'Return to Test',
      type: isMock ? 'info' : 'warning',
    });

    if (confirmed) {
      await this.performSubmission();
    }
  }

  private async autoSubmit(): Promise<void> {
    this.notificationService.warning(
      'Exam Timer Expired',
      'The allocated examination duration has ended. The system is sealing and submitting your responses.',
      6000
    );
    await this.performSubmission();
  }

  private async performSubmission(): Promise<void> {
    const payload = JSON.stringify({
      candidateId: this.sessionMeta().candidateId,
      examId: this.examId() || 'NES-2026-S1',
      mode: this.deliveryMode(),
      answers: this.questions().map((q) => ({
        id: q.id,
        selected: q.selectedOptionId || null,
      })),
      timestamp: new Date().toISOString(),
    });

    const shaHash = await hashSha256(payload);
    const simulatedKey = 'candidate-session-private-key-2026';
    const sig = await signSubmissionHash(shaHash, simulatedKey);

    this.submissionReceipt.set({
      signature: sig,
      hash: shaHash,
      timestamp: new Date().toLocaleString(),
    });
  }
}
