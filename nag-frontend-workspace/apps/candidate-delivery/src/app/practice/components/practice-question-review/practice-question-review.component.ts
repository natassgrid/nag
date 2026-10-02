import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MathRendererComponent } from '@nag-frontend-workspace/shared-ui-components';
import { QuestionResult } from '../../models';

export type QuestionReviewFilter = 'ALL' | 'CORRECT' | 'INCORRECT' | 'SKIPPED';

export interface ParsedOption {
  id: string;
  text: string;
}

@Component({
  selector: 'app-practice-question-review',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule, MathRendererComponent],
  templateUrl: './practice-question-review.component.html',
  styleUrl: './practice-question-review.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PracticeQuestionReviewComponent {
  readonly questionResults = input.required<QuestionResult[]>();
  readonly activeFilter = signal<QuestionReviewFilter>('ALL');

  readonly totalCount = computed(() => this.questionResults().length);
  readonly correctCount = computed(() => this.questionResults().filter((q) => q.correct).length);
  readonly incorrectCount = computed(
    () => this.questionResults().filter((q) => !q.correct && q.candidateAnswer).length
  );
  readonly skippedCount = computed(
    () => this.questionResults().filter((q) => !q.candidateAnswer).length
  );

  readonly filteredResults = computed(() => {
    const list = this.questionResults();
    const filter = this.activeFilter();
    switch (filter) {
      case 'CORRECT':
        return list.filter((q) => q.correct);
      case 'INCORRECT':
        return list.filter((q) => !q.correct && q.candidateAnswer);
      case 'SKIPPED':
        return list.filter((q) => !q.candidateAnswer);
      case 'ALL':
      default:
        return list;
    }
  });

  setFilter(filter: QuestionReviewFilter): void {
    this.activeFilter.set(filter);
  }

  parseOptions(optionsJson?: string | null): ParsedOption[] {
    if (!optionsJson) return [];
    try {
      const parsed = typeof optionsJson === 'string' ? JSON.parse(optionsJson) : optionsJson;
      if (Array.isArray(parsed)) {
        return parsed.map((opt: any, idx: number) => ({
          id: String(opt.id || opt.optionId || String.fromCharCode(65 + idx)),
          text: opt.text || opt.content || String(opt),
        }));
      }
    } catch (e) {
      // Return empty if parse fails
    }
    return [];
  }

  isOptionSelected(qr: QuestionResult, optId: string): boolean {
    if (!qr.candidateAnswer) return false;
    const ans = qr.candidateAnswer.trim();
    if (ans === optId) return true;
    if (ans.startsWith('[') && ans.endsWith(']')) {
      return ans.includes(optId);
    }
    return false;
  }

  isOptionCorrect(qr: QuestionResult, optId: string): boolean {
    if (!qr.correctAnswer) return false;
    const correct = qr.correctAnswer.trim();
    if (correct === optId) return true;
    if (correct.startsWith('[') && correct.endsWith(']')) {
      return correct.includes(optId);
    }
    return false;
  }
}
