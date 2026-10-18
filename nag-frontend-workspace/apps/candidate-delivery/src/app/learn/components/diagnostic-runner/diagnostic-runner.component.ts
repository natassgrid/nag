import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  signal,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MathRendererComponent } from '@nag-frontend-workspace/shared-ui-components';
import {
  DiagnosticSession,
  DiagnosticQuestion,
  DiagnosticSubmitRequest,
  DiagnosticSubmitResult,
} from '../../models/learner.model';

@Component({
  selector: 'app-diagnostic-runner',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MathRendererComponent,
  ],
  templateUrl: './diagnostic-runner.component.html',
  styleUrl: './diagnostic-runner.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DiagnosticRunnerComponent {
  readonly session = input<DiagnosticSession | null>(null);
  readonly submitting = input<boolean>(false);
  readonly result = input<DiagnosticSubmitResult | null>(null);

  readonly submitted = output<DiagnosticSubmitRequest>();
  readonly skipped = output<void>();
  readonly finished = output<void>();

  readonly currentIndex = signal<number>(0);
  readonly answers = signal<Record<string, string>>({});
  readonly questionTimeSpent = signal<Record<string, number>>({});
  readonly showSkipConfirm = signal<boolean>(false);
  private questionEnteredAt = Date.now();

  readonly questions = computed<DiagnosticQuestion[]>(() => {
    return this.session()?.questions || [];
  });

  readonly totalQuestions = computed<number>(() => {
    return this.questions().length;
  });

  readonly currentQuestion = computed<DiagnosticQuestion | null>(() => {
    const list = this.questions();
    const idx = this.currentIndex();
    return list[idx] || null;
  });

  readonly progressPercent = computed<number>(() => {
    const total = this.totalQuestions();
    if (total === 0) return 0;
    return Math.round(((this.currentIndex() + 1) / total) * 100);
  });

  readonly answeredCount = computed<number>(() => {
    return Object.keys(this.answers()).length;
  });

  private flushCurrentQuestionTime(): void {
    const q = this.currentQuestion();
    if (!q) return;
    const now = Date.now();
    const elapsedSec = Math.max(1, Math.round((now - this.questionEnteredAt) / 1000));
    this.questionTimeSpent.update((prev) => ({
      ...prev,
      [q.id]: (prev[q.id] || 0) + elapsedSec,
    }));
    this.questionEnteredAt = now;
  }

  selectOption(optionId: string): void {
    const q = this.currentQuestion();
    if (!q) return;
    this.answers.update((prev) => ({
      ...prev,
      [q.id]: optionId,
    }));
  }

  isOptionSelected(optionId: string): boolean {
    const q = this.currentQuestion();
    if (!q) return false;
    return this.answers()[q.id] === optionId;
  }

  nextQuestion(): void {
    if (this.currentIndex() < this.totalQuestions() - 1) {
      this.flushCurrentQuestionTime();
      this.currentIndex.update((i) => i + 1);
    }
  }

  previousQuestion(): void {
    if (this.currentIndex() > 0) {
      this.flushCurrentQuestionTime();
      this.currentIndex.update((i) => i - 1);
    }
  }

  submitDiagnostic(): void {
    this.flushCurrentQuestionTime();
    const ansMap = this.answers();
    const timeMap = this.questionTimeSpent();
    const payload: DiagnosticSubmitRequest = {
      answers: Object.entries(ansMap).map(([questionId, selectedOptionId]) => ({
        questionId,
        selectedOptionId,
        timeSpentSeconds: timeMap[questionId] || 1,
      })),
    };
    this.submitted.emit(payload);
  }

  confirmSkip(): void {
    this.showSkipConfirm.set(true);
  }

  cancelSkip(): void {
    this.showSkipConfirm.set(false);
  }

  doSkip(): void {
    this.showSkipConfirm.set(false);
    this.skipped.emit();
  }

  onFinish(): void {
    this.finished.emit();
  }
}
