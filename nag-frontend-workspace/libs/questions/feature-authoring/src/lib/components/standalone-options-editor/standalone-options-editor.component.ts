import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import {
  QuestionOption,
  QuestionType,
} from '@nag-frontend-workspace/questions-data-access';

@Component({
  selector: 'nag-standalone-options-editor',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule],
  templateUrl: './standalone-options-editor.component.html',
  styleUrl: './standalone-options-editor.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StandaloneOptionsEditorComponent {
  readonly options = input<QuestionOption[]>([]);
  readonly type = input<QuestionType>('MULTIPLE_CHOICE');

  readonly optionsChange = output<QuestionOption[]>();

  readonly isSingleChoice = computed(() => {
    const t = this.type();
    return t === 'MULTIPLE_CHOICE' || t === 'SINGLE_MCQ';
  });

  readonly isMultipleChoice = computed(() => {
    const t = this.type();
    return t === 'MULTIPLE_SELECT' || t === 'MULTI_MCQ' || t === 'MULTIPLE_MCQ';
  });

  readonly isOptionsQuestion = computed(() => {
    return this.isSingleChoice() || this.isMultipleChoice();
  });

  readonly correctCount = computed(() => {
    return this.options().filter((o) => o.isCorrect).length;
  });

  readonly hasEmptyOption = computed(() => {
    return this.options().some((o) => !o.text || !o.text.trim());
  });

  readonly validationMessage = computed(() => {
    if (!this.isOptionsQuestion()) return null;
    const opts = this.options();
    if (opts.length < 2) {
      return 'At least 2 options are required.';
    }
    const count = this.correctCount();
    if (this.isSingleChoice()) {
      if (count === 0) return 'Please mark exactly one option as the correct answer.';
      if (count > 1) return 'Single correct MCQ cannot have multiple correct answers.';
    } else if (this.isMultipleChoice()) {
      if (count === 0) return 'Please mark at least one option as a correct answer.';
    }
    if (this.hasEmptyOption()) {
      return 'All options must have non-empty text or formula.';
    }
    return null;
  });

  getOptionLetter(index: number): string {
    return String.fromCharCode(65 + index);
  }

  onAddOption(): void {
    const current = [...this.options()];
    if (current.length >= 6) return;
    const nextIdx = current.length;
    current.push({
      id: this.getOptionLetter(nextIdx),
      text: '',
      isCorrect: false,
    });
    this.optionsChange.emit(current);
  }

  onRemoveOption(index: number): void {
    const current = [...this.options()];
    if (current.length > 2) {
      current.splice(index, 1);
      current.forEach((opt, idx) => {
        opt.id = this.getOptionLetter(idx);
      });
      this.optionsChange.emit(current);
    }
  }

  onOptionTextChange(index: number, text: string): void {
    const current = this.options().map((opt, idx) =>
      idx === index ? { ...opt, text } : opt
    );
    this.optionsChange.emit(current);
  }

  onToggleCorrect(index: number): void {
    const isSingle = this.isSingleChoice();
    const current = this.options().map((opt, idx) => {
      if (isSingle) {
        return { ...opt, isCorrect: idx === index };
      } else {
        return idx === index ? { ...opt, isCorrect: !opt.isCorrect } : opt;
      }
    });
    this.optionsChange.emit(current);
  }
}
