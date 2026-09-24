import {
  Component,
  OnInit,
  OnDestroy,
  inject,
  signal,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  MathRendererComponent,
} from '@nag-frontend-workspace/shared-ui-components';
import {
  hashSha256,
  signSubmissionHash,
} from '@nag-frontend-workspace/shared-util-crypto';
import {
  I18nService,
  SUPPORTED_LANGUAGES,
} from '@nag-frontend-workspace/shared-util-i18n';

export interface ExamItem {
  id: string;
  order: number;
  questionCode: string;
  content: string;
  options: Array<{ id: string; text: string }>;
  marks: number;
  negativeMarks: number;
  selectedOptionId?: string;
  isFlagged?: boolean;
  isVisited?: boolean;
}

@Component({
  selector: 'app-exam-delivery',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MathRendererComponent,
  ],
  templateUrl: './exam-delivery.component.html',
  styleUrl: './exam-delivery.component.scss',
})
export class ExamDeliveryComponent implements OnInit, OnDestroy {
  readonly i18nService = inject(I18nService);
  readonly supportedLanguages = SUPPORTED_LANGUAGES;

  remainingSeconds = signal<number>(5400); // 90 minutes
  private timerInterval: any = null;

  questions = signal<ExamItem[]>([
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

  currentIndex = signal<number>(0);

  currentItem = computed(() => {
    const list = this.questions();
    const idx = this.currentIndex();
    return list[idx] || null;
  });

  submissionReceipt = signal<{
    signature: string;
    hash: string;
    timestamp: string;
  } | null>(null);

  formattedTime = computed(() => {
    const s = this.remainingSeconds();
    const hrs = Math.floor(s / 3600);
    const mins = Math.floor((s % 3600) / 60);
    const secs = s % 60;
    return `${hrs.toString().padStart(2, '0')}:${mins
      .toString()
      .padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  });

  countAnswered = computed(
    () => this.questions().filter((q) => q.selectedOptionId).length
  );
  countFlagged = computed(
    () => this.questions().filter((q) => q.isFlagged).length
  );
  countUnvisited = computed(
    () => this.questions().filter((q) => !q.isVisited && !q.selectedOptionId).length
  );

  ngOnInit(): void {
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

  getLetter(idx: number): string {
    return String.fromCharCode(65 + idx);
  }

  async confirmSubmission(): Promise<void> {
    const answered = this.countAnswered();
    const total = this.questions().length;
    if (
      confirm(
        `You have answered ${answered} of ${total} questions. Are you sure you want to finalize and cryptographically submit your exam?`
      )
    ) {
      await this.performSubmission();
    }
  }

  private async autoSubmit(): Promise<void> {
    alert('Exam timer has expired! System is sealing and submitting responses.');
    await this.performSubmission();
  }

  private async performSubmission(): Promise<void> {
    const payload = JSON.stringify({
      candidateId: '849202',
      examId: 'NES-2026-S1',
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
