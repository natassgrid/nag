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

import { Component, Inject, ViewChild, ChangeDetectorRef, OnInit, OnDestroy } from '@angular/core';
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
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { HttpEventType } from '@angular/common/http';
import { AssetService } from './asset.service';
import { AssetResponse, AssetType } from './asset.model';
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
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatTabsModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule,
    PaginatedTableComponent
  ],
  templateUrl: './asset-picker-dialog.component.html',
  styleUrls: ['./asset-picker-dialog.component.scss']
})
export class AssetPickerDialogComponent implements OnInit, OnDestroy {

  @ViewChild('pickerTable') pickerTable!: PaginatedTableComponent<AssetResponse>;

  selectedTabIndex = 0;

  // Upload State
  selectedFile: File | null = null;
  previewUrl: string | null = null;
  customAltText = '';
  customTitle = '';
  dragOver = false;
  uploading = false;
  uploadProgress = 0;
  uploadError = '';

  columns: ColumnDef<AssetResponse>[] = [
    { key: 'originalFilename', header: 'Filename', sortable: true },
    { key: 'assetType', header: 'Type', sortable: true },
    { key: 'fileSize', header: 'Size', cell: (row) => this.assetService.formatFileSize(row.fileSize), sortable: true },
    { key: 'createdAt', header: 'Uploaded', type: 'date', sortable: true },
    { key: 'actions', header: '', type: 'actions' }
  ];

  fetcher: PaginatedDataFetcher<AssetResponse> = (req) => {
    return this.assetService.searchAssets({
      filename: req.search || undefined,
      assetType: this.data.assetType || undefined,
      status: 'ACTIVE',
      sort: req.sort,
      order: req.order,
      page: req.page,
      size: req.size
    });
  };

  constructor(
    public dialogRef: MatDialogRef<AssetPickerDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: AssetPickerDialogData,
    private assetService: AssetService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {}

  ngOnDestroy(): void {
    if (this.previewUrl) {
      URL.revokeObjectURL(this.previewUrl);
    }
  }

  get acceptedTypes(): string {
    if (this.data?.assetType === 'IMAGE') {
      return 'image/png,image/jpeg,image/webp,image/svg+xml';
    } else if (this.data?.assetType === 'AUDIO') {
      return 'audio/mpeg,audio/aac,audio/wav';
    } else if (this.data?.assetType === 'VIDEO') {
      return 'video/mp4';
    }
    return 'image/png,image/jpeg,image/webp,image/svg+xml,audio/mpeg,audio/aac,audio/wav,video/mp4';
  }

  get acceptedHint(): string {
    if (this.data?.assetType === 'IMAGE') {
      return 'Supported: PNG, JPEG, WebP, SVG (max 100 MB)';
    }
    return 'Supported: PNG, JPEG, WebP, SVG, MP3, AAC, WAV, MP4 (max 100 MB)';
  }

  select(asset: AssetResponse): void {
    this.dialogRef.close(asset);
  }

  uploadNew(): void {
    this.selectedTabIndex = 1;
  }

  onTabChange(index: number): void {
    this.selectedTabIndex = index;
    if (index === 0 && this.pickerTable) {
      this.pickerTable.reload();
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragOver = true;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver = false;
    const file = event.dataTransfer?.files[0];
    if (file) {
      this.selectFile(file);
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files?.[0]) {
      this.selectFile(input.files[0]);
    }
  }

  selectFile(file: File): void {
    this.uploadError = '';
    if (file.size > 100 * 1024 * 1024) {
      this.uploadError = 'File exceeds 100 MB limit';
      return;
    }

    if (this.previewUrl) {
      URL.revokeObjectURL(this.previewUrl);
      this.previewUrl = null;
    }

    this.selectedFile = file;

    if (file.type.startsWith('image/')) {
      this.previewUrl = URL.createObjectURL(file);
    }

    if (!this.customTitle) {
      this.customTitle = file.name.replace(/\.[^/.]+$/, '');
    }
    if (!this.customAltText) {
      this.customAltText = this.customTitle;
    }

    this.cdr.markForCheck();
  }

  clearSelectedFile(): void {
    if (this.previewUrl) {
      URL.revokeObjectURL(this.previewUrl);
      this.previewUrl = null;
    }
    this.selectedFile = null;
    this.customAltText = '';
    this.customTitle = '';
    this.uploadProgress = 0;
    this.uploadError = '';
    this.cdr.markForCheck();
  }

  formatSize(bytes: number): string {
    return this.assetService.formatFileSize(bytes);
  }

  private cleanErrorMessage(err: any): string {
    if (err?.status === 413) {
      return 'The uploaded file exceeds the maximum allowed size (100 MB). Please upload a smaller file.';
    }
    let msg = err?.error?.message || err?.error?.detail || err?.error?.error || (typeof err?.error === 'string' ? err.error : '') || err?.message || 'Upload failed. Please try again.';
    if (typeof msg === 'string' && /<[a-z][\s\S]*>/i.test(msg)) {
      const titleMatch = msg.match(/<title[^>]*>(.*?)<\/title>/i);
      const h1Match = msg.match(/<h1[^>]*>(.*?)<\/h1>/i);
      if (h1Match && h1Match[1] && !h1Match[1].toLowerCase().includes('error')) {
        msg = h1Match[1].replace(/\s+/g, ' ').trim();
      } else if (titleMatch && titleMatch[1]) {
        msg = titleMatch[1].replace(/\s+/g, ' ').trim();
      } else {
        msg = msg.replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim();
      }
    }
    return msg;
  }

  uploadAndSelect(): void {
    if (!this.selectedFile) return;

    this.uploading = true;
    this.uploadProgress = 0;
    this.uploadError = '';
    this.cdr.markForCheck();

    this.assetService.uploadWithProgress(this.selectedFile).subscribe({
      next: (event) => {
        if (event.type === HttpEventType.UploadProgress && event.total) {
          this.uploadProgress = Math.round((100 * event.loaded) / event.total);
          this.cdr.markForCheck();
        } else if (event.type === HttpEventType.Response) {
          const uploadedAsset = event.body?.data;
          if (uploadedAsset) {
            // If custom alt text or title were provided, update metadata
            const hasCustomMetadata = (this.customAltText && this.customAltText !== uploadedAsset.altText) ||
                                      (this.customTitle && this.customTitle !== uploadedAsset.title);

            if (hasCustomMetadata) {
              this.assetService.updateMetadata(uploadedAsset.id, {
                altText: this.customAltText || undefined,
                title: this.customTitle || undefined
              }).subscribe({
                next: (updatedAsset) => {
                  this.uploading = false;
                  this.snackBar.open('Asset uploaded & selected!', 'OK', { duration: 2500 });
                  this.dialogRef.close(updatedAsset || { ...uploadedAsset, altText: this.customAltText, title: this.customTitle });
                },
                error: () => {
                  // If metadata update fails, still return asset with client-side altText/title
                  this.uploading = false;
                  this.snackBar.open('Asset uploaded & selected!', 'OK', { duration: 2500 });
                  this.dialogRef.close({ ...uploadedAsset, altText: this.customAltText, title: this.customTitle });
                }
              });
            } else {
              this.uploading = false;
              this.snackBar.open('Asset uploaded & selected!', 'OK', { duration: 2500 });
              this.dialogRef.close(uploadedAsset);
            }
          } else {
            this.uploading = false;
            this.uploadError = 'No asset returned from server.';
            this.cdr.markForCheck();
          }
        }
      },
      error: (err) => {
        this.uploading = false;
        this.uploadError = this.cleanErrorMessage(err);
        this.cdr.markForCheck();
      }
    });
  }
}
