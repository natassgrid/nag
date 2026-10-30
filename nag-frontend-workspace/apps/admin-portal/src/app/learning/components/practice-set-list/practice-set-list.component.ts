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
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTooltipModule } from '@angular/material/tooltip';
import { PracticeSet } from '../../models';

@Component({
  selector: 'app-practice-set-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatSlideToggleModule,
    MatTooltipModule,
  ],
  templateUrl: './practice-set-list.component.html',
  styleUrls: ['./practice-set-list.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PracticeSetListComponent {
  sets = input.required<PracticeSet[]>();
  edit = output<PracticeSet>();
  curate = output<PracticeSet>();
  delete = output<PracticeSet>();
  togglePublish = output<PracticeSet>();
  create = output<void>();
  bulkPublish = output<PracticeSet[]>();
  bulkUnpublish = output<PracticeSet[]>();
  bulkDelete = output<PracticeSet[]>();

  readonly pageIndex = signal<number>(0);
  readonly pageSize = signal<number>(10);
  readonly pageSizeOptions = [10, 20, 50];

  readonly totalPages = computed(() => {
    const size = this.pageSize();
    if (size <= 0) return 1;
    return Math.max(1, Math.ceil(this.sets().length / size));
  });

  readonly paginatedSets = computed(() => {
    const start = this.pageIndex() * this.pageSize();
    return this.sets().slice(start, start + this.pageSize());
  });

  readonly displayStart = computed(() => {
    if (this.sets().length === 0) return 0;
    return this.pageIndex() * this.pageSize() + 1;
  });

  readonly displayEnd = computed(() => {
    return Math.min((this.pageIndex() + 1) * this.pageSize(), this.sets().length);
  });

  readonly selectedIds = signal<Set<string>>(new Set<string>());

  readonly isAllSelected = computed(() => {
    const list = this.paginatedSets() || [];
    if (list.length === 0) return false;
    const selected = this.selectedIds();
    return list.every((s) => s && selected.has(s.id));
  });

  readonly isPartiallySelected = computed(() => {
    const list = this.paginatedSets() || [];
    if (list.length === 0) return false;
    const selected = this.selectedIds();
    const count = list.filter((s) => s && selected.has(s.id)).length;
    return count > 0 && count < list.length;
  });

  readonly selectedSets = computed(() => {
    const selected = this.selectedIds();
    const list = this.sets() || [];
    return list.filter((s) => s && selected.has(s.id));
  });

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
    const pageItems = this.paginatedSets() || [];
    if (this.isAllSelected()) {
      this.selectedIds.update((set) => {
        const next = new Set(set);
        for (const item of pageItems) {
          if (item) next.delete(item.id);
        }
        return next;
      });
    } else {
      this.selectedIds.update((set) => {
        const next = new Set(set);
        for (const item of pageItems) {
          if (item) next.add(item.id);
        }
        return next;
      });
    }
  }

  deselectAll(): void {
    this.selectedIds.set(new Set<string>());
  }

  onBulkPublish(): void {
    const selected = this.selectedSets();
    if (selected.length > 0) {
      this.bulkPublish.emit(selected);
    }
  }

  onBulkUnpublish(): void {
    const selected = this.selectedSets();
    if (selected.length > 0) {
      this.bulkUnpublish.emit(selected);
    }
  }

  onBulkDelete(): void {
    const selected = this.selectedSets();
    if (selected.length === 0) return;
    if (
      window.confirm(
        `Are you sure you want to delete ${selected.length} selected practice set(s)? This cannot be undone.`
      )
    ) {
      this.bulkDelete.emit(selected);
    }
  }

  onFirstPage(): void {
    this.pageIndex.set(0);
  }

  onPrevPage(): void {
    const current = this.pageIndex();
    if (current > 0) {
      this.pageIndex.set(current - 1);
    }
  }

  onNextPage(): void {
    const current = this.pageIndex();
    if (current < this.totalPages() - 1) {
      this.pageIndex.set(current + 1);
    }
  }

  onLastPage(): void {
    this.pageIndex.set(Math.max(0, this.totalPages() - 1));
  }

  onPageSizeChange(event: Event): void {
    const target = event.target as HTMLSelectElement;
    const newSize = parseInt(target.value, 10);
    this.pageSize.set(newSize);
    this.pageIndex.set(0);
  }
}
