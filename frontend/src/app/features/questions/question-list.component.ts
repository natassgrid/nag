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
import { QuestionFormDialogComponent } from './question-form-dialog.component';
import { AiGenerateDialogComponent } from './ai-generate-dialog/ai-generate-dialog.component';
import { QuestionTranslationDialogComponent } from './translation/question-translation-dialog.component';
import { PassageFormDialogComponent } from './passage/passage-form-dialog.component';
import { PassageService, PassageResponse } from './passage.service';
import { SubjectTopicService, Subject } from './subject-topic.service';
import {
  PaginatedTableComponent,
  ColumnDef,
  PaginatedDataFetcher,
  FilterCategory
} from '../../shared/components/paginated-table';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';

const DEFAULT_SUBJECT_OPTIONS = [
  { label: 'Quantitative Aptitude', value: 'Quantitative Aptitude' },
  { label: 'General Intelligence and Reasoning', value: 'General Intelligence and Reasoning' },
  { label: 'English Language', value: 'English Language' },
  { label: 'General Awareness', value: 'General Awareness' },
  { label: 'Computer Aptitude', value: 'Computer Aptitude' },
  { label: 'Mathematics', value: 'Mathematics' },
  { label: 'Physics', value: 'Physics' },
  { label: 'Chemistry', value: 'Chemistry' }
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
      subject,
      subjectId,
      difficulty: activeDifficulty || undefined,
      state: activeState || undefined,
      page: req.page,
      size: req.size
    });
  };

  constructor(
    private questionService: QuestionService,
    private passageService: PassageService,
    private snackBar: MatSnackBar,
    private subjectTopicService: SubjectTopicService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.columns = [
      { key: 'hasImages', header: 'Media', type: 'custom', template: this.visualsTmpl },
      { key: 'subject', header: 'Subject', sortable: true },
      { key: 'topic', header: 'Topic', sortable: true },
      {
        key: 'difficulty',
        header: 'Difficulty',
        type: 'chip',
        chipClass: (val) => 'chip-' + (val || '').toLowerCase(),
        sortable: true
      },
      { key: 'questionType', header: 'Type', type: 'custom', template: this.typeTmpl, sortable: true },
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
    this.loadSubjects();
  }

  loadSubjects(): void {
    this.subjectTopicService.getSubjects().subscribe({
      next: (subjects) => {
        this.subjects = subjects || [];
        const subjectOptions = this.subjects.length > 0
          ? this.subjects.map(s => ({ label: s.name, value: s.id.toString() }))
          : DEFAULT_SUBJECT_OPTIONS;
        this.filterCategories = this.filterCategories.map(cat => {
          if (cat.key === 'subject') {
            return {
              ...cat,
              options: subjectOptions
            };
          }
          return cat;
        });
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.warn('Failed to load subjects for filter:', err);
        this.filterCategories = this.filterCategories.map(cat => {
          if (cat.key === 'subject') {
            return {
              ...cat,
              options: DEFAULT_SUBJECT_OPTIONS
            };
          }
          return cat;
        });
        this.cdr.markForCheck();
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

  openCreatePassageDrawer(): void {
    this.editingPassage = undefined;
    this.passageDrawerOpen = true;
  }

  onPassageDrawerClose(saved: boolean): void {
    this.passageDrawerOpen = false;
    this.editingPassage = undefined;
    if (saved) {
      this.reload();
    }
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
    if (question.passageId) {
      this.passageService.getPassage(question.passageId).subscribe({
        next: (passage) => {
          this.editingPassage = passage;
          this.passageDrawerOpen = true;
          this.cdr.markForCheck();
        },
        error: (err) => {
          console.error('Failed to load passage for question:', err);
          this.snackBar.open('Failed to load passage for question', 'Close', { duration: 3000 });
        }
      });
      return;
    }
    this.editingQuestion = question;
    this.drawerOpen = true;
  }

  openTranslationDrawer(question: QuestionResponse): void {
    this.translatingQuestion = question;
    this.translationDrawerOpen = true;
  }

  onTranslationDrawerClose(updated?: boolean): void {
    this.translationDrawerOpen = false;
    this.translatingQuestion = undefined;
    if (updated) {
      this.reload();
    }
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
    const rawSubject = Array.isArray(this.filters['subject']) ? this.filters['subject'][0] : this.filters['subject'];
    const activeDifficulty = Array.isArray(this.filters['difficulty']) ? this.filters['difficulty'][0] : this.filters['difficulty'];
    const activeState = Array.isArray(this.filters['state']) ? this.filters['state'][0] : this.filters['state'];

    const isNumericSubject = rawSubject !== undefined && rawSubject !== null && rawSubject !== '' && !isNaN(Number(rawSubject));
    const subjectId = isNumericSubject ? Number(rawSubject) : undefined;
    const subject = !isNumericSubject && rawSubject ? String(rawSubject) : undefined;

    this.exporting = true;
    this.questionService.exportQuestions({
      format,
      subject,
      subjectId,
      difficulty: activeDifficulty || undefined,
      state: activeState || undefined
    }).subscribe({
      next: (blob: Blob) => {
        this.exporting = false;
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        const date = new Date().toISOString().slice(0, 10);
        a.download = `question-bank-export-${date}.zip`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
        this.snackBar.open(`Exported questions (${format.toUpperCase()}) successfully`, 'Close', { duration: 3000 });
      },
      error: (err) => {
        this.exporting = false;
        const msg = err.error?.message || 'Export failed';
        this.snackBar.open(msg, 'Close', { duration: 4000 });
      }
    });
  }

  onImportFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;
    const file = input.files[0];
    this.importing = true;
    this.questionService.importQuestions(file).subscribe({
      next: (res) => {
        this.importing = false;
        input.value = '';
        const msg = `Import complete: ${res.successfulCount} imported, ${res.duplicateCount} duplicates, ${res.failedCount} failed`;
        this.snackBar.open(msg, 'Close', { duration: 5000 });
        this.reload();
      },
      error: (err) => {
        this.importing = false;
        input.value = '';
        const msg = err.error?.message || 'Import failed';
        this.snackBar.open(msg, 'Close', { duration: 4000 });
      }
    });
  }
}
