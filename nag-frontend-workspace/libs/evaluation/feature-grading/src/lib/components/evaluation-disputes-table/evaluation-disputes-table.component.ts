import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
  signal,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  StatusBadgeComponent,
  StatusVariant,
  EmptyStateComponent,
} from '@nag-frontend-workspace/shared-ui-components';
import { DisputeRecord } from '@nag-frontend-workspace/evaluation-data-access';

@Component({
  selector: 'nag-evaluation-disputes-table',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    StatusBadgeComponent,
    EmptyStateComponent,
  ],
  templateUrl: './evaluation-disputes-table.component.html',
  styleUrl: './evaluation-disputes-table.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EvaluationDisputesTableComponent {
  readonly disputes = input<DisputeRecord[]>([]);

  readonly resolve = output<{ dispute: DisputeRecord; resolution: 'RESOLVED' | 'REJECTED' }>();
  readonly bulkResolve = output<{ disputes: DisputeRecord[]; resolution: 'RESOLVED' | 'REJECTED' }>();

  // Pagination State
  readonly pageIndex = signal<number>(0);
  readonly pageSize = signal<number>(10);
  readonly pageSizeOptions = [10, 20, 50];

  readonly totalPages = computed(() => {
    const total = (this.disputes() || []).length;
    return Math.max(1, Math.ceil(total / this.pageSize()));
  });

  readonly paginatedDisputes = computed(() => {
    const list = this.disputes() || [];
    const start = this.pageIndex() * this.pageSize();
    return list.slice(start, start + this.pageSize());
  });

  readonly displayStart = computed(() => {
    const total = (this.disputes() || []).length;
    if (total === 0) return 0;
    return this.pageIndex() * this.pageSize() + 1;
  });

  readonly displayEnd = computed(() => {
    const total = (this.disputes() || []).length;
    return Math.min((this.pageIndex() + 1) * this.pageSize(), total);
  });

  // Row Selection State
  readonly selectedIds = signal<Set<string>>(new Set<string>());

  readonly isAllSelected = computed(() => {
    const items = this.paginatedDisputes();
    if (items.length === 0) return false;
    const ids = this.selectedIds();
    return items.every((d) => ids.has(d.id));
  });

  readonly isPartiallySelected = computed(() => {
    const items = this.paginatedDisputes();
    if (items.length === 0) return false;
    const ids = this.selectedIds();
    const count = items.filter((d) => ids.has(d.id)).length;
    return count > 0 && count < items.length;
  });

  toggleSelectAll(): void {
    const items = this.paginatedDisputes();
    const current = new Set(this.selectedIds());
    if (this.isAllSelected()) {
      for (const item of items) {
        current.delete(item.id);
      }
    } else {
      for (const item of items) {
        current.add(item.id);
      }
    }
    this.selectedIds.set(current);
  }

  toggleSelect(id: string, event: Event): void {
    event.stopPropagation();
    const current = new Set(this.selectedIds());
    if (current.has(id)) {
      current.delete(id);
    } else {
      current.add(id);
    }
    this.selectedIds.set(current);
  }

  deselectAll(): void {
    this.selectedIds.set(new Set<string>());
  }

  onBulkResolve(resolution: 'RESOLVED' | 'REJECTED'): void {
    const selected = (this.disputes() || []).filter((d) => this.selectedIds().has(d.id));
    if (selected.length === 0) return;
    this.bulkResolve.emit({ disputes: selected, resolution });
    this.deselectAll();
  }

  goToFirstPage(): void {
    this.pageIndex.set(0);
  }

  goToPreviousPage(): void {
    this.pageIndex.update((p) => Math.max(0, p - 1));
  }

  goToNextPage(): void {
    this.pageIndex.update((p) => Math.min(this.totalPages() - 1, p + 1));
  }

  goToLastPage(): void {
    this.pageIndex.set(this.totalPages() - 1);
  }

  onPageSizeChange(newSize: string | number): void {
    this.pageSize.set(Number(newSize));
    this.pageIndex.set(0);
  }

  disputeStatusVariant(status: string): StatusVariant {
    switch (status) {
      case 'OPEN':
        return 'warn';
      case 'RESOLVED':
        return 'success';
      case 'REJECTED':
        return 'error';
      default:
        return 'neutral';
    }
  }
}
