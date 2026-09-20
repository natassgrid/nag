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
 * GNU sound Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

import { Component, OnInit, ViewChild, ChangeDetectionStrategy, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatRadioModule } from '@angular/material/radio';
import { map } from 'rxjs/operators';
import { AdminService, UserAccountResponse } from '../admin.service';
import { NotificationService } from '../../../core/services/notification.service';
import {
  PaginatedTableComponent,
  PaginatedDataFetcher,
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

  // Signals for Local State Management
  readonly filters = signal<Record<string, any>>({});
  readonly inviteDrawerOpen = signal<boolean>(false);
  readonly createDrawerOpen = signal<boolean>(false);
  readonly editDrawerOpen = signal<boolean>(false);
  readonly roleDrawerOpen = signal<boolean>(false);
  readonly saving = signal<boolean>(false);

  // Invite Form Signals
  readonly inviteEmail = signal<string>('');
  readonly inviteFullName = signal<string>('');
  readonly inviteSpecialization = signal<string>('');
  readonly inviteRoles = signal<string[]>(['QUESTION_AUTHOR']);

  // Create Form Signals
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
  readonly isInviteValid = computed(() => {
    return this.inviteFullName().trim().length > 0 &&
           this.inviteEmail().trim().length > 0 &&
           this.inviteRoles().length > 0;
  });

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
        { label: 'Pending Setup', value: 'PENDING_SETUP' },
        { label: 'Pending Verification', value: 'PENDING_VERIFICATION' },
        { label: 'Deactivated', value: 'DEACTIVATED' }
      ]
    },
    {
      key: 'roles',
      label: 'Role',
      expanded: false,
      options: [
        { label: 'Super Admin', value: 'SUPER_ADMIN' },
        { label: 'Security Admin', value: 'SECURITY_ADMIN' },
        { label: 'Question Author', value: 'QUESTION_AUTHOR' },
        { label: 'Reviewer', value: 'REVIEWER' },
        { label: 'Exam Controller', value: 'EXAM_CONTROLLER' },
        { label: 'Candidate', value: 'CANDIDATE' }
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
      { key: 'username', header: 'Username / Email', sortable: true },
      {
        key: 'accountStatus',
        header: 'Status',
        type: 'custom',
        template: this.statusTmpl,
        sortable: true
      },
      { key: 'mfaEnabled', header: '2FA', type: 'custom', template: this.mfaTmpl, sortable: true },
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

  openInviteDrawer(): void {
    this.inviteEmail.set('');
    this.inviteFullName.set('');
    this.inviteSpecialization.set('');
    this.inviteRoles.set(['QUESTION_AUTHOR']);
    this.inviteDrawerOpen.set(true);
  }

  submitInvite(): void {
    if (!this.isInviteValid()) return;
    this.saving.set(true);
    this.adminService.inviteUser({
      email: this.inviteEmail().trim(),
      fullName: this.inviteFullName().trim(),
      specialization: this.inviteSpecialization().trim() || undefined,
      roles: this.inviteRoles()
    }).subscribe({
      next: () => {
        this.notificationService.success('User invited successfully.');
        this.inviteDrawerOpen.set(false);
        this.saving.set(false);
        this.reload();
      },
      error: (err: any) => {
        this.notificationService.error(err?.error?.message || 'Failed to send invite.');
        this.saving.set(false);
      }
    });
  }

  openCreateDrawer(): void {
    this.newFullName.set('');
    this.newEmail.set('');
    this.newPassword.set('');
    this.newRoles.set(['CANDIDATE']);
    this.createDrawerOpen.set(true);
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
        this.notificationService.success('User created successfully.');
        this.createDrawerOpen.set(false);
        this.saving.set(false);
        this.reload();
      },
      error: (err: any) => {
        this.notificationService.error(err?.error?.message || 'Failed to create user.');
        this.saving.set(false);
      }
    });
  }

  openEditDrawer(user: UserAccountResponse): void {
    this.editingUser.set(user);
    this.editStatus.set(user.accountStatus === 'DEACTIVATED' ? 'DEACTIVATED' : 'ACTIVE');
    this.editMfaEnabled.set(user.mfaEnabled ?? false);
    this.editDrawerOpen.set(true);
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
        this.notificationService.success('User updated successfully.');
        this.editDrawerOpen.set(false);
        this.saving.set(false);
        this.reload();
      },
      error: (err: any) => {
        this.notificationService.error(err?.error?.message || 'Failed to update user.');
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

  submitRoleAction(): void {
    const user = this.roleUser();
    if (!user) return;
    this.saving.set(true);
    const obs$ = this.roleAction() === 'ASSIGN'
      ? this.adminService.assignRole(user.id, this.selectedRole())
      : this.adminService.revokeRole(user.id, this.selectedRole());

    obs$.subscribe({
      next: () => {
        this.notificationService.success(`Role ${this.selectedRole()} ${this.roleAction().toLowerCase()}ed successfully.`);
        this.roleDrawerOpen.set(false);
        this.saving.set(false);
        this.reload();
      },
      error: (err: any) => {
        this.notificationService.error(err?.error?.message || 'Failed to update user roles.');
        this.saving.set(false);
      }
    });
  }
}
