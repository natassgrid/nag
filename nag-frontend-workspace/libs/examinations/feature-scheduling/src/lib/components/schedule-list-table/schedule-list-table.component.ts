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
import { ScheduleResponse } from '@nag-frontend-workspace/examinations-data-access';

@Component({
  selector: 'nag-schedule-list-table',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './schedule-list-table.component.html',
  styleUrl: './schedule-list-table.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ScheduleListTableComponent {
  readonly schedules = input<ScheduleResponse[]>([]);
  readonly selectedSchedule = input<ScheduleResponse | null>(null);
  readonly loading = input<boolean>(false);

  readonly scheduleSelect = output<ScheduleResponse>();
  readonly viewShifts = output<ScheduleResponse>();
  readonly transitionClick = output<ScheduleResponse>();
  readonly amendClick = output<ScheduleResponse>();
  readonly createClick = output<void>();
  readonly bulkTransitionClick = output<string[]>();

  // Pagination State
  readonly pageIndex = signal<number>(0);
  readonly pageSize = signal<number>(10);
  readonly pageSizeOptions = [10, 20, 50];

  readonly totalPages = computed(() => {
    const total = (this.schedules() || []).length;
    return Math.max(1, Math.ceil(total / this.pageSize()));
  });

  readonly paginatedSchedules = computed(() => {
    const list = this.schedules() || [];
    const start = this.pageIndex() * this.pageSize();
    return list.slice(start, start + this.pageSize());
  });

  readonly displayStart = computed(() => {
    const total = (this.schedules() || []).length;
    if (total === 0) return 0;
    return this.pageIndex() * this.pageSize() + 1;
  });

  readonly displayEnd = computed(() => {
    const total = (this.schedules() || []).length;
    return Math.min((this.pageIndex() + 1) * this.pageSize(), total);
  });

  // Row Selection State
  readonly selectedIds = signal<Set<string>>(new Set<string>());

  readonly isAllSelected = computed(() => {
    const items = this.paginatedSchedules();
    if (items.length === 0) return false;
    const ids = this.selectedIds();
    return items.every((s) => ids.has(s.id));
  });

  readonly isPartiallySelected = computed(() => {
    const items = this.paginatedSchedules();
    if (items.length === 0) return false;
    const ids = this.selectedIds();
    const count = items.filter((s) => ids.has(s.id)).length;
    return count > 0 && count < items.length;
  });

  toggleSelectAll(): void {
    const items = this.paginatedSchedules();
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

  onBulkTransition(): void {
    this.bulkTransitionClick.emit(Array.from(this.selectedIds()));
  }

  exportSelectedCsv(): void {
    const selected = (this.schedules() || []).filter((s) => this.selectedIds().has(s.id));
    if (selected.length === 0) return;

    const headers = [
      'ID',
      'Schedule Name',
      'Version',
      'Notification No',
      'Exam Date',
      'Reserve Date',
      'Status',
      'Change Reason',
    ];
    const rows = selected.map((s) => [
      `"${s.id || ''}"`,
      `"${(s.scheduleName || '').replace(/"/g, '""')}"`,
      `"${s.scheduleVersion ?? ''}"`,
      `"${s.notificationNumber || ''}"`,
      `"${s.examDate || ''}"`,
      `"${s.reserveDate || ''}"`,
      `"${s.status || ''}"`,
      `"${(s.changeReason || '').replace(/"/g, '""')}"`,
    ]);

    const csvContent = [headers.join(','), ...rows.map((r) => r.join(','))].join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `schedules-export-${new Date().toISOString().slice(0, 10)}.csv`;
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

  onSelect(schedule: ScheduleResponse): void {
    this.scheduleSelect.emit(schedule);
  }

  onViewShifts(schedule: ScheduleResponse, event: Event): void {
    event.stopPropagation();
    this.viewShifts.emit(schedule);
  }

  onTransition(schedule: ScheduleResponse, event: Event): void {
    event.stopPropagation();
    this.transitionClick.emit(schedule);
  }

  onAmend(schedule: ScheduleResponse, event: Event): void {
    event.stopPropagation();
    this.amendClick.emit(schedule);
  }

  onCreate(): void {
    this.createClick.emit();
  }
}
