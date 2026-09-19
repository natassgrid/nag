/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

import { Component, OnInit, ChangeDetectorRef, ViewChild, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { tap } from 'rxjs/operators';
import { QuestionService, QuestionResponse } from './question.service';
import {
  PaginatedTableComponent,
  ColumnDef,
  PaginatedDataFetcher
} from '../../shared/components/paginated-table';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { MathRendererComponent } from '../../shared/components/math-renderer/math-renderer.component';

@Component({
  selector: 'app-question-review',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatSnackBarModule,
    MatChipsModule,
    MatTooltipModule,
    MatDividerModule,
    MatFormFieldModule,
    MatInputModule,
    PaginatedTableComponent,
    PageHeaderComponent,
    MathRendererComponent
  ],
  templateUrl: './question-review.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./question-review.component.scss']
})
export class QuestionReviewComponent {

  @ViewChild('paginatedTable') paginatedTable!: PaginatedTableComponent<QuestionResponse>;

  questions: QuestionResponse[] = [];
  selected: QuestionResponse | null = null;
  rejectComment = '';
  acting = false;
  viewMode: 'table' | 'split' = 'table';

  columns: ColumnDef<QuestionResponse>[] = [\n    { key: 'subject', header: 'Subject', sortable: true },\n    { key: 'topic', header: 'Topic', sortable: true },\n    {\n      key: 'difficulty',\n      header: 'Difficulty',\n      type: 'chip',\n      chipClass: (val) => 'chip-' + (val || 'medium').toLowerCase(),\n      sortable: true\n    },\n    {\n      key: 'questionType',\n      header: 'Type',\n      cell: (row) => this.formatType(row.questionType),\n      sortable: true\n    },\n    { key: 'createdAt', header: 'Created', type: 'date', sortable: true },\n    { key: 'actions', header: 'Action', type: 'actions' }\n  ];

  fetcher: PaginatedDataFetcher<QuestionResponse> = (req) => {
    return this.questionService.getQuestionsForReview(req.page, req.size).pipe(
      tap(page => {
        const list = page?.content ?? (Array.isArray(page) ? page : []);
        this.questions = [...list];
        if (this.selected) {
          const still = this.questions.find(q => q.id === this.selected!.id);
          this.selected = still ?? (this.questions[0] ?? null);
        } else {
          this.selected = this.questions[0] ?? null;
        }
        this.cdr.detectChanges();
      })
    );
  };

  constructor(
    private questionService: QuestionService,
    private snackBar: MatSnackBar,
    private sanitizer: DomSanitizer,
    private cdr: ChangeDetectorRef
  ) {}

  getSafeImageUrl(url?: string | null): SafeUrl | string {
    if (!url) return '';
    if (url.startsWith('data:') || url.startsWith('blob:')) {
      return this.sanitizer.bypassSecurityTrustUrl(url);
    }
    return url;
  }

  reload(): void {
    this.paginatedTable?.reload();
  }

  select(question: QuestionResponse): void {
    this.selected = question;
    this.rejectComment = '';
  }

  approve(): void {
    if (!this.selected) return;
    this.acting = true;
    this.questionService.approveQuestion(this.selected.id).subscribe({
      next: () => {
        this.acting = false;
        this.snackBar.open('Question approved', 'Close', { duration: 3000 });
        this.reload();
      },
      error: () => {
        this.acting = false;
        this.snackBar.open('Failed to approve question', 'Close', { duration: 4000 });
      }
    });
  }

  reject(): void {
    if (!this.selected || !this.rejectComment.trim()) return;
    this.acting = true;
    this.questionService.rejectQuestion(this.selected.id, this.rejectComment.trim()).subscribe({
      next: () => {
        this.acting = false;
        this.snackBar.open('Question rejected', 'Close', { duration: 3000 });
        this.selected = null;
        this.reload();
      },
      error: () => {
        this.acting = false;
        this.snackBar.open('Failed to reject question', 'Close', { duration: 4000 });
      }
    });
  }

  getDiffClass(diff?: string): string {
    switch (diff?.toUpperCase()) {
      case 'EASY': return 'diff-easy';
      case 'MEDIUM': return 'diff-medium';
      case 'HARD': return 'diff-hard';
      case 'EXPERT': return 'diff-expert';
      default: return '';
    }
  }

  formatType(type?: string): string {
    switch (type) {
      case 'SINGLE_MCQ': return 'Single Choice (MCQ)';
      case 'MULTI_MCQ': return 'Multiple Choice (MSQ)';
      case 'NUMERICAL': return 'Numerical';
      case 'DESCRIPTIVE': return 'Descriptive';
      default: return type || 'N/A';
    }
  }

  isMcq(question?: QuestionResponse | null): boolean {
    return (
      question?.questionType === 'SINGLE_MCQ' ||
      question?.questionType === 'MULTI_MCQ'
    );
  }
}
