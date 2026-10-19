import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { QuestionPaperMetadata, PaperExtractionStatus } from '../../models';

@Component({
  selector: 'nag-paper-list-table',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
  ],
  templateUrl: './paper-list-table.component.html',
  styleUrl: './paper-list-table.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaperListTableComponent {
  papers = input<QuestionPaperMetadata[]>([]);
  loading = input<boolean>(false);
  totalElements = input<number>(0);
  page = input<number>(0);
  pageSize = input<number>(20);
  pageSizeOptions = input<number[]>([10, 20, 50]);
  searchQuery = input<string>('');
  statusFilter = input<string>('ALL');

  readonly Math = Math;

  searchChange = output<string>();
  statusChange = output<string>();
  pageChange = output<{ pageIndex: number; pageSize: number }>();
  selectPaper = output<QuestionPaperMetadata>();
  applyAnswerKey = output<QuestionPaperMetadata>();
  retryExtraction = output<QuestionPaperMetadata>();
  cancelExtraction = output<QuestionPaperMetadata>();
  refresh = output<void>();

  readonly totalPages = computed(() => {
    const size = this.pageSize();
    if (size <= 0) return 1;
    return Math.max(1, Math.ceil(this.totalElements() / size));
  });

  readonly statusOptions: { value: string; label: string }[] = [
    { value: 'ALL', label: 'All Extraction Statuses' },
    { value: 'PENDING', label: 'Pending' },
    { value: 'PROCESSING', label: 'Processing' },
    { value: 'COMPLETED', label: 'Completed' },
    { value: 'PARTIAL_FAILED', label: 'Partially Failed' },
    { value: 'FAILED', label: 'Failed' },
    { value: 'CANCELLED', label: 'Cancelled' },
  ];

  getStatusClass(status: PaperExtractionStatus | string): string {
    switch (status) {
      case 'COMPLETED':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      case 'PROCESSING':
        return 'bg-amber-50 text-amber-700 border-amber-200';
      case 'PENDING':
        return 'bg-blue-50 text-blue-700 border-blue-200';
      case 'PARTIAL_FAILED':
        return 'bg-orange-50 text-orange-700 border-orange-200';
      case 'FAILED':
        return 'bg-rose-50 text-rose-700 border-rose-200';
      case 'CANCELLED':
        return 'bg-slate-100 text-slate-600 border-slate-200';
      default:
        return 'bg-slate-50 text-slate-600 border-slate-200';
    }
  }

  getStatusIcon(status: PaperExtractionStatus | string): string {
    switch (status) {
      case 'COMPLETED':
        return 'check_circle';
      case 'PROCESSING':
        return 'autorenew';
      case 'PENDING':
        return 'hourglass_empty';
      case 'PARTIAL_FAILED':
        return 'warning';
      case 'FAILED':
        return 'error';
      case 'CANCELLED':
        return 'cancel';
      default:
        return 'help_outline';
    }
  }

  calculateProgress(paper: QuestionPaperMetadata): number {
    if (paper.progressPercentage != null && paper.progressPercentage >= 0) {
      return Math.min(100, Math.round(paper.progressPercentage));
    }
    if (paper.totalPages > 0) {
      return Math.min(100, Math.round((paper.pagesProcessed / paper.totalPages) * 100));
    }
    return paper.extractionStatus === 'COMPLETED' ? 100 : 0;
  }

  isCancelable(paper: QuestionPaperMetadata): boolean {
    return paper.extractionStatus === 'PENDING' || paper.extractionStatus === 'PROCESSING';
  }

  isRetryable(paper: QuestionPaperMetadata): boolean {
    return (
      paper.extractionStatus === 'FAILED' ||
      paper.extractionStatus === 'PARTIAL_FAILED' ||
      paper.extractionStatus === 'CANCELLED'
    );
  }

  onPrevPage(): void {
    const current = this.page();
    if (current > 0) {
      this.pageChange.emit({ pageIndex: current - 1, pageSize: this.pageSize() });
    }
  }

  onNextPage(): void {
    const current = this.page();
    if (current < this.totalPages() - 1) {
      this.pageChange.emit({ pageIndex: current + 1, pageSize: this.pageSize() });
    }
  }

  onPageSizeChange(event: Event): void {
    const target = event.target as HTMLSelectElement;
    const newSize = parseInt(target.value, 10);
    this.pageChange.emit({ pageIndex: 0, pageSize: newSize });
  }
}
