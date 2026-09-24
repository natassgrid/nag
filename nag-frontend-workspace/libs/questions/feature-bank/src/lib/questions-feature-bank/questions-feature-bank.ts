import {
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  PageHeaderComponent,
  SearchInputComponent,
  EmptyStateComponent,
} from '@nag-frontend-workspace/shared-ui-components';
import {
  QuestionBankService,
  Question,
  VectorSearchResult,
} from '@nag-frontend-workspace/questions-data-access';
import {
  QuestionsUiQuestionCard,
  QuestionCardData,
} from '@nag-frontend-workspace/questions-ui-question-card';

@Component({
  selector: 'nag-questions-feature-bank',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    PageHeaderComponent,
    SearchInputComponent,
    EmptyStateComponent,
    QuestionsUiQuestionCard,
  ],
  templateUrl: './questions-feature-bank.component.html',
  styleUrl: './questions-feature-bank.component.scss',
})
export class QuestionsFeatureBank implements OnInit {
  private readonly router = inject(Router);
  readonly questionService = inject(QuestionBankService);

  showVectorDrawer = signal<boolean>(false);
  vectorQueryPrompt = '';
  searchingVector = signal<boolean>(false);

  ngOnInit(): void {
    this.questionService.loadQuestions().subscribe();
  }

  onSearchTextChange(query: string): void {
    if (!query) {
      this.questionService.loadQuestions().subscribe();
    } else {
      this.questionService.loadQuestions({ query }).subscribe();
    }
  }

  runVectorSearch(): void {
    if (!this.vectorQueryPrompt.trim()) return;
    this.searchingVector.set(true);
    this.questionService.searchVectorSimilar(this.vectorQueryPrompt).subscribe({
      next: (results: VectorSearchResult[]) => {
        const mappedQuestions: Question[] = results.map((r) => ({
          id: r.id,
          code: r.code,
          content: r.content,
          type: 'MULTIPLE_CHOICE',
          difficulty: r.difficulty,
          status: 'APPROVED',
          marks: 4,
          negativeMarks: 1,
          options: [],
        }));
        this.questionService.questions.set(mappedQuestions);
        this.searchingVector.set(false);
      },
      error: () => this.searchingVector.set(false),
    });
  }

  triggerNewQuestion(): void {
    this.router.navigate(['/questions/authoring']);
  }

  onEditQuestion(card: QuestionCardData): void {
    this.router.navigate(['/questions/authoring'], { queryParams: { id: card.id } });
  }

  onDeleteQuestion(id: string): void {
    if (confirm('Are you sure you want to delete this question?')) {
      this.questionService.deleteQuestion(id).subscribe();
    }
  }

  toCardData(q: Question): QuestionCardData {
    return {
      id: q.id,
      code: q.code,
      type: q.type,
      difficulty: q.difficulty,
      status: q.status,
      content: q.content,
      options: q.options?.map((opt) => ({
        id: opt.id,
        text: opt.text,
        isCorrect: opt.isCorrect,
      })),
      marks: q.marks,
      negativeMarks: q.negativeMarks,
      tags: q.tags,
    };
  }
}
