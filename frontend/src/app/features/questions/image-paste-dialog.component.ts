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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.\n */

import { Component, Inject, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';

export interface ImagePasteDialogData {
  title?: string;
  currentUrl?: string;
  altText?: string;
  showAltText?: boolean;
}

export interface ImagePasteDialogResult {
  imageUrl: string;
  altText: string;
}

@Component({
  selector: 'app-image-paste-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title class="dialog-title">
      <mat-icon class="title-icon">data_object</mat-icon>
      {{ data.title || 'Paste Base64 / Data URI' }}
    </h2>

    <mat-dialog-content class="dialog-content">
      <p class="dialog-hint">
        Paste a Base64 Image string, Data URI (<code>data:image/svg+xml;base64,...</code>, <code>data:image/png;base64,...</code>), or raw <code>&lt;svg&gt;...&lt;/svg&gt;</code> code.
      </p>

      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Image Data URI or Raw SVG</mat-label>
        <textarea
          matInput
          [(ngModel)]="rawInput"
          (ngModelChange)="onInputChange()"
          rows="5"
          placeholder="data:image/png;base64,iVBORw0KGgo... or <svg>...</svg>"
          spellcheck="false"
        ></textarea>
        <button
          mat-icon-button
          matSuffix
          *ngIf="rawInput"
          (click)="rawInput = ''; onInputChange()"
          matTooltip="Clear input"
          type="button"
        >
          <mat-icon>clear</mat-icon>
        </button>
      </mat-form-field>

      <mat-form-field appearance="outline" class="full-width" *ngIf="data.showAltText !== false">
        <mat-label>Alt Text / Description (Optional)</mat-label>
        <input
          matInput
          [(ngModel)]="altText"
          placeholder="e.g. Mirror reflection diagram"
        />
      </mat-form-field>

      <!-- Live Preview -->
      <div class="preview-box" *ngIf="previewUrl">
        <div class="preview-header">
          <mat-icon>visibility</mat-icon>
          <span>Image Preview</span>
        </div>
        <div class="preview-img-wrapper">
          <img [src]="safePreviewUrl" [alt]="altText || 'Pasted image'" class="preview-img" />
        </div>
      </div>

      <div class="error-msg" *ngIf="parseError">
        <mat-icon>error_outline</mat-icon>
        <span>{{ parseError }}</span>
      </div>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button (click)="dialogRef.close(null)" type="button">Cancel</button>
      <button
        mat-raised-button
        color="primary"
        [disabled]="!normalizedUrl"
        (click)="apply()"
        type="button"
      >
        Apply Image
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-title {
      display: flex;
      align-items: center;
      gap: 8px;
      margin: 0;
      font-size: 18px;
      font-weight: 600;
      color: #1a237e;

      .title-icon {
        color: #3f51b5;
      }
    }

    .dialog-content {
      display: flex;
      flex-direction: column;
      gap: 12px;
      min-width: 440px;
      max-width: 580px;
      padding-top: 8px;
    }

    .dialog-hint {
      font-size: 13px;
      color: #616161;
      margin: 0 0 6px 0;
      line-height: 1.4;

      code {
        background: #f1f3f4;
        padding: 2px 4px;
        border-radius: 3px;
        font-size: 12px;
      }
    }

    .full-width {
      width: 100%;
    }

    textarea {
      font-family: monospace;
      font-size: 12px;
      line-height: 1.4;
    }

    .preview-box {
      border: 1px solid #e0e0e0;
      border-radius: 6px;
      background: #fafafa;
      padding: 10px;
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .preview-header {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 12px;
      font-weight: 600;
      color: #424242;
      text-transform: uppercase;
      letter-spacing: 0.5px;

      mat-icon {
        font-size: 16px;
        width: 16px;
        height: 16px;
      }
    }

    .preview-img-wrapper {
      display: flex;
      justify-content: center;
      align-items: center;
      background: #ffffff;
      border: 1px dashed #bdbdbd;
      border-radius: 4px;
      padding: 8px;
      min-height: 80px;
      max-height: 200px;
      overflow: auto;
    }

    .preview-img {
      max-height: 180px;
      max-width: 100%;
      object-fit: contain;
    }

    .error-msg {
      display: flex;
      align-items: center;
      gap: 6px;
      color: #c62828;
      font-size: 12px;
      font-weight: 500;

      mat-icon {
        font-size: 16px;
        width: 16px;
        height: 16px;
      }
    }
  `]
})
export class ImagePasteDialogComponent {
  rawInput = '';
  altText = '';
  normalizedUrl = '';
  previewUrl = '';
  safePreviewUrl: SafeUrl = '';
  parseError = '';

  constructor(
    public dialogRef: MatDialogRef<ImagePasteDialogComponent, ImagePasteDialogResult | null>,
    @Inject(MAT_DIALOG_DATA) public data: ImagePasteDialogData,
    private sanitizer: DomSanitizer,
    private cdr: ChangeDetectorRef
  ) {
    if (data.currentUrl) {
      this.rawInput = data.currentUrl;
      this.altText = data.altText || '';
      this.onInputChange();
    }
    if (data.altText) {
      this.altText = data.altText;
    }
  }

  onInputChange(): void {
    const input = (this.rawInput || '').trim();
    this.parseError = '';
    this.normalizedUrl = '';
    this.previewUrl = '';

    if (!input) {
      this.cdr.markForCheck();
      return;
    }

    try {
      if (input.startsWith('<svg') || input.startsWith('<?xml') && input.includes('<svg')) {
        // Raw SVG string -> base64 Data URI
        const b64 = btoa(unescape(encodeURIComponent(input)));
        this.normalizedUrl = `data:image/svg+xml;base64,${b64}`;
      } else if (input.startsWith('data:image/')) {
        // Standard data URI
        this.normalizedUrl = input;
      } else if (input.startsWith('http://') || input.startsWith('https://') || input.startsWith('/')) {
        // HTTP or Asset URL
        this.normalizedUrl = input;
      } else if (/^[A-Za-z0-9+/=]+$/.test(input.replace(/\s+/g, ''))) {
        // Bare Base64 string without data prefix
        const cleanB64 = input.replace(/\s+/g, '');
        // Default to PNG or check header
        this.normalizedUrl = `data:image/png;base64,${cleanB64}`;
      } else {
        this.parseError = 'Unrecognized image format. Please enter a valid data URI, Base64 string, or SVG code.';
      }

      if (this.normalizedUrl) {
        this.previewUrl = this.normalizedUrl;
        this.safePreviewUrl = this.sanitizer.bypassSecurityTrustUrl(this.normalizedUrl);
      }
    } catch (e: any) {
      this.parseError = `Failed to process image data: ${e?.message || e}`;
    }

    this.cdr.markForCheck();
  }

  apply(): void {
    if (!this.normalizedUrl) return;
    this.dialogRef.close({
      imageUrl: this.normalizedUrl,
      altText: this.altText.trim()
    });
  }
}
