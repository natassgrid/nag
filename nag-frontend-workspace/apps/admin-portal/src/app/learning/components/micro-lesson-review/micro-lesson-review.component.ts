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
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MathRendererComponent } from '@nag-frontend-workspace/shared-ui-components';
import { MicroLessonAdminService } from '../../services/micro-lesson-admin.service';
import {
  MicroLessonReviewItem,
  ReviewMicroLessonRequest,
} from '../../models/micro-lesson.model';

@Component({
  selector: 'app-micro-lesson-review',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MathRendererComponent,
  ],
  templateUrl: './micro-lesson-review.component.html',
  styleUrls: ['./micro-lesson-review.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MicroLessonReviewComponent implements OnInit {
  private readonly microLessonService = inject(MicroLessonAdminService);
  private readonly snackBar = inject(MatSnackBar);

  readonly lessons = signal<MicroLessonReviewItem[]>([]);
  readonly isLoading = signal(true);
  readonly statusFilter = signal<string>('ALL');
  readonly searchQuery = signal<string>('');
  readonly selectedLesson = signal<MicroLessonReviewItem | null>(null);
  readonly reviewerNotes = signal<string>('');
  readonly isSubmitting = signal(false);
  readonly isBatchGenerating = signal(false);

  readonly statusOptions = [
    { label: 'All', value: 'ALL' },
    { label: 'AI Generated', value: 'AI_GENERATED' },
    { label: 'In Review', value: 'IN_REVIEW' },
    { label: 'Approved', value: 'APPROVED' },
    { label: 'Published', value: 'PUBLISHED' },
    { label: 'Rejected', value: 'REJECTED' },
  ];

  readonly filteredLessons = computed(() => {
    let list = this.lessons();
    const query = this.searchQuery().trim().toLowerCase();
    if (query) {
      list = list.filter(
        (item) =>
          item.subtopicName.toLowerCase().includes(query) ||
          item.topicName.toLowerCase().includes(query) ||
          item.subjectName.toLowerCase().includes(query)
      );
    }
    const status = this.statusFilter();
    if (status !== 'ALL') {
      list = list.filter((item) => item.status === status);
    }
    return list;
  });

  readonly totalCount = computed(() => this.lessons().length);
  readonly aiGeneratedCount = computed(
    () => this.lessons().filter((l) => l.status === 'AI_GENERATED').length
  );
  readonly approvedCount = computed(
    () => this.lessons().filter((l) => l.status === 'APPROVED' || l.status === 'PUBLISHED').length
  );

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

  selectLesson(lesson: MicroLessonReviewItem) {
    this.selectedLesson.set(lesson);
    this.reviewerNotes.set(lesson.reviewerNotes || '');
  }

  closePreview() {
    this.selectedLesson.set(null);
    this.reviewerNotes.set('');
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
        // Update item in local list
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

  triggerBatchGenerate() {
    this.isBatchGenerating.set(true);
    this.microLessonService.triggerBatchGenerate({}).subscribe({
      next: (res) => {
        this.isBatchGenerating.set(false);
        this.snackBar.open(
          res.message || `Queued ${res.totalQueued} micro-lessons for generation`,
          'OK',
          { duration: 4000 }
        );
        this.loadLessons();
      },
      error: (err) => {
        this.isBatchGenerating.set(false);
        console.error('Failed to trigger batch generation', err);
        this.snackBar.open('Batch generation trigger failed', 'Close', {
          duration: 3000,
        });
      },
    });
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
