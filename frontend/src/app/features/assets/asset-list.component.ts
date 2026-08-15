/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { AssetService } from './asset.service';
import { AssetResponse, AssetType, AssetStatus } from './asset.model';
import { AssetUploadDialogComponent } from './asset-upload-dialog.component';
import { AssetMetadataDialogComponent } from './asset-metadata-dialog.component';
import { AssetPreviewDialogComponent } from './asset-preview-dialog.component';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../shared/components/confirm-dialog/confirm-dialog.component';
import {
  PaginatedTableComponent,
  ColumnDef,
  PaginatedDataFetcher,
  FilterCategory
} from '../../shared/components/paginated-table';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';

@Component({
  selector: 'app-asset-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatSnackBarModule,
    MatChipsModule,
    MatTooltipModule,
    MatMenuModule,
    PaginatedTableComponent,
    PageHeaderComponent,
    AssetUploadDialogComponent,
    AssetMetadataDialogComponent
  ],
  template: `
    <div class="page-layout" role="main" aria-labelledby="asset-library-heading">
      <app-page-header
        title="Asset Library"
        subtitle="Upload, manage, and organize media assets for examinations."
        icon="perm_media"
      >
        <button mat-raised-button color="primary" (click)="openUploadDrawer()" aria-label="Upload a new asset">
          <mat-icon>cloud_upload</mat-icon>
          Upload Asset
        </button>
      </app-page-header>

      <!-- Reusable Paginated Table -->
      <app-paginated-table
        #paginatedTable
        title="Assets List"
        [fetcher]="fetcher"
        [columns]="columns"
        [filterCategories]="filterCategories"
        (filterChange)="onFilterChange($event)"
        [actionsTemplate]="actionsTmpl"
        searchPlaceholder="Filter by filename, type, or status..."
      ></app-paginated-table>

      <!-- Actions Column Template -->
      <ng-template #actionsTmpl let-row>
        <button mat-icon-button matTooltip="Preview" (click)="previewAsset(row); $event.stopPropagation()" aria-label="Preview asset">
          <mat-icon>visibility</mat-icon>
        </button>
        <button mat-icon-button matTooltip="Edit Metadata" (click)="openMetadataDrawer(row); $event.stopPropagation()" aria-label="Edit metadata">
          <mat-icon>edit</mat-icon>
        </button>
        <button mat-icon-button [matMenuTriggerFor]="moreMenu" (click)="$event.stopPropagation()" aria-label="Asset actions menu">
          <mat-icon>more_vert</mat-icon>
        </button>
        <mat-menu #moreMenu="matMenu">
          <a mat-menu-item [href]="getDownloadUrl(row.id)" target="_blank">
            <mat-icon>download</mat-icon> Download
          </a>
          <button mat-menu-item *ngIf="row.status === 'ACTIVE'" (click)="archiveAsset(row)">
            <mat-icon>archive</mat-icon> Archive
          </button>
          <button mat-menu-item *ngIf="row.status === 'ARCHIVED'" (click)="restoreAsset(row)">
            <mat-icon>unarchive</mat-icon> Restore
          </button>
          <button mat-menu-item (click)="deleteAsset(row)">
            <mat-icon color="warn">delete</mat-icon> Delete
          </button>
        </mat-menu>
      </ng-template>

      <!-- ── RIGHT COLLAPSIBLE UPLOAD DRAWER ── -->
      <app-asset-upload-dialog
        [isOpen]="uploadDrawerOpen"
        (close)="onUploadDrawerClose($event)"
      ></app-asset-upload-dialog>

      <!-- ── RIGHT COLLAPSIBLE METADATA DRAWER ── -->
      <app-asset-metadata-dialog
        [isOpen]="metadataDrawerOpen"
        [asset]="editingAsset"
        (close)="onMetadataDrawerClose($event)"
      ></app-asset-metadata-dialog>
    </div>
  `,
  styles: [`
    ::ng-deep .chip-image { background-color: #e3f2fd !important; color: #1565c0 !important; }
    ::ng-deep .chip-audio { background-color: #fce4ec !important; color: #c62828 !important; }
    ::ng-deep .chip-video { background-color: #f3e5f5 !important; color: #6a1b9a !important; }
    ::ng-deep .chip-active { background-color: #e8f5e9 !important; color: #2e7d32 !important; }
    ::ng-deep .chip-archived { background-color: #f5f5f5 !important; color: #616161 !important; }
  `]
})
export class AssetListComponent implements OnInit {

  @ViewChild('paginatedTable') paginatedTable!: PaginatedTableComponent<AssetResponse>;

  uploadDrawerOpen = false;
  metadataDrawerOpen = false;
  editingAsset?: AssetResponse;

  filters: Record<string, any> = {};

  filterCategories: FilterCategory[] = [
    {
      key: 'assetType',
      label: 'Asset Type',
      expanded: true,
      options: [
        { label: 'Image', value: 'IMAGE' },
        { label: 'Audio', value: 'AUDIO' },
        { label: 'Video', value: 'VIDEO' }
      ]
    },
    {
      key: 'status',
      label: 'Status',
      expanded: false,
      options: [
        { label: 'Active', value: 'ACTIVE' },
        { label: 'Archived', value: 'ARCHIVED' }
      ]
    },
    {
      key: 'createdAt',
      label: 'Uploaded Date',
      expanded: false,
      options: [
        { label: 'Today', value: 'TODAY' },
        { label: 'Last 7 Days', value: 'LAST_7_DAYS' },
        { label: 'Last 30 Days', value: 'LAST_30_DAYS' }
      ]
    }
  ];

  columns: ColumnDef<AssetResponse>[] = [
    { key: 'originalFilename', header: 'Filename', sortable: true },
    { key: 'assetType', header: 'Type', type: 'chip', chipClass: (val) => 'chip-' + (val || '').toLowerCase(), sortable: true },
    { key: 'fileSize', header: 'Size', cell: (row) => this.assetService.formatFileSize(row.fileSize), sortable: true },
    { key: 'status', header: 'Status', type: 'chip', chipClass: (val) => 'chip-' + (val || '').toLowerCase(), sortable: true },
    { key: 'title', header: 'Title', sortable: true },
    { key: 'createdAt', header: 'Uploaded', type: 'date', sortable: true },
    { key: 'actions', header: 'Actions', type: 'actions' }
  ];

  fetcher: PaginatedDataFetcher<AssetResponse> = (req) => {
    const typeVal = Array.isArray(this.filters['assetType']) ? this.filters['assetType'][0] : this.filters['assetType'];
    const statusVal = Array.isArray(this.filters['status']) ? this.filters['status'][0] : this.filters['status'];

    return this.assetService.searchAssets({
      filename: req.search || undefined,
      assetType: (typeVal as AssetType) || undefined,
      status: (statusVal as AssetStatus) || undefined,
      tags: this.filters['tags'] || undefined,
      page: req.page,
      size: req.size
    });
  };

  constructor(private assetService: AssetService, private dialog: MatDialog, private snackBar: MatSnackBar) {}

  ngOnInit(): void {}

  onFilterChange(updatedFilters: Record<string, any>): void {
    this.filters = { ...updatedFilters };
  }

  getDownloadUrl(id: string): string { return this.assetService.getDownloadUrl(id); }

  openUploadDrawer(): void {
    this.uploadDrawerOpen = true;
  }

  onUploadDrawerClose(result: AssetResponse | null): void {
    this.uploadDrawerOpen = false;
    if (result) {
      this.snackBar.open('Asset uploaded successfully', 'OK', { duration: 3000 });
      this.paginatedTable?.reload();
    }
  }

  openMetadataDrawer(asset: AssetResponse): void {
    this.editingAsset = asset;
    this.metadataDrawerOpen = true;
  }

  onMetadataDrawerClose(result: AssetResponse | null): void {
    this.metadataDrawerOpen = false;
    if (result) {
      this.snackBar.open('Metadata updated', 'OK', { duration: 3000 });
      this.paginatedTable?.reload();
    }
  }

  previewAsset(asset: AssetResponse): void {
    this.dialog.open(AssetPreviewDialogComponent, {
      width: '720px',
      data: { asset }
    });
  }

  archiveAsset(asset: AssetResponse): void {
    this.assetService.archiveAsset(asset.id).subscribe({
      next: () => { this.snackBar.open('Asset archived', 'OK', { duration: 3000 }); this.paginatedTable?.reload(); },
      error: (err) => this.snackBar.open(err?.error?.message || 'Failed to archive', 'Dismiss', { duration: 4000 })
    });
  }

  restoreAsset(asset: AssetResponse): void {
    this.assetService.restoreAsset(asset.id).subscribe({
      next: () => { this.snackBar.open('Asset restored', 'OK', { duration: 3000 }); this.paginatedTable?.reload(); },
      error: (err) => this.snackBar.open(err?.error?.message || 'Failed to restore', 'Dismiss', { duration: 4000 })
    });
  }

  deleteAsset(asset: AssetResponse): void {
    this.assetService.deleteAsset(asset.id).subscribe({
      next: () => { this.snackBar.open('Asset deleted', 'OK', { duration: 3000 }); this.paginatedTable?.reload(); },
      error: (err) => this.snackBar.open(err?.error?.message || 'Failed to delete', 'Dismiss', { duration: 4000 })
    });
  }
}
