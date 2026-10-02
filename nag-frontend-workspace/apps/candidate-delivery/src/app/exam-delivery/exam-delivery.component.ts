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
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
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

const PREVIEW_QUESTIONS: ExamItem[] = [
  {
    id: 'prev-1',
    order: 1,
    questionCode: 'SAMPLE-NAV-01',
    content:
      '**Sample Orientation Item**: Which section of the NAG candidate runtime environment allows you to monitor unanswered questions and mark items for later review?',
    options: [
      { id: 'opt-a', text: 'Top Proctoring Heartbeat Ribbon' },
      { id: 'opt-b', text: 'Right-hand Question Palette with color-coded item indicators' },
      { id: 'opt-c', text: 'Language selector dropdown' },
      { id: 'opt-d', text: 'Cryptographic hash audit proof ledger' },
    ],
    marks: 4,
    negativeMarks: 0,
    isVisited: true,
  },
  {
    id: 'prev-2',
    order: 2,
    questionCode: 'SAMPLE-APT-02',
    content:
      '**Sample Quantitative Aptitude**: A train traveling at a constant speed of $72\\text{ km/h}$ crosses a $200\\text{ m}$ long station platform in $20\\text{ seconds}$. What is the length of the train in meters?\\n\\n$$\\text{Speed} = 72 \\times \\frac{5}{18} = 20\\text{ m/s}$$',
    options: [
      { id: 'opt-a', text: '$150\\text{ m}$' },
      { id: 'opt-b', text: '$200\\text{ m}$' },
      { id: 'opt-c', text: '$250\\text{ m}$' },
      { id: 'opt-d', text: '$300\\text{ m}$' },
    ],
    marks: 4,
    negativeMarks: 1,
  },
  {
    id: 'prev-3',
    order: 3,
    questionCode: 'SAMPLE-SYS-03',
    content:
      '**Sample Architecture Pattern**: In the zero-trust assessment delivery architecture, candidate responses are periodically sealed using which cryptographic mechanism?',
    options: [
      { id: 'opt-a', text: 'Client LocalStorage base64 encoded strings' },
      { id: 'opt-b', text: 'Tamper-evident SHA-256 digital signature digests' },
      { id: 'opt-c', text: 'Unsigned plaintext JSON web tokens' },
      { id: 'opt-d', text: 'Browser cookie session tracking' },
    ],
    marks: 4,
    negativeMarks: 1,
  },
];

const PRACTICE_QUESTIONS: ExamItem[] = [
  {
    id: 'prac-1',
    order: 1,
    questionCode: 'PRAC-ALGO-101',
    content:
      'What is the tightest worst-case asymptotic time complexity of building a Max-Heap from an unsorted array of $n$ elements using Floyd’s linear build-heap algorithm?\\n\\n$$\\sum_{h=0}^{\\lfloor \\lg n \\rfloor} \\left\\lceil \\frac{n}{2^{h+1}} \\right\\rceil O(h) = O(n)$$',
    options: [
      { id: 'opt-a', text: '$O(n \\log n)$' },
      { id: 'opt-b', text: '$O(n)$' },
      { id: 'opt-c', text: '$O(\\log n)$' },
      { id: 'opt-d', text: '$O(n^2)$' },
    ],
    marks: 4,
    negativeMarks: 1,
    correctOptionId: 'opt-b',
    isVisited: true,
  },
  {
    id: 'prac-2',
    order: 2,
    questionCode: 'PRAC-MATH-202',
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
    correctOptionId: 'opt-a',
  },
  {
    id: 'prac-3',
    order: 3,
    questionCode: 'PRAC-SYS-305',
    content:
      'In an operating system with a 32-bit virtual address space and a $4\\text{ KB}$ page size, calculate the number of page entries required in a single-level page table.\\n\\n$$\\text{Number of Pages} = \\frac{2^{32}}{2^{12}} = 2^{20}$$',
    options: [
      { id: 'opt-a', text: '$2^{10} = 1,024$' },
      { id: 'opt-b', text: '$2^{20} = 1,048,576$' },
      { id: 'opt-c', text: '$2^{12} = 4,096$' },
      { id: 'opt-d', text: '$2^{32} = 4,294,967,296$' },
    ],
    marks: 4,
    negativeMarks: 1,
    correctOptionId: 'opt-b',
  },
  {
    id: 'prac-4',
    order: 4,
    questionCode: 'PRAC-DS-404',
    content:
      'Which data structure provides amortized $O(1)$ time complexity for both `find` and `union` operations with path compression and rank heuristics?',
    options: [
      { id: 'opt-a', text: 'Disjoint Set Union (Union-Find)' },
      { id: 'opt-b', text: 'Fibonacci Heap' },
      { id: 'opt-c', text: 'Red-Black Balanced Search Tree' },
      { id: 'opt-d', text: 'Trie Data Structure' },
    ],
    marks: 4,
    negativeMarks: 1,
    correctOptionId: 'opt-a',
  },
  {
    id: 'prac-5',
    order: 5,
    questionCode: 'PRAC-NET-505',
    content:
      'In the TCP/IP protocol suite, which congestion control mechanism dynamically adjusts the congestion window (CWND) size upon encountering triple duplicate ACKs?',
    options: [
      { id: 'opt-a', text: 'Slow Start Exponential Growth' },
      { id: 'opt-b', text: 'Fast Retransmit and Fast Recovery' },
      { id: 'opt-c', text: 'Nagle Algorithm Packet Coalescing' },
      { id: 'opt-d', text: 'Sliding Window Flow Control' },
    ],
    marks: 4,
    negativeMarks: 1,
    correctOptionId: 'opt-b',
  },
];

const LIVE_QUESTIONS: ExamItem[] = [
  {
    id: 'live-1',
    order: 1,
    questionCode: 'NES-ALGO-101',
    content:
      'What is the tightest worst-case asymptotic time complexity of building a Max-Heap from an unsorted array of $n$ elements using Floyd’s algorithm?\\n\\n$$\\sum_{h=0}^{\\lfloor \\lg n \\rfloor} \\left\\lceil \\frac{n}{2^{h+1}} \\right\\rceil O(h) = O(n)$$',
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
    id: 'live-2',
    order: 2,
    questionCode: 'NES-MATH-202',
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
    id: 'live-3',
    order: 3,
    questionCode: 'NES-SYS-305',
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
];

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
  readonly router = inject(Router);
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

  readonly questions = signal<ExamItem[]>(LIVE_QUESTIONS);
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
        this.questions.set(JSON.parse(JSON.stringify(PRACTICE_QUESTIONS)));
        this.sessionMeta.set({
          sessionId: 'MOCK-SESSION-SIM-2026',
          candidateId: 'PRACTICE-CANDIDATE',
          title: 'Official Practice Mock Assessment',
        });
      } else if (modeParam === 'PREVIEW') {
        this.deliveryMode.set('PREVIEW');
        this.questions.set(JSON.parse(JSON.stringify(PREVIEW_QUESTIONS)));
        this.sessionMeta.set({
          sessionId: 'PREVIEW-SAMPLE-2026',
          candidateId: 'SAMPLE-GUEST',
          title: 'Examination Structure Preview',
        });
      } else {
        this.deliveryMode.set('LIVE');
        this.questions.set(JSON.parse(JSON.stringify(LIVE_QUESTIONS)));
        this.sessionMeta.set({
          sessionId: 'NES-2026-A48',
          candidateId: '849202',
          title: 'National Examination Session',
        });
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
    const mode = this.deliveryMode();

    if (mode === 'PREVIEW') {
      const confirmed = await this.notificationService.confirm({
        title: 'Exit Examination Preview?',
        message: 'You are currently in Preview Mode. You can return to your dashboard or browse catalog anytime.',
        confirmText: 'Exit to Dashboard',
        cancelText: 'Stay in Preview',
        type: 'info',
      });
      if (confirmed) {
        this.router.navigate(['/dashboard']);
      }
      return;
    }

    const answered = this.countAnswered();
    const total = this.questions().length;
    const isMock = mode === 'PRACTICE';

    const confirmed = await this.notificationService.confirm({
      title: isMock ? 'Complete Practice Mock Session?' : 'Finalize and Submit Exam Responses?',
      message: `You have answered ${answered} of ${total} questions.\n${isMock ? 'Your practice score and instant review breakdown will be generated.' : 'Once submitted, your responses will be cryptographically hashed, sealed, and cannot be modified.'}`,
      confirmText: isMock ? 'Submit Practice Mock' : 'Submit & Seal Exam',
      cancelText: 'Return to Test',
      type: isMock ? 'info' : 'warning',
    });

    if (confirmed) {
      await this.performSubmission();
    }
  }

  private async autoSubmit(): Promise<void> {
    if (this.deliveryMode() === 'PREVIEW') {
      return;
    }
    this.notificationService.warning(
      'Exam Timer Expired',
      'The allocated examination duration has ended. The system is sealing and submitting your responses.',
      6000
    );
    await this.performSubmission();
  }

  private async performSubmission(): Promise<void> {
    const mode = this.deliveryMode();
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

    const payload = JSON.stringify({
      candidateId: this.sessionMeta().candidateId,
      examId: this.examId() || 'NES-2026-S1',
      mode,
      score: calculatedScore,
      answers: currentQuestions.map((q) => ({
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
      mode,
      score: Math.max(0, calculatedScore),
      totalMarks,
      correctCount,
      incorrectCount,
      unansweredCount,
    });
  }
}
