import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { QuestionService, QuestionResponse } from './question.service';
import { QuestionFormDialogComponent, QuestionFormDialogData } from './question-form-dialog.component';
import { SubjectTopicService, Subject } from './subject-topic.service';
import {
  PaginatedTableComponent,
  ColumnDef,
  PaginatedDataFetcher
} from '../../shared/components/paginated-table';

@Component({
  selector: 'app-question-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatSnackBarModule,
    MatCardModule,
    MatChipsModule,
    MatTooltipModule,
    MatAutocompleteModule,
    PaginatedTableComponent
  ],
  template: `
    <div class="question-list-container">
      <mat-card>
        <mat-card-header>
          <mat-card-title>Question Bank</mat-card-title>
        </mat-card-header>
        <mat-card-content>

          <!-- Filters -->
          <div class="filters-row">
            <mat-form-field appearance="outline">
              <mat-label>Subject</mat-label>
              <input
                matInput
                [(ngModel)]="filters.subject"
                [matAutocomplete]="subjectAuto"
                (ngModelChange)="filterSubjects($event)"
                (blur)="applyFilters()"
                placeholder="Type to search..."
              />
              <mat-autocomplete #subjectAuto="matAutocomplete" (optionSelected)="applyFilters()">
                <mat-option value="">All Subjects</mat-option>
                <mat-option *ngFor="let s of filteredSubjects" [value]="s.name">{{ s.name }}</mat-option>
              </mat-autocomplete>
              <button
                *ngIf="filters.subject"
                mat-icon-button
                matSuffix
                (click)="filters.subject=''; applyFilters()"
                aria-label="Clear"
              >
                <mat-icon>close</mat-icon>
              </button>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Difficulty</mat-label>
              <mat-select [(ngModel)]="filters.difficulty" (selectionChange)="applyFilters()">
                <mat-option value="">All</mat-option>
                <mat-option value="EASY">Easy</mat-option>
                <mat-option value="MEDIUM">Medium</mat-option>
                <mat-option value="HARD">Hard</mat-option>
              </mat-select>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>State</mat-label>
              <mat-select [(ngModel)]="filters.state" (selectionChange)="applyFilters()">
                <mat-option value="">All</mat-option>
                <mat-option value="DRAFT">Draft</mat-option>
                <mat-option value="REVIEW">Review</mat-option>
                <mat-option value="APPROVED">Approved</mat-option>
                <mat-option value="PUBLISHED">Published</mat-option>
                <mat-option value="ARCHIVED">Archived</mat-option>
              </mat-select>
            </mat-form-field>
          </div>

          <!-- Reusable Paginated Table -->
          <app-paginated-table
            #paginatedTable
            [fetcher]="fetcher"
            [columns]="columns"
            [filters]="filters"
            [actionsTemplate]="actionsTmpl"
            searchPlaceholder="Search questions..."
          ></app-paginated-table>

          <!-- Custom Actions Column Template -->
          <ng-template #actionsTmpl let-row>
            <button mat-icon-button matTooltip="Edit" (click)="openEditDialog(row)">
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

        </mat-card-content>
      </mat-card>

      <!-- FAB -->
      <button
        mat-fab
        color="primary"
        class="create-fab"
        matTooltip="Create Question"
        (click)="openCreateDialog()"
      >
        <mat-icon>add</mat-icon>
      </button>
    </div>
  `,
  styles: [`
    .question-list-container {
      padding: 24px;
      position: relative;
    }
    .filters-row {
      display: flex;
      gap: 16px;
      flex-wrap: wrap;
      margin-bottom: 16px;
    }
    .filters-row mat-form-field {
      min-width: 180px;
    }
    .create-fab {
      position: fixed;
      bottom: 32px;
      right: 32px;
    }
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

  filters = {
    subject: '',
    difficulty: '',
    state: ''
  };

  subjects: Subject[] = [];
  filteredSubjects: Subject[] = [];

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
    return this.questionService.getQuestions({
      subject: this.filters.subject || undefined,
      difficulty: this.filters.difficulty || undefined,
      state: this.filters.state || undefined,
      page: req.page,
      size: req.size
    });
  };

  constructor(
    private questionService: QuestionService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private subjectTopicService: SubjectTopicService
  ) {}

  ngOnInit(): void {
    this.loadSubjects();
  }

  loadSubjects(): void {
    this.subjectTopicService.getSubjects().subscribe(subjects => {
      this.subjects = subjects;
      this.filteredSubjects = subjects;
    });
  }

  filterSubjects(value: string): void {
    const filter = (value || '').toLowerCase();
    this.filteredSubjects = this.subjects.filter(s => s.name.toLowerCase().includes(filter));
  }

  applyFilters(): void {
    this.filters = { ...this.filters }; // trigger ngOnChanges in PaginatedTableComponent
  }

  reload(): void {
    this.paginatedTable?.reload();
  }

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(QuestionFormDialogComponent, {
      width: '600px',
      data: {} as QuestionFormDialogData
    });

    dialogRef.afterClosed().subscribe((result: QuestionResponse | undefined) => {
      if (result) {
        this.snackBar.open('Question created successfully', 'Close', { duration: 3000 });
        this.reload();
      }
    });
  }

  openEditDialog(question: QuestionResponse): void {
    const dialogRef = this.dialog.open(QuestionFormDialogComponent, {
      width: '600px',
      data: { question } as QuestionFormDialogData
    });

    dialogRef.afterClosed().subscribe((result: QuestionResponse | undefined) => {
      if (result) {
        this.snackBar.open('Question updated successfully', 'Close', { duration: 3000 });
        this.reload();
      }
    });
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
