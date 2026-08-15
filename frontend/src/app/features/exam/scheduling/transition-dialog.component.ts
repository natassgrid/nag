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
  template: `
    <h2 mat-dialog-title>Transition Schedule Status</h2>
    <mat-dialog-content>
      <div class="current-status-info">
        <strong>Current Status:</strong> <span class="chip">{{ data.status }}</span>
      </div>
      <form [formGroup]="form" class="form-container">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Target Status</mat-label>
          <mat-select formControlName="targetStatus" required>
            <mat-option *ngFor="let option of availableOptions" [value]="option">
              {{ option }}
            </mat-option>
          </mat-select>
          <mat-error *ngIf="form.get('targetStatus')?.hasError('required')">Target status is required</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Comment (Optional)</mat-label>
          <textarea matInput formControlName="comment" rows="3" placeholder="Add approval or review comment..."></textarea>
        </mat-form-field>
      </form>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button (click)="cancel()">Cancel</button>
      <button mat-raised-button color="primary" [disabled]="form.invalid" (click)="save()">Submit Transition</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .current-status-info {
      margin-bottom: 16px;
      font-size: 14px;
    }
    .chip {
      background: #e0e0e0;
      padding: 4px 8px;
      border-radius: 4px;
      font-weight: 600;
    }
    .form-container {
      display: flex;
      flex-direction: column;
      gap: 12px;
      min-width: 360px;
    }
    .full-width {
      width: 100%;
    }
  `]
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
