import {
  Component,
  ChangeDetectionStrategy,
  inject,
  OnInit,
  OnDestroy,
  signal,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import {
  MathRendererComponent,
  NotificationService,
} from '@nag-frontend-workspace/shared-ui-components';
import { MicroLessonAdminService } from '../../services/micro-lesson-admin.service';
import {
  MicroLessonReviewItem,
  ReviewMicroLessonRequest,
  BatchGenerateResponse,
  MicroLessonStats,
  adaptToReviewItem,
} from '../../models/micro-lesson.model';
import { GenerateMicroLessonDialogComponent } from '../generate-micro-lesson-dialog/generate-micro-lesson-dialog.component';
import { BatchGenerateMicroLessonDialogComponent } from '../batch-generate-dialog/batch-generate-dialog.component';
import { BatchTranslateMicroLessonDialogComponent } from '../batch-translate-micro-lesson-dialog/batch-translate-micro-lesson-dialog.component';
import { Subscription, timer } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import {
  MicroLessonStatsComponent,
  MicroLessonBatchBannerComponent,
  MicroLessonFiltersComponent,
  MicroLessonBulkToolbarComponent,
  MicroLessonTableComponent,
  MicroLessonDetailDrawerComponent,
} from './components';

@Component({
  selector: 'app-micro-lesson-review',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatProgressBarModule,
    MatDialogModule,
    MicroLessonStatsComponent,
    MicroLessonBatchBannerComponent,
    MicroLessonFiltersComponent,
    MicroLessonBulkToolbarComponent,
    MicroLessonTableComponent,
    MicroLessonDetailDrawerComponent,
  ],
  templateUrl: './micro-lesson-review.component.html',
  styleUrls: ['./micro-lesson-review.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MicroLessonReviewComponent implements OnInit, OnDestroy {
  private readonly microLessonService = inject(MicroLessonAdminService);
  private readonly notificationService = inject(NotificationService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);

  readonly lessons = signal<MicroLessonReviewItem[]>([]);
  readonly isLoading = signal(true);
  readonly statusFilter = signal<string>('ALL');
  readonly searchQuery = signal<string>('');
  readonly selectedSubject = signal<string>('ALL');
  readonly selectedTopic = signal<string>('ALL');

  // Dynamic Metrics & Statistics
  readonly stats = signal<MicroLessonStats>({
    total: 0,
    aiGenerated: 0,
    inReview: 0,
    approved: 0,
    published: 0,
    rejected: 0,
  });
  readonly totalCount = computed(() => this.stats().total);
  readonly aiGeneratedCount = computed(() => this.stats().aiGenerated);
  readonly approvedCount = computed(
    () => this.stats().approved + this.stats().published
  );

  // Dynamic DB-Backed Filter Options
  readonly availableSubjects = signal<string[]>(['ALL']);
  readonly availableTopics = signal<string[]>(['ALL']);
  readonly distinctSubjects = computed(() => this.availableSubjects());
  readonly distinctTopics = computed(() => this.availableTopics());

  // Pagination State
  readonly pageIndex = signal<number>(0);
  readonly pageSize = signal<number>(10);
  readonly totalElements = signal<number>(0);
  readonly totalPages = signal<number>(0);
  readonly pageSizeOptions = [10, 20, 50, 100];

  readonly displayStart = computed(() =>
    this.totalElements() === 0 ? 0 : this.pageIndex() * this.pageSize() + 1
  );
  readonly displayEnd = computed(() =>
    Math.min((this.pageIndex() + 1) * this.pageSize(), this.totalElements())
  );

  // Filtered lessons matches current paginated DB page
  readonly filteredLessons = computed(() => this.lessons());

  readonly selectedLesson = signal<MicroLessonReviewItem | null>(null);
  readonly reviewerNotes = signal<string>('');
  readonly isSubmitting = signal(false);
  readonly isBatchGenerating = signal(false);

  // Active Batch Progress Tracking
  readonly activeBatchJob = signal<BatchGenerateResponse | null>(null);
  readonly isCancellingBatch = signal<boolean>(false);
  private activeBatchSub?: Subscription;

  // Delete State
  readonly isDeleting = signal(false);
  readonly isBulkDeleting = signal(false);

  // Bulk Selection State
  readonly selectedIds = signal<Set<string>>(new Set());
  readonly isBulkSubmitting = signal(false);

  // In-Place Content Editor State
  readonly isEditingContent = signal<boolean>(false);
  readonly editedCoreConcept = signal<string>('');
  readonly editedTrapAlert = signal<string>('');
  readonly editedFormulaCheatSheet = signal<string>('');

  readonly statusOptions = [
    { label: 'All', value: 'ALL' },
    { label: 'AI Generated', value: 'AI_GENERATED' },
    { label: 'In Review', value: 'IN_REVIEW' },
    { label: 'Approved', value: 'APPROVED' },
    { label: 'Published', value: 'PUBLISHED' },
    { label: 'Rejected', value: 'REJECTED' },
  ];

  readonly isAllSelected = computed(() => {
    const items = this.lessons();
    if (items.length === 0) return false;
    const selected = this.selectedIds();
    return items.every((i) => selected.has(i.id));
  });

  ngOnInit() {
    this.loadStats();
    this.loadFilterOptions();
    this.loadLessons();
    this.checkActiveBatchJob();
  }

  ngOnDestroy(): void {
    this.stopActiveBatchPolling();
  }

  checkActiveBatchJob(): void {
    this.microLessonService.getActiveBatchJob().subscribe({
      next: (job) => {
        if (job) {
          this.activeBatchJob.set(job);
          if (job.status === 'IN_PROGRESS' || job.status === 'QUEUED') {
            if (job.batchId) {
              this.startActiveBatchPolling(job.batchId);
            }
          }
        }
      },
      error: () => {},
    });
  }

  startActiveBatchPolling(batchId: string): void {
    this.stopActiveBatchPolling();
    this.activeBatchSub = timer(2000, 2000)
      .pipe(switchMap(() => this.microLessonService.getBatchJobStatus(batchId)))
      .subscribe({
        next: (job) => {
          this.activeBatchJob.set(job);
          if (
            job.status === 'COMPLETED' ||
            job.status === 'FAILED' ||
            job.status === 'CANCELLED'
          ) {
            this.stopActiveBatchPolling();
            this.loadLessons();
            this.loadStats();
            this.loadFilterOptions(
              this.selectedSubject() === 'ALL' ? undefined : this.selectedSubject()
            );
            if (job.status === 'COMPLETED') {
              this.snackBar.open(
                `Batch generation complete: ${job.totalGenerated} micro-lessons generated!`,
                'OK',
                { duration: 5000 }
              );
            }
          }
        },
        error: (err) => console.error('Failed to poll active batch job', err),
      });
  }

  stopActiveBatchPolling(): void {
    if (this.activeBatchSub) {
      this.activeBatchSub.unsubscribe();
      this.activeBatchSub = undefined;
    }
  }

  cancelActiveBatch(): void {
    const job = this.activeBatchJob();
    if (!job?.batchId) return;

    this.isCancellingBatch.set(true);
    this.microLessonService.cancelBatchJob(job.batchId).subscribe({
      next: (res) => {
        this.isCancellingBatch.set(false);
        this.activeBatchJob.set(res);
        this.stopActiveBatchPolling();
        this.loadLessons();
        this.loadStats();
        this.snackBar.open('Batch generation cancelled.', 'OK', { duration: 3000 });
      },
      error: (err) => {
        this.isCancellingBatch.set(false);
        console.error('Failed to cancel batch', err);
      },
    });
  }

  dismissBatchBanner(): void {
    this.activeBatchJob.set(null);
  }

  loadStats() {
    this.microLessonService.getStats().subscribe({
      next: (stats) => this.stats.set(stats),
      error: (err) => console.error('Failed to load stats', err),
    });
  }

  loadFilterOptions(subject?: string) {
    this.microLessonService.getFilterOptions(subject).subscribe({
      next: (opts) => {
        if (opts.subjects && opts.subjects.length > 0) {
          const subjs = ['ALL', ...Array.from(new Set(opts.subjects)).sort()];
          this.availableSubjects.set(subjs);
        }
        if (opts.topics) {
          const tops = ['ALL', ...Array.from(new Set(opts.topics)).sort()];
          this.availableTopics.set(tops);
        }
      },
      error: (err) => console.error('Failed to load filter options', err),
    });
  }

  loadLessons() {
    this.isLoading.set(true);
    this.microLessonService
      .listForReview({
        status: this.statusFilter(),
        subject: this.selectedSubject(),
        topic: this.selectedTopic(),
        query: this.searchQuery(),
        page: this.pageIndex(),
        size: this.pageSize(),
        sortBy: 'createdAt',
        direction: 'desc',
      })
      .subscribe({
        next: (response) => {
          const items = (response.content || []).map(adaptToReviewItem);
          this.lessons.set(items);
          this.totalElements.set(response.totalElements || 0);
          this.totalPages.set(response.totalPages || 0);
          this.isLoading.set(false);
        },
        error: (err) => {
          console.error('Failed to load micro-lessons for review', err);
          this.isLoading.set(false);
          this.snackBar.open('Failed to load micro-lessons', 'Close', {
            duration: 3000,
          });
        },
      });
  }

  onSubjectChange(subject: string) {
    this.selectedSubject.set(subject);
    this.selectedTopic.set('ALL');
    this.pageIndex.set(0);
    this.loadFilterOptions(subject === 'ALL' ? undefined : subject);
    this.loadLessons();
  }

  onTopicChange(topic: string) {
    this.selectedTopic.set(topic);
    this.pageIndex.set(0);
    this.loadLessons();
  }

  onStatusChange(status: string) {
    this.statusFilter.set(status);
    this.pageIndex.set(0);
    this.loadLessons();
  }

  onSearchInput(query: string) {
    this.searchQuery.set(query);
    this.pageIndex.set(0);
    this.loadLessons();
  }

  nextPage() {
    if (this.pageIndex() < this.totalPages() - 1) {
      this.pageIndex.update((p) => p + 1);
      this.loadLessons();
    }
  }

  prevPage() {
    if (this.pageIndex() > 0) {
      this.pageIndex.update((p) => p - 1);
      this.loadLessons();
    }
  }

  firstPage() {
    if (this.pageIndex() !== 0) {
      this.pageIndex.set(0);
      this.loadLessons();
    }
  }

  lastPage() {
    const last = Math.max(0, this.totalPages() - 1);
    if (this.pageIndex() !== last) {
      this.pageIndex.set(last);
      this.loadLessons();
    }
  }

  onPageSizeChange(size: number | string) {
    this.pageSize.set(Number(size));
    this.pageIndex.set(0);
    this.loadLessons();
  }

  async deleteLesson(lesson: MicroLessonReviewItem, event?: Event) {
    if (event) {
      event.stopPropagation();
    }
    const title = lesson.subtopicName || lesson.subtopic || 'this micro-lesson';
    const confirmed = await this.notificationService.confirm({
      title: 'Delete Micro-Lesson?',
      message: `Are you sure you want to permanently delete "${title}"? This action cannot be undone.`,
      confirmText: 'Delete Permanently',
      cancelText: 'Cancel',
      type: 'danger',
    });

    if (!confirmed) return;

    this.isDeleting.set(true);
    this.microLessonService.deleteMicroLesson(lesson.id).subscribe({
      next: () => {
        this.isDeleting.set(false);
        if (this.selectedLesson()?.id === lesson.id) {
          this.closePreview();
        }
        this.selectedIds.update((set) => {
          const updated = new Set(set);
          updated.delete(lesson.id);
          return updated;
        });
        this.snackBar.open(`Deleted micro-lesson: "${title}"`, 'OK', {
          duration: 3000,
        });
        this.loadLessons();
        this.loadStats();
        this.loadFilterOptions(
          this.selectedSubject() === 'ALL' ? undefined : this.selectedSubject()
        );
      },
      error: (err) => {
        this.isDeleting.set(false);
        this.snackBar.open(
          err?.error?.message || 'Failed to delete micro-lesson.',
          'Close',
          { duration: 3000 }
        );
      },
    });
  }

  async bulkDeleteSelected() {
    const ids = Array.from(this.selectedIds());
    if (ids.length === 0) return;

    const confirmed = await this.notificationService.confirm({
      title: `Delete ${ids.length} Micro-Lessons?`,
      message: `Are you sure you want to permanently delete the ${ids.length} selected micro-lessons? This action cannot be undone.`,
      confirmText: 'Delete Selected',
      cancelText: 'Cancel',
      type: 'danger',
    });

    if (!confirmed) return;

    this.isBulkDeleting.set(true);
    this.microLessonService.bulkDelete(ids).subscribe({
      next: (res) => {
        this.isBulkDeleting.set(false);
        this.selectedIds.set(new Set());
        if (this.selectedLesson() && ids.includes(this.selectedLesson()!.id)) {
          this.closePreview();
        }
        this.snackBar.open(
          `Successfully deleted ${res.deletedCount} micro-lessons.`,
          'OK',
          { duration: 3000 }
        );
        this.loadLessons();
        this.loadStats();
        this.loadFilterOptions(
          this.selectedSubject() === 'ALL' ? undefined : this.selectedSubject()
        );
      },
      error: (err) => {
        this.isBulkDeleting.set(false);
        this.snackBar.open(
          err?.error?.message || 'Failed to bulk delete micro-lessons.',
          'Close',
          { duration: 3000 }
        );
      },
    });
  }

  openAuthoringStudio(lesson?: MicroLessonReviewItem, event?: Event) {
    if (event) {
      event.stopPropagation();
    }
    if (lesson?.id) {
      this.router.navigate(['/learning/micro-lessons/authoring'], {
        queryParams: { edit: lesson.id },
      });
    } else {
      this.router.navigate(['/learning/micro-lessons/authoring']);
    }
  }

  openGenerateDialog() {
    const dialogRef = this.dialog.open(GenerateMicroLessonDialogComponent, {
      width: '560px',
      panelClass: 'nag-dialog-clean',
    });

    dialogRef.afterClosed().subscribe((createdItem: MicroLessonReviewItem | any | null) => {
      if (createdItem) {
        const item = adaptToReviewItem(createdItem);
        this.selectLesson(item);
        const name = item.subtopicName || item.subtopic || 'New Lesson';
        this.snackBar.open(
          `Synthesized micro-lesson: "${name}"`,
          'View',
          { duration: 4000 }
        );
        this.loadLessons();
        this.loadStats();
        this.loadFilterOptions(
          this.selectedSubject() === 'ALL' ? undefined : this.selectedSubject()
        );
      }
    });
  }

  selectLesson(lesson: MicroLessonReviewItem) {
    const adapted = adaptToReviewItem(lesson);
    this.selectedLesson.set(adapted);
    this.reviewerNotes.set(adapted.reviewerNotes || '');
    this.isEditingContent.set(false);
  }

  closePreview() {
    this.selectedLesson.set(null);
    this.reviewerNotes.set('');
    this.isEditingContent.set(false);
  }

  toggleSelectAll(checked: boolean) {
    if (checked) {
      const allIds = new Set(this.lessons().map((i) => i.id));
      this.selectedIds.set(allIds);
    } else {
      this.selectedIds.set(new Set());
    }
  }

  toggleSelect(id: string, event: Event) {
    event.stopPropagation();
    this.selectedIds.update((set) => {
      const updated = new Set(set);
      if (updated.has(id)) {
        updated.delete(id);
      } else {
        updated.add(id);
      }
      return updated;
    });
  }

  clearSelection() {
    this.selectedIds.set(new Set());
  }

  bulkSubmit(action: 'APPROVE' | 'REJECT' | 'PUBLISH') {
    const ids = Array.from(this.selectedIds());
    if (ids.length === 0) return;

    this.isBulkSubmitting.set(true);
    let completedCount = 0;

    ids.forEach((id) => {
      this.microLessonService
        .reviewMicroLesson(id, { action, reviewerNotes: 'Bulk moderated by curriculum faculty.' })
        .subscribe({
          next: (updated) => {
            completedCount++;
            const adapted = adaptToReviewItem(updated);
            this.lessons.update((list) =>
              list.map((item) => (item.id === adapted.id ? adapted : item))
            );
            if (completedCount === ids.length) {
              this.isBulkSubmitting.set(false);
              this.selectedIds.set(new Set());
              this.snackBar.open(
                `Successfully updated ${ids.length} micro-lessons to ${action}.`,
                'OK',
                { duration: 3000 }
              );
              this.loadStats();
            }
          },
          error: () => {
            completedCount++;
            if (completedCount === ids.length) {
              this.isBulkSubmitting.set(false);
              this.snackBar.open('Bulk moderation finished with warnings.', 'Close', {
                duration: 3000,
              });
              this.loadStats();
            }
          },
        });
    });
  }

  startEditing(lesson: MicroLessonReviewItem) {
    this.editedCoreConcept.set(lesson.content?.coreConcept || lesson.coreConcept || '');
    this.editedTrapAlert.set(lesson.content?.trapAlert || lesson.trap || '');
    this.editedFormulaCheatSheet.set(
      lesson.content?.formulaCheatSheet ||
      (Array.isArray(lesson.cheatSheet) ? lesson.cheatSheet.join('\n\n') : '')
    );
    this.isEditingContent.set(true);
  }

  cancelEditing() {
    this.isEditingContent.set(false);
  }

  saveEditedContent() {
    const current = this.selectedLesson();
    if (!current) return;

    this.isSubmitting.set(true);
    const updatedContent = {
      ...current.content,
      coreConcept: this.editedCoreConcept(),
      trapAlert: this.editedTrapAlert(),
      formulaCheatSheet: this.editedFormulaCheatSheet(),
    };

    const req: ReviewMicroLessonRequest = {
      action: 'EDIT',
      reviewerNotes: this.reviewerNotes(),
      updatedContent,
    };

    this.microLessonService.reviewMicroLesson(current.id, req).subscribe({
      next: (updated) => {
        this.isSubmitting.set(false);
        this.isEditingContent.set(false);
        this.snackBar.open('Micro-lesson content and LaTeX updated successfully.', 'OK', {
          duration: 3000,
        });
        const adapted = adaptToReviewItem(updated);
        this.lessons.update((list) =>
          list.map((item) => (item.id === adapted.id ? adapted : item))
        );
        this.selectedLesson.set(adapted);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        console.error('Failed to update content', err);
        this.snackBar.open(
          err?.error?.message || 'Failed to update micro-lesson content.',
          'Close',
          { duration: 3000 }
        );
      },
    });
  }

  submitReview(action: 'APPROVE' | 'REJECT' | 'PUBLISH') {
    const current = this.selectedLesson();
    if (!current) return;

    this.isSubmitting.set(true);
    const req: ReviewMicroLessonRequest = {
      action,
      reviewerNotes: this.reviewerNotes(),
    };

    this.microLessonService.reviewMicroLesson(current.id, req).subscribe({
      next: (updated) => {
        this.isSubmitting.set(false);
        this.snackBar.open(`Micro-lesson updated to ${action}`, 'OK', {
          duration: 3000,
        });
        const adapted = adaptToReviewItem(updated);
        this.lessons.update((list) =>
          list.map((item) => (item.id === adapted.id ? adapted : item))
        );
        this.selectedLesson.set(adapted);
        this.loadStats();
      },
      error: (err) => {
        this.isSubmitting.set(false);
        console.error('Failed to submit review', err);
        this.snackBar.open('Failed to submit review', 'Close', {
          duration: 3000,
        });
      },
    });
  }

  openBatchGenerateDialog() {
    const dialogRef = this.dialog.open(BatchGenerateMicroLessonDialogComponent, {
      width: '560px',
      panelClass: 'nag-dialog-clean',
    });

    dialogRef.afterClosed().subscribe((res: BatchGenerateResponse | null) => {
      if (res) {
        if (res.status === 'IN_PROGRESS' || res.status === 'QUEUED') {
          this.activeBatchJob.set(res);
          if (res.batchId) {
            this.startActiveBatchPolling(res.batchId);
          }
        } else {
          this.activeBatchJob.set(res);
          this.snackBar.open(
            res.message || `Batch generation complete (${res.totalGenerated} generated).`,
            'OK',
            { duration: 4000 }
          );
        }
        this.loadLessons();
        this.loadStats();
        this.loadFilterOptions(
          this.selectedSubject() === 'ALL' ? undefined : this.selectedSubject()
        );
      }
    });
  }

  openBatchTranslateDialog() {
    const dialogRef = this.dialog.open(BatchTranslateMicroLessonDialogComponent, {
      width: '560px',
      panelClass: 'nag-dialog-clean',
    });

    dialogRef.afterClosed().subscribe((res) => {
      if (res) {
        this.loadLessons();
        this.loadStats();
      }
    });
  }

  getStatusBadgeClass(status: string): string {
    switch (status) {
      case 'AI_GENERATED':
        return 'bg-purple-50 text-purple-700 border-purple-200';
      case 'IN_REVIEW':
        return 'bg-amber-50 text-amber-700 border-amber-200';
      case 'APPROVED':
        return 'bg-blue-50 text-blue-700 border-blue-200';
      case 'PUBLISHED':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      case 'REJECTED':
        return 'bg-rose-50 text-rose-700 border-rose-200';
      default:
        return 'bg-slate-50 text-slate-700 border-slate-200';
    }
  }
}
