import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
  output,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuditRecord } from '../../models/audit-log.model';

@Component({
  selector: 'nag-audit-table-view',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
  ],
  templateUrl: './audit-table-view.component.html',
  styleUrl: './audit-table-view.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuditTableViewComponent {
  readonly records = input<AuditRecord[]>([]);
  readonly loading = input<boolean>(false);

  readonly inspectRecord = output<AuditRecord>();

  readonly pageIndex = signal<number>(0);
  readonly pageSize = signal<number>(10);
  readonly pageSizeOptions: number[] = [10, 20, 50];
  readonly selectedIds = signal<Set<string>>(new Set<string>());

  readonly totalPages = computed(() => {
    const list = this.records() || [];
    return Math.ceil(list.length / this.pageSize()) || 1;
  });

  readonly safePageIndex = computed(() => {
    const total = this.totalPages();
    const current = this.pageIndex();
    return current >= total ? Math.max(0, total - 1) : current;
  });

  readonly paginatedRecords = computed(() => {
    const list = this.records() || [];
    const page = this.safePageIndex();
    const size = this.pageSize();
    return list.slice(page * size, (page + 1) * size);
  });

  readonly displayStart = computed(() => {
    const list = this.records() || [];
    if (list.length === 0) return 0;
    return this.safePageIndex() * this.pageSize() + 1;
  });

  readonly displayEnd = computed(() => {
    const list = this.records() || [];
    return Math.min((this.safePageIndex() + 1) * this.pageSize(), list.length);
  });

  readonly isAllSelected = computed(() => {
    const pageItems = this.paginatedRecords();
    if (pageItems.length === 0) return false;
    const sel = this.selectedIds();
    return pageItems.every((r) => sel.has(r.id));
  });

  readonly isPartiallySelected = computed(() => {
    const pageItems = this.paginatedRecords();
    if (pageItems.length === 0) return false;
    const sel = this.selectedIds();
    const count = pageItems.filter((r) => sel.has(r.id)).length;
    return count > 0 && count < pageItems.length;
  });

  readonly selectedCount = computed(() => this.selectedIds().size);

  copyHash(hash: string): void {
    if (!hash) return;
    navigator.clipboard?.writeText(hash);
  }

  onPageSizeChange(newSize: number): void {
    this.pageSize.set(newSize);
    this.pageIndex.set(0);
  }

  goToFirstPage(): void {
    this.pageIndex.set(0);
  }

  goToPrevPage(): void {
    if (this.safePageIndex() > 0) {
      this.pageIndex.set(this.safePageIndex() - 1);
    }
  }

  goToNextPage(): void {
    if (this.safePageIndex() < this.totalPages() - 1) {
      this.pageIndex.set(this.safePageIndex() + 1);
    }
  }

  goToLastPage(): void {
    this.pageIndex.set(Math.max(0, this.totalPages() - 1));
  }

  toggleSelectAll(checked: boolean): void {
    const current = new Set(this.selectedIds());
    const pageItems = this.paginatedRecords();
    if (checked) {
      pageItems.forEach((r) => current.add(r.id));
    } else {
      pageItems.forEach((r) => current.delete(r.id));
    }
    this.selectedIds.set(current);
  }

  toggleSelect(id: string): void {
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

  copySelectedHashes(): void {
    const sel = this.selectedIds();
    const selectedList = this.records().filter((r) => sel.has(r.id));
    const hashes = selectedList.map((r) => r.hash).filter(Boolean).join('\n');
    if (hashes) {
      navigator.clipboard?.writeText(hashes);
    }
  }

  exportSelectedCsv(): void {
    const sel = this.selectedIds();
    const selectedList = this.records().filter((r) => sel.has(r.id));
    if (selectedList.length === 0) return;

    const headers = [
      'Block Number',
      'Timestamp',
      'Service',
      'Actor',
      'Actor Role',
      'Action',
      'Target',
      'Severity',
      'Status',
      'IP Address',
      'Hash',
    ];

    const rows = selectedList.map((r) =>
      [
        r.blockNumber ?? '',
        `"${(r.timestamp || '').replace(/"/g, '""')}"`,
        `"${(r.service || '').replace(/"/g, '""')}"`,
        `"${(r.actor || '').replace(/"/g, '""')}"`,
        `"${(r.actorRole || '').replace(/"/g, '""')}"`,
        `"${(r.action || '').replace(/"/g, '""')}"`,
        `"${(r.target || '').replace(/"/g, '""')}"`,
        r.severity || '',
        r.status || '',
        r.ipAddress || '',
        `"${(r.hash || '').replace(/"/g, '""')}"`,
      ].join(',')
    );

    const csvContent =
      'data:text/csv;charset=utf-8,' +
      encodeURIComponent([headers.join(','), ...rows].join('\n'));
    const downloadAnchor = document.createElement('a');
    downloadAnchor.setAttribute('href', csvContent);
    downloadAnchor.setAttribute(
      'download',
      `nag_audit_selected_${Date.now()}.csv`
    );
    document.body.appendChild(downloadAnchor);
    downloadAnchor.click();
    downloadAnchor.remove();
  }
}
