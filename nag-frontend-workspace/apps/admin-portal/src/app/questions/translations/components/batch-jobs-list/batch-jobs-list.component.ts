import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
} from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { BatchTranslationJobResponse } from '@nag-frontend-workspace/questions-data-access';

@Component({
  selector: 'nag-batch-jobs-list',
  standalone: true,
  imports: [CommonModule, DatePipe, MatButtonModule, MatIconModule, MatProgressBarModule],
  templateUrl: './batch-jobs-list.component.html',
  styleUrl: './batch-jobs-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BatchJobsListComponent {
  batchJobs = input<BatchTranslationJobResponse[]>([]);
  loadingBatchJobs = input<boolean>(false);

  backToQuestions = output<void>();
  refreshJobs = output<void>();
  cancelJob = output<string>();
}
