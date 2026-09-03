import { Component, OnInit, ViewChild, ChangeDetectionStrategy } from '@angular/core';
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

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { QuestionService, QuestionResponse } from './question.service';
import { QuestionFormDialogComponent } from './question-form-dialog.component';
import { AiGenerateDialogComponent } from './ai-generate-dialog/ai-generate-dialog.component';
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
    QuestionFormDialogComponent,
    AiGenerateDialogComponent
  ],
  templateUrl: './question-list.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./question-list.component.scss']
})
export class QuestionListComponent implements OnInit {

  @ViewChild('paginatedTable') paginatedTable!: PaginatedTableComponent<QuestionResponse>;

  drawerOpen = false;
  editingQuestion?: QuestionResponse;
  aiDrawerOpen = false;

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

  openAiGenerateDrawer(): void {
    this.aiDrawerOpen = true;
  }

  onAiDrawerClose(hasSaved: boolean): void {
    this.aiDrawerOpen = false;
    if (hasSaved) {
      this.reload();
    }
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

  exporting = false;
  importing = false;

  /** Downloads a ZIP export of questions matching the active filters. */
  exportQuestions(format: 'json' | 'csv'): void {
    const activeSubject = Array.isArray(this.filters['subject']) ? this.filters['subject'][0] : this.filters['subject'];
    const activeDifficulty = Array.isArray(this.filters['difficulty']) ? this.filters['difficulty'][0] : this.filters['difficulty'];
    const activeState = Array.isArray(this.filters['state']) ? this.filters['state'][0] : this.filters['state'];

    this.exporting = true;
    this.questionService.exportQuestions({
      format,
      subject: activeSubject || undefined,
      difficulty: activeDifficulty || undefined,
      state: activeState || undefined
    }).subscribe({
      next: (blob) => {
        this.exporting = false;
        this.triggerDownload(blob, `questions-export-${format}-${new Date().toISOString().slice(0, 10)}.zip`);
        this.snackBar.open('Export ready', 'Close', { duration: 3000 });
      },
      error: () => {
        this.exporting = false;
        this.snackBar.open('Export failed', 'Close', { duration: 4000 });
      }
    });
  }

  /** Handles the hidden file input change: uploads the selected ZIP for import. */
  onImportFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files[0];
    if (!file) {
      return;
    }
    this.importing = true;
    this.questionService.importQuestions(file).subscribe({
      next: (result) => {
        this.importing = false;
        input.value = '';
        this.snackBar.open(
          `Imported ${result.imported} question(s), ${result.failed} failed`,
          'Close',
          { duration: 4000 });
        this.reload();
      },
      error: (err) => {
        this.importing = false;
        input.value = '';
        this.snackBar.open(err?.error?.message || 'Import failed', 'Close', { duration: 4000 });
      }
    });
  }

  private triggerDownload(blob: Blob, fileName: string): void {
    const url = window.URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = fileName;
    anchor.click();
    window.URL.revokeObjectURL(url);
  }
}
