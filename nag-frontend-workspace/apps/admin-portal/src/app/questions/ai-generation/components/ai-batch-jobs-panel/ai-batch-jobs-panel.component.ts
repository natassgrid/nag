import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import {
  BatchItem,
  BatchGenerationJob,
} from '@nag-frontend-workspace/questions-data-access';

@Component({
  selector: 'nag-ai-batch-jobs-panel',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './ai-batch-jobs-panel.component.html',
  styleUrl: './ai-batch-jobs-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AiBatchJobsPanelComponent {
  batchItems = input<BatchItem[]>([]);
  batchJobs = input<BatchGenerationJob[]>([]);
  loadingBatchJobs = input<boolean>(false);
  submittingBatch = input<boolean>(false);
  totalBatchQuestions = input<number>(0);

  submitBatch = output<void>();
  removeBatchItem = output<number>();
  loadBatchJobs = output<void>();
  cancelBatchJob = output<string>();

  onSubmitBatch(): void {
    this.submitBatch.emit();
  }

  onRemoveBatchItem(index: number): void {
    this.removeBatchItem.emit(index);
  }

  onLoadBatchJobs(): void {
    this.loadBatchJobs.emit();
  }

  onCancelBatchJob(jobId: string): void {
    this.cancelBatchJob.emit(jobId);
  }
}
