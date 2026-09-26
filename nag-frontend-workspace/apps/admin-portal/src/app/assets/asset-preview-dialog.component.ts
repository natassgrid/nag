/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

import {
  Component,
  OnInit,
  OnDestroy,
  ChangeDetectorRef,
  ChangeDetectionStrategy,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { HttpClient, HttpEventType } from '@angular/common/http';
import { AssetResponse } from './asset.model';
import { AssetService } from './asset.service';
import {
  AssetPreviewHeaderComponent,
  AssetPreviewMediaViewerComponent,
  AssetPreviewMetadataRibbonComponent,
} from './components';

export interface AssetPreviewDialogData {
  asset: AssetResponse;
}

@Component({
  selector: 'app-asset-preview-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatProgressBarModule,
    MatSnackBarModule,
    AssetPreviewHeaderComponent,
    AssetPreviewMediaViewerComponent,
    AssetPreviewMetadataRibbonComponent,
  ],
  templateUrl: './asset-preview-dialog.component.html',
  styleUrls: ['./asset-preview-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssetPreviewDialogComponent implements OnInit, OnDestroy {
  readonly dialogRef = inject(MatDialogRef<AssetPreviewDialogComponent>);
  readonly data = inject<AssetPreviewDialogData>(MAT_DIALOG_DATA);
  readonly assetService = inject(AssetService);
  private readonly http = inject(HttpClient);
  private readonly snackBar = inject(MatSnackBar);
  private readonly cdr = inject(ChangeDetectorRef);

  downloadUrl: string;
  blobUrl: string | null = null;
  loading = true;
  loadError = false;
  replacing = false;
  uploadProgress = 0;

  constructor() {
    this.downloadUrl = this.assetService.getDownloadUrl(this.data.asset.id);
  }

  ngOnInit(): void {
    this.loadMedia();
  }

  ngOnDestroy(): void {
    if (this.blobUrl) {
      URL.revokeObjectURL(this.blobUrl);
    }
  }

  loadMedia(): void {
    this.loading = true;
    this.loadError = false;
    this.http.get(this.downloadUrl, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        if (this.blobUrl) {
          URL.revokeObjectURL(this.blobUrl);
        }
        this.blobUrl = URL.createObjectURL(blob);
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.loadError = true;
        this.cdr.detectChanges();
      },
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const file = input.files[0];
    this.replacing = true;
    this.uploadProgress = 0;

    this.assetService.replaceContentWithProgress(this.data.asset.id, file).subscribe({
      next: (httpEvent) => {
        if (httpEvent.type === HttpEventType.UploadProgress && httpEvent.total) {
          this.uploadProgress = Math.round((100 * httpEvent.loaded) / httpEvent.total);
          this.cdr.detectChanges();
        } else if (httpEvent.type === HttpEventType.Response) {
          this.replacing = false;
          if (httpEvent.body?.data) {
            this.data.asset = httpEvent.body.data;
          }
          this.snackBar.open('Asset binary successfully replaced and verified!', 'Dismiss', {
            duration: 3000,
          });
          this.loadMedia();
        }
      },
      error: (err) => {
        this.replacing = false;
        this.snackBar.open(
          err.error?.message || 'Failed to replace file content. Please check file format.',
          'Dismiss',
          { duration: 4000 }
        );
        this.cdr.detectChanges();
      },
    });
  }

  copyUrl(): void {
    const directUrl = `${window.location.origin}${this.downloadUrl}`;
    navigator.clipboard.writeText(directUrl).then(() => {
      this.snackBar.open('Asset direct link copied to clipboard', 'OK', {
        duration: 2500,
      });
    });
  }

  download(): void {
    if (this.blobUrl) {
      const a = document.createElement('a');
      a.href = this.blobUrl;
      a.download = this.data.asset.originalFilename;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
    } else {
      window.open(this.downloadUrl, '_blank');
    }
  }
}
