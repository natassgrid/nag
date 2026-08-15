import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { QuestionService, QuestionResponse } from './question.service';
import { QuestionFormDialogComponent } from './question-form-dialog.component';
import { SubjectTopicService, Subject } from './subject-topic.service';
import {
  PaginatedTableComponent,
  ColumnDef,
  PaginatedDataFetcher,
  FilterCategory
} from '../../shared/components/paginated-table';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';

@Component({
  selector: 'app-question-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatChipsModule,
    MatTooltipModule,
    PaginatedTableComponent,
    PageHeaderComponent,
    QuestionFormDialogComponent
  ],
  template: `
    <div class="page-layout">
      <app-page-header
        title="Question Bank"
        subtitle="Create, review, and manage examination questions."
        icon="quiz"
      >
        <button mat-raised-button color="primary" (click)="openCreateDrawer()">
          <mat-icon>add</mat-icon>
          Create Question
        </button>
      </app-page-header>

      <!-- Reusable Paginated Table with Filter Drawer -->
      <app-paginated-table
        #paginatedTable
        [fetcher]="fetcher"
        [columns]="columns"
        [filters]="filters"
        [filterCategories]="filterCategories"
        (filterChange)="onFilterChange($event)"
        [actionsTemplate]="actionsTmpl"
        title="Questions List"
        searchPlaceholder="Search questions..."
      ></app-paginated-table>

      <!-- Custom Actions Column Template -->
      <ng-template #actionsTmpl let-row>
        <button mat-icon-button matTooltip="Edit" (click)="openEditDrawer(row)">
          <mat-icon>edit</mat-icon>
        </button>
        <button
          mat-icon-button
          matTooltip="Submit for Review"
          (click)="submitForReview(row)"
          *ngIf="row.state === 'DRAFT'"
        >
          <mat-icon>send</mat-icon>
        </button>
      </ng-template>

      <!-- ── RIGHT COLLAPSIBLE DRAWER FORM ── -->
      <app-question-form-dialog
        [isOpen]="drawerOpen"
        [question]="editingQuestion"
        (close)="onDrawerClose($event)"
      ></app-question-form-dialog>
    </div>
  `,
  styles: [`
    ::ng-deep .chip-easy { background-color: #c8e6c9 !important; }
    ::ng-deep .chip-medium { background-color: #fff9c4 !important; }
    ::ng-deep .chip-hard { background-color: #ffcdd2 !important; }
    ::ng-deep .chip-state-draft { background-color: #e0e0e0 !important; }
    ::ng-deep .chip-state-review { background-color: #bbdefb !important; }
    ::ng-deep .chip-state-approved { background-color: #c8e6c9 !important; }
    ::ng-deep .chip-state-published { background-color: #b2dfdb !important; }
    ::ng-deep .chip-state-archived { background-color: #f5f5f5 !important; }
  `]
})
export class QuestionListComponent implements OnInit {

  @ViewChild('paginatedTable') paginatedTable!: PaginatedTableComponent<QuestionResponse>;

  drawerOpen = false;
  editingQuestion?: QuestionResponse;

  filters: Record<string, any> = {};

  filterCategories: FilterCategory[] = [
    {
      key: 'questionType',
      label: 'Question Type',
      expanded: true,
      options: [
        { label: 'MCQ', value: 'SINGLE_MCQ' },
        { label: 'True / False', value: 'TRUE_FALSE' },
        { label: 'Descriptive', value: 'DESCRIPTIVE' },
        { label: 'Coding', value: 'CODING' }
      ]
    },
    {
      key: 'state',
      label: 'Status',
      expanded: false,
      options: [
        { label: 'Draft', value: 'DRAFT' },
        { label: 'Review', value: 'REVIEW' },
        { label: 'Approved', value: 'APPROVED' },
        { label: 'Published', value: 'PUBLISHED' },
        { label: 'Archived', value: 'ARCHIVED' }
      ]
    },
    {
      key: 'subject',
      label: 'Subject',
      expanded: false,
      options: []
    },
    {
      key: 'difficulty',
      label: 'Difficulty',
      expanded: false,
      options: [
        { label: 'Easy', value: 'EASY' },
        { label: 'Medium', value: 'MEDIUM' },
        { label: 'Hard', value: 'HARD' }
      ]
    },
    {
      key: 'createdAt',
      label: 'Created Date',
      expanded: false,
      options: [
        { label: 'Today', value: 'TODAY' },
        { label: 'Last 7 Days', value: 'LAST_7_DAYS' },
        { label: 'Last 30 Days', value: 'LAST_30_DAYS' }
      ]
    }
  ];

  subjects: Subject[] = [];

  columns: ColumnDef<QuestionResponse>[] = [
    { key: 'subject', header: 'Subject', sortable: true },
    { key: 'topic', header: 'Topic', sortable: true },
    {
      key: 'difficulty',
      header: 'Difficulty',
      type: 'chip',
      chipClass: (val) => 'chip-' + (val || '').toLowerCase(),
      sortable: true
    },
    { key: 'questionType', header: 'Type', sortable: true },
    {
      key: 'state',
      header: 'State',
      type: 'chip',
      chipClass: (val) => 'chip-state-' + (val || '').toLowerCase(),
      sortable: true
    },
    { key: 'createdAt', header: 'Created', type: 'date', sortable: true },
    { key: 'actions', header: 'Actions', type: 'actions' }
  ];

  fetcher: PaginatedDataFetcher<QuestionResponse> = (req) => {
    const activeSubject = Array.isArray(this.filters['subject']) ? this.filters['subject'][0] : this.filters['subject'];
    const activeDifficulty = Array.isArray(this.filters['difficulty']) ? this.filters['difficulty'][0] : this.filters['difficulty'];
    const activeState = Array.isArray(this.filters['state']) ? this.filters['state'][0] : this.filters['state'];

    return this.questionService.getQuestions({
      subject: activeSubject || undefined,
      difficulty: activeDifficulty || undefined,
      state: activeState || undefined,
      page: req.page,
      size: req.size
    });
  };

  constructor(
    private questionService: QuestionService,
    private snackBar: MatSnackBar,
    private subjectTopicService: SubjectTopicService
  ) {}

  ngOnInit(): void {
    this.loadSubjects();
  }

  loadSubjects(): void {
    this.subjectTopicService.getSubjects().subscribe(subjects => {
      this.subjects = subjects;
      const subjectCat = this.filterCategories.find(c => c.key === 'subject');
      if (subjectCat) {
        subjectCat.options = subjects.map(s => ({ label: s.name, value: s.name }));
      }
    });
  }

  onFilterChange(updatedFilters: Record<string, any>): void {
    this.filters = { ...updatedFilters };
  }

  reload(): void {
    this.paginatedTable?.reload();
  }

  openCreateDrawer(): void {
    this.editingQuestion = undefined;
    this.drawerOpen = true;
  }

  openEditDrawer(question: QuestionResponse): void {
    this.editingQuestion = question;
    this.drawerOpen = true;
  }

  onDrawerClose(result: QuestionResponse | null): void {
    this.drawerOpen = false;
    if (result) {
      const msg = this.editingQuestion ? 'Question updated successfully' : 'Question created successfully';
      this.snackBar.open(msg, 'Close', { duration: 3000 });
      this.reload();
    }
  }

  submitForReview(question: QuestionResponse): void {
    this.questionService.submitForReview(question.id).subscribe({
      next: () => {
        this.snackBar.open('Question submitted for review', 'Close', { duration: 3000 });
        this.reload();
      },
      error: (err) => {
        const message = err.error?.message || 'Failed to submit question for review';
        this.snackBar.open(message, 'Close', { duration: 3000 });
      }
    });
  }
}
