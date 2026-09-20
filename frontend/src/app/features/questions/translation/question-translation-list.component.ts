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
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatMenuModule } from '@angular/material/menu';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { QuestionService, QuestionResponse } from '../question.service';
import { TranslationService, SUPPORTED_LANGUAGES, BatchTranslationJobResponse } from './translation.service';
import { SubjectTopicService, Subject } from '../subject-topic.service';
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
    MatSnackBarModule,
    MatChipsModule,
    MatTooltipModule,
    MatCardModule,
    MatProgressBarModule,
    MatMenuModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatSlideToggleModule,
    MatCheckboxModule,
    PaginatedTableComponent,
    PageHeaderComponent,
    QuestionTranslationDialogComponent
  ],
  templateUrl: './question-translation-list.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./question-translation-list.component.scss']
})
export class QuestionTranslationListComponent implements OnInit {

  @ViewChild('paginatedTable') paginatedTable!: PaginatedTableComponent<QuestionResponse>;

  drawerOpen = false;
  batchModalOpen = false;
  selectedQuestion?: QuestionResponse;
  selectedLanguageForDrawer: string = 'hi';
  selectedLanguage: string = 'hi';

  activeBatchJob?: BatchTranslationJobResponse;

  languages = SUPPORTED_LANGUAGES;
  subjects: Subject[] = [];

  // Batch modal form fields
  batchSourceLanguage = 'en';
  batchTargetLanguage = 'hi';
  batchTargetStatus = 'PUBLISHED';
  batchSubjectFilter = '';
  batchOverwriteExisting = false;
  isSubmittingBatch = false;

  get batchTargetLang(): string { return this.batchTargetLanguage; }
  set batchTargetLang(val: string) { this.batchTargetLanguage = val; }

  get batchSubject(): string { return this.batchSubjectFilter; }
  set batchSubject(val: string) { this.batchSubjectFilter = val; }

  get batchOverwrite(): boolean { return this.batchOverwriteExisting; }
  set batchOverwrite(val: boolean) { this.batchOverwriteExisting = val; }

  get batchSubmitting(): boolean { return this.isSubmittingBatch; }
  set batchSubmitting(val: boolean) { this.isSubmittingBatch = val; }

  filters: Record<string, any> = {
    targetLang: 'hi'
  };

  filterCategories: FilterCategory[] = [
    {
      key: 'targetLang',
      label: 'Target Language',
      expanded: true,
      options: SUPPORTED_LANGUAGES.map(l => ({ label: `${l.name} (${l.nativeName})`, value: l.code }))
    },
    {
      key: 'translationStatus',
      label: 'Translation Status',
      expanded: true,
      options: [
        { label: 'All', value: 'ALL' },
        { label: 'Missing / Untranslated', value: 'MISSING' },
        { label: 'Draft', value: 'DRAFT' },
        { label: 'Pending Review', value: 'PENDING_REVIEW' },
        { label: 'Approved & Published', value: 'APPROVED_PUBLISHED' },
        { label: 'Rejected', value: 'REJECTED' }
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
    {
      key: 'translationStatus',
      header: 'Translation Status',
      type: 'chip',
      cell: (row) => this.getTranslationStatusLabel(row),
      chipClass: (val, row) => this.getTranslationStatusClass(row)
    },
    { key: 'actions', header: 'Localization', type: 'actions' }
  ];

  fetcher: PaginatedDataFetcher<QuestionResponse> = (req) => {
    const rawSubject = Array.isArray(this.filters['subject']) ? this.filters['subject'][0] : this.filters['subject'];
    const activeDifficulty = Array.isArray(this.filters['difficulty']) ? this.filters['difficulty'][0] : this.filters['difficulty'];
    const rawTargetLang = Array.isArray(this.filters['targetLang']) ? this.filters['targetLang'][0] : this.filters['targetLang'];
    const rawTransStatus = Array.isArray(this.filters['translationStatus']) ? this.filters['translationStatus'][0] : this.filters['translationStatus'];

    const isNumericSubject = rawSubject !== undefined && rawSubject !== null && rawSubject !== '' && !isNaN(Number(rawSubject));
    const subjectId = isNumericSubject ? Number(rawSubject) : undefined;
    const subject = !isNumericSubject && rawSubject ? String(rawSubject) : undefined;

    return this.questionService.getQuestions({
      subject,
      subjectId,
      difficulty: activeDifficulty || undefined,
      targetLang: rawTargetLang && rawTargetLang !== '' ? rawTargetLang : undefined,
      translationStatus: rawTransStatus && rawTransStatus !== 'ALL' && rawTransStatus !== '' ? rawTransStatus : undefined,
      search: req.search,
      sort: req.sort,
      order: req.order,
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

  loadSubjects(): void {
    this.subjectTopicService.getSubjects().subscribe({
      next: (subjects) => {
        this.subjects = subjects || [];
        const subjectOptions = this.subjects.length > 0
          ? this.subjects.map(s => ({ label: s.name, value: s.id.toString() }))
          : DEFAULT_SUBJECT_OPTIONS;
        this.filterCategories = this.filterCategories.map(cat => {
          if (cat.key === 'subject') {
            return { ...cat, options: subjectOptions };
          }
          return cat;
        });
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.warn('Failed to load subjects:', err);
      }
    });
  }

  checkForActiveBatchJob(): void {
    const savedJobId = localStorage.getItem('active_batch_translation_job_id');
    if (savedJobId) {
      this.translationService.getBatchJobStatus(savedJobId).subscribe({
        next: (job) => {
          if (job.status === 'PENDING' || job.status === 'IN_PROGRESS') {
            this.activeBatchJob = job;
            this.pollBatchJob(job.id || job.jobId || savedJobId);
          } else {
            localStorage.removeItem('active_batch_translation_job_id');
          }
          this.cdr.markForCheck();
        },
        error: () => {
          localStorage.removeItem('active_batch_translation_job_id');
        }
      });
    }
  }

  pollBatchJob(jobId: string): void {
    setTimeout(() => {
      const currentId = this.activeBatchJob?.id || this.activeBatchJob?.jobId;
      if (!this.activeBatchJob || currentId !== jobId) return;
      this.translationService.getBatchJobStatus(jobId).subscribe({
        next: (job) => {
          this.activeBatchJob = job;
          this.cdr.markForCheck();
          if (job.status === 'PENDING' || job.status === 'IN_PROGRESS') {
            this.pollBatchJob(jobId);
          } else {
            localStorage.removeItem('active_batch_translation_job_id');
            this.snackBar.open(`Batch Translation Completed: ${job.successfulQuestions ?? job.translatedCount ?? 0} translated`, 'Close', { duration: 5000 });
            this.reload();
          }
        }
      });
    }, 3000);
  }

  onFilterChange(updatedFilters: Record<string, any>): void {
    this.filters = { ...updatedFilters };
    if (updatedFilters['targetLang']) {
      this.selectedLanguage = Array.isArray(updatedFilters['targetLang'])
        ? updatedFilters['targetLang'][0]
        : updatedFilters['targetLang'];
      this.batchTargetLanguage = this.selectedLanguage;
    }
  }

  reload(): void {
    this.paginatedTable?.reload();
  }

  getActiveTargetLang(): string {
    return this.selectedLanguage || 'hi';
  }

  getLanguageName(code: string): string {
    const lang = this.translationService.getLanguage(code);
    return lang ? `${lang.name} (${lang.nativeName})` : code;
  }

  openTranslationDrawer(question: QuestionResponse, langCode = 'hi'): void {
    this.selectedQuestion = question;
    this.selectedLanguageForDrawer = langCode;
    this.drawerOpen = true;
  }

  onDrawerClose(saved?: boolean): void {
    this.drawerOpen = false;
    this.selectedQuestion = undefined;
    if (saved) {
      this.reload();
    }
  }

  openBatchModal(): void {
    this.batchModalOpen = true;
  }

  closeBatchModal(): void {
    this.batchModalOpen = false;
  }

  triggerBatchAutoTranslate(): void {
    this.submitBatchTranslation();
  }

  submitBatchTranslation(): void {
    this.isSubmittingBatch = true;
    this.translationService.startBatchTranslation({
      sourceLanguage: this.batchSourceLanguage,
      targetLanguage: this.batchTargetLanguage,
      targetStatus: this.batchTargetStatus,
      subject: this.batchSubjectFilter || undefined,
      overwriteExisting: this.batchOverwriteExisting
    }).subscribe({
      next: (job) => {
        this.isSubmittingBatch = false;
        this.batchModalOpen = false;
        this.activeBatchJob = job;
        const jobId = job.id || job.jobId;
        if (jobId) {
          localStorage.setItem('active_batch_translation_job_id', jobId);
          this.pollBatchJob(jobId);
        }
        this.snackBar.open(`Batch auto-translation job started (${job.totalQuestions} questions)`, 'Close', { duration: 4000 });
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.isSubmittingBatch = false;
        this.snackBar.open(err?.error?.message || 'Failed to start batch translation', 'Close', { duration: 5000 });
        this.cdr.markForCheck();
      }
    });
  }

  cancelActiveBatchJob(): void {
    if (!this.activeBatchJob) return;
    const jobId = this.activeBatchJob.id || this.activeBatchJob.jobId;
    if (!jobId) return;
    this.translationService.cancelBatchJob(jobId).subscribe({
      next: (job) => {
        this.activeBatchJob = job;
        localStorage.removeItem('active_batch_translation_job_id');
        this.snackBar.open('Batch job cancelled', 'Close', { duration: 3000 });
        this.cdr.markForCheck();
      }
    });
  }

  dismissBatchCard(): void {
    this.activeBatchJob = undefined;
  }

  getTranslationStatusLabel(question: QuestionResponse): string {
    const lang = this.selectedLanguage || 'hi';
    const status = question.translationStatusMap?.[lang] || question.translationStatus || 'MISSING';
    switch (status) {
      case 'APPROVED':
      case 'APPROVED_PUBLISHED':
        return 'Translated & Approved';
      case 'PENDING_REVIEW':
      case 'REVIEW':
        return 'Review Pending';
      case 'DRAFT':
        return 'Draft In Progress';
      case 'REJECTED':
        return 'Rejected / Needs Fix';
      case 'MISSING':
      default:
        return 'Missing';
    }
  }

  getTranslationStatusClass(question: QuestionResponse): string {
    const lang = this.selectedLanguage || 'hi';
    const status = question.translationStatusMap?.[lang] || question.translationStatus || 'MISSING';
    switch (status) {
      case 'APPROVED':
      case 'APPROVED_PUBLISHED':
        return 'status-approved';
      case 'PENDING_REVIEW':
      case 'REVIEW':
        return 'status-review';
      case 'DRAFT':
        return 'status-draft';
      case 'REJECTED':
        return 'status-rejected';
      case 'MISSING':
      default:
        return 'status-missing';
    }
  }

  truncateContent(content: string, limit = 80): string {
    if (!content) return '';
    const clean = content.replace(/<[^>]*>/g, '').replace(/&nbsp;/g, ' ');
    return clean.length > limit ? clean.substring(0, limit) + '...' : clean;
  }
}
