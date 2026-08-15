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

import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { HttpEventType } from '@angular/common/http';
import { AssetService } from './asset.service';
import { AssetResponse } from './asset.model';
import { RightDrawerComponent } from '../../shared/components/right-drawer/right-drawer.component';

@Component({
  selector: 'app-asset-upload-dialog',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatProgressBarModule, RightDrawerComponent],
  template: `
    <app-right-drawer
      [isOpen]="isOpen"
      title="Upload Asset"
      subtitle="Add image, audio, or video media files to the library."
      width="480px"
      (close)="cancel()"
    >
      <div drawer-body>
        <div
          class="drop-zone"
          [class.drag-over]="dragOver"
          [class.has-file]="!!selectedFile"
          (dragover)="onDragOver($event)"
          (dragleave)="dragOver = false"
          (drop)="onDrop($event)"
          (click)="fileInput.click()"
        >
          <input #fileInput type="file" hidden (change)="onFileSelected($event)"
                 accept="image/png,image/jpeg,image/webp,image/svg+xml,audio/mpeg,audio/aac,audio/wav,video/mp4" />
          <mat-icon class="upload-icon">{{ selectedFile ? 'check_circle' : 'cloud_upload' }}</mat-icon>
          <p *ngIf="!selectedFile">Drag & drop a file here or click to browse</p>
          <p *ngIf="selectedFile" class="file-info">
            {{ selectedFile.name }} ({{ formatSize(selectedFile.size) }})
          </p>
          <span class="hint">Supported: PNG, JPEG, WebP, SVG, MP3, AAC, WAV, MP4 (max 100 MB)</span>
        </div>

        <mat-progress-bar *ngIf="uploading" mode="determinate" [value]="progress"></mat-progress-bar>
        <p *ngIf="error" class="error-msg">{{ error }}</p>
      </div>

      <div drawer-footer>
        <button mat-button [disabled]="uploading" (click)="cancel()">Cancel</button>
        <button mat-raised-button color="primary" [disabled]="!selectedFile || uploading" (click)="upload()">
          {{ uploading ? 'Uploading...' : 'Upload' }}
        </button>
      </div>
    </app-right-drawer>
  `,
  styles: [`
    .drop-zone {
      border: 2px dashed #bdbdbd;
      border-radius: 12px;
      padding: 40px 24px;
      text-align: center;
      cursor: pointer;
      transition: border-color 0.2s, background 0.2s;
      margin-bottom: 16px;
    }
    .drop-zone:hover, .drop-zone.drag-over { border-color: #1976d2; background: #e3f2fd; }
    .drop-zone.has-file { border-color: #4caf50; background: #e8f5e9; }
    .upload-icon { font-size: 48px; width: 48px; height: 48px; color: #757575; }
    .drop-zone.has-file .upload-icon { color: #4caf50; }
    .file-info { font-weight: 500; margin: 8px 0 0; }
    .hint { font-size: 12px; color: #9e9e9e; }
    .error-msg { color: #c62828; font-size: 13px; margin-top: 8px; }
  `]
})
export class AssetUploadDialogComponent implements OnChanges {
  @Input() isOpen = false;
  @Output() close = new EventEmitter<AssetResponse | null>();

  selectedFile: File | null = null;
  dragOver = false;
  uploading = false;
  progress = 0;
  error = '';

  constructor(private assetService: AssetService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen'] && this.isOpen) {
      this.selectedFile = null;
      this.uploading = false;
      this.error = '';
      this.progress = 0;
    }
  }

  onDragOver(event: DragEvent): void { event.preventDefault(); this.dragOver = true; }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver = false;
    const file = event.dataTransfer?.files[0];
    if (file) this.selectFile(file);
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files?.[0]) this.selectFile(input.files[0]);
  }

  selectFile(file: File): void {
    this.error = '';
    if (file.size > 100 * 1024 * 1024) { this.error = 'File exceeds 100 MB limit'; return; }
    this.selectedFile = file;
  }

  cancel(): void {
    this.close.emit(null);
  }

  upload(): void {
    if (!this.selectedFile) return;
    this.uploading = true;
    this.progress = 0;
    this.error = '';

    this.assetService.uploadWithProgress(this.selectedFile).subscribe({
      next: (event) => {
        if (event.type === HttpEventType.UploadProgress && event.total) {
          this.progress = Math.round(100 * event.loaded / event.total);
        } else if (event.type === HttpEventType.Response) {
          this.uploading = false;
          this.close.emit(event.body?.data || null);
        }
      },
      error: (err) => {
        this.uploading = false;
        this.error = err?.error?.message || 'Upload failed. Please try again.';
      }
    });
  }

  formatSize(bytes: number): string { return this.assetService.formatFileSize(bytes); }
}
