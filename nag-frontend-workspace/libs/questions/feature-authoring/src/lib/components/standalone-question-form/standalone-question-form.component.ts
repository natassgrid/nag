import {
  Component,
  input,
  output,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import {
  MathRendererComponent,
  StatusBadgeComponent,
  StatusVariant,
} from '@nag-frontend-workspace/shared-ui-components';
import {
  DifficultyLevel,
  QuestionType,
  QuestionOption,
} from '@nag-frontend-workspace/questions-data-access';
import { COGNITIVE_LEVELS } from '../../models/authoring.model';

@Component({
  selector: 'nag-standalone-question-form',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatIconModule,
    MathRendererComponent,
    StatusBadgeComponent,
  ],
  templateUrl: './standalone-question-form.component.html',
  styleUrl: './standalone-question-form.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StandaloneQuestionFormComponent {
  readonly cognitiveLevels = COGNITIVE_LEVELS;

  readonly cognitiveLevel = input<string>('UNDERSTAND');
  readonly difficulty = input<DifficultyLevel>('MEDIUM');
  readonly type = input<QuestionType>('MULTIPLE_CHOICE');
  readonly marks = input<number>(4);
  readonly negativeMarks = input<number>(1);
  readonly content = input<string>('');
  readonly explanation = input<string>('');
  readonly options = input<QuestionOption[]>([]);

  readonly cognitiveLevelChange = output<string>();
  readonly difficultyChange = output<DifficultyLevel>();
  readonly typeChange = output<QuestionType>();
  readonly marksChange = output<number>();
  readonly negativeMarksChange = output<number>();
  readonly contentChange = output<string>();
  readonly explanationChange = output<string>();
  readonly optionsChange = output<QuestionOption[]>();

  difficultyVariant(diff: string): StatusVariant {
    switch (diff) {
      case 'EASY':
        return 'success';
      case 'MEDIUM':
        return 'warn';
      case 'HARD':
      case 'EXPERT':
        return 'error';
      default:
        return 'neutral';
    }
  }

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
    const currentType = this.type();
    const current = this.options().map((opt, idx) => {
      if (currentType === 'MULTIPLE_CHOICE' || currentType === 'SINGLE_MCQ') {
        return { ...opt, isCorrect: idx === index };
      } else {
        return idx === index ? { ...opt, isCorrect: !opt.isCorrect } : opt;
      }
    });
    this.optionsChange.emit(current);
  }
}
