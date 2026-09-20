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
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { MatMenuModule } from '@angular/material/menu';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatRadioModule } from '@angular/material/radio';
import { map } from 'rxjs/operators';
import { AdminService, UserAccountResponse } from '../services/admin.service';
import { NotificationService } from '../../../core/services/notification.service';
import {
  PaginatedTableComponent,
  PaginatedDataFetcher,
  PaginatedResponse,
  FilterCategory
} from '../../../shared/components/paginated-table';
import { ColumnDef } from '../../../shared/components/paginated-table/pagination.model';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { RightDrawerComponent } from '../../../shared/components/right-drawer/right-drawer.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatChipsModule,
    MatMenuModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatRadioModule,
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
  @ViewChild('actionsTmpl', { static: true }) actionsTmpl!: any;

  columns: ColumnDef<UserAccountResponse>[] = [];

  readonly availableRoles = [
    'SUPER_ADMIN',
    'SECURITY_ADMIN',
    'QUESTION_AUTHOR',
    'REVIEWER',
    'SUBJECT_MATTER_EXPERT',
    'APPROVER',
    'EXAM_CONTROLLER',
    'TRANSLATOR',
    'EVALUATOR',
    'AUDITOR',
    'CANDIDATE'
  ];

  // Signals for Local State
  readonly inviteDrawerOpen = signal<boolean>(false);
  readonly createDrawerOpen = signal<boolean>(false);
  readonly editDrawerOpen = signal<boolean>(false);
  readonly roleDrawerOpen = signal<boolean>(false);
  readonly saving = signal<boolean>(false);

  // Filters signal
  readonly filters = signal<Record<string, any>>({});

  // Invite Form Signals
  readonly inviteFullName = signal<string>('');
  readonly inviteEmail = signal<string>('');
  readonly inviteSpecialization = signal<string>('');
  readonly inviteRoles = signal<string[]>(['QUESTION_AUTHOR']);

  // Create Form Signals
  readonly newFullName = signal<string>('');
  readonly newEmail = signal<string>('');
  readonly newPassword = signal<string>('');
  readonly newRoles = signal<string[]>(['QUESTION_AUTHOR']);

  // Edit Form Signals
  readonly editingUser = signal<UserAccountResponse | null>(null);
  readonly editStatus = signal<'ACTIVE' | 'DEACTIVATED'>('ACTIVE');
  readonly editMfaEnabled = signal<boolean>(false);

  // Role Form Signals
  readonly roleUser = signal<UserAccountResponse | null>(null);
  readonly selectedRole = signal<string>('CANDIDATE');
  readonly roleAction = signal<'ASSIGN' | 'REVOKE'>('ASSIGN');

  // Validation
  readonly isInviteValid = computed(() => {
    return this.inviteFullName().trim().length > 0 &&
      this.inviteEmail().trim().includes('@') &&
      this.inviteRoles().length > 0;
  });

  readonly isCreateValid = computed(() => {
    return this.newFullName().trim().length > 0 &&
      this.newEmail().trim().includes('@') &&
      this.newPassword().length >= 8 &&
      this.newRoles().length > 0;
  });

  filterCategories: FilterCategory[] = [
    {
      key: 'status',
      label: 'Status',
      expanded: true,
      options: [
        { label: 'Active', value: 'ACTIVE' },
        { label: 'Deactivated', value: 'DEACTIVATED' },
        { label: 'Pending Invite', value: 'PENDING_INVITE' }
      ]
    },
    {
      key: 'role',
      label: 'Role',
      expanded: true,
      options: this.availableRoles.map(r => ({ label: r, value: r }))
    }
  ];

  fetcher: PaginatedDataFetcher<UserAccountResponse> = (req) => {
    return this.adminService.getUsers().pipe(
      map((users: UserAccountResponse[]) => {
        let filtered: UserAccountResponse[] = users;
        if (req.search) {
          const query = req.search.toLowerCase();
          filtered = users.filter(u =>
            (u.username && u.username.toLowerCase().includes(query)) ||
            (u.email && u.email.toLowerCase().includes(query))
          );
        }

        // Apply filters
        const activeFilters = req.filters || this.filters();
        if (activeFilters['status']?.length) {
          filtered = filtered.filter(u => activeFilters['status'].includes(u.accountStatus));
        }
        if (activeFilters['role']?.length) {
          filtered = filtered.filter(u =>
            u.roles && u.roles.some(r => activeFilters['role'].includes(r))
          );
        }

        // Apply sorting
        if (req.sort) {
          const sortKey = req.sort as keyof UserAccountResponse;
          const order = req.order === 'desc' ? -1 : 1;
          filtered = [...filtered].sort((a, b) => {
            const valA = (a[sortKey] ?? '').toString().toLowerCase();
            const valB = (b[sortKey] ?? '').toString().toLowerCase();
            if (valA < valB) return -1 * order;
            if (valA > valB) return 1 * order;
            return 0;
          });
        }

        // Apply pagination
        const startIndex = req.page * req.size;
        const paged = filtered.slice(startIndex, startIndex + req.size);

        return {
          content: paged,
          totalElements: filtered.length,
          totalPages: Math.ceil(filtered.length / req.size),
          size: req.size,
          number: req.page
        } as PaginatedResponse<UserAccountResponse>;
      })
    );
  };

  constructor(
    private adminService: AdminService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.columns = [
      { key: 'username', header: 'Name / Username', sortable: true },
      { key: 'email', header: 'Email Address', sortable: true },
      { key: 'accountStatus', header: 'Status', type: 'custom', template: this.statusTmpl, sortable: true },
      { key: 'roles', header: 'Assigned Roles', type: 'custom', template: this.rolesTmpl },
      { key: 'mfaEnabled', header: 'MFA', type: 'custom', template: this.mfaTmpl },
      { key: 'actions', header: '', type: 'custom', template: this.actionsTmpl }
    ];
  }

  onFilterChange(filters: Record<string, any>): void {
    this.filters.set({ ...filters });
  }

  reload(): void {
    this.paginatedTable?.reload();
  }

  openInviteDrawer(): void {
    this.inviteFullName.set('');
    this.inviteEmail.set('');
    this.inviteSpecialization.set('');
    this.inviteRoles.set(['QUESTION_AUTHOR']);
    this.inviteDrawerOpen.set(true);
  }

  sendAdminInvite(): void {
    this.submitInvite();
  }

  submitInvite(): void {
    if (!this.isInviteValid()) return;
    this.saving.set(true);
    this.adminService.inviteAdmin({
      fullName: this.inviteFullName().trim(),
      email: this.inviteEmail().trim(),
      specialization: this.inviteSpecialization().trim() || undefined,
      roles: this.inviteRoles()
    }).subscribe({
      next: () => {
        this.notificationService.showSuccess('User invited successfully.');
        this.inviteDrawerOpen.set(false);
        this.saving.set(false);
        this.reload();
      },
      error: (err: any) => {
        this.notificationService.showError(err?.error?.message || 'Failed to send invite.');
        this.saving.set(false);
      }
    });
  }

  openCreateDrawer(): void {
    this.newFullName.set('');
    this.newEmail.set('');
    this.newPassword.set('');
    this.newRoles.set(['QUESTION_AUTHOR']);
    this.createDrawerOpen.set(true);
  }

  saveCreateUser(): void {
    this.submitCreate();
  }

  submitCreate(): void {
    if (!this.isCreateValid()) return;
    this.saving.set(true);
    this.adminService.createUser({
      fullName: this.newFullName().trim(),
      email: this.newEmail().trim(),
      password: this.newPassword(),
      roles: this.newRoles()
    }).subscribe({
      next: () => {
        this.notificationService.showSuccess('User created successfully.');
        this.createDrawerOpen.set(false);
        this.saving.set(false);
        this.reload();
      },
      error: (err: any) => {
        this.notificationService.showError(err?.error?.message || 'Failed to create user.');
        this.saving.set(false);
      }
    });
  }

  openEditDrawer(user: UserAccountResponse): void {
    this.editingUser.set(user);
    this.editStatus.set(user.accountStatus === 'DEACTIVATED' ? 'DEACTIVATED' : 'ACTIVE');
    this.editMfaEnabled.set(!!user.mfaEnabled);
    this.editDrawerOpen.set(true);
  }

  saveEditUser(): void {
    this.submitEdit();
  }

  submitEdit(): void {
    const user = this.editingUser();
    if (!user) return;
    this.saving.set(true);
    this.adminService.updateUser(user.id, {
      accountStatus: this.editStatus(),
      mfaEnabled: this.editMfaEnabled()
    }).subscribe({
      next: () => {
        this.notificationService.showSuccess('User updated successfully.');
        this.editDrawerOpen.set(false);
        this.saving.set(false);
        this.reload();
      },
      error: (err: any) => {
        this.notificationService.showError(err?.error?.message || 'Failed to update user.');
        this.saving.set(false);
      }
    });
  }

  deactivateUser(user: UserAccountResponse): void {
    this.adminService.deactivateUser(user.id).subscribe({
      next: () => {
        this.notificationService.showSuccess(`User ${user.username || user.email} deactivated.`);
        this.reload();
      },
      error: (err: any) => {
        this.notificationService.showError(err?.error?.message || 'Failed to deactivate user.');
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
    this.submitRoleAction();
  }

  submitRoleAction(): void {
    const user = this.roleUser();
    if (!user) return;
    this.saving.set(true);
    this.adminService.assignRole(user.id, this.selectedRole(), this.roleAction()).subscribe({
      next: () => {
        this.notificationService.showSuccess(`Role ${this.selectedRole()} ${this.roleAction().toLowerCase()}ed successfully.`);
        this.roleDrawerOpen.set(false);
        this.saving.set(false);
        this.reload();
      },
      error: (err: any) => {
        this.notificationService.showError(err?.error?.message || 'Failed to update user roles.');
        this.saving.set(false);
      }
    });
  }
}
