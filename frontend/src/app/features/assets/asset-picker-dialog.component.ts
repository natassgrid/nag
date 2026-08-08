import { Component, Inject, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatTabsModule } from '@angular/material/tabs';
import { AssetService } from './asset.service';
import { AssetResponse, AssetType } from './asset.model';
import { AssetUploadDialogComponent } from './asset-upload-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import {
  PaginatedTableComponent,
  ColumnDef,
  PaginatedDataFetcher
} from '../../shared/components/paginated-table';

export interface AssetPickerDialogData {
  assetType?: AssetType;
  title?: string;
}

@Component({
  selector: 'app-asset-picker-dialog',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatDialogModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatButtonModule, MatIconModule, MatCardModule, MatTabsModule,
    MatSnackBarModule, PaginatedTableComponent
  ],
  template: `
    <h2 mat-dialog-title>{{ data.title || 'Select Asset' }}</h2>
    <mat-dialog-content>
      <div class="picker-toolbar">
        <button mat-stroked-button color="primary" (click)="uploadNew()">
          <mat-icon>cloud_upload</mat-icon> Upload New
        </button>
      </div>

      <app-paginated-table
        #pickerTable
        [fetcher]="fetcher"
        [columns]="columns"
        [actionsTemplate]="selectTmpl"
        searchPlaceholder="Search assets..."
        [defaultPageSize]="10"
      ></app-paginated-table>

      <ng-template #selectTmpl let-row>
        <button mat-raised-button color="primary" (click)="select(row)">Select</button>
      </ng-template>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="dialogRef.close()">Cancel</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .picker-toolbar { display: flex; justify-content: flex-end; margin-bottom: 12px; }
    mat-dialog-content { min-width: 600px; max-height: 500px; }
  `]
})
export class AssetPickerDialogComponent {

  @ViewChild('pickerTable') pickerTable!: PaginatedTableComponent<AssetResponse>;

  columns: ColumnDef<AssetResponse>[] = [
    { key: 'originalFilename', header: 'Filename', sortable: true },
    { key: 'assetType', header: 'Type', sortable: true },
    { key: 'fileSize', header: 'Size', cell: (row) => this.assetService.formatFileSize(row.fileSize) },
    { key: 'createdAt', header: 'Uploaded', type: 'date' },
    { key: 'actions', header: '', type: 'actions' }
  ];

  fetcher: PaginatedDataFetcher<AssetResponse> = (req) => {
    return this.assetService.searchAssets({
      filename: req.search || undefined,
      assetType: this.data.assetType || undefined,
      status: 'ACTIVE',
      page: req.page,
      size: req.size
    });
  };

  constructor(
    public dialogRef: MatDialogRef<AssetPickerDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: AssetPickerDialogData,
    private assetService: AssetService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  select(asset: AssetResponse): void {
    this.dialogRef.close(asset);
  }

  uploadNew(): void {
    const uploadRef = this.dialog.open(AssetUploadDialogComponent, { width: '600px', disableClose: true });
    uploadRef.afterClosed().subscribe(result => {
      if (result) {
        this.snackBar.open('Uploaded! Selecting...', 'OK', { duration: 2000 });
        this.dialogRef.close(result);
      }
    });
  }
}
