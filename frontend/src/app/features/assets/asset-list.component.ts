import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatCardModule } from '@angular/material/card';
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
  PaginatedDataFetcher
} from '../../shared/components/paginated-table';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';

@Component({
  selector: 'app-asset-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatSnackBarModule,
    MatCardModule,
    MatChipsModule,
    MatTooltipModule,
    MatMenuModule,
    PaginatedTableComponent,
    PageHeaderComponent
  ],
  template: `
    <div class="page-layout">
      <app-page-header
        title="Asset Library"
        subtitle="Upload, manage, and organize media assets for examinations."
        icon="perm_media"
      >
        <button mat-raised-button color="primary" (click)="openUploadDialog()">
          <mat-icon>cloud_upload</mat-icon>
          Upload Asset
        </button>
      </app-page-header>

      <mat-card>
        <mat-card-content>
          <div class="filters-row">
            <mat-form-field appearance="outline">
              <mat-label>Asset Type</mat-label>
              <mat-select [(ngModel)]="filters.assetType" (selectionChange)="applyFilters()">
                <mat-option value="">All Types</mat-option>
                <mat-option value="IMAGE">Image</mat-option>
                <mat-option value="AUDIO">Audio</mat-option>
                <mat-option value="VIDEO">Video</mat-option>
              </mat-select>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Status</mat-label>
              <mat-select [(ngModel)]="filters.status" (selectionChange)="applyFilters()">
                <mat-option value="">All</mat-option>
                <mat-option value="ACTIVE">Active</mat-option>
                <mat-option value="ARCHIVED">Archived</mat-option>
              </mat-select>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Tags</mat-label>
              <input matInput [(ngModel)]="filters.tags" (blur)="applyFilters()" placeholder="Filter by tags..." />
            </mat-form-field>
          </div>

          <app-paginated-table
            #paginatedTable
            [fetcher]="fetcher"
            [columns]="columns"
            [filters]="filters"
            [actionsTemplate]="actionsTmpl"
            searchPlaceholder="Search assets by filename..."
          ></app-paginated-table>

          <ng-template #actionsTmpl let-row>
            <button mat-icon-button matTooltip="Preview" (click)="previewAsset(row); $event.stopPropagation()">
              <mat-icon>visibility</mat-icon>
            </button>
            <button mat-icon-button matTooltip="Edit Metadata" (click)="openMetadataDialog(row); $event.stopPropagation()">
              <mat-icon>edit</mat-icon>
            </button>
            <button mat-icon-button [matMenuTriggerFor]="moreMenu" (click)="$event.stopPropagation()">
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
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .filters-row {
      display: flex;
      gap: 16px;
      flex-wrap: nowrap;
      margin-bottom: 16px;
      align-items: center;
    }
    .filters-row mat-form-field { flex: 1; min-width: 0; }
    ::ng-deep .chip-image { background-color: #e3f2fd !important; color: #1565c0 !important; }
    ::ng-deep .chip-audio { background-color: #fce4ec !important; color: #c62828 !important; }
    ::ng-deep .chip-video { background-color: #f3e5f5 !important; color: #6a1b9a !important; }
    ::ng-deep .chip-active { background-color: #e8f5e9 !important; color: #2e7d32 !important; }
    ::ng-deep .chip-archived { background-color: #f5f5f5 !important; color: #616161 !important; }
  `]
})
export class AssetListComponent implements OnInit {

  @ViewChild('paginatedTable') paginatedTable!: PaginatedTableComponent<AssetResponse>;

  filters = { assetType: '', status: '', tags: '' };

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
    return this.assetService.searchAssets({
      filename: req.search || undefined,
      assetType: (this.filters['assetType'] as AssetType) || undefined,
      status: (this.filters['status'] as AssetStatus) || undefined,
      tags: this.filters['tags'] || undefined,
      page: req.page,
      size: req.size
    });
  };

  constructor(private assetService: AssetService, private dialog: MatDialog, private snackBar: MatSnackBar) {}

  ngOnInit(): void {}

  applyFilters(): void { this.filters = { ...this.filters }; }

  getDownloadUrl(id: string): string { return this.assetService.getDownloadUrl(id); }

  openUploadDialog(): void {
    const dialogRef = this.dialog.open(AssetUploadDialogComponent, { width: '600px', disableClose: true });
    dialogRef.afterClosed().subscribe(result => {
      if (result) { this.snackBar.open('Asset uploaded successfully', 'OK', { duration: 3000 }); this.paginatedTable?.reload(); }
    });
  }

  openMetadataDialog(asset: AssetResponse): void {
    const dialogRef = this.dialog.open(AssetMetadataDialogComponent, { width: '500px', data: { asset } });
    dialogRef.afterClosed().subscribe(result => {
      if (result) { this.snackBar.open('Metadata updated', 'OK', { duration: 3000 }); this.paginatedTable?.reload(); }
    });
  }

  previewAsset(asset: AssetResponse): void {
    this.dialog.open(AssetPreviewDialogComponent, {
      width: '720px',
      data: { asset }
    });
  }

  archiveAsset(asset: AssetResponse): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Archive Asset', message: `Archive "${asset.originalFilename}"?`, confirmText: 'Archive', color: 'primary', icon: 'archive' } as ConfirmDialogData
    });
    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.assetService.archiveAsset(asset.id).subscribe({
          next: () => { this.snackBar.open('Asset archived', 'OK', { duration: 3000 }); this.paginatedTable?.reload(); },
          error: (err) => this.snackBar.open(err?.error?.message || 'Failed to archive', 'Dismiss', { duration: 4000 })
        });
      }
    });
  }

  restoreAsset(asset: AssetResponse): void {
    this.assetService.restoreAsset(asset.id).subscribe({
      next: () => { this.snackBar.open('Asset restored', 'OK', { duration: 3000 }); this.paginatedTable?.reload(); },
      error: (err) => this.snackBar.open(err?.error?.message || 'Failed to restore', 'Dismiss', { duration: 4000 })
    });
  }

  deleteAsset(asset: AssetResponse): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Delete Asset', message: `Delete "${asset.originalFilename}"? Referenced assets cannot be deleted.`, confirmText: 'Delete', color: 'warn', icon: 'delete' } as ConfirmDialogData
    });
    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.assetService.deleteAsset(asset.id).subscribe({
          next: () => { this.snackBar.open('Asset deleted', 'OK', { duration: 3000 }); this.paginatedTable?.reload(); },
          error: (err) => this.snackBar.open(err?.error?.message || 'Failed to delete', 'Dismiss', { duration: 4000 })
        });
      }
    });
  }
}
