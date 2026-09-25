/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

import {
  Component,
  ChangeDetectorRef,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import {
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { HttpEventType } from '@angular/common/http';
import { AssetService } from './asset.service';
import { AssetResponse } from './asset.model';

@Component({
  selector: 'app-asset-upload-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  templateUrl: './asset-upload-dialog.component.html',
  styleUrls: ['./asset-upload-dialog.component.scss'],
})
export class AssetUploadDialogComponent {
  readonly dialogRef = inject(MatDialogRef<AssetUploadDialogComponent>);
  readonly assetService = inject(AssetService);
  private readonly fb = inject(FormBuilder);
  private readonly cdr = inject(ChangeDetectorRef);

  selectedFile: File | null = null;
  dragOver = false;
  uploading = false;
  progress = 0;
  errorMessage = '';

  metadataForm = this.fb.group({
    title: [''],
    altText: [''],
    tags: [''],
    description: [''],
  });

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragOver = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.dragOver = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver = false;
    const file = event.dataTransfer?.files?.[0];
    if (file) this.selectFile(file);
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files?.[0]) this.selectFile(input.files[0]);
  }

  selectFile(file: File): void {
    this.errorMessage = '';
    const maxBytes = 100 * 1024 * 1024; // 100 MB
    if (file.size > maxBytes) {
      this.errorMessage = 'The file exceeds the 100 MB size limit.';
      return;
    }
    this.selectedFile = file;
    if (!this.metadataForm.value.title) {
      const cleanName = file.name.replace(/\.[^/.]+$/, '').replace(/[_-]/g, ' ');
      this.metadataForm.patchValue({ title: cleanName });
    }
  }

  upload(): void {
    if (!this.selectedFile) return;

    this.uploading = true;
    this.progress = 0;
    this.errorMessage = '';

    this.assetService.uploadWithProgress(this.selectedFile).subscribe({
      next: (event) => {
        if (event.type === HttpEventType.UploadProgress && event.total) {
          this.progress = Math.round((100 * event.loaded) / event.total);
          this.cdr.markForCheck();
        } else if (event.type === HttpEventType.Response) {
          const uploadedAsset: AssetResponse | undefined = event.body?.data;
          if (uploadedAsset) {
            const formVal = this.metadataForm.value;
            if (formVal.title || formVal.altText || formVal.tags || formVal.description) {
              this.assetService
                .updateMetadata(uploadedAsset.id, {
                  title: formVal.title || undefined,
                  altText: formVal.altText || undefined,
                  tags: formVal.tags || undefined,
                  description: formVal.description || undefined,
                })
                .subscribe({
                  next: (updated) => {
                    this.uploading = false;
                    this.dialogRef.close(updated);
                  },
                  error: () => {
                    this.uploading = false;
                    this.dialogRef.close(uploadedAsset);
                  },
                });
            } else {
              this.uploading = false;
              this.dialogRef.close(uploadedAsset);
            }
          } else {
            this.uploading = false;
            this.dialogRef.close(null);
          }
          this.cdr.markForCheck();
        }
      },
      error: (err) => {
        this.uploading = false;
        if (err.status === 413) {
          this.errorMessage =
            'File exceeds maximum permitted upload payload limit (100 MB).';
        } else {
          this.errorMessage =
            err?.error?.message ||
            err?.error?.detail ||
            err?.message ||
            'Upload failed. Please check network connectivity.';
        }
        this.cdr.markForCheck();
      },
    });
  }

  formatSize(bytes: number): string {
    return this.assetService.formatFileSize(bytes);
  }
}
