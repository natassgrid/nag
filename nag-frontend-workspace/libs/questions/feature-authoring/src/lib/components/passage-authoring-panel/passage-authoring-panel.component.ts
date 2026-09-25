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
  AuthoringSubQuestion,
  COGNITIVE_LEVELS,
} from '../../models/authoring.model';

@Component({
  selector: 'nag-passage-authoring-panel',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatIconModule,
    MathRendererComponent,
    StatusBadgeComponent,
  ],
  templateUrl: './passage-authoring-panel.component.html',
  styleUrl: './passage-authoring-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PassageAuthoringPanelComponent {
  readonly cognitiveLevels = COGNITIVE_LEVELS;

  readonly passageTitle = input<string>('');
  readonly passageContent = input<string>('');
  readonly subQuestions = input<AuthoringSubQuestion[]>([]);
  readonly activeSubQuestionIndex = input<number>(0);

  readonly passageTitleChange = output<string>();
  readonly passageContentChange = output<string>();
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
      explanation: '',
      options: [
        { id: 'A', text: '', isCorrect: true },
        { id: 'B', text: '', isCorrect: false },
        { id: 'C', text: '', isCorrect: false },
        { id: 'D', text: '', isCorrect: false },
      ],
    });
    this.subQuestionsChange.emit(list);
    this.activeSubQuestionIndexChange.emit(list.length - 1);
  }

  onRemoveSubQuestion(index: number): void {
    const list = [...this.subQuestions()];
    if (list.length <= 2) {
      this.validationError.emit('A comprehension passage requires a minimum of 2 sub-questions.');
      return;
    }
    list.splice(index, 1);
    list.forEach((sq, idx) => {
      sq.passageOrderIndex = idx + 1;
    });
    this.subQuestionsChange.emit(list);
    if (this.activeSubQuestionIndex() >= list.length) {
      this.activeSubQuestionIndexChange.emit(list.length - 1);
    }
  }

  onSubQuestionFieldChange(field: keyof AuthoringSubQuestion, value: any): void {
    const currentIdx = this.activeSubQuestionIndex();
    const list = this.subQuestions().map((sq, idx) =>
      idx === currentIdx ? { ...sq, [field]: value } : sq
    );
    this.subQuestionsChange.emit(list);
  }

  onAddSubQuestionOption(sqIndex: number): void {
    const list = this.subQuestions().map((sq, idx) => {
      if (idx !== sqIndex) return sq;
      if (sq.options.length >= 6) return sq;
      const nextOptIdx = sq.options.length;
      return {
        ...sq,
        options: [
          ...sq.options,
          {
            id: this.getOptionLetter(nextOptIdx),
            text: '',
            isCorrect: false,
          },
        ],
      };
    });
    this.subQuestionsChange.emit(list);
  }

  onRemoveSubQuestionOption(sqIndex: number, optIndex: number): void {
    const list = this.subQuestions().map((sq, idx) => {
      if (idx !== sqIndex) return sq;
      if (sq.options.length <= 2) return sq;
      const nextOptions = [...sq.options];
      nextOptions.splice(optIndex, 1);
      nextOptions.forEach((opt, oIdx) => {
        opt.id = this.getOptionLetter(oIdx);
      });
      return { ...sq, options: nextOptions };
    });
    this.subQuestionsChange.emit(list);
  }

  onSubQuestionOptionTextChange(sqIndex: number, optIndex: number, text: string): void {
    const list = this.subQuestions().map((sq, idx) => {
      if (idx !== sqIndex) return sq;
      const nextOptions = sq.options.map((opt, oIdx) =>
        oIdx === optIndex ? { ...opt, text } : opt
      );
      return { ...sq, options: nextOptions };
    });
    this.subQuestionsChange.emit(list);
  }

  onToggleSubQuestionCorrect(sqIndex: number, optIndex: number): void {
    const list = this.subQuestions().map((sq, idx) => {
      if (idx !== sqIndex) return sq;
      const isSingle = sq.questionType === 'MULTIPLE_CHOICE' || sq.questionType === 'SINGLE_MCQ';
      const nextOptions = sq.options.map((opt, oIdx) => {
        if (isSingle) {
          return { ...opt, isCorrect: oIdx === optIndex };
        } else {
          return oIdx === optIndex ? { ...opt, isCorrect: !opt.isCorrect } : opt;
        }
      });
      return { ...sq, options: nextOptions };
    });
    this.subQuestionsChange.emit(list);
  }
}
