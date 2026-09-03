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

import { Component, Inject, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { HttpClient } from '@angular/common/http';
import { AssetResponse } from './asset.model';
import { AssetService } from './asset.service';

export interface AssetPreviewDialogData {
  asset: AssetResponse;
}

@Component({
  selector: 'app-asset-preview-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule, MatChipsModule, MatProgressSpinnerModule],
  templateUrl: './asset-preview-dialog.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./asset-preview-dialog.component.scss']
})
export class AssetPreviewDialogComponent {
  downloadUrl: string;
  blobUrl: string | null = null;
  loading = true;

  constructor(
    public dialogRef: MatDialogRef<AssetPreviewDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: AssetPreviewDialogData,
    private assetService: AssetService,
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {
    this.downloadUrl = this.assetService.getDownloadUrl(data.asset.id);
    this.loadBlobUrl();
  }

  private loadBlobUrl(): void {
    this.http.get(this.downloadUrl, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        this.blobUrl = URL.createObjectURL(blob);
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  ngOnDestroy(): void {
    if (this.blobUrl) {
      URL.revokeObjectURL(this.blobUrl);
    }
  }

  getTypeIcon(): string {
    switch (this.data.asset.assetType) {
      case 'IMAGE': return 'image';
      case 'AUDIO': return 'audiotrack';
      case 'VIDEO': return 'videocam';
      default: return 'insert_drive_file';
    }
  }

  formatSize(bytes: number): string {
    return this.assetService.formatFileSize(bytes);
  }

  formatDuration(seconds: number): string {
    const m = Math.floor(seconds / 60);
    const s = Math.round(seconds % 60);
    return m > 0 ? `${m}m ${s}s` : `${s}s`;
  }

  download(): void {
    const url = this.blobUrl;
    if (url) {
      const a = document.createElement('a');
      a.href = url;
      a.download = this.data.asset.originalFilename;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
    } else {
      // Blob not yet loaded, fetch and download
      this.http.get(this.downloadUrl, { responseType: 'blob' }).subscribe(blob => {
        const blobUrl = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = blobUrl;
        a.download = this.data.asset.originalFilename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(blobUrl);
      });
    }
  }
}
