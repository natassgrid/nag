import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { EmptyStateComponent } from '@nag-frontend-workspace/shared-ui-components';
import {
  QuestionsUiQuestionCard,
  QuestionCardData,
} from '@nag-frontend-workspace/questions-ui-question-card';
import { Question } from '@nag-frontend-workspace/questions-data-access';

@Component({
  selector: 'nag-bank-items-list',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    EmptyStateComponent,
    QuestionsUiQuestionCard,
  ],
  templateUrl: './bank-items-list.component.html',
  styleUrl: './bank-items-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BankItemsListComponent {
  readonly questions = input<Question[]>([]);
  readonly loading = input<boolean>(false);
  readonly currentPage = input<number>(0);
  readonly totalPages = input<number>(0);
  readonly pageSize = input<number>(20);
  readonly total = input<number>(0);
  readonly showingStart = input<number>(0);
  readonly showingEnd = input<number>(0);

  readonly pageChange = output<number>();
  readonly editQuestion = output<QuestionCardData>();
  readonly deleteQuestion = output<string>();
  readonly createQuestion = output<void>();

  mapQuestionToCard(q: Question): QuestionCardData {
    return {
      id: q.id,
      code: q.code,
      content: q.content,
      type: q.type,
      difficulty: q.difficulty,
      status: q.status,
      marks: q.marks,
      negativeMarks: q.negativeMarks,
      options: q.options || [],
      tags: q.tags,
    };
  }
}
