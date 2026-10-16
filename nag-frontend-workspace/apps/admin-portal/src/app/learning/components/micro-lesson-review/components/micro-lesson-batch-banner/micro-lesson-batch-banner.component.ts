import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { BatchGenerateResponse } from '../../../../models/micro-lesson.model';

@Component({
  selector: 'app-micro-lesson-batch-banner',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatProgressBarModule],
  templateUrl: './micro-lesson-batch-banner.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MicroLessonBatchBannerComponent {
  readonly batchJob = input<BatchGenerateResponse | null>(null);
  readonly isCancelling = input<boolean>(false);

  readonly viewProgress = output<void>();
  readonly cancel = output<void>();
  readonly dismiss = output<void>();
}
