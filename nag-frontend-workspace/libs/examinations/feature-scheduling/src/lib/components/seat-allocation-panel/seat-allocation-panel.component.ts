import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  signal,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import {
  ShiftResponse,
  SeatAllocationResponse,
  CentreResponse,
} from '@nag-frontend-workspace/examinations-data-access';

@Component({
  selector: 'nag-seat-allocation-panel',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './seat-allocation-panel.component.html',
  styleUrl: './seat-allocation-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SeatAllocationPanelComponent {
  readonly allocations = input<SeatAllocationResponse[]>([]);
  readonly selectedShift = input<ShiftResponse | null>(null);
  readonly loading = input<boolean>(false);
  readonly centres = input<CentreResponse[]>([]);

  readonly addAllocationClick = output<void>();
  readonly bulkRemoveAllocations = output<string[]>();

  // Pagination State
  readonly pageIndex = signal<number>(0);
  readonly pageSize = signal<number>(10);
  readonly pageSizeOptions = [10, 20, 50];

  readonly totalPages = computed(() => {
    const total = (this.allocations() || []).length;
    return Math.max(1, Math.ceil(total / this.pageSize()));
  });

  readonly paginatedAllocations = computed(() => {
    const list = this.allocations() || [];
    const start = this.pageIndex() * this.pageSize();
    return list.slice(start, start + this.pageSize());
  });

  readonly displayStart = computed(() => {
    const total = (this.allocations() || []).length;
    if (total === 0) return 0;
    return this.pageIndex() * this.pageSize() + 1;
  });

  readonly displayEnd = computed(() => {
    const total = (this.allocations() || []).length;
    return Math.min((this.pageIndex() + 1) * this.pageSize(), total);
  });

  // Row Selection State
  readonly selectedIds = signal<Set<string>>(new Set<string>());

  readonly isAllSelected = computed(() => {
    const items = this.paginatedAllocations();
    if (items.length === 0) return false;
    const ids = this.selectedIds();
    return items.every((a) => ids.has(a.id));
  });

  readonly isPartiallySelected = computed(() => {
    const items = this.paginatedAllocations();
    if (items.length === 0) return false;
    const ids = this.selectedIds();
    const count = items.filter((a) => ids.has(a.id)).length;
    return count > 0 && count < items.length;
  });

  toggleSelectAll(): void {
    const items = this.paginatedAllocations();
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

  onBulkRemove(): void {
    this.bulkRemoveAllocations.emit(Array.from(this.selectedIds()));
    this.deselectAll();
  }

  exportSelectedCsv(): void {
    const selected = (this.allocations() || []).filter((a) => this.selectedIds().has(a.id));
    if (selected.length === 0) return;

    const headers = [
      'ID',
      'Centre ID',
      'Centre Name',
      'Total Capacity',
      'Available Seats',
      'Reserved Quota',
      'PwD Seats',
      'Emergency Buffer',
    ];
    const rows = selected.map((a) => [
      `"${a.id || ''}"`,
      `"${a.centreId || ''}"`,
      `"${(this.getCentreName(a.centreId) || '').replace(/"/g, '""')}"`,
      `"${a.totalSeats ?? ''}"`,
      `"${a.availableSeats ?? ''}"`,
      `"${a.reservedSeats ?? ''}"`,
      `"${a.pwdSeats ?? ''}"`,
      `"${a.emergencyBufferSeats ?? ''}"`,
    ]);

    const csvContent = [headers.join(','), ...rows.map((r) => r.join(','))].join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `seat-allocations-export-${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
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

  onAddAllocation(): void {
    this.addAllocationClick.emit();
  }

  getCentreName(centreId: string): string {
    const c = (this.centres() || []).find((item) => item?.id === centreId);
    return c ? `${c.centreName} (${c.city}, ${c.state})` : centreId;
  }
}
