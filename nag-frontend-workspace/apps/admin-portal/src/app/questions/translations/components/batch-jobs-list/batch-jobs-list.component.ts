import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  computed,
} from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { BatchTranslationJobResponse } from '@nag-frontend-workspace/questions-data-access';

@Component({
  selector: 'nag-batch-jobs-list',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatPaginatorModule,
  ],
  templateUrl: './batch-jobs-list.component.html',
  styleUrl: './batch-jobs-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BatchJobsListComponent {
  batchJobs = input<BatchTranslationJobResponse[]>([]);
  loadingBatchJobs = input<boolean>(false);
  totalElements = input<number>(0);
  page = input<number>(0);
  pageSize = input<number>(20);
  pageSizeOptions = input<number[]>([10, 20, 50]);

  backToQuestions = output<void>();
  refreshJobs = output<void>();
  cancelJob = output<string>();
  resumeJob = output<string>();
  pageChange = output<{ pageIndex: number; pageSize: number }>();

  readonly sortedJobs = computed(() => {
    return [...this.batchJobs()].sort((a, b) => {
      const timeA = new Date(a.updatedAt || a.createdAt || 0).getTime();
      const timeB = new Date(b.updatedAt || b.createdAt || 0).getTime();
      return timeB - timeA;
    });
  });

  onPageChange(event: PageEvent): void {
    this.pageChange.emit({
      pageIndex: event.pageIndex,
      pageSize: event.pageSize,
    });
  }
}
