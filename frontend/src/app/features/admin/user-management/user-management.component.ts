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

import { Component, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatMenuModule } from '@angular/material/menu';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { map } from 'rxjs/operators';
import { AdminService, UserAccountResponse } from '../services/admin.service';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import {
  PaginatedTableComponent,
  ColumnDef,
  PaginatedDataFetcher,
  FilterCategory
} from '../../../shared/components/paginated-table';
import { RightDrawerComponent } from '../../../shared/components/right-drawer/right-drawer.component';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatSnackBarModule,
    MatMenuModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    PaginatedTableComponent,
    PageHeaderComponent,
    RightDrawerComponent
  ],
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.scss']
})
export class UserManagementComponent {

  @ViewChild('paginatedTable') paginatedTable!: PaginatedTableComponent<UserAccountResponse>;
  @ViewChild('mfaTmpl', { static: true }) mfaTmpl!: any;
  @ViewChild('rolesTmpl', { static: true }) rolesTmpl!: any;

  columns: ColumnDef<UserAccountResponse>[] = [];
  filters: Record<string, any> = {};

  availableRoles = ['ADMIN', 'CANDIDATE', 'AUTHOR', 'REVIEWER', 'PROCTOR'];

  // Drawer States
  createDrawerOpen = false;
  editDrawerOpen = false;
  roleDrawerOpen = false;
  saving = false;

  // Create Form State
  newFullName = '';
  newEmail = '';
  newPassword = '';
  newRoles: string[] = ['CANDIDATE'];

  // Edit Form State
  editingUser: UserAccountResponse | null = null;
  editStatus: 'ACTIVE' | 'DEACTIVATED' = 'ACTIVE';
  editMfaEnabled = false;

  // Role Form State
  roleUser: UserAccountResponse | null = null;
  selectedRole = 'CANDIDATE';
  roleAction: 'ASSIGN' | 'REVOKE' = 'ASSIGN';

  filterCategories: FilterCategory[] = [
    {
      key: 'accountStatus',
      label: 'Status',
      expanded: true,
      options: [
        { label: 'Active', value: 'ACTIVE' },
        { label: 'Deactivated', value: 'DEACTIVATED' }
      ]
    },
    {
      key: 'roles',
      label: 'Role',
      expanded: false,
      options: [
        { label: 'Admin', value: 'ADMIN' },
        { label: 'Candidate', value: 'CANDIDATE' },
        { label: 'Author', value: 'AUTHOR' },
        { label: 'Reviewer', value: 'REVIEWER' },
        { label: 'Proctor', value: 'PROCTOR' }
      ]
    },
    {
      key: 'mfaEnabled',
      label: 'MFA Status',
      expanded: false,
      options: [
        { label: 'Enabled', value: true },
        { label: 'Disabled', value: false }
      ]
    }
  ];

  fetcher: PaginatedDataFetcher<UserAccountResponse> = (req) => {
    return this.adminService.getUsers().pipe(
      map(users => {
        let filtered = users;
        if (req.search) {
          const query = req.search.toLowerCase();
          filtered = users.filter(u =>
            u.username.toLowerCase().includes(query) ||
            u.accountStatus.toLowerCase().includes(query) ||
            (u.roles && u.roles.some(r => r.toLowerCase().includes(query)))
          );
        }
        if (req.filters) {
          if (req.filters['accountStatus']) {
            const statusVals = Array.isArray(req.filters['accountStatus']) ? req.filters['accountStatus'] : [req.filters['accountStatus']];
            filtered = filtered.filter(u => statusVals.includes(u.accountStatus));
          }
          if (req.filters['roles']) {
            const roleVals = Array.isArray(req.filters['roles']) ? req.filters['roles'] : [req.filters['roles']];
            filtered = filtered.filter(u => u.roles && u.roles.some(r => roleVals.includes(r)));
          }
          if (req.filters['mfaEnabled'] !== undefined) {
            const mfaVal = req.filters['mfaEnabled'];
            filtered = filtered.filter(u => u.mfaEnabled === mfaVal);
          }
        }
        const start = req.page * req.size;
        const paged = filtered.slice(start, start + req.size);
        return {
          content: paged,
          totalElements: filtered.length,
          totalPages: Math.ceil(filtered.length / req.size),
          size: req.size,
          number: req.page
        };
      })
    );
  };

  constructor(
    private adminService: AdminService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.columns = [
      { key: 'username', header: 'Username', sortable: true },
      {
        key: 'accountStatus',
        header: 'Status',
        type: 'chip',
        chipClass: (val) => val === 'ACTIVE' ? 'active-status' : 'inactive-status',
        sortable: true
      },
      { key: 'mfaEnabled', header: 'MFA', type: 'custom', template: this.mfaTmpl, sortable: true },
      { key: 'roles', header: 'Roles', type: 'custom', template: this.rolesTmpl },
      { key: 'createdAt', header: 'Created', type: 'date', sortable: true },
      { key: 'actions', header: 'Actions', type: 'actions' }
    ];
  }

  onFilterChange(filters: Record<string, any>): void {
    this.filters = { ...filters };
  }

  reload(): void {
    this.paginatedTable?.reload();
  }

  openCreateDrawer(): void {
    this.newFullName = '';
    this.newEmail = '';
    this.newPassword = '';
    this.newRoles = ['ROLE_EXAM_ADMIN'];
    this.createDrawerOpen = true;
  }

  saveCreateUser(): void {
    if (!this.newFullName.trim() || !this.newEmail.trim() || !this.newPassword.trim()) return;
    this.saving = true;
    this.adminService.createUser({
      fullName: this.newFullName.trim(),
      email: this.newEmail.trim(),
      password: this.newPassword,
      roles: this.newRoles
    }).subscribe({
      next: () => {
        this.snackBar.open('User created successfully', 'Close', { duration: 3000 });
        this.createDrawerOpen = false;
        this.saving = false;
        this.reload();
      },
      error: (err) => {
        this.snackBar.open(err.error?.message || 'Failed to create user', 'Close', { duration: 3000 });
        this.saving = false;
      }
    });
  }

  openEditDrawer(user: UserAccountResponse): void {
    this.editingUser = user;
    this.editStatus = user.accountStatus as any;
    this.editMfaEnabled = !!user.mfaEnabled;
    this.editDrawerOpen = true;
  }

  saveEditUser(): void {
    if (!this.editingUser) return;
    this.saving = true;
    this.adminService.updateUser(this.editingUser.id, {
      accountStatus: this.editStatus,
      mfaEnabled: this.editMfaEnabled
    }).subscribe({
      next: () => {
        this.snackBar.open('User updated successfully', 'OK', { duration: 3000 });
        this.saving = false;
        this.editDrawerOpen = false;
        this.reload();
      },
      error: (err) => {
        this.saving = false;
        const msg = err.error?.detail || err.error?.message || 'Failed to update user';
        this.snackBar.open(msg, 'Dismiss', { duration: 5000 });
      }
    });
  }

  openRoleDrawer(user: UserAccountResponse): void {
    this.roleUser = user;
    this.selectedRole = 'CANDIDATE';
    this.roleAction = 'ASSIGN';
    this.roleDrawerOpen = true;
  }

  saveRoleChange(): void {
    if (!this.roleUser || !this.selectedRole) return;
    this.saving = true;
    this.adminService.assignRole(this.roleUser.id, this.selectedRole, this.roleAction).subscribe({
      next: (res) => {
        this.snackBar.open(res.message, 'OK', { duration: 3000 });
        this.saving = false;
        this.roleDrawerOpen = false;
        this.reload();
      },
      error: () => {
        this.saving = false;
        this.snackBar.open('Role operation failed', 'Dismiss', { duration: 5000 });
      }
    });
  }

  deactivateUser(user: UserAccountResponse): void {
    this.adminService.deactivateUser(user.id).subscribe({
      next: () => {
        this.snackBar.open('User deactivated', 'OK', { duration: 3000 });
        this.reload();
      },
      error: () => {
        this.snackBar.open('Failed to deactivate user', 'Dismiss', { duration: 5000 });
      }
    });
  }
}
