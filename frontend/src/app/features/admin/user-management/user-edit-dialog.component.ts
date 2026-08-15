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
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { UserAccountResponse } from '../services/admin.service';

export interface UserEditDialogData {
  user: UserAccountResponse;
}

export interface UserEditDialogResult {
  fullName?: string;
  accountStatus?: string;
  mfaEnabled?: boolean;
}

const ACCOUNT_STATUSES = ['ACTIVE', 'LOCKED', 'DEACTIVATED', 'PENDING_VERIFICATION'];

@Component({
  selector: 'app-user-edit-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatSlideToggleModule
  ],
  template: `
    <h2 mat-dialog-title>Edit User — {{ data.user.username }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="edit-form">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Username / Display Name</mat-label>
          <input matInput formControlName="fullName" />
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Account Status</mat-label>
          <mat-select formControlName="accountStatus">
            <mat-option *ngFor="let status of statuses" [value]="status">{{ status }}</mat-option>
          </mat-select>
        </mat-form-field>

        <mat-slide-toggle formControlName="mfaEnabled" class="toggle-field">
          MFA Enabled
        </mat-slide-toggle>
      </form>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-raised-button color="primary"
              (click)="submit()"
              aria-label="Save user changes">
        Save Changes
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .edit-form {
      display: flex;
      flex-direction: column;
      gap: 12px;
      min-width: 380px;
      padding-top: 8px;
    }
    .full-width { width: 100%; }
    .toggle-field { margin: 8px 0; }
  `]
})
export class UserEditDialogComponent {
  form: FormGroup;
  statuses = ACCOUNT_STATUSES;

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<UserEditDialogComponent, UserEditDialogResult>,
    @Inject(MAT_DIALOG_DATA) public data: UserEditDialogData
  ) {
    this.form = this.fb.group({
      fullName: [data.user.username],
      accountStatus: [data.user.accountStatus],
      mfaEnabled: [data.user.mfaEnabled]
    });
  }

  submit(): void {
    const value = this.form.value;
    const result: UserEditDialogResult = {};

    // Only send changed fields
    if (value.fullName !== this.data.user.username) {
      result.fullName = value.fullName;
    }
    if (value.accountStatus !== this.data.user.accountStatus) {
      result.accountStatus = value.accountStatus;
    }
    if (value.mfaEnabled !== this.data.user.mfaEnabled) {
      result.mfaEnabled = value.mfaEnabled;
    }

    this.dialogRef.close(result);
  }
}
