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
  templateUrl: './asset-picker-dialog.component.html',
  styleUrls: ['./asset-picker-dialog.component.scss']
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
