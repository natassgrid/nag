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

import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export interface ConfirmSubmitData {
  totalQuestions: number;
  answeredCount: number;
}

@Component({
  selector: 'app-confirm-submit-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <h2 mat-dialog-title>Confirm Submission</h2>
    <mat-dialog-content>
      <p>Are you sure you want to submit the exam?</p>
      <p class="summary">
        You have answered <strong>{{ data.answeredCount }}</strong> out of
        <strong>{{ data.totalQuestions }}</strong> questions.
      </p>
      <p *ngIf="data.answeredCount < data.totalQuestions" class="warning-text">
        <mat-icon class="warning-icon">warning</mat-icon>
        {{ data.totalQuestions - data.answeredCount }} question(s) are unanswered.
      </p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button (click)="onCancel()" aria-label="Cancel submission">
        Cancel
      </button>
      <button mat-raised-button color="warn" (click)="onConfirm()" aria-label="Confirm submission">
        Submit Exam
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .summary {
      margin: 12px 0;
      font-size: 0.95rem;
    }
    .warning-text {
      display: flex;
      align-items: center;
      gap: 6px;
      color: #e65100;
      font-size: 0.9rem;
    }
    .warning-icon {
      font-size: 18px;
      width: 18px;
      height: 18px;
      color: #e65100;
    }
  `]
})
export class ConfirmSubmitDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ConfirmSubmitDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ConfirmSubmitData
  ) {}

  onCancel(): void {
    this.dialogRef.close(false);
  }

  onConfirm(): void {
    this.dialogRef.close(true);
  }
}
