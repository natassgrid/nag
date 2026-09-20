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

import { Component, OnInit, ViewChild, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { QuestionService, QuestionResponse } from './question.service';
import { SubjectTopicService, Subject } from './subject-topic.service';
import { PassageResponse } from './passage.service';
import {
  PaginatedTableComponent,
  ColumnDef,
  PaginatedDataFetcher,
  FilterCategory
} from '../../shared/components/paginated-table';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { QuestionFormDialogComponent } from './question-form-dialog.component';
import { AiGenerateDialogComponent } from './ai-generate-dialog/ai-generate-dialog.component';
import { QuestionTranslationDialogComponent } from './translation/question-translation-dialog.component';
import { PassageFormDialogComponent } from './passage/passage-form-dialog.component';

export const DEFAULT_SUBJECT_OPTIONS = [
  { label: 'Physics', value: 'Physics' },
  { label: 'Chemistry', value: 'Chemistry' },
  { label: 'Mathematics', value: 'Mathematics' },
  { label: 'Biology', value: 'Biology' },
  { label: 'Computer Science', value: 'Computer Science' },
  { label: 'General Knowledge', value: 'General Knowledge' },
  { label: 'English', value: 'English' }
];

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
    AiGenerateDialogComponent,
    QuestionTranslationDialogComponent,
    PassageFormDialogComponent
  ],
  templateUrl: './question-list.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./question-list.component.scss']
})
export class QuestionListComponent implements OnInit {

  @ViewChild('paginatedTable') paginatedTable!: PaginatedTableComponent<QuestionResponse>;
  @ViewChild('visualsTmpl', { static: true }) visualsTmpl!: any;
  @ViewChild('typeTmpl', { static: true }) typeTmpl!: any;

  drawerOpen = false;
  editingQuestion?: QuestionResponse;
  aiDrawerOpen = false;
  translationDrawerOpen = false;
  translatingQuestion?: QuestionResponse;
  passageDrawerOpen = false;
  editingPassage?: PassageResponse;
  exporting = false;
  importing = false;

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
      options: DEFAULT_SUBJECT_OPTIONS
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

  columns: ColumnDef<QuestionResponse>[] = [];

  fetcher: PaginatedDataFetcher<QuestionResponse> = (req) => {
    const rawSubject = Array.isArray(this.filters['subject']) ? this.filters['subject'][0] : this.filters['subject'];
    const activeDifficulty = Array.isArray(this.filters['difficulty']) ? this.filters['difficulty'][0] : this.filters['difficulty'];
    const activeState = Array.isArray(this.filters['state']) ? this.filters['state'][0] : this.filters['state'];

    const isNumericSubject = rawSubject !== undefined && rawSubject !== null && rawSubject !== '' && !isNaN(Number(rawSubject));
    const subjectId = isNumericSubject ? Number(rawSubject) : undefined;
    const subject = !isNumericSubject && rawSubject ? String(rawSubject) : undefined;

    return this.questionService.getQuestions({
      page: req.page,
      size: req.size,
      subjectId,
      subject,
      difficulty: activeDifficulty || undefined,
      state: activeState || undefined,
      search: req.search,
      sort: req.sort,
      order: req.order
    });
  };

  constructor(
    private questionService: QuestionService,
    private subjectTopicService: SubjectTopicService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.columns = [
      {
        key: 'content',
        header: 'Question Content',
        cell: (q: QuestionResponse) => {
          const raw = q.content || '';
          return raw.replace(/<[^>]*>/g, '').trim().slice(0, 100) + (raw.length > 100 ? '…' : '');
        },
        sortable: true
      },
      {
        key: 'questionType',
        header: 'Type',
        type: 'custom',
        template: this.typeTmpl,
        sortable: true
      },
      {
        key: 'subject',
        header: 'Subject',
        cell: (q: QuestionResponse) => q.subject || '—',
        sortable: true
      },
      {
        key: 'topic',
        header: 'Topic',
        cell: (q: QuestionResponse) => q.topic || '—',
        sortable: true
      },
      {
        key: 'difficulty',
        header: 'Difficulty',
        type: 'chip',
        chipClass: (val: string) => 'diff-' + (val ?? '').toLowerCase(),
        sortable: true
      },
      {
        key: 'cognitiveLevel',
        header: 'Cognitive Level',
        cell: (q: QuestionResponse) => q.cognitiveLevel ? q.cognitiveLevel.charAt(0).toUpperCase() + q.cognitiveLevel.slice(1).toLowerCase() : '—'
      },
      {
        key: 'hasVisuals',
        header: 'Visuals',
        type: 'custom',
        template: this.visualsTmpl
      },
      {
        key: 'state',
        header: 'Status',
        type: 'chip',
        chipClass: (val: string) => 'status-' + (val ?? '').toLowerCase(),
        sortable: true
      },
      {
        key: 'actions',
        header: 'Actions',
        type: 'actions'
      }
    ];

    this.loadSubjects();
  }

  loadSubjects(): void {
    this.subjectTopicService.getSubjects().subscribe({
      next: (subjects) => {
        this.subjects = subjects;
        const options = subjects.map(s => ({
          label: s.name,
          value: s.name
        }));
        if (options.length > 0) {
          const category = this.filterCategories.find(c => c.key === 'subject');
          if (category) {
            category.options = options;
          }
        }
      },
      error: () => {}
    });
  }

  onFilterChange(filters: Record<string, any>): void {
    this.filters = { ...filters };
    this.reload();
  }

  reload(): void {
    this.paginatedTable?.reload();
  }

  openCreateDrawer(): void {
    this.editingQuestion = undefined;
    this.drawerOpen = true;
  }

  openCreatePassageDrawer(): void {
    this.editingPassage = undefined;
    this.passageDrawerOpen = true;
  }

  openEditDrawer(question: QuestionResponse): void {
    this.editingQuestion = question;
    this.drawerOpen = true;
  }

  openAiGenerateDrawer(): void {
    this.aiDrawerOpen = true;
  }

  openTranslationDrawer(question: QuestionResponse): void {
    this.translatingQuestion = question;
    this.translationDrawerOpen = true;
  }

  onDrawerClose(saved?: boolean): void {
    this.drawerOpen = false;
    this.editingQuestion = undefined;
    if (saved) this.reload();
  }

  onPassageDrawerClose(saved?: boolean): void {
    this.passageDrawerOpen = false;
    this.editingPassage = undefined;
    if (saved) this.reload();
  }

  onTranslationDrawerClose(saved?: boolean): void {
    this.translationDrawerOpen = false;
    this.translatingQuestion = undefined;
    if (saved) this.reload();
  }

  onAiDrawerClose(saved?: boolean): void {
    this.aiDrawerOpen = false;
    if (saved) this.reload();
  }

  onDrawerSaved(): void {
    this.drawerOpen = false;
    this.editingQuestion = undefined;
    this.reload();
  }

  onPassageSaved(): void {
    this.passageDrawerOpen = false;
    this.editingPassage = undefined;
    this.reload();
  }

  onAiQuestionsSaved(): void {
    this.reload();
  }

  onTranslationSaved(): void {
    this.translationDrawerOpen = false;
    this.translatingQuestion = undefined;
    this.reload();
  }

  submitForReview(q: QuestionResponse): void {
    this.questionService.submitForReview(q.id).subscribe({
      next: () => {
        this.snackBar.open('Question submitted for review', 'Close', { duration: 3000 });
        this.reload();
      },
      error: (err) => {
        this.snackBar.open(err.error?.message || 'Failed to submit for review', 'Close', { duration: 4000 });
      }
    });
  }

  exportQuestions(format: 'json' | 'csv'): void {
    this.exporting = true;
    const rawSubject = Array.isArray(this.filters['subject']) ? this.filters['subject'][0] : this.filters['subject'];
    const activeDifficulty = Array.isArray(this.filters['difficulty']) ? this.filters['difficulty'][0] : this.filters['difficulty'];
    const activeState = Array.isArray(this.filters['state']) ? this.filters['state'][0] : this.filters['state'];
    const isNumericSubject = rawSubject !== undefined && rawSubject !== null && rawSubject !== '' && !isNaN(Number(rawSubject));
    const subjectId = isNumericSubject ? Number(rawSubject) : undefined;
    const subject = !isNumericSubject && rawSubject ? String(rawSubject) : undefined;

    this.questionService.exportQuestions({
      format,
      subject,
      subjectId,
      difficulty: activeDifficulty || undefined,
      state: activeState || undefined,
      search: this.paginatedTable?.searchQuery || undefined
    }).subscribe({
      next: (blob: Blob) => {
        this.exporting = false;
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `questions-export-${new Date().toISOString().slice(0, 10)}.zip`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
        this.snackBar.open('Export downloaded successfully', 'Close', { duration: 3000 });
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.exporting = false;
        this.snackBar.open(err?.error?.message || 'Export failed', 'Close', { duration: 4000 });
        this.cdr.detectChanges();
      }
    });
  }

  onImportFileSelected(event: Event): void {
    this.onFileSelected(event);
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;
    const file = input.files[0];
    this.importing = true;
    this.snackBar.open(`Importing ${file.name}...`, undefined, { duration: 2000 });
    this.questionService.importQuestions(file).subscribe({
      next: (result) => {
        this.importing = false;
        input.value = '';
        const msg = `Import complete: ${result.successfulCount} imported, ${result.duplicateCount} duplicates, ${result.failedCount} failed`;
        this.snackBar.open(msg, 'Close', { duration: 5000 });
        this.reload();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.importing = false;
        input.value = '';
        this.snackBar.open(err?.error?.message || 'Import failed', 'Close', { duration: 5000 });
        this.cdr.detectChanges();
      }
    });
  }
}
