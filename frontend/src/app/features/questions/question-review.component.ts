import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatBadgeModule } from '@angular/material/badge';
import { QuestionService, QuestionResponse } from './question.service';

@Component({
  selector: 'app-question-review',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatSnackBarModule,
    MatChipsModule,
    MatTooltipModule,
    MatPaginatorModule,
    MatDividerModule,
    MatFormFieldModule,
    MatInputModule,
    MatBadgeModule
  ],
  template: `
    <div class="review-container">

      <!-- Page header -->
      <div class="page-header">
        <h2 class="page-title">
          <mat-icon>rate_review</mat-icon>
          Review Queue
          <span class="badge" *ngIf="totalElements > 0">{{ totalElements }}</span>
        </h2>
        <div class="view-toggle">
          <button
            mat-icon-button
            [color]="viewMode === 'table' ? 'primary' : ''"
            (click)="viewMode = 'table'"
            matTooltip="Table View"
            aria-label="Switch to Table View"
          >
            <mat-icon>table_chart</mat-icon>
          </button>
          <button
            mat-icon-button
            [color]="viewMode === 'split' ? 'primary' : ''"
            (click)="viewMode = 'split'"
            matTooltip="Split View"
            aria-label="Switch to Split View"
          >
            <mat-icon>view_sidebar</mat-icon>
          </button>
        </div>
      </div>

      <!-- ── TABLE VIEW ── -->
      <ng-container *ngIf="viewMode === 'table'">
        <div class="table-panel">
          <div class="table-container">
            <table mat-table [dataSource]="dataSource" class="question-table">

              <!-- Subject Column -->
              <ng-container matColumnDef="subject">
                <th mat-header-cell *matHeaderCellDef>Subject</th>
                <td mat-cell *matCellDef="let q" class="subject-cell">{{ q.subject }}</td>
              </ng-container>

              <!-- Topic Column -->
              <ng-container matColumnDef="topic">
                <th mat-header-cell *matHeaderCellDef>Topic</th>
                <td mat-cell *matCellDef="let q">
                  {{ q.topic }}
                  <span *ngIf="q.subtopic" class="subtopic-text"> · {{ q.subtopic }}</span>
                </td>
              </ng-container>

              <!-- Difficulty Column -->
              <ng-container matColumnDef="difficulty">
                <th mat-header-cell *matHeaderCellDef>Difficulty</th>
                <td mat-cell *matCellDef="let q">
                  <mat-chip-set>
                    <mat-chip [class]="getDiffClass(q.difficulty)" class="diff-chip">
                      {{ q.difficulty || 'N/A' }}
                    </mat-chip>
                  </mat-chip-set>
                </td>
              </ng-container>

              <!-- Type Column -->
              <ng-container matColumnDef="questionType">
                <th mat-header-cell *matHeaderCellDef>Type</th>
                <td mat-cell *matCellDef="let q">
                  <mat-chip-set>
                    <mat-chip class="type-chip">{{ formatType(q.questionType) }}</mat-chip>
                  </mat-chip-set>
                </td>
              </ng-container>

              <!-- Created Column -->
              <ng-container matColumnDef="createdAt">
                <th mat-header-cell *matHeaderCellDef>Created</th>
                <td mat-cell *matCellDef="let q">{{ q.createdAt | date:'shortDate' }}</td>
              </ng-container>

              <!-- Actions Column -->
              <ng-container matColumnDef="actions">
                <th mat-header-cell *matHeaderCellDef>Action</th>
                <td mat-cell *matCellDef="let q">
                  <button
                    mat-stroked-button
                    color="primary"
                    (click)="select(q); $event.stopPropagation()"
                  >
                    <mat-icon>rate_review</mat-icon>
                    Review
                  </button>
                </td>
              </ng-container>

              <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
              <tr
                mat-row
                *matRowDef="let row; columns: displayedColumns;"
                [class.selected-row]="selected?.id === row.id"
                (click)="select(row)"
              ></tr>

              <tr class="mat-row" *matNoDataRow>
                <td class="mat-cell no-data-cell" [attr.colspan]="displayedColumns.length">
                  No questions pending review.
                </td>
              </tr>
            </table>
          </div>

          <mat-paginator
            [length]="totalElements"
            [pageSize]="pageSize"
            [pageIndex]="pageIndex"
            [pageSizeOptions]="[10, 20, 50]"
            (page)="onPageChange($event)"
            aria-label="Select page">
          </mat-paginator>
        </div>
      </ng-container>

      <div class="layout" [class.table-mode-layout]="viewMode === 'table'">

        <!-- ── LEFT: question list (for Split View) ── -->
        <div class="list-panel" *ngIf="viewMode === 'split'">
          <div class="list-empty" *ngIf="questions.length === 0">
            <mat-icon>inbox</mat-icon>
            <p>No questions pending review.</p>
          </div>

          <div
            *ngFor="let q of questions"
            class="list-item"
            [class.selected]="selected?.id === q.id"
            (click)="select(q)"
            role="button"
            [attr.aria-pressed]="selected?.id === q.id"
          >
            <div class="list-item-top">
              <span class="list-subject">{{ q.subject }}</span>
              <mat-chip-set>
                <mat-chip [class]="getDiffClass(q.difficulty)" class="diff-chip">
                  {{ q.difficulty || 'N/A' }}
                </mat-chip>
              </mat-chip-set>
            </div>
            <div class="list-item-mid">{{ q.topic }}<span *ngIf="q.subtopic"> · {{ q.subtopic }}</span></div>
            <div class="list-item-preview">{{ (q.content || '') | slice:0:60 }}{{ (q.content || '').length > 60 ? '…' : '' }}</div>
            <div class="list-item-meta">
              <mat-chip-set>
                <mat-chip class="type-chip">{{ formatType(q.questionType) }}</mat-chip>
              </mat-chip-set>
            </div>
          </div>

          <mat-paginator
            [length]="totalElements"
            [pageSize]="pageSize"
            [pageIndex]="pageIndex"
            [pageSizeOptions]="[10, 20, 50]"
            (page)="onPageChange($event)"
            aria-label="Select page">
          </mat-paginator>
        </div>

        <!-- ── RIGHT / BELOW: detail panel ── -->
        <div class="detail-panel" *ngIf="selected; else noSelection">

          <!-- Metadata row -->
          <div class="detail-meta">
            <mat-chip [class]="getDiffClass(selected.difficulty)">{{ selected.difficulty || 'N/A' }}</mat-chip>
            <mat-chip class="type-chip">{{ formatType(selected.questionType) }}</mat-chip>
            <mat-chip class="level-chip" *ngIf="selected.cognitiveLevel">{{ selected.cognitiveLevel }}</mat-chip>
            <span class="meta-text">{{ selected.subject }} / {{ selected.topic }}<span *ngIf="selected.subtopic"> / {{ selected.subtopic }}</span></span>
          </div>

          <mat-divider></mat-divider>

          <!-- Question content -->
          <div class="detail-section">
            <div class="section-label">Question</div>
            <div class="question-content" [innerHTML]="selected.content || ''"></div>
          </div>

          <!-- Options for MCQ/MSQ -->
          <div class="detail-section" *ngIf="isMcq(selected)">
            <div class="section-label">Answer Options</div>
            <div
              *ngFor="let opt of selected.options || []"
              class="option-row"
              [class.option-correct]="opt.isCorrect"
            >
              <mat-icon class="option-icon" [class.correct-icon]="opt.isCorrect">
                {{ opt.isCorrect ? 'check_circle' : 'radio_button_unchecked' }}
              </mat-icon>
              <span class="option-id">{{ opt.id }}.</span>
              <span class="option-text">{{ opt.text || '(empty)' }}</span>
              <mat-chip *ngIf="opt.isCorrect" class="correct-chip">Correct</mat-chip>
            </div>
          </div>

          <!-- Answer key for non-MCQ -->
          <div class="detail-section" *ngIf="!isMcq(selected) && selected.answerKey">
            <div class="section-label">Answer Key</div>
            <div class="answer-key">{{ selected.answerKey }}</div>
          </div>

          <mat-divider></mat-divider>

          <!-- Review actions -->
          <div class="actions-section">

            <!-- Approve -->
            <div class="action-block approve-block">
              <button
                mat-raised-button
                color="primary"
                [disabled]="acting"
                (click)="approve()"
                aria-label="Approve question"
              >
                <mat-icon>check_circle</mat-icon>
                Approve
              </button>
              <span class="action-hint">Moves question to Approved state</span>
            </div>

            <mat-divider [vertical]="true" class="vertical-divider"></mat-divider>

            <!-- Reject with feedback -->
            <div class="action-block reject-block">
              <mat-form-field appearance="outline" class="feedback-field">
                <mat-label>Rejection feedback</mat-label>
                <textarea
                  matInput
                  [(ngModel)]="rejectComment"
                  rows="3"
                  placeholder="Explain what needs to be corrected…"
                  aria-label="Rejection feedback"
                ></textarea>
                <mat-hint>Required before rejecting</mat-hint>
              </mat-form-field>
              <button
                mat-raised-button
                color="warn"
                [disabled]="!rejectComment.trim() || acting"
                (click)="reject()"
                aria-label="Reject question"
              >
                <mat-icon>cancel</mat-icon>
                Reject &amp; Return
              </button>
            </div>
          </div>

        </div>

        <!-- No selection placeholder -->
        <ng-template #noSelection>
          <div class="no-selection" *ngIf="questions.length > 0">
            <mat-icon>touch_app</mat-icon>
            <p>Select a question from the table/list to review it.</p>
          </div>
        </ng-template>

      </div>
    </div>
  `,
  styles: [`
    .review-container {
      padding: 24px;
      max-width: 1400px;
    }

    /* Header */
    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;
    }
    .page-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 22px;
      font-weight: 500;
      margin: 0;
    }
    .page-title mat-icon { font-size: 26px; height: 26px; width: 26px; }
    .badge {
      background: #f44336;
      color: white;
      border-radius: 12px;
      padding: 2px 8px;
      font-size: 13px;
      font-weight: 600;
    }
    .view-toggle { display: flex; gap: 4px; }

    /* Layout */
    .layout {
      display: flex;
      gap: 20px;
      align-items: flex-start;
    }
    .layout.table-mode-layout {
      flex-direction: column;
      margin-top: 20px;
    }
    .layout.table-mode-layout .detail-panel {
      width: 100%;
    }

    /* Table Panel */
    .table-panel {
      border: 1px solid #e0e0e0;
      border-radius: 8px;
      background: white;
      overflow: hidden;
    }
    .table-container { overflow-x: auto; }
    .question-table { width: 100%; }
    .subject-cell { font-weight: 600; }
    .subtopic-text { color: #616161; font-size: 12px; }
    .selected-row { background-color: #e3f2fd !important; }
    .no-data-cell {
      text-align: center;
      padding: 48px;
      color: #9e9e9e;
    }

    /* Left list */
    .list-panel {
      width: 320px;
      flex-shrink: 0;
      border: 1px solid #e0e0e0;
      border-radius: 8px;
      overflow: hidden;
      background: white;
    }
    .list-empty {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 48px 16px;
      color: #9e9e9e;
    }
    .list-empty mat-icon { font-size: 48px; height: 48px; width: 48px; margin-bottom: 8px; }
    .list-item {
      padding: 12px 16px;
      border-bottom: 1px solid #f0f0f0;
      cursor: pointer;
      transition: background 0.15s;
    }
    .list-item:hover { background: #f5f5f5; }
    .list-item.selected { background: #e3f2fd; border-left: 3px solid #1976d2; }
    .list-item-top {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 2px;
    }
    .list-subject { font-weight: 600; font-size: 14px; }
    .list-item-mid { font-size: 12px; color: #616161; margin-bottom: 4px; }
    .list-item-preview { font-size: 12px; color: #757575; margin-bottom: 6px; line-height: 1.4; }
    .list-item-meta { display: flex; gap: 4px; }
    .diff-chip { font-size: 11px !important; height: 20px !important; padding: 0 6px !important; }
    .type-chip { font-size: 11px !important; height: 20px !important; padding: 0 6px !important; background: #ede7f6 !important; }

    /* Detail panel */
    .detail-panel {
      flex: 1;
      border: 1px solid #e0e0e0;
      border-radius: 8px;
      padding: 24px;
      background: white;
      min-height: 400px;
    }
    .detail-meta {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;
      margin-bottom: 16px;
    }
    .meta-text { font-size: 13px; color: #616161; margin-left: 4px; }
    .level-chip { background: #e8f5e9 !important; }

    /* Sections */
    .detail-section { margin: 20px 0; }
    .section-label {
      font-size: 11px;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.08em;
      color: #9e9e9e;
      margin-bottom: 10px;
    }
    .question-content {
      font-size: 16px;
      line-height: 1.6;
      color: #212121;
    }
    .answer-key {
      background: #f5f5f5;
      border-radius: 6px;
      padding: 12px;
      font-family: monospace;
      font-size: 14px;
    }

    /* Options */
    .option-row {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 12px;
      border-radius: 6px;
      margin-bottom: 6px;
      border: 1px solid #e0e0e0;
    }
    .option-row.option-correct {
      background: #f1f8e9;
      border-color: #aed581;
    }
    .option-icon { font-size: 18px; height: 18px; width: 18px; color: #bdbdbd; }
    .option-icon.correct-icon { color: #66bb6a; }
    .option-id { font-weight: 700; color: #3f51b5; width: 20px; }
    .option-text { flex: 1; font-size: 14px; }
    .correct-chip {
      background: #c5e1a5 !important;
      font-size: 11px !important;
      height: 20px !important;
      padding: 0 6px !important;
    }

    /* Actions */
    .actions-section {
      display: flex;
      gap: 24px;
      margin-top: 20px;
      align-items: flex-start;
    }
    .action-block { display: flex; flex-direction: column; gap: 8px; }
    .approve-block { min-width: 140px; justify-content: flex-start; padding-top: 4px; }
    .reject-block { flex: 1; }
    .action-hint { font-size: 11px; color: #9e9e9e; }
    .feedback-field { width: 100%; }
    .vertical-divider { height: auto; align-self: stretch; }

    /* No selection */
    .no-selection {
      flex: 1;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      color: #9e9e9e;
      min-height: 300px;
      gap: 8px;
      border: 1px dashed #e0e0e0;
      border-radius: 8px;
    }
    .no-selection mat-icon { font-size: 48px; height: 48px; width: 48px; }

    /* Difficulty chips */
    .chip-easy   { background-color: #c8e6c9 !important; }
    .chip-medium { background-color: #fff9c4 !important; }
    .chip-hard   { background-color: #ffcdd2 !important; }
  `]
})
export class QuestionReviewComponent implements OnInit {

  displayedColumns = ['subject', 'topic', 'difficulty', 'questionType', 'createdAt', 'actions'];
  dataSource = new MatTableDataSource<QuestionResponse>([]);
  questions: QuestionResponse[] = [];
  selected: QuestionResponse | null = null;
  rejectComment = '';
  acting = false;
  viewMode: 'table' | 'split' = 'table';

  pageIndex = 0;
  pageSize = 20;
  totalElements = 0;

  constructor(
    private questionService: QuestionService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadReviewQuestions();
  }

  loadReviewQuestions(): void {
    this.questionService.getQuestionsForReview(this.pageIndex, this.pageSize).subscribe({
      next: (page) => {
        console.log('[ReviewComponent] page received:', page);
        const list = page?.content ?? (Array.isArray(page) ? page : []);
        this.questions = [...list];
        this.dataSource.data = [...list];
        this.totalElements = page?.totalElements ?? this.questions.length;
        if (this.selected) {
          const still = this.questions.find(q => q.id === this.selected!.id);
          this.selected = still ?? (this.questions[0] ?? null);
        } else {
          this.selected = this.questions[0] ?? null;
        }
        this.rejectComment = '';
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('[ReviewComponent] error:', err);
        this.snackBar.open('Failed to load questions for review', 'Close', { duration: 3000 });
        this.cdr.detectChanges();
      }
    });
  }

  select(question: QuestionResponse): void {
    this.selected = question;
    this.rejectComment = '';
    this.cdr.detectChanges();
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.selected = null;
    this.loadReviewQuestions();
  }

  approve(): void {
    if (!this.selected) return;
    this.acting = true;
    this.questionService.approveQuestion(this.selected.id).subscribe({
      next: () => {
        this.snackBar.open('Question approved', 'Close', { duration: 3000 });
        this.acting = false;
        this.loadReviewQuestions();
      },
      error: (err) => {
        this.snackBar.open(err.error?.message || 'Failed to approve question', 'Close', { duration: 3000 });
        this.acting = false;
        this.cdr.detectChanges();
      }
    });
  }

  reject(): void {
    if (!this.selected || !this.rejectComment.trim()) return;
    this.acting = true;
    this.questionService.rejectQuestion(this.selected.id, this.rejectComment.trim()).subscribe({
      next: () => {
        this.snackBar.open('Question rejected and returned to author', 'Close', { duration: 3000 });
        this.acting = false;
        this.loadReviewQuestions();
      },
      error: (err) => {
        this.snackBar.open(err.error?.message || 'Failed to reject question', 'Close', { duration: 3000 });
        this.acting = false;
        this.cdr.detectChanges();
      }
    });
  }

  isMcq(q: QuestionResponse): boolean {
    return q?.questionType === 'SINGLE_MCQ' || q?.questionType === 'MULTI_MCQ';
  }

  getDiffClass(difficulty?: string): string {
    if (!difficulty) return 'chip-medium';
    return 'chip-' + difficulty.toLowerCase();
  }

  formatType(type?: string): string {
    if (!type) return '';
    const map: Record<string, string> = {
      SINGLE_MCQ: 'MCQ',
      MULTI_MCQ: 'MSQ',
      NUMERICAL: 'Numerical',
      DESCRIPTIVE: 'Descriptive',
      MATRIX_MATCH: 'Matrix',
      ASSERTION_REASON: 'A&R',
      CODING: 'Coding',
      CASE_STUDY: 'Case Study'
    };
    return map[type] || type;
  }
}
