import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { QuestionService, QuestionResponse } from './question.service';
import { RejectDialogComponent } from './reject-dialog.component';

@Component({
  selector: 'app-question-review',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatDialogModule,
    MatSnackBarModule,
    MatChipsModule,
    MatTooltipModule,
    MatProgressSpinnerModule
  ],
  template: `
    <div class="review-container">
      <mat-card>
        <mat-card-header>
          <mat-card-title>Questions Pending Review</mat-card-title>
          <mat-card-subtitle>
            Review and approve or reject questions submitted by authors
          </mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <div *ngIf="loading" class="loading-container">
            <mat-spinner diameter="40"></mat-spinner>
          </div>

          <div class="table-container" *ngIf="!loading">
            <table mat-table [dataSource]="dataSource" class="review-table">
              <ng-container matColumnDef="subject">
                <th mat-header-cell *matHeaderCellDef>Subject</th>
                <td mat-cell *matCellDef="let row">{{ row.subject }}</td>
              </ng-container>

              <ng-container matColumnDef="topic">
                <th mat-header-cell *matHeaderCellDef>Topic</th>
                <td mat-cell *matCellDef="let row">{{ row.topic }}</td>
              </ng-container>

              <ng-container matColumnDef="difficulty">
                <th mat-header-cell *matHeaderCellDef>Difficulty</th>
                <td mat-cell *matCellDef="let row">
                  <mat-chip-set>
                    <mat-chip [class]="'chip-' + row.difficulty?.toLowerCase()">
                      {{ row.difficulty }}
                    </mat-chip>
                  </mat-chip-set>
                </td>
              </ng-container>

              <ng-container matColumnDef="questionType">
                <th mat-header-cell *matHeaderCellDef>Type</th>
                <td mat-cell *matCellDef="let row">{{ row.questionType }}</td>
              </ng-container>

              <ng-container matColumnDef="content">
                <th mat-header-cell *matHeaderCellDef>Content</th>
                <td mat-cell *matCellDef="let row" class="content-cell">
                  {{ row.content | slice:0:80 }}{{ row.content?.length > 80 ? '...' : '' }}
                </td>
              </ng-container>

              <ng-container matColumnDef="authorId">
                <th mat-header-cell *matHeaderCellDef>Author</th>
                <td mat-cell *matCellDef="let row" class="author-cell">
                  {{ row.authorId | slice:0:8 }}...
                </td>
              </ng-container>

              <ng-container matColumnDef="actions">
                <th mat-header-cell *matHeaderCellDef>Actions</th>
                <td mat-cell *matCellDef="let row">
                  <button
                    mat-icon-button
                    color="primary"
                    matTooltip="Approve"
                    aria-label="Approve question"
                    (click)="approveQuestion(row)"
                  >
                    <mat-icon>check_circle</mat-icon>
                  </button>
                  <button
                    mat-icon-button
                    color="warn"
                    matTooltip="Reject"
                    aria-label="Reject question"
                    (click)="openRejectDialog(row)"
                  >
                    <mat-icon>cancel</mat-icon>
                  </button>
                </td>
              </ng-container>

              <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
              <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>

              <tr class="mat-row" *matNoDataRow>
                <td class="mat-cell no-data-cell" [attr.colspan]="displayedColumns.length">
                  No questions pending review.
                </td>
              </tr>
            </table>
          </div>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .review-container {
      padding: 24px;
    }
    .loading-container {
      display: flex;
      justify-content: center;
      padding: 48px;
    }
    .table-container {
      overflow-x: auto;
    }
    .review-table {
      width: 100%;
    }
    .content-cell {
      max-width: 250px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .author-cell {
      font-family: monospace;
      font-size: 0.85em;
    }
    .no-data-cell {
      text-align: center;
      padding: 24px;
      color: rgba(0, 0, 0, 0.54);
    }
    .chip-easy { background-color: #c8e6c9 !important; }
    .chip-medium { background-color: #fff9c4 !important; }
    .chip-hard { background-color: #ffcdd2 !important; }
  `]
})
export class QuestionReviewComponent implements OnInit {
  displayedColumns = ['subject', 'topic', 'difficulty', 'questionType', 'content', 'authorId', 'actions'];
  dataSource = new MatTableDataSource<QuestionResponse>([]);
  loading = false;

  constructor(
    private questionService: QuestionService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadReviewQuestions();
  }

  loadReviewQuestions(): void {
    this.loading = true;
    this.questionService.getQuestionsForReview().subscribe({
      next: (questions) => {
        this.dataSource.data = questions;
        this.loading = false;
      },
      error: () => {
        this.snackBar.open('Failed to load questions for review', 'Close', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  approveQuestion(question: QuestionResponse): void {
    this.questionService.approveQuestion(question.id).subscribe({
      next: () => {
        this.snackBar.open('Question approved successfully', 'Close', { duration: 3000 });
        this.loadReviewQuestions();
      },
      error: (err) => {
        const message = err.error?.message || 'Failed to approve question';
        this.snackBar.open(message, 'Close', { duration: 3000 });
      }
    });
  }

  openRejectDialog(question: QuestionResponse): void {
    const dialogRef = this.dialog.open(RejectDialogComponent, {
      width: '450px'
    });

    dialogRef.afterClosed().subscribe((comments: string | undefined) => {
      if (comments) {
        this.questionService.rejectQuestion(question.id, comments).subscribe({
          next: () => {
            this.snackBar.open('Question rejected and returned to author', 'Close', { duration: 3000 });
            this.loadReviewQuestions();
          },
          error: (err) => {
            const message = err.error?.message || 'Failed to reject question';
            this.snackBar.open(message, 'Close', { duration: 3000 });
          }
        });
      }
    });
  }
}
