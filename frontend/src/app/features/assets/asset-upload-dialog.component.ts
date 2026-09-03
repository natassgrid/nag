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

import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges, ChangeDetectionStrategy } from '@angular/core';
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
  templateUrl: './asset-upload-dialog.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./asset-upload-dialog.component.scss']
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
