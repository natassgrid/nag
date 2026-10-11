import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MathRendererComponent } from '@nag-frontend-workspace/shared-ui-components';
import { I18nService } from '@nag-frontend-workspace/shared-util-i18n';
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
  private readonly i18nService = inject(I18nService, { optional: true });

  readonly questionResults = input.required<QuestionResult[]>();
  readonly preferredLanguage = input<string>();
  readonly selectedLanguage = input<string>();
  readonly activeFilter = signal<QuestionReviewFilter>('ALL');

  readonly effectiveLanguage = computed(() => {
    return this.selectedLanguage() || this.preferredLanguage() || this.i18nService?.currentLanguage() || 'en';
  });

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

  hasTranslation(qr: QuestionResult): boolean {
    const lang = (qr.primaryLanguage || this.effectiveLanguage()).toLowerCase();
    if (lang === 'en') return false;
    if (qr.primaryTranslation && qr.primaryTranslation.content) return true;
    if (qr.translations && qr.translations[lang]?.content) return true;
    return false;
  }

  isBilingual(qr: QuestionResult): boolean {
    return this.hasTranslation(qr);
  }

  isFallback(qr: QuestionResult): boolean {
    const lang = (qr.primaryLanguage || this.effectiveLanguage()).toLowerCase();
    if (lang === 'en') return false;
    if (qr.fallbackToEnglish) return true;
    return !this.hasTranslation(qr);
  }

  getPrimaryContent(qr: QuestionResult): string {
    const lang = (qr.primaryLanguage || this.effectiveLanguage()).toLowerCase();
    if (qr.primaryTranslation?.content) {
      return qr.primaryTranslation.content;
    }
    if (qr.translations && qr.translations[lang]?.content) {
      return qr.translations[lang].content;
    }
    return qr.content || 'Practice Question';
  }

  getDisplayContent(qr: QuestionResult): string {
    return this.getPrimaryContent(qr);
  }

  getBaselineEnglishContent(qr: QuestionResult): string {
    return qr.content || '';
  }

  getOptionText(qr: QuestionResult, opt: ParsedOption): string {
    const lang = (qr.primaryLanguage || this.effectiveLanguage()).toLowerCase();
    if (qr.primaryTranslation?.options) {
      const match = qr.primaryTranslation.options.find(
        (o) => o.id.toLowerCase() === opt.id.toLowerCase()
      );
      if (match && match.text) {
        return match.text;
      }
    } else if (qr.translations && qr.translations[lang]?.options) {
      const match = qr.translations[lang].options?.find(
        (o) => o.id.toLowerCase() === opt.id.toLowerCase()
      );
      if (match && match.text) {
        return match.text;
      }
    }
    return opt.text;
  }

  getEnglishReferenceOptionText(qr: QuestionResult, opt: ParsedOption): string | null {
    if (this.hasTranslation(qr)) {
      return opt.text || null;
    }
    return null;
  }

  getResolvedOptions(qr: QuestionResult): Array<{ id: string; text: string; englishText?: string | null }> {
    return this.parseOptions(qr.optionsJson).map((opt) => ({
      id: opt.id,
      text: this.getOptionText(qr, opt),
      englishText: this.getEnglishReferenceOptionText(qr, opt),
    }));
  }

  hasTranslatedExplanation(qr: QuestionResult): boolean {
    const lang = (qr.primaryLanguage || this.effectiveLanguage()).toLowerCase();
    if (lang === 'en') return false;
    if (qr.primaryTranslation && qr.primaryTranslation.explanation) return true;
    if (qr.translations && qr.translations[lang]?.explanation) return true;
    return false;
  }

  getTranslatedExplanation(qr: QuestionResult): string | null {
    const lang = (qr.primaryLanguage || this.effectiveLanguage()).toLowerCase();
    if (qr.primaryTranslation?.explanation) {
      return qr.primaryTranslation.explanation;
    }
    if (qr.translations && qr.translations[lang]?.explanation) {
      return qr.translations[lang].explanation;
    }
    return null;
  }

  getDisplayExplanation(qr: QuestionResult): string | null {
    return this.getTranslatedExplanation(qr) || qr.explanation || null;
  }

  getBaselineEnglishExplanation(qr: QuestionResult): string | null {
    return qr.explanation || null;
  }
}
