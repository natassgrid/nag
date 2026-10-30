import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  inject,
  signal,
  computed,
} from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AssetService } from '../../asset.service';
import { AssetResponse } from '../../asset.model';

@Component({
  selector: 'nag-asset-table-view',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatTooltipModule,
  ],
  templateUrl: './asset-table-view.component.html',
  styleUrl: './asset-table-view.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssetTableViewComponent {
  readonly assetService = inject(AssetService);

  assets = input<AssetResponse[]>([]);
  failedImages = input<Set<string>>(new Set<string>());

  preview = output<AssetResponse>();
  replaceBinary = output<AssetResponse>();
  editMetadata = output<AssetResponse>();
  copyCdnUrl = output<AssetResponse>();
  archive = output<AssetResponse>();
  restore = output<AssetResponse>();
  delete = output<AssetResponse>();
  imageError = output<string>();

  // Bulk actions outputs
  bulkArchive = output<AssetResponse[]>();
  bulkRestore = output<AssetResponse[]>();
  bulkDelete = output<AssetResponse[]>();

  // Selection state
  readonly selectedIds = signal<Set<string>>(new Set<string>());

  readonly isAllSelected = computed(() => {
    const list = this.assets();
    if (list.length === 0) return false;
    const selected = this.selectedIds();
    return list.every((a) => selected.has(a.id));
  });

  readonly isPartiallySelected = computed(() => {
    const list = this.assets();
    if (list.length === 0) return false;
    const selected = this.selectedIds();
    const someSelected = list.some((a) => selected.has(a.id));
    return someSelected && !this.isAllSelected();
  });

  readonly selectedAssets = computed(() => {
    const selected = this.selectedIds();
    return (this.assets() || []).filter((a) => selected.has(a.id));
  });

  readonly activeSelectedCount = computed(() => {
    return this.selectedAssets().filter((a) => a.status === 'ACTIVE').length;
  });

  readonly archivedSelectedCount = computed(() => {
    return this.selectedAssets().filter((a) => a.status === 'ARCHIVED').length;
  });

  isImageFailed(id: string): boolean {
    return this.failedImages().has(id);
  }

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
    const list = this.assets();
    if (checked) {
      list.forEach((a) => next.add(a.id));
    } else {
      list.forEach((a) => next.delete(a.id));
    }
    this.selectedIds.set(next);
  }

  clearSelection(): void {
    this.selectedIds.set(new Set());
  }

  triggerBulkArchive(): void {
    const activeItems = this.selectedAssets().filter((a) => a.status === 'ACTIVE');
    if (activeItems.length > 0) {
      this.bulkArchive.emit(activeItems);
      this.clearSelection();
    }
  }

  triggerBulkRestore(): void {
    const archivedItems = this.selectedAssets().filter((a) => a.status === 'ARCHIVED');
    if (archivedItems.length > 0) {
      this.bulkRestore.emit(archivedItems);
      this.clearSelection();
    }
  }

  triggerBulkDelete(): void {
    const items = this.selectedAssets();
    if (items.length > 0) {
      this.bulkDelete.emit(items);
      this.clearSelection();
    }
  }
}
