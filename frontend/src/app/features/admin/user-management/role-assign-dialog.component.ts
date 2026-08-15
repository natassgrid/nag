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
  templateUrl: './role-assign-dialog.component.html',
  styleUrls: ['./role-assign-dialog.component.scss']
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
