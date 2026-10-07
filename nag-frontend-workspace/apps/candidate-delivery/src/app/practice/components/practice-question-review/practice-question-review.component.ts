import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MathRendererComponent } from '@nag-frontend-workspace/shared-ui-components';
import { QuestionResult } from '../../models';

export type QuestionReviewFilter = 'ALL' | 'CORRECT' | 'INCORRECT' | 'SKIPPED' | 'FLAGGED';

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
  readonly flaggedCount = computed(
    () => this.questionResults().filter((q) => q.markedForReview).length
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
      case 'FLAGGED':
        return list.filter((q) => q.markedForReview);
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

  parseOptionIds(value?: string | null): Set<string> {
    if (!value) return new Set();
    const trimmed = value.trim();
    const set = new Set<string>();
    if (trimmed.startsWith('[') && trimmed.endsWith(']')) {
      try {
        const parsed = JSON.parse(trimmed);
        if (Array.isArray(parsed)) {
          parsed.forEach((item) => set.add(String(item).trim().toLowerCase()));
          return set;
        }
      } catch (e) {
        // Fallback below
      }
      trimmed.slice(1, -1).split(',').forEach((p) => {
        const clean = p.replace(/["']/g, '').trim().toLowerCase();
        if (clean) set.add(clean);
      });
      return set;
    }
    for (const part of trimmed.split(',')) {
      const clean = part.replace(/["']/g, '').trim().toLowerCase();
      if (clean) set.add(clean);
    }
    return set;
  }

  isOptionSelected(qr: QuestionResult, optId: string): boolean {
    if (!qr.candidateAnswer) return false;
    const selectedSet = this.parseOptionIds(qr.candidateAnswer);
    return selectedSet.has(optId.trim().toLowerCase());
  }

  isOptionCorrect(qr: QuestionResult, optId: string): boolean {
    if (!qr.correctAnswer) return false;
    const correctSet = this.parseOptionIds(qr.correctAnswer);
    return correctSet.has(optId.trim().toLowerCase());
  }

  getQuestionTypeLabel(qr: QuestionResult): string {
    const t = (qr.questionType || '').toUpperCase().trim();
    if (t === 'MULTI_MCQ' || t === 'MCQ_MULTI') return 'Multiple Choice (Multi-Select)';
    if (t === 'NUMERICAL' || t === 'NUMERIC') return 'Numerical Value';
    if (t === 'SUBJECTIVE' || t === 'DESCRIPTIVE') return 'Subjective / Descriptive';
    if (t === 'SINGLE_MCQ' || t === 'MCQ_SINGLE') return 'Multiple Choice (Single-Select)';
    if (this.parseOptions(qr.optionsJson).length > 0) return 'Multiple Choice';
    if (qr.candidateAnswer || qr.correctAnswer) return 'Numerical / Direct Value';
    return 'Question';
  }

  isNumericalOrDirect(qr: QuestionResult): boolean {
    const t = (qr.questionType || '').toUpperCase().trim();
    if (t === 'NUMERICAL' || t === 'NUMERIC' || t === 'SUBJECTIVE' || t === 'DESCRIPTIVE') return true;
    return this.parseOptions(qr.optionsJson).length === 0;
  }
}
