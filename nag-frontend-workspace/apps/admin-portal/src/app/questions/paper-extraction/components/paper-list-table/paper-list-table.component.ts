import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  computed,
  signal,
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
  resumeExtraction = output<QuestionPaperMetadata>();
  deletePaper = output<QuestionPaperMetadata>();
  refresh = output<void>();
  bulkResume = output<QuestionPaperMetadata[]>();
  bulkRetry = output<QuestionPaperMetadata[]>();
  bulkDelete = output<QuestionPaperMetadata[]>();

  readonly selectedIds = signal<Set<string>>(new Set<string>());

  readonly isAllSelected = computed(() => {
    const list = this.papers() || [];
    if (list.length === 0) return false;
    const selected = this.selectedIds();
    return list.every((p) => p && selected.has(p.id));
  });

  readonly isPartiallySelected = computed(() => {
    const list = this.papers() || [];
    if (list.length === 0) return false;
    const selected = this.selectedIds();
    const count = list.filter((p) => p && selected.has(p.id)).length;
    return count > 0 && count < list.length;
  });

  readonly selectedPapers = computed(() => {
    const selected = this.selectedIds();
    const list = this.papers() || [];
    return list.filter((p) => p && selected.has(p.id));
  });

  readonly resumableSelectedPapers = computed(() => {
    return this.selectedPapers().filter((p) => this.isResumable(p));
  });

  readonly retryableSelectedPapers = computed(() => {
    return this.selectedPapers().filter((p) => this.isRetryable(p));
  });

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
    { value: 'QUOTA_EXHAUSTED', label: 'Quota Exhausted' },
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
      case 'QUOTA_EXHAUSTED':
        return 'bg-amber-100 text-amber-900 border-amber-300';
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
      case 'QUOTA_EXHAUSTED':
        return 'hourglass_top';
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

  isResumable(paper: QuestionPaperMetadata): boolean {
    return !!paper.resumable;
  }

  isSelected(id: string): boolean {
    return this.selectedIds().has(id);
  }

  toggleSelect(id: string): void {
    this.selectedIds.update((set) => {
      const next = new Set(set);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  }

  toggleSelectAll(): void {
    const list = this.papers() || [];
    if (this.isAllSelected()) {
      this.selectedIds.update((set) => {
        const next = new Set(set);
        for (const p of list) {
          if (p) next.delete(p.id);
        }
        return next;
      });
    } else {
      this.selectedIds.update((set) => {
        const next = new Set(set);
        for (const p of list) {
          if (p) next.add(p.id);
        }
        return next;
      });
    }
  }

  deselectAll(): void {
    this.selectedIds.set(new Set<string>());
  }

  onBulkResume(): void {
    const resumable = this.resumableSelectedPapers();
    if (resumable.length > 0) {
      this.bulkResume.emit(resumable);
    }
  }

  onBulkRetry(): void {
    const retryable = this.retryableSelectedPapers();
    if (retryable.length > 0) {
      this.bulkRetry.emit(retryable);
    }
  }

  onBulkDelete(): void {
    const selected = this.selectedPapers();
    if (selected.length === 0) return;
    if (
      window.confirm(
        `Are you sure you want to delete ${selected.length} selected question paper(s) and their staging questions? This cannot be undone.`
      )
    ) {
      this.bulkDelete.emit(selected);
    }
  }

  onFirstPage(): void {
    this.pageChange.emit({ pageIndex: 0, pageSize: this.pageSize() });
  }

  onLastPage(): void {
    const lastPage = Math.max(0, this.totalPages() - 1);
    this.pageChange.emit({ pageIndex: lastPage, pageSize: this.pageSize() });
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

  onDeletePaper(paper: QuestionPaperMetadata): void {
    if (window.confirm(`Are you sure you want to delete "${paper.examinationName}" and all its staging questions? This cannot be undone.`)) {
      this.deletePaper.emit(paper);
    }
  }
}
