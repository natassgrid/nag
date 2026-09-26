import {
  ChangeDetectionStrategy,
  Component,
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

  getOptionLetter(index: number): string {
    return String.fromCharCode(65 + index);
  }

  onAddOption(): void {
    const current = [...this.options()];
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
    const isSingle = this.type() === 'MULTIPLE_CHOICE';
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
