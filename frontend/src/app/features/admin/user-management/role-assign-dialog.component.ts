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
import { FormsModule } from '@angular/forms';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';

export interface RoleAssignDialogData {
  username: string;
  userId: string;
  currentRoles: string[];
}

export interface RoleAssignDialogResult {
  role: string;
  action: 'ASSIGN' | 'REVOKE';
}

const ALL_ROLES = [
  'SUPER_ADMIN',
  'SECURITY_ADMIN',
  'QUESTION_AUTHOR',
  'REVIEWER',
  'APPROVER',
  'EXAM_CONTROLLER',
  'TRANSLATOR',
  'EVALUATOR',
  'AUDITOR',
  'CANDIDATE'
];

@Component({
  selector: 'app-role-assign-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatSelectModule,
    MatButtonModule
  ],
  template: `
    <h2 mat-dialog-title>Manage Roles — {{ data.username }}</h2>
    <mat-dialog-content>
      <p>Current roles: {{ data.currentRoles.length ? data.currentRoles.join(', ') : 'None' }}</p>

      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Select Role</mat-label>
        <mat-select [(value)]="selectedRole">
          <mat-option *ngFor="let role of allRoles" [value]="role">{{ role }}</mat-option>
        </mat-select>
      </mat-form-field>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-raised-button color="primary"
              [disabled]="!selectedRole"
              (click)="submit('ASSIGN')"
              aria-label="Assign selected role">
        Assign
      </button>
      <button mat-raised-button color="warn"
              [disabled]="!selectedRole || !isCurrentRole(selectedRole)"
              (click)="submit('REVOKE')"
              aria-label="Revoke selected role">
        Revoke
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .full-width { width: 100%; }
    mat-dialog-content { min-width: 320px; }
  `]
})
export class RoleAssignDialogComponent {
  allRoles = ALL_ROLES;
  selectedRole: string | null = null;

  constructor(
    public dialogRef: MatDialogRef<RoleAssignDialogComponent, RoleAssignDialogResult>,
    @Inject(MAT_DIALOG_DATA) public data: RoleAssignDialogData
  ) {}

  isCurrentRole(role: string | null): boolean {
    return !!role && this.data.currentRoles.includes(role);
  }

  submit(action: 'ASSIGN' | 'REVOKE'): void {
    if (this.selectedRole) {
      this.dialogRef.close({ role: this.selectedRole, action });
    }
  }
}
