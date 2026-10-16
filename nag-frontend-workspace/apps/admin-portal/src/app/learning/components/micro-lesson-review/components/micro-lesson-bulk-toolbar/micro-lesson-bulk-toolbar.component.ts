import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-micro-lesson-bulk-toolbar',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './micro-lesson-bulk-toolbar.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MicroLessonBulkToolbarComponent {
  readonly selectedCount = input<number>(0);
  readonly isBulkSubmitting = input<boolean>(false);
  readonly isBulkDeleting = input<boolean>(false);

  readonly bulkSubmit = output<'APPROVE' | 'REJECT' | 'PUBLISH'>();
  readonly bulkDelete = output<void>();
  readonly clearSelection = output<void>();
}
