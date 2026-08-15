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

import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { ScheduleResponse, ScheduleTransitionRequest } from './scheduling.service';

@Component({
  selector: 'app-transition-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule
  ],
  templateUrl: './transition-dialog.component.html',
  styleUrls: ['./transition-dialog.component.scss']
})
export class TransitionDialogComponent implements OnInit {
  form!: FormGroup;
  availableOptions: string[] = [];

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<TransitionDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ScheduleResponse
  ) {}

  ngOnInit(): void {
    this.availableOptions = this.getValidTransitions(this.data.status);

    this.form = this.fb.group({
      targetStatus: [this.availableOptions[0] || '', Validators.required],
      comment: ['']
    });
  }

  private getValidTransitions(currentStatus: string): string[] {
    switch (currentStatus) {
      case 'DRAFT':
        return ['SCHEDULER_REVIEW', 'CANCELLED'];
      case 'SCHEDULER_REVIEW':
        return ['CONTROLLER_APPROVED', 'CANCELLED'];
      case 'CONTROLLER_APPROVED':
        return ['SECURITY_REVIEW', 'CANCELLED'];
      case 'SECURITY_REVIEW':
        return ['CHAIRMAN_APPROVED', 'CANCELLED'];
      case 'CHAIRMAN_APPROVED':
        return ['PUBLISHED', 'CANCELLED'];
      case 'PUBLISHED':
        return ['CANCELLED'];
      default:
        return [];
    }
  }

  save(): void {
    if (this.form.valid) {
      const val = this.form.value;
      const result: ScheduleTransitionRequest = {
        targetStatus: val.targetStatus,
        comment: val.comment || undefined
      };
      this.dialogRef.close(result);
    }
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
