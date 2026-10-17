import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MathRendererComponent } from '@nag-frontend-workspace/shared-ui-components';
import { MicroLessonReviewItem } from '../../../../models/micro-lesson.model';

@Component({
  selector: 'app-micro-lesson-table',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, MathRendererComponent],
  templateUrl: './micro-lesson-table.component.html',
  styleUrl: './micro-lesson-table.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MicroLessonTableComponent {
  readonly lessons = input<MicroLessonReviewItem[]>([]);
  readonly selectedLessonId = input<string | null>(null);
  readonly selectedIds = input<Set<string>>(new Set());
  readonly isAllSelected = input<boolean>(false);
  readonly displayStart = input<number>(0);
  readonly displayEnd = input<number>(0);
  readonly totalElements = input<number>(0);
  readonly totalPages = input<number>(0);
  readonly pageIndex = input<number>(0);
  readonly pageSize = input<number>(20);
  readonly pageSizeOptions = input<number[]>([10, 20, 50]);
  readonly isLoading = input<boolean>(false);

  readonly lessonSelect = output<MicroLessonReviewItem>();
  readonly editLesson = output<{ lesson: MicroLessonReviewItem; event?: Event }>();
  readonly toggleSelect = output<{ id: string; event: Event }>();
  readonly toggleSelectAll = output<boolean>();
  readonly deleteLesson = output<{ lesson: MicroLessonReviewItem; event?: Event }>();
  readonly pageSizeChange = output<number>();
  readonly firstPage = output<void>();
  readonly prevPage = output<void>();
  readonly nextPage = output<void>();
  readonly lastPage = output<void>();

  getStatusBadgeClass(status: string): string {
    switch (status) {
      case 'APPROVED':
        return 'bg-blue-50 text-blue-700 border-blue-200';
      case 'PUBLISHED':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      case 'REJECTED':
        return 'bg-rose-50 text-rose-700 border-rose-200';
      case 'IN_REVIEW':
        return 'bg-amber-50 text-amber-700 border-amber-200';
      default:
        return 'bg-purple-50 text-purple-700 border-purple-200';
    }
  }
}
