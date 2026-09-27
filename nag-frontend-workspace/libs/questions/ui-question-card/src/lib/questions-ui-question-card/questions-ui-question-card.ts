import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import {
  MathRendererComponent,
  StatusBadgeComponent,
  StatusVariant,
} from '@nag-frontend-workspace/shared-ui-components';

export interface QuestionCardOption {
  id: string;
  text: string;
  isCorrect?: boolean;
}

export interface QuestionCardData {
  id: string;
  code?: string;
  content: string;
  type?: string;
  difficulty?: string;
  status?: string;
  marks?: number;
  negativeMarks?: number;
  options?: QuestionCardOption[];
  tags?: string[];
}

@Component({
  selector: 'nag-question-card',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    MathRendererComponent,
    StatusBadgeComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './questions-ui-question-card.component.html',
  styleUrl: './questions-ui-question-card.component.scss',
})
export class QuestionsUiQuestionCard {
  data = input.required<QuestionCardData>();
  showActions = input<boolean>(true);
  showCorrectOption = input<boolean>(false);
  selected = input<boolean>(false);

  cardEdit = output<QuestionCardData>();
  cardDelete = output<string>();
  cardSelect = output<QuestionCardData>();

  difficultyVariant(): StatusVariant {
    const diff = (this.data().difficulty || '').toUpperCase();
    if (diff === 'EASY') return 'success';
    if (diff === 'MEDIUM') return 'warn';
    if (diff === 'HARD') return 'error';
    return 'neutral';
  }

  formatType(type?: string): string {
    if (!type) return '';
    return type.replace(/_/g, ' ').toLowerCase();
  }

  getOptionLabel(idx: number): string {
    return String.fromCharCode(65 + idx);
  }

  onEditClicked(e: Event): void {
    e.stopPropagation();
    this.cardEdit.emit(this.data());
  }

  onDeleteClicked(e: Event): void {
    e.stopPropagation();
    this.cardDelete.emit(this.data().id);
  }
}
