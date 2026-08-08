import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { HttpEventType } from '@angular/common/http';
import { AssetService } from './asset.service';
import { AssetResponse } from './asset.model';

@Component({
  selector: 'app-asset-upload-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule, MatProgressBarModule],
  template: `
    <h2 mat-dialog-title>Upload Asset</h2>
    <mat-dialog-content>
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
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [disabled]="uploading" (click)="dialogRef.close()">Cancel</button>
      <button mat-raised-button color="primary" [disabled]="!selectedFile || uploading" (click)="upload()">
        {{ uploading ? 'Uploading...' : 'Upload' }}
      </button>
    </mat-dialog-actions>
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
export class AssetUploadDialogComponent {

  selectedFile: File | null = null;
  dragOver = false;
  uploading = false;
  progress = 0;
  error = '';

  constructor(public dialogRef: MatDialogRef<AssetUploadDialogComponent>, private assetService: AssetService) {}

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
          this.dialogRef.close(event.body?.data);
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
