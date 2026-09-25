import {
  Component,
  OnInit,
  computed,
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

  selectedSubject = signal<string>('ALL');
  selectedDifficulty = signal<string>('ALL');
  selectedStatus = signal<string>('ALL');
  searchQuery = signal<string>('');

  readonly subjects = [
    'ALL',
    'Quantitative Aptitude / Mathematical Abilities',
    'General Intelligence and Reasoning',
    'English Language and Comprehension',
    'General Awareness',
    'Data Interpretation and Logical Analysis',
  ];

  readonly difficulties = ['ALL', 'EASY', 'MEDIUM', 'HARD'];
  readonly statuses = ['ALL', 'APPROVED', 'REVIEW', 'DRAFT', 'REJECTED'];
  readonly pageSizes = [10, 20, 50, 100];

  readonly showingStart = computed(() => {
    const total = this.questionService.total();
    if (total === 0) return 0;
    return this.questionService.currentPage() * this.questionService.pageSize() + 1;
  });

  readonly showingEnd = computed(() => {
    const total = this.questionService.total();
    return Math.min(
      (this.questionService.currentPage() + 1) * this.questionService.pageSize(),
      total
    );
  });

  readonly hasActiveFilters = computed(() => {
    return (
      !!this.searchQuery() ||
      this.selectedSubject() !== 'ALL' ||
      this.selectedDifficulty() !== 'ALL' ||
      this.selectedStatus() !== 'ALL'
    );
  });

  ngOnInit(): void {
    this.applyFilters(0);
  }

  onSearchTextChange(query: string): void {
    this.searchQuery.set(query);
    this.applyFilters(0);
  }

  onSubjectChange(subject: string): void {
    this.selectedSubject.set(subject);
    this.applyFilters(0);
  }

  onDifficultyChange(difficulty: string): void {
    this.selectedDifficulty.set(difficulty);
    this.applyFilters(0);
  }

  onStatusChange(status: string): void {
    this.selectedStatus.set(status);
    this.applyFilters(0);
  }

  onPageSizeChange(sizeStr: string): void {
    const size = parseInt(sizeStr, 10) || 20;
    this.questionService.filter.update((f) => ({ ...f, size }));
    this.applyFilters(0);
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.questionService.totalPages()) {
      this.applyFilters(page);
    }
  }

  resetFilters(): void {
    this.searchQuery.set('');
    this.selectedSubject.set('ALL');
    this.selectedDifficulty.set('ALL');
    this.selectedStatus.set('ALL');
    this.applyFilters(0);
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
          type: 'SINGLE_MCQ',
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
    this.router.navigate(['/questions/authoring'], {
      queryParams: { id: card.id },
    });
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

  private applyFilters(page = 0): void {
    this.questionService
      .loadQuestions({
        search: this.searchQuery(),
        subject: this.selectedSubject(),
        difficulty: this.selectedDifficulty(),
        state: this.selectedStatus(),
        page,
        size: this.questionService.filter().size || 20,
      })
      .subscribe();
  }
}
