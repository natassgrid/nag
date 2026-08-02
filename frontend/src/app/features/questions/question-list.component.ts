import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
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

@Component({
  selector: 'app-question-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
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
    MatAutocompleteModule
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
              <input matInput [(ngModel)]="filters.subject"
                     [matAutocomplete]="subjectAuto"
                     (ngModelChange)="filterSubjects($event)"
                     (blur)="applyFilters()"
                     placeholder="Type to search...">
              <mat-autocomplete #subjectAuto="matAutocomplete" (optionSelected)="applyFilters()">
                <mat-option value="">All Subjects</mat-option>
                <mat-option *ngFor="let s of filteredSubjects" [value]="s.name">{{ s.name }}</mat-option>
              </mat-autocomplete>
              <button *ngIf="filters.subject" mat-icon-button matSuffix (click)="filters.subject=''; applyFilters()" aria-label="Clear">
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

          <!-- Table -->
          <div class="table-container">
            <table mat-table [dataSource]="dataSource" matSort class="question-table">
              <ng-container matColumnDef="subject">
                <th mat-header-cell *matHeaderCellDef mat-sort-header>Subject</th>
                <td mat-cell *matCellDef="let row">{{ row.subject }}</td>
              </ng-container>

              <ng-container matColumnDef="topic">
                <th mat-header-cell *matHeaderCellDef mat-sort-header>Topic</th>
                <td mat-cell *matCellDef="let row">{{ row.topic }}</td>
              </ng-container>

              <ng-container matColumnDef="difficulty">
                <th mat-header-cell *matHeaderCellDef mat-sort-header>Difficulty</th>
                <td mat-cell *matCellDef="let row">
                  <mat-chip-set>
                    <mat-chip [class]="'chip-' + row.difficulty?.toLowerCase()">
                      {{ row.difficulty }}
                    </mat-chip>
                  </mat-chip-set>
                </td>
              </ng-container>

              <ng-container matColumnDef="questionType">
                <th mat-header-cell *matHeaderCellDef mat-sort-header>Type</th>
                <td mat-cell *matCellDef="let row">{{ row.questionType }}</td>
              </ng-container>

              <ng-container matColumnDef="state">
                <th mat-header-cell *matHeaderCellDef mat-sort-header>State</th>
                <td mat-cell *matCellDef="let row">
                  <mat-chip-set>
                    <mat-chip [class]="'chip-state-' + row.state?.toLowerCase()">
                      {{ row.state }}
                    </mat-chip>
                  </mat-chip-set>
                </td>
              </ng-container>

              <ng-container matColumnDef="createdAt">
                <th mat-header-cell *matHeaderCellDef mat-sort-header>Created</th>
                <td mat-cell *matCellDef="let row">{{ row.createdAt | date:'short' }}</td>
              </ng-container>

              <ng-container matColumnDef="actions">
                <th mat-header-cell *matHeaderCellDef>Actions</th>
                <td mat-cell *matCellDef="let row">
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
                </td>
              </ng-container>

              <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
              <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>

              <tr class="mat-row" *matNoDataRow>
                <td class="mat-cell no-data-cell" [attr.colspan]="displayedColumns.length">
                  No questions found.
                </td>
              </tr>
            </table>
          </div>

          <mat-paginator
            [length]="totalElements"
            [pageSize]="pageSize"
            [pageIndex]="pageIndex"
            [pageSizeOptions]="[10, 25, 50]"
            showFirstLastButtons
            (page)="onPageChange($event)"
          ></mat-paginator>
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
    .table-container {
      overflow-x: auto;
    }
    .question-table {
      width: 100%;
    }
    .no-data-cell {
      text-align: center;
      padding: 24px;
      color: rgba(0, 0, 0, 0.54);
    }
    .create-fab {
      position: fixed;
      bottom: 32px;
      right: 32px;
    }
    .chip-easy { background-color: #c8e6c9 !important; }
    .chip-medium { background-color: #fff9c4 !important; }
    .chip-hard { background-color: #ffcdd2 !important; }
    .chip-state-draft { background-color: #e0e0e0 !important; }
    .chip-state-review { background-color: #bbdefb !important; }
    .chip-state-approved { background-color: #c8e6c9 !important; }
    .chip-state-published { background-color: #b2dfdb !important; }
    .chip-state-archived { background-color: #f5f5f5 !important; }
  `]
})
export class QuestionListComponent implements OnInit {
  displayedColumns = ['subject', 'topic', 'difficulty', 'questionType', 'state', 'createdAt', 'actions'];
  dataSource = new MatTableDataSource<QuestionResponse>([]);

  filters = {
    subject: '',
    difficulty: '',
    state: ''
  };

  pageIndex = 0;
  pageSize = 25;
  totalElements = 0;

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  constructor(
    private questionService: QuestionService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private subjectTopicService: SubjectTopicService
  ) {}

  subjects: Subject[] = [];
  filteredSubjects: Subject[] = [];

  ngOnInit(): void {
    this.loadSubjects();
    this.loadQuestions();
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

  ngAfterViewInit(): void {
    this.dataSource.sort = this.sort;
  }

  loadQuestions(): void {
    const filters: any = { page: this.pageIndex, size: this.pageSize };
    if (this.filters.subject)    filters.subject = this.filters.subject;
    if (this.filters.difficulty) filters.difficulty = this.filters.difficulty;
    if (this.filters.state)      filters.state = this.filters.state;

    this.questionService.getQuestions(filters).subscribe({
      next: (page) => {
        this.dataSource.data = page.content;
        this.totalElements = page.totalElements;
      },
      error: () => {
        this.snackBar.open('Failed to load questions', 'Close', { duration: 3000 });
      }
    });
  }

  onPageChange(event: any): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadQuestions();
  }

  applyFilters(): void {
    this.pageIndex = 0; // reset to first page on filter change
    this.loadQuestions();
  }

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(QuestionFormDialogComponent, {
      width: '600px',
      data: {} as QuestionFormDialogData
    });

    dialogRef.afterClosed().subscribe((result: QuestionResponse | undefined) => {
      if (result) {
        this.snackBar.open('Question created successfully', 'Close', { duration: 3000 });
        this.loadQuestions();
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
        this.loadQuestions();
      }
    });
  }

  submitForReview(question: QuestionResponse): void {
    this.questionService.submitForReview(question.id).subscribe({
      next: () => {
        this.snackBar.open('Question submitted for review', 'Close', { duration: 3000 });
        this.loadQuestions();
      },
      error: (err) => {
        const message = err.error?.message || 'Failed to submit question for review';
        this.snackBar.open(message, 'Close', { duration: 3000 });
      }
    });
  }
}
