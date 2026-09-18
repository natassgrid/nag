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
 */

import { Component, OnInit, OnDestroy, ViewChild, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatMenuModule } from '@angular/material/menu';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { Subscription, interval } from 'rxjs';

import { QuestionService, QuestionResponse } from '../question.service';
import { SubjectTopicService, Subject } from '../subject-topic.service';
import {
  TranslationService,
  SUPPORTED_LANGUAGES,
  SupportedLanguage,
  BatchTranslationJobResponse,
  BatchTranslationRequest
} from './translation.service';
import { QuestionTranslationDialogComponent } from './question-translation-dialog.component';
import {
  PaginatedTableComponent,
  ColumnDef,
  PaginatedDataFetcher,
  FilterCategory
} from '../../../shared/components/paginated-table';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';

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
  selector: 'app-question-translation-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatTooltipModule,
    MatSelectModule,
    MatFormFieldModule,
    MatSnackBarModule,
    MatMenuModule,
    MatCardModule,
    MatProgressBarModule,
    MatSlideToggleModule,
    PaginatedTableComponent,
    PageHeaderComponent,
    QuestionTranslationDialogComponent
  ],
  templateUrl: './question-translation-list.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./question-translation-list.component.scss']
})
export class QuestionTranslationListComponent implements OnInit, OnDestroy {
  @ViewChild('paginatedTable') paginatedTable!: PaginatedTableComponent<QuestionResponse>;

  languages: SupportedLanguage[] = SUPPORTED_LANGUAGES;
  selectedLanguage: string = 'hi';

  drawerOpen = false;
  selectedQuestion?: QuestionResponse;
  selectedLanguageForDrawer = 'hi';

  filters: Record<string, any> = {};
  subjects: Subject[] = [];

  // Batch Translation Modal & Tracker State
  batchModalOpen = false;
  batchTargetLanguage = 'hi';
  batchOverwriteExisting = false;
  batchTargetStatus = 'PUBLISHED';
  batchSubjectFilter = '';
  isSubmittingBatch = false;
  activeBatchJob: BatchTranslationJobResponse | null = null;
  private pollSub?: Subscription;

  filterCategories: FilterCategory[] = [
    {
      key: 'subject',
      label: 'Subject',
      expanded: true,
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
      key: 'questionType',
      label: 'Question Type',
      expanded: false,
      options: [
        { label: 'MCQ (Single Correct)', value: 'SINGLE_MCQ' },
        { label: 'MSQ (Multiple Correct)', value: 'MULTI_MCQ' },
        { label: 'Numerical', value: 'NUMERICAL' },
        { label: 'Descriptive', value: 'DESCRIPTIVE' }
      ]
    }
  ];

  columns: ColumnDef<QuestionResponse>[] = [
    { key: 'subject', header: 'Subject', sortable: true },
    { key: 'topic', header: 'Topic', sortable: true },
    {
      key: 'content',
      header: 'Question Content',
      cell: (row) => this.truncateContent(row.content)
    },
    {
      key: 'difficulty',
      header: 'Difficulty',
      type: 'chip',
      chipClass: (val) => 'chip-' + (val || 'medium').toLowerCase(),
      sortable: true
    },
    {
      key: 'state',
      header: 'Source Status',
      type: 'chip',
      chipClass: (val) => 'chip-state-' + (val || 'draft').toLowerCase(),
      sortable: true
    },
    { key: 'actions', header: 'Localization', type: 'actions' }
  ];

  fetcher: PaginatedDataFetcher<QuestionResponse> = (req) => {
    const rawSubject = Array.isArray(this.filters['subject']) ? this.filters['subject'][0] : this.filters['subject'];
    const activeDifficulty = Array.isArray(this.filters['difficulty']) ? this.filters['difficulty'][0] : this.filters['difficulty'];

    const isNumericSubject = rawSubject !== undefined && rawSubject !== null && rawSubject !== '' && !isNaN(Number(rawSubject));
    const subjectId = isNumericSubject ? Number(rawSubject) : undefined;
    const subject = !isNumericSubject && rawSubject ? String(rawSubject) : undefined;

    return this.questionService.getQuestions({
      subject,
      subjectId,
      difficulty: activeDifficulty || undefined,
      page: req.page,
      size: req.size
    });
  };

  constructor(
    private questionService: QuestionService,
    private translationService: TranslationService,
    private subjectTopicService: SubjectTopicService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadSubjects();
    this.checkForActiveBatchJob();
  }

  ngOnDestroy(): void {
    this.stopPolling();
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
        console.warn('Failed to load subjects:', err);
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

  checkForActiveBatchJob(): void {
    this.translationService.listBatchJobs().subscribe({
      next: (jobs) => {
        if (jobs && jobs.length > 0) {
          const active = jobs.find(j => j.status === 'IN_PROGRESS' || j.status === 'PENDING');
          if (active) {
            this.activeBatchJob = active;
            this.startPolling(active.id);
            this.cdr.markForCheck();
          }
        }
      },
      error: (err) => console.warn('Could not fetch batch jobs:', err)
    });
  }

  onFilterChange(updatedFilters: Record<string, any>): void {
    this.filters = { ...updatedFilters };
  }

  reload(): void {
    this.paginatedTable?.reload();
  }

  openTranslationDrawer(question: QuestionResponse, langCode: string = 'hi'): void {
    this.selectedQuestion = question;
    this.selectedLanguageForDrawer = langCode;
    this.drawerOpen = true;
  }

  onDrawerClose(updated: boolean): void {
    this.drawerOpen = false;
    if (updated) {
      this.reload();
    }
  }

  // ---------------------------------------------------------------------------
  // Batch Translation Actions
  // ---------------------------------------------------------------------------

  openBatchModal(): void {
    this.batchModalOpen = true;
  }

  closeBatchModal(): void {
    this.batchModalOpen = false;
  }

  triggerBatchAutoTranslate(): void {
    this.isSubmittingBatch = true;
    const req: BatchTranslationRequest = {
      sourceLanguage: 'en',
      targetLanguage: this.batchTargetLanguage,
      targetStatus: this.batchTargetStatus,
      subject: this.batchSubjectFilter ? this.batchSubjectFilter : undefined,
      overwriteExisting: this.batchOverwriteExisting,
      batchSize: 50,
      throttleDelayMs: 50,
      maxConcurrency: 2
    };

    this.translationService.startBatchTranslation(req).subscribe({
      next: (job) => {
        this.isSubmittingBatch = false;
        this.batchModalOpen = false;
        this.activeBatchJob = job;
        this.snackBar.open(
          `Batch translation started for ${this.getLanguageName(this.batchTargetLanguage)} (Job ID: ${job.id.substring(0, 8)}...)`,
          'Close',
          { duration: 4000 }
        );
        this.startPolling(job.id);
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.isSubmittingBatch = false;
        this.snackBar.open(
          `Failed to start batch translation: ${err?.error?.message || err.message || 'Unknown error'}`,
          'Close',
          { duration: 5000 }
        );
        this.cdr.markForCheck();
      }
    });
  }

  startPolling(jobId: string): void {
    this.stopPolling();
    this.pollSub = interval(2500).subscribe(() => {
      this.translationService.getBatchJobStatus(jobId).subscribe({
        next: (job) => {
          this.activeBatchJob = job;
          this.cdr.markForCheck();

          if (job.status === 'COMPLETED' || job.status === 'FAILED' || job.status === 'CANCELLED') {
            this.stopPolling();
            if (job.status === 'COMPLETED') {
              this.snackBar.open(
                `Batch translation completed! ${job.successfulQuestions} questions translated and published.`,
                'Refresh',
                { duration: 6000 }
              ).onAction().subscribe(() => this.reload());
              this.reload();
            }
          }
        },
        error: (err) => console.warn('Failed to poll batch translation job:', err)
      });
    });
  }

  cancelActiveBatchJob(): void {
    if (!this.activeBatchJob) return;
    this.translationService.cancelBatchJob(this.activeBatchJob.id).subscribe({
      next: (job) => {
        this.activeBatchJob = job;
        this.stopPolling();
        this.snackBar.open('Batch translation job cancelled.', 'Close', { duration: 3000 });
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.snackBar.open(`Failed to cancel job: ${err?.message || 'Error'}`, 'Close', { duration: 4000 });
      }
    });
  }

  dismissBatchCard(): void {
    this.activeBatchJob = null;
    this.stopPolling();
  }

  private stopPolling(): void {
    if (this.pollSub) {
      this.pollSub.unsubscribe();
      this.pollSub = undefined;
    }
  }

  truncateContent(text: string): string {
    if (!text) return '';
    const clean = text.replace(/<[^>]*>/g, '');
    return clean.length > 80 ? clean.substring(0, 80) + '...' : clean;
  }

  getLanguageName(code: string): string {
    const found = this.languages.find(l => l.code === code);
    return found ? `${found.nativeName} (${found.name})` : code;
  }
}
