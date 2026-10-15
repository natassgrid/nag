import {
  Component,
  ChangeDetectionStrategy,
  inject,
  OnInit,
  signal,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MathRendererComponent } from '@nag-frontend-workspace/shared-ui-components';
import { MicroLessonAdminService } from '../../services/micro-lesson-admin.service';
import {
  MicroLessonReviewItem,
  ReviewMicroLessonRequest,
  BatchGenerateResponse,
} from '../../models/micro-lesson.model';
import { GenerateMicroLessonDialogComponent } from '../generate-micro-lesson-dialog/generate-micro-lesson-dialog.component';
import { BatchGenerateMicroLessonDialogComponent } from '../batch-generate-dialog/batch-generate-dialog.component';

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
    MatDialogModule,
    MathRendererComponent,
  ],
  templateUrl: './micro-lesson-review.component.html',
  styleUrls: ['./micro-lesson-review.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MicroLessonReviewComponent implements OnInit {
  private readonly microLessonService = inject(MicroLessonAdminService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  readonly lessons = signal<MicroLessonReviewItem[]>([]);
  readonly isLoading = signal(true);
  readonly statusFilter = signal<string>('ALL');
  readonly searchQuery = signal<string>('');
  readonly selectedSubject = signal<string>('ALL');
  readonly selectedTopic = signal<string>('ALL');

  readonly selectedLesson = signal<MicroLessonReviewItem | null>(null);
  readonly reviewerNotes = signal<string>('');
  readonly isSubmitting = signal(false);
  readonly isBatchGenerating = signal(false);

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

  readonly distinctSubjects = computed(() => {
    const list = this.lessons()
      .map((l) => l.subjectName)
      .filter((s): s is string => !!s && s.trim().length > 0);
    return ['ALL', ...Array.from(new Set(list)).sort()];
  });

  readonly distinctTopics = computed(() => {
    let list = this.lessons();
    const sub = this.selectedSubject();
    if (sub !== 'ALL') {
      list = list.filter((l) => l.subjectName === sub);
    }
    const topics = list
      .map((l) => l.topicName)
      .filter((t): t is string => !!t && t.trim().length > 0);
    return ['ALL', ...Array.from(new Set(topics)).sort()];
  });

  readonly filteredLessons = computed(() => {
    let list = this.lessons();
    const query = this.searchQuery().trim().toLowerCase();
    const status = this.statusFilter();
    const subject = this.selectedSubject();
    const topic = this.selectedTopic();

    if (status !== 'ALL') {
      list = list.filter((item) => item.status === status);
    }
    if (subject !== 'ALL') {
      list = list.filter((item) => item.subjectName === subject);
    }
    if (topic !== 'ALL') {
      list = list.filter((item) => item.topicName === topic);
    }
    if (query) {
      list = list.filter(
        (item) =>
          item.subtopicName.toLowerCase().includes(query) ||
          item.topicName.toLowerCase().includes(query) ||
          item.subjectName.toLowerCase().includes(query)
      );
    }
    return list;
  });

  readonly totalCount = computed(() => this.lessons().length);
  readonly aiGeneratedCount = computed(
    () => this.lessons().filter((l) => l.status === 'AI_GENERATED').length
  );
  readonly approvedCount = computed(
    () =>
      this.lessons().filter(
        (l) => l.status === 'APPROVED' || l.status === 'PUBLISHED'
      ).length
  );

  readonly isAllSelected = computed(() => {
    const items = this.filteredLessons();
    if (items.length === 0) return false;
    const selected = this.selectedIds();
    return items.every((i) => selected.has(i.id));
  });

  ngOnInit() {
    this.loadLessons();
  }

  loadLessons() {
    this.isLoading.set(true);
    this.microLessonService.listForReview().subscribe({
      next: (response) => {
        this.lessons.set(response.content || []);
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

  openGenerateDialog() {
    const dialogRef = this.dialog.open(GenerateMicroLessonDialogComponent, {
      width: '560px',
      panelClass: 'nag-dialog-clean',
    });

    dialogRef.afterClosed().subscribe((createdItem: MicroLessonReviewItem | null) => {
      if (createdItem) {
        this.lessons.update((list) => [createdItem, ...list]);
        this.selectLesson(createdItem);
        this.snackBar.open(
          `Synthesized micro-lesson: "${createdItem.subtopicName}"`,
          'View',
          { duration: 4000 }
        );
      }
    });
  }

  selectLesson(lesson: MicroLessonReviewItem) {
    this.selectedLesson.set(lesson);
    this.reviewerNotes.set(lesson.reviewerNotes || '');
    this.isEditingContent.set(false);
  }

  closePreview() {
    this.selectedLesson.set(null);
    this.reviewerNotes.set('');
    this.isEditingContent.set(false);
  }

  toggleSelectAll(checked: boolean) {
    if (checked) {
      const allIds = new Set(this.filteredLessons().map((i) => i.id));
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
            this.lessons.update((list) =>
              list.map((item) => (item.id === updated.id ? updated : item))
            );
            if (completedCount === ids.length) {
              this.isBulkSubmitting.set(false);
              this.selectedIds.set(new Set());
              this.snackBar.open(
                `Successfully updated ${ids.length} micro-lessons to ${action}.`,
                'OK',
                { duration: 3000 }
              );
            }
          },
          error: () => {
            completedCount++;
            if (completedCount === ids.length) {
              this.isBulkSubmitting.set(false);
              this.snackBar.open('Bulk moderation finished with warnings.', 'Close', {
                duration: 3000,
              });
            }
          },
        });
    });
  }

  startEditing(lesson: MicroLessonReviewItem) {
    this.editedCoreConcept.set(lesson.content?.coreConcept || '');
    this.editedTrapAlert.set(lesson.content?.trapAlert || '');
    this.editedFormulaCheatSheet.set(lesson.content?.formulaCheatSheet || '');
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
        this.lessons.update((list) =>
          list.map((item) => (item.id === updated.id ? updated : item))
        );
        this.selectedLesson.set(updated);
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
        this.lessons.update((list) =>
          list.map((item) => (item.id === updated.id ? updated : item))
        );
        this.selectedLesson.set(updated);
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
        this.snackBar.open(
          res.message || `Queued ${res.totalQueued} micro-lessons for batch generation.`,
          'OK',
          { duration: 4000 }
        );
        this.loadLessons();
      }
    });
  }

  triggerBatchGenerate() {
    this.openBatchGenerateDialog();
  }

  getStatusBadgeClass(status: string): string {
    switch (status) {
      case 'PUBLISHED':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      case 'APPROVED':
        return 'bg-blue-50 text-blue-700 border-blue-200';
      case 'IN_REVIEW':
        return 'bg-amber-50 text-amber-700 border-amber-200';
      case 'REJECTED':
        return 'bg-rose-50 text-rose-700 border-rose-200';
      case 'AI_GENERATED':
      default:
        return 'bg-purple-50 text-purple-700 border-purple-200';
    }
  }
}
