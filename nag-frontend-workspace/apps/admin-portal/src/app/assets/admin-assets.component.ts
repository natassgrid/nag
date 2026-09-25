/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

import {
  Component,
  OnInit,
  signal,
  computed,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { HttpEventType } from '@angular/common/http';
import { PageHeaderComponent } from '@nag-frontend-workspace/shared-ui-components';
import { AssetService } from './asset.service';
import { AssetResponse, AssetType, AssetStatus } from './asset.model';
import { AssetUploadDialogComponent } from './asset-upload-dialog.component';
import { AssetPreviewDialogComponent } from './asset-preview-dialog.component';
import { AssetMetadataDialogComponent } from './asset-metadata-dialog.component';

@Component({
  selector: 'app-admin-assets',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatMenuModule,
    PageHeaderComponent,
  ],
  templateUrl: './admin-assets.component.html',
  styleUrl: './admin-assets.component.scss',
})
export class AdminAssetsComponent implements OnInit {
  readonly assetService = inject(AssetService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  // Core State Signals
  assets = signal<AssetResponse[]>([]);
  totalElements = signal<number>(0);
  totalPages = signal<number>(0);
  currentPage = signal<number>(0);
  pageSize = signal<number>(12);
  loading = signal<boolean>(false);
  failedImages = signal<Set<string>>(new Set<string>());

  // Filters & Controls
  searchQuery = signal<string>('');
  selectedType = signal<string>('ALL');
  selectedStatus = signal<string>('ACTIVE');
  viewMode = signal<'grid' | 'table'>('grid');

  selectedAssetForReplace: AssetResponse | null = null;

  // Computed metrics
  totalStorageBytes = computed(() => {
    return this.assets().reduce((acc, a) => acc + (a.fileSize || 0), 0);
  });

  totalStorageFormatted = computed(() => {
    return this.assetService.formatFileSize(this.totalStorageBytes());
  });

  imageCount = computed(() => {
    return this.assets().filter((a) => a.assetType === 'IMAGE').length;
  });

  audioCount = computed(() => {
    return this.assets().filter((a) => a.assetType === 'AUDIO').length;
  });

  videoCount = computed(() => {
    return this.assets().filter((a) => a.assetType === 'VIDEO').length;
  });

  ngOnInit(): void {
    this.loadAssets();
  }

  loadAssets(): void {
    this.loading.set(true);

    const typeFilter =
      this.selectedType() === 'ALL'
        ? undefined
        : (this.selectedType() as AssetType);
    const statusFilter =
      this.selectedStatus() === 'ALL'
        ? undefined
        : (this.selectedStatus() as AssetStatus);

    this.assetService
      .searchAssets({
        filename: this.searchQuery().trim() || undefined,
        assetType: typeFilter,
        status: statusFilter,
        page: this.currentPage(),
        size: this.pageSize(),
        sort: 'createdAt',
        order: 'desc',
      })
      .subscribe({
        next: (res) => {
          this.assets.set(res.content || []);
          this.totalElements.set(res.totalElements || 0);
          this.totalPages.set(res.totalPages || 0);
          this.loading.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          this.assets.set([]);
          this.totalElements.set(0);
          this.totalPages.set(0);
          if (err?.status !== 404) {
            this.snackBar.open(
              err?.error?.message || 'Failed to load media assets from server.',
              'Dismiss',
              { duration: 4000 }
            );
          }
        },
      });
  }

  onImageError(id: string): void {
    this.failedImages.update((s) => new Set(s).add(id));
  }

  isImageFailed(id: string): boolean {
    return this.failedImages().has(id);
  }

  onSearchChange(query: string): void {
    this.searchQuery.set(query);
    this.currentPage.set(0);
    this.loadAssets();
  }

  onTypeChange(type: string): void {
    this.selectedType.set(type);
    this.currentPage.set(0);
    this.loadAssets();
  }

  onStatusChange(status: string): void {
    this.selectedStatus.set(status);
    this.currentPage.set(0);
    this.loadAssets();
  }

  openUploadModal(): void {
    const ref = this.dialog.open(AssetUploadDialogComponent, {
      width: '560px',
      disableClose: true,
    });

    ref.afterClosed().subscribe((result: AssetResponse | null) => {
      if (result) {
        this.snackBar.open(
          `Asset "${result.originalFilename}" uploaded successfully.`,
          'OK',
          { duration: 3500 }
        );
        this.currentPage.set(0);
        this.loadAssets();
      }
    });
  }

  previewAsset(asset: AssetResponse): void {
    const ref = this.dialog.open(AssetPreviewDialogComponent, {
      width: '760px',
      data: { asset },
    });

    ref.afterClosed().subscribe(() => {
      this.failedImages.update((s) => {
        const copy = new Set(s);
        copy.delete(asset.id);
        return copy;
      });
      this.loadAssets();
    });
  }

  triggerReplaceBinary(asset: AssetResponse, fileInput: HTMLInputElement): void {
    this.selectedAssetForReplace = asset;
    fileInput.click();
  }

  onBinaryFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0 || !this.selectedAssetForReplace) return;

    const file = input.files[0];
    const assetId = this.selectedAssetForReplace.id;
    this.selectedAssetForReplace = null;

    this.snackBar.open('Replacing media binary...', '', { duration: 2000 });
    this.assetService.replaceContentWithProgress(assetId, file).subscribe({
      next: (httpEvent) => {
        if (httpEvent.type === HttpEventType.Response) {
          this.snackBar.open('Asset binary successfully replaced and verified!', 'OK', { duration: 3000 });
          this.failedImages.update((s) => {
            const copy = new Set(s);
            copy.delete(assetId);
            return copy;
          });
          this.loadAssets();
        }
      },
      error: (err) => {
        this.snackBar.open(err?.error?.message || 'Failed to replace file content.', 'Dismiss', { duration: 4000 });
      },
    });
    input.value = '';
  }

  editMetadata(asset: AssetResponse): void {
    const ref = this.dialog.open(AssetMetadataDialogComponent, {
      width: '560px',
      data: { asset },
    });

    ref.afterClosed().subscribe((updated: AssetResponse | null) => {
      if (updated) {
        this.snackBar.open('Asset metadata updated.', 'OK', {
          duration: 3000,
        });
        this.loadAssets();
      }
    });
  }

  copyCdnUrl(asset: AssetResponse): void {
    const url = `${window.location.origin}${this.assetService.getDownloadUrl(
      asset.id
    )}`;
    navigator.clipboard.writeText(url).then(() => {
      this.snackBar.open('Asset direct URL copied to clipboard', 'OK', {
        duration: 2500,
      });
    });
  }

  archiveAsset(asset: AssetResponse): void {
    this.assetService.archiveAsset(asset.id).subscribe({
      next: () => {
        this.snackBar.open(
          `Asset "${asset.originalFilename}" archived.`,
          'OK',
          { duration: 3000 }
        );
        this.loadAssets();
      },
      error: (err) => {
        this.snackBar.open(
          err?.error?.message || 'Failed to archive asset.',
          'Dismiss',
          { duration: 4000 }
        );
      },
    });
  }

  restoreAsset(asset: AssetResponse): void {
    this.assetService.restoreAsset(asset.id).subscribe({
      next: () => {
        this.snackBar.open(
          `Asset "${asset.originalFilename}" restored to active status.`,
          'OK',
          { duration: 3000 }
        );
        this.loadAssets();
      },
      error: (err) => {
        this.snackBar.open(
          err?.error?.message || 'Failed to restore asset.',
          'Dismiss',
          { duration: 4000 }
        );
      },
    });
  }

  deleteAsset(asset: AssetResponse): void {
    const confirmed = confirm(
      `Are you sure you want to permanently delete "${asset.originalFilename}"?\nThis cannot be undone.`
    );
    if (!confirmed) return;

    this.assetService.deleteAsset(asset.id).subscribe({
      next: () => {
        this.snackBar.open('Asset deleted successfully.', 'OK', {
          duration: 3000,
        });
        this.loadAssets();
      },
      error: (err) => {
        this.snackBar.open(
          err?.error?.message || 'Failed to delete asset. Ensure no questions reference it.',
          'Dismiss',
          { duration: 4500 }
        );
      },
    });
  }

  onPageChange(newPage: number): void {
    if (newPage >= 0 && newPage < this.totalPages()) {
      this.currentPage.set(newPage);
      this.loadAssets();
    }
  }
}
