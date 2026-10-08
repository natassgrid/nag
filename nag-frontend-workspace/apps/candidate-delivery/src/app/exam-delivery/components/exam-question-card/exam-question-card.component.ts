import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MathRendererComponent } from '@nag-frontend-workspace/shared-ui-components';
import { ExamItem, ExamOption } from '../../models';

@Component({
  selector: 'nag-exam-question-card',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MathRendererComponent,
  ],
  templateUrl: './exam-question-card.component.html',
  styleUrl: './exam-question-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExamQuestionCardComponent {
  readonly item = input.required<ExamItem>();
  readonly currentIndex = input.required<number>();
  readonly totalQuestions = input.required<number>();

  readonly selectOption = output<string>();
  readonly clearResponse = output<void>();
  readonly toggleFlag = output<void>();
  readonly previous = output<void>();
  readonly next = output<void>();

  getLetter(idx: number): string {
    return String.fromCharCode(65 + idx);
  }

  hasTranslation(): boolean {
    const it = this.item();
    return !!(it.primaryTranslation && it.primaryTranslation.content);
  }

  isFallback(): boolean {
    const it = this.item();
    if (it.fallbackToEnglish) {
      return true;
    }
    return !!(it.primaryLanguage && it.primaryLanguage !== 'en' && !this.hasTranslation());
  }

  getOptionText(opt: ExamOption): string {
    const it = this.item();
    if (it.primaryTranslation?.options) {
      const match = it.primaryTranslation.options.find((o) => o.id === opt.id);
      if (match && match.text) {
        return match.text;
      }
    }
    return opt.text || (opt as any).content || '';
  }

  getEnglishReferenceOptionText(opt: ExamOption): string | null {
    if (this.hasTranslation()) {
      return opt.text || (opt as any).content || null;
    }
    return null;
  }

  onSelectOption(optionId: string): void {
    this.selectOption.emit(optionId);
  }

  onClearResponse(): void {
    this.clearResponse.emit();
  }

  onToggleFlag(): void {
    this.toggleFlag.emit();
  }

  onPrevious(): void {
    this.previous.emit();
  }

  onNext(): void {
    this.next.emit();
  }
}
