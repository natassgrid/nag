/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

import {
  Component,
  Inject,
  OnInit,
  OnDestroy,
  ChangeDetectorRef,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { HttpClient } from '@angular/common/http';
import { AssetResponse } from './asset.model';
import { AssetService } from './asset.service';

export interface AssetPreviewDialogData {
  asset: AssetResponse;
}

@Component({
  selector: 'app-asset-preview-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
  templateUrl: './asset-preview-dialog.component.html',
  styleUrls: ['./asset-preview-dialog.component.scss'],
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
