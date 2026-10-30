import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  signal,
  computed,
  effect,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CentreResponse } from '@nag-frontend-workspace/examinations-data-access';

@Component({
  selector: 'nag-centre-table-list',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './centre-table-list.component.html',
  styleUrl: './centre-table-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CentreTableListComponent {
  centres = input<CentreResponse[]>([]);
  loading = input<boolean>(false);
  searchQuery = input<string>('');
  stateFilter = input<string>('ALL');

  registerFirst = output<void>();
  deactivate = output<CentreResponse>();
  bulkDeactivate = output<CentreResponse[]>();

  // Pagination state signals
  readonly pageIndex = signal<number>(0);
  readonly pageSize = signal<number>(10);
  readonly pageSizeOptions: readonly number[] = [10, 20, 50];

  // Selection state
  readonly selectedIds = signal<Set<string>>(new Set<string>());

  private previousCentres: CentreResponse[] = [];

  constructor() {
    effect(() => {
      const current = this.centres();
      if (current !== this.previousCentres) {
        this.previousCentres = current;
        this.pageIndex.set(0);
      } else {
        const total = current.length;
        const maxPages = Math.max(1, Math.ceil(total / this.pageSize()));
        if (this.pageIndex() >= maxPages) {
          this.pageIndex.set(0);
        }
      }
    });
  }

  // Computed pagination
  readonly totalPages = computed(() => {
    const total = this.centres().length;
    const size = this.pageSize();
    return Math.max(1, Math.ceil(total / size));
  });

  readonly paginatedCentres = computed(() => {
    const list = this.centres() || [];
    const start = this.pageIndex() * this.pageSize();
    return list.slice(start, start + this.pageSize());
  });

  readonly displayStart = computed(() => {
    const total = this.centres().length;
    if (total === 0) return 0;
    return this.pageIndex() * this.pageSize() + 1;
  });

  readonly displayEnd = computed(() => {
    const total = this.centres().length;
    const end = (this.pageIndex() + 1) * this.pageSize();
    return Math.min(total, end);
  });

  // Computed selection state
  readonly isAllSelected = computed(() => {
    const pageItems = this.paginatedCentres();
    if (pageItems.length === 0) return false;
    const selected = this.selectedIds();
    return pageItems.every((c) => selected.has(c.id));
  });

  readonly isPartiallySelected = computed(() => {
    const pageItems = this.paginatedCentres();
    if (pageItems.length === 0) return false;
    const selected = this.selectedIds();
    const someSelected = pageItems.some((c) => selected.has(c.id));
    return someSelected && !this.isAllSelected();
  });

  readonly selectedCentres = computed(() => {
    const selected = this.selectedIds();
    return (this.centres() || []).filter((c) => selected.has(c.id));
  });

  readonly activeSelectedCount = computed(() => {
    return this.selectedCentres().filter((c) => c.active !== false).length;
  });

  // Pagination navigation methods
  onPageSizeChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    const newSize = parseInt(select.value, 10) || 10;
    this.pageSize.set(newSize);
    this.pageIndex.set(0);
  }

  firstPage(): void {
    this.pageIndex.set(0);
  }

  previousPage(): void {
    if (this.pageIndex() > 0) {
      this.pageIndex.update((p) => p - 1);
    }
  }

  nextPage(): void {
    if (this.pageIndex() + 1 < this.totalPages()) {
      this.pageIndex.update((p) => p + 1);
    }
  }

  lastPage(): void {
    this.pageIndex.set(Math.max(0, this.totalPages() - 1));
  }

  // Selection actions
  toggleRowSelection(id: string): void {
    const next = new Set(this.selectedIds());
    if (next.has(id)) {
      next.delete(id);
    } else {
      next.add(id);
    }
    this.selectedIds.set(next);
  }

  toggleSelectAll(event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    const next = new Set(this.selectedIds());
    const pageItems = this.paginatedCentres();
    if (checked) {
      pageItems.forEach((c) => next.add(c.id));
    } else {
      pageItems.forEach((c) => next.delete(c.id));
    }
    this.selectedIds.set(next);
  }

  clearSelection(): void {
    this.selectedIds.set(new Set());
  }

  triggerBulkDeactivate(): void {
    const activeSelected = this.selectedCentres().filter((c) => c.active !== false);
    if (activeSelected.length > 0) {
      this.bulkDeactivate.emit(activeSelected);
    }
  }

  exportSelectedCsv(): void {
    const selected = this.selectedCentres();
    if (selected.length === 0) return;

    const headers = [
      'ID',
      'Centre Name',
      'Building',
      'Floor',
      'Laboratory',
      'City',
      'State',
      'District',
      'Total Capacity',
      'Status',
    ];

    const escapeCsv = (val: unknown): string => {
      if (val === null || val === undefined) return '""';
      const str = String(val).replace(/"/g, '""');
      return `"${str}"`;
    };

    const rows = selected.map((c) => [
      escapeCsv(c.id),
      escapeCsv(c.centreName || (c as any).name),
      escapeCsv(c.building || ''),
      escapeCsv(c.floor || ''),
      escapeCsv(c.laboratoryIdentifier || ''),
      escapeCsv(c.cityName || c.city || ''),
      escapeCsv(c.stateName || c.state || ''),
      escapeCsv(c.district || ''),
      escapeCsv(c.totalCapacity ?? ''),
      escapeCsv(c.active !== false ? 'Active' : 'Inactive'),
    ]);

    const csvContent = [headers.join(','), ...rows.map((r) => r.join(','))].join('\r\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `examination-centres-selected-${new Date().toISOString().slice(0, 10)}.csv`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }
}
