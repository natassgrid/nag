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

import { Component, OnInit, ViewChild, signal, computed, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatMenuModule } from '@angular/material/menu';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { map } from 'rxjs/operators';
import { AdminService, UserAccountResponse } from '../services/admin.service';
import { NotificationService } from '../../../core/services/notification.service';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
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
    MatMenuModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    PaginatedTableComponent,
    PageHeaderComponent,
    RightDrawerComponent,
    StatusBadgeComponent
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.scss']
})
export class UserManagementComponent implements OnInit {

  @ViewChild('paginatedTable') paginatedTable!: PaginatedTableComponent<UserAccountResponse>;
  @ViewChild('statusTmpl', { static: true }) statusTmpl!: any;
  @ViewChild('mfaTmpl', { static: true }) mfaTmpl!: any;
  @ViewChild('rolesTmpl', { static: true }) rolesTmpl!: any;

  columns: ColumnDef<UserAccountResponse>[] = [];

  readonly availableRoles = ['ADMIN', 'CANDIDATE', 'AUTHOR', 'REVIEWER', 'PROCTOR'];

  // Signals for Local State Management
  readonly filters = signal<Record<string, any>>({});
  readonly createDrawerOpen = signal<boolean>(false);
  readonly editDrawerOpen = signal<boolean>(false);
  readonly roleDrawerOpen = signal<boolean>(false);
  readonly saving = signal<boolean>(false);

  // Form Signals
  readonly newFullName = signal<string>('');
  readonly newEmail = signal<string>('');
  readonly newPassword = signal<string>('');
  readonly newRoles = signal<string[]>(['CANDIDATE']);

  readonly editingUser = signal<UserAccountResponse | null>(null);
  readonly editStatus = signal<'ACTIVE' | 'DEACTIVATED'>('ACTIVE');
  readonly editMfaEnabled = signal<boolean>(false);

  readonly roleUser = signal<UserAccountResponse | null>(null);
  readonly selectedRole = signal<string>('CANDIDATE');
  readonly roleAction = signal<'ASSIGN' | 'REVOKE'>('ASSIGN');

  // Computed state
  readonly isCreateValid = computed(() => {
    return this.newFullName().trim().length > 0 &&
           this.newEmail().trim().length > 0 &&
           this.newPassword().trim().length > 0;
  });

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
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.columns = [
      { key: 'username', header: 'Username', sortable: true },
      {
        key: 'accountStatus',
        header: 'Status',
        type: 'custom',
        template: this.statusTmpl,
        sortable: true
      },
      { key: 'mfaEnabled', header: 'MFA', type: 'custom', template: this.mfaTmpl, sortable: true },
      { key: 'roles', header: 'Roles', type: 'custom', template: this.rolesTmpl },
      { key: 'createdAt', header: 'Created', type: 'date', sortable: true },
      { key: 'actions', header: 'Actions', type: 'actions' }
    ];
  }

  onFilterChange(filters: Record<string, any>): void {
    this.filters.set({ ...filters });
  }

  reload(): void {
    this.paginatedTable?.reload();
  }

  openCreateDrawer(): void {
    this.newFullName.set('');
    this.newEmail.set('');
    this.newPassword.set('');
    this.newRoles.set(['ROLE_EXAM_ADMIN']);
    this.createDrawerOpen.set(true);
  }

  saveCreateUser(): void {
    if (!this.isCreateValid()) return;
    this.saving.set(true);
    this.adminService.createUser({
      fullName: this.newFullName().trim(),
      email: this.newEmail().trim(),
      password: this.newPassword(),
      roles: this.newRoles()
    }).subscribe({
      next: () => {
        this.notificationService.showSuccess('User created successfully');
        this.createDrawerOpen.set(false);
        this.saving.set(false);
        this.reload();
      },
      error: () => {
        this.saving.set(false);
      }
    });
  }

  openEditDrawer(user: UserAccountResponse): void {
    this.editingUser.set(user);
    this.editStatus.set(user.accountStatus as any);
    this.editMfaEnabled.set(!!user.mfaEnabled);
    this.editDrawerOpen.set(true);
  }

  saveEditUser(): void {
    const user = this.editingUser();
    if (!user) return;
    this.saving.set(true);
    this.adminService.updateUser(user.id, {
      accountStatus: this.editStatus(),
      mfaEnabled: this.editMfaEnabled()
    }).subscribe({
      next: () => {
        this.notificationService.showSuccess('User updated successfully');
        this.saving.set(false);
        this.editDrawerOpen.set(false);
        this.reload();
      },
      error: () => {
        this.saving.set(false);
      }
    });
  }

  openRoleDrawer(user: UserAccountResponse): void {
    this.roleUser.set(user);
    this.selectedRole.set('CANDIDATE');
    this.roleAction.set('ASSIGN');
    this.roleDrawerOpen.set(true);
  }

  saveRoleChange(): void {
    const user = this.roleUser();
    if (!user) return;
    this.saving.set(true);
    this.adminService.assignRole(user.id, this.selectedRole(), this.roleAction()).subscribe({
      next: (res) => {
        this.notificationService.showSuccess(res.message || 'Role updated successfully');
        this.saving.set(false);
        this.roleDrawerOpen.set(false);
        this.reload();
      },
      error: () => {
        this.saving.set(false);
      }
    });
  }

  deactivateUser(user: UserAccountResponse): void {
    this.adminService.deactivateUser(user.id).subscribe({
      next: () => {
        this.notificationService.showSuccess('User deactivated successfully');
        this.reload();
      },
      error: () => {}
    });
  }
}
