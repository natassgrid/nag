/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

import {
  Component,
  OnInit,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormsModule,
  ReactiveFormsModule,
  FormBuilder,
} from '@angular/forms';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AssetService } from './asset.service';
import { AssetResponse, AssetMetadataUpdate } from './asset.model';

export interface AssetMetadataDialogData {
  asset: AssetResponse;
}

@Component({
  selector: 'app-asset-metadata-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './asset-metadata-dialog.component.html',
  styleUrls: ['./asset-metadata-dialog.component.scss'],
})
export class AssetMetadataDialogComponent implements OnInit {
  readonly dialogRef = inject(MatDialogRef<AssetMetadataDialogComponent>);
  readonly data = inject<AssetMetadataDialogData>(MAT_DIALOG_DATA);
  readonly assetService = inject(AssetService);
  private readonly fb = inject(FormBuilder);

  saving = false;
  errorMessage = '';

  metadataForm = this.fb.group({
    title: [''],
    description: [''],
    altText: [''],
    tags: [''],
    language: [''],
  });

  ngOnInit(): void {
    const a = this.data.asset;
    this.metadataForm.patchValue({
      title: a.title || '',
      description: a.description || '',
      altText: a.altText || '',
      tags: a.tags || '',
      language: a.language || '',
    });
  }

  save(): void {
    this.saving = true;
    this.errorMessage = '';

    const payload: AssetMetadataUpdate = {
      title: this.metadataForm.value.title || undefined,
      description: this.metadataForm.value.description || undefined,
      altText: this.metadataForm.value.altText || undefined,
      tags: this.metadataForm.value.tags || undefined,
      language: this.metadataForm.value.language || undefined,
    };

    this.assetService.updateMetadata(this.data.asset.id, payload).subscribe({
      next: (updated) => {
        this.saving = false;
        this.dialogRef.close(updated);
      },
      error: (err) => {
        this.saving = false;
        this.errorMessage =
          err?.error?.message ||
          err?.error?.detail ||
          'Failed to update metadata. Please try again.';
      },
    });
  }
}
