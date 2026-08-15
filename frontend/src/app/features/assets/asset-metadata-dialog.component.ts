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

import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AssetService } from './asset.service';
import { AssetResponse } from './asset.model';
import { RightDrawerComponent } from '../../shared/components/right-drawer/right-drawer.component';

@Component({
  selector: 'app-asset-metadata-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatProgressSpinnerModule, RightDrawerComponent],
  templateUrl: './asset-metadata-dialog.component.html',
  styleUrls: ['./asset-metadata-dialog.component.scss']
})
export class AssetMetadataDialogComponent implements OnInit, OnChanges {
  @Input() isOpen = false;
  @Input() asset?: AssetResponse;
  @Output() close = new EventEmitter<AssetResponse | null>();

  form!: FormGroup;
  saving = false;
  error = '';

  constructor(
    private fb: FormBuilder,
    private assetService: AssetService
  ) {
    this.initForm();
  }

  ngOnInit(): void {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen'] && this.isOpen) {
      this.initForm();
    }
  }

  initForm(): void {
    const a = this.asset;
    this.form = this.fb.group({
      title: [a?.title || ''],
      description: [a?.description || ''],
      altText: [a?.altText || ''],
      tags: [a?.tags || ''],
      language: [a?.language || '']
    });
  }

  cancel(): void {
    this.close.emit(null);
  }

  save(): void {
    if (!this.asset) return;
    this.saving = true;
    this.error = '';
    this.assetService.updateMetadata(this.asset.id, this.form.value).subscribe({
      next: (updated) => { this.saving = false; this.close.emit(updated); },
      error: (err) => { this.saving = false; this.error = err?.error?.message || 'Failed to update'; }
    });
  }
}
