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
  StatusBadgeComponent,
  StatusVariant,
} from '@nag-frontend-workspace/shared-ui-components';
import {
  AuthoringSubQuestion,
  COGNITIVE_LEVELS,
} from '../../models/authoring.model';

@Component({
  selector: 'nag-passage-subquestion-editor',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, StatusBadgeComponent],
  templateUrl: './passage-subquestion-editor.component.html',
  styleUrl: './passage-subquestion-editor.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PassageSubquestionEditorComponent {
  readonly cognitiveLevels = COGNITIVE_LEVELS;

  readonly subQuestions = input<AuthoringSubQuestion[]>([]);
  readonly activeSubQuestionIndex = input<number>(0);

  readonly subQuestionsChange = output<AuthoringSubQuestion[]>();
  readonly activeSubQuestionIndexChange = output<number>();
  readonly validationError = output<string>();

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

  onAddSubQuestion(): void {
    const list = [...this.subQuestions()];
    if (list.length >= 6) {
      this.validationError.emit('A comprehension passage can contain at most 6 sub-questions.');
      return;
    }
    const nextIdx = list.length + 1;
    list.push({
      passageOrderIndex: nextIdx,
      content: '',
      questionType: 'MULTIPLE_CHOICE',
      difficulty: 'MEDIUM',
      cognitiveLevel: 'UNDERSTAND',
      marks: 4,
      negativeMarks: 1,
      options: [
        { id: 'A', text: '', isCorrect: true },
        { id: 'B', text: '', isCorrect: false },
        { id: 'C', text: '', isCorrect: false },
        { id: 'D', text: '', isCorrect: false },
      ],
      explanation: '',
    });
    this.subQuestionsChange.emit(list);
    this.activeSubQuestionIndexChange.emit(list.length - 1);
  }

  onRemoveSubQuestion(index: number): void {
    const list = [...this.subQuestions()];
    if (list.length <= 2) {
      this.validationError.emit('A passage must contain at least 2 linked sub-questions.');
      return;
    }
    list.splice(index, 1);
    list.forEach((sq, idx) => {
      sq.passageOrderIndex = idx + 1;
    });
    this.subQuestionsChange.emit(list);
    if (this.activeSubQuestionIndex() >= list.length) {
      this.activeSubQuestionIndexChange.emit(Math.max(0, list.length - 1));
    }
  }

  onSubQuestionFieldChange<K extends keyof AuthoringSubQuestion>(
    field: K,
    value: AuthoringSubQuestion[K]
  ): void {
    const list = [...this.subQuestions()];
    const current = { ...list[this.activeSubQuestionIndex()], [field]: value };
    list[this.activeSubQuestionIndex()] = current;
    this.subQuestionsChange.emit(list);
  }

  onAddSubQuestionOption(subQIndex: number): void {
    const list = [...this.subQuestions()];
    const sq = { ...list[subQIndex] };
    const options = [...sq.options];
    if (options.length >= 6) return;
    const nextLetter = this.getOptionLetter(options.length);
    options.push({ id: nextLetter, text: '', isCorrect: false });
    sq.options = options;
    list[subQIndex] = sq;
    this.subQuestionsChange.emit(list);
  }

  onRemoveSubQuestionOption(subQIndex: number, optIndex: number): void {
    const list = [...this.subQuestions()];
    const sq = { ...list[subQIndex] };
    const options = [...sq.options];
    if (options.length <= 2) return;
    options.splice(optIndex, 1);
    options.forEach((opt, idx) => {
      opt.id = this.getOptionLetter(idx);
    });
    sq.options = options;
    list[subQIndex] = sq;
    this.subQuestionsChange.emit(list);
  }

  onToggleSubQuestionCorrect(subQIndex: number, optIndex: number): void {
    const list = [...this.subQuestions()];
    const sq = { ...list[subQIndex] };
    const isSingle =
      sq.questionType === 'MULTIPLE_CHOICE' ||
      sq.questionType === 'SINGLE_MCQ';

    sq.options = sq.options.map((opt, idx) => {
      if (isSingle) {
        return { ...opt, isCorrect: idx === optIndex };
      } else {
        return idx === optIndex ? { ...opt, isCorrect: !opt.isCorrect } : opt;
      }
    });
    list[subQIndex] = sq;
    this.subQuestionsChange.emit(list);
  }

  onSubQuestionOptionTextChange(
    subQIndex: number,
    optIndex: number,
    text: string
  ): void {
    const list = [...this.subQuestions()];
    const sq = { ...list[subQIndex] };
    sq.options = sq.options.map((opt, idx) =>
      idx === optIndex ? { ...opt, text } : opt
    );
    list[subQIndex] = sq;
    this.subQuestionsChange.emit(list);
  }
}
