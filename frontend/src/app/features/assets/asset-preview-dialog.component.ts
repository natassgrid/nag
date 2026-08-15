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

import { Component, Inject, ChangeDetectorRef } from '@angular/core';
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
  template: `
    <h2 mat-dialog-title class="preview-title">
      <mat-icon>{{ getTypeIcon() }}</mat-icon>
      {{ data.asset.originalFilename }}
    </h2>
    <mat-dialog-content class="preview-content">
      <!-- Loading -->
      <div *ngIf="loading" class="media-container">
        <mat-spinner diameter="40"></mat-spinner>
      </div>

      <!-- Image Preview -->
      <div *ngIf="!loading && data.asset.assetType === 'IMAGE' && blobUrl" class="media-container">
        <img [src]="blobUrl" [alt]="data.asset.altText || data.asset.originalFilename" class="preview-image" />
      </div>

      <!-- Audio Preview -->
      <div *ngIf="!loading && data.asset.assetType === 'AUDIO' && blobUrl" class="media-container audio-container">
        <mat-icon class="audio-icon">audiotrack</mat-icon>
        <audio controls [src]="blobUrl" class="preview-audio">
          Your browser does not support audio playback.
        </audio>
      </div>

      <!-- Video Preview -->
      <div *ngIf="!loading && data.asset.assetType === 'VIDEO' && blobUrl" class="media-container">
        <video controls [src]="blobUrl" class="preview-video">
          Your browser does not support video playback.
        </video>
      </div>

      <!-- Load failed -->
      <div *ngIf="!loading && !blobUrl" class="media-container">
        <mat-icon style="font-size:48px;width:48px;height:48px;color:#bbb;">broken_image</mat-icon>
        <p style="color:#999;">Failed to load preview</p>
      </div>

      <!-- Metadata -->
      <div class="metadata-section">
        <div class="metadata-row">
          <span class="label">Type</span>
          <mat-chip-set><mat-chip>{{ data.asset.assetType }}</mat-chip></mat-chip-set>
        </div>
        <div class="metadata-row">
          <span class="label">Size</span>
          <span>{{ formatSize(data.asset.fileSize) }}</span>
        </div>
        <div class="metadata-row" *ngIf="data.asset.width && data.asset.height">
          <span class="label">Dimensions</span>
          <span>{{ data.asset.width }} × {{ data.asset.height }} px</span>
        </div>
        <div class="metadata-row" *ngIf="data.asset.durationSeconds">
          <span class="label">Duration</span>
          <span>{{ formatDuration(data.asset.durationSeconds) }}</span>
        </div>
        <div class="metadata-row" *ngIf="data.asset.codec">
          <span class="label">Codec</span>
          <span>{{ data.asset.codec }}</span>
        </div>
        <div class="metadata-row" *ngIf="data.asset.title">
          <span class="label">Title</span>
          <span>{{ data.asset.title }}</span>
        </div>
        <div class="metadata-row" *ngIf="data.asset.description">
          <span class="label">Description</span>
          <span>{{ data.asset.description }}</span>
        </div>
        <div class="metadata-row" *ngIf="data.asset.tags">
          <span class="label">Tags</span>
          <span>{{ data.asset.tags }}</span>
        </div>
        <div class="metadata-row">
          <span class="label">SHA-256</span>
          <span class="hash">{{ data.asset.sha256Hash }}</span>
        </div>
        <div class="metadata-row">
          <span class="label">Uploaded</span>
          <span>{{ data.asset.createdAt | date:'medium' }}</span>
        </div>
        <div class="metadata-row">
          <span class="label">Status</span>
          <mat-chip-set><mat-chip [class]="'chip-' + data.asset.status.toLowerCase()">{{ data.asset.status }}</mat-chip></mat-chip-set>
        </div>
      </div>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="download()">
        <mat-icon>download</mat-icon> Download
      </button>
      <button mat-button (click)="dialogRef.close()">Close</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .preview-title { display: flex; align-items: center; gap: 8px; }
    .preview-content { min-width: 500px; max-width: 700px; }
    .media-container { display: flex; justify-content: center; align-items: center; margin-bottom: 16px; background: #f5f5f5; border-radius: 8px; padding: 16px; min-height: 200px; }
    .preview-image { max-width: 100%; max-height: 400px; border-radius: 4px; object-fit: contain; }
    .preview-video { max-width: 100%; max-height: 400px; border-radius: 4px; }
    .preview-audio { width: 100%; }
    .audio-container { flex-direction: column; gap: 16px; }
    .audio-icon { font-size: 48px; width: 48px; height: 48px; color: #757575; }
    .metadata-section { border-top: 1px solid #e0e0e0; padding-top: 12px; }
    .metadata-row { display: flex; align-items: center; gap: 12px; padding: 6px 0; font-size: 13px; }
    .metadata-row .label { font-weight: 500; color: #616161; min-width: 100px; }
    .hash { font-family: monospace; font-size: 11px; word-break: break-all; color: #757575; }
    ::ng-deep .chip-active { background-color: #e8f5e9 !important; color: #2e7d32 !important; }
    ::ng-deep .chip-archived { background-color: #f5f5f5 !important; color: #616161 !important; }
  `]
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
