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
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { map } from 'rxjs/operators';
import {
  PaginatedTableComponent,
  PaginatedDataFetcher,
  PaginatedResponse,
  ColumnDef,
  FilterCategory
} from '../../../shared/components/paginated-table';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { RightDrawerComponent } from '../../../shared/components/right-drawer/right-drawer.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { NotificationService } from '../../../core/services/notification.service';
import {
  AdminService,
  RoleDefinitionResponse,
  CreateRoleRequest,
  UpdateRoleRequest
} from '../services/admin.service';

@Component({
  selector: 'app-role-management',
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
    MatSlideToggleModule,
    MatDialogModule,
    PaginatedTableComponent,
    PageHeaderComponent,
    RightDrawerComponent,
    StatusBadgeComponent
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './role-management.component.html',
  styleUrls: ['./role-management.component.scss']
})
export class RoleManagementComponent implements OnInit {

  @ViewChild('paginatedTable') paginatedTable!: PaginatedTableComponent<RoleDefinitionResponse>;
  @ViewChild('statusTmpl', { static: true }) statusTmpl!: any;
  @ViewChild('permsTmpl', { static: true }) permsTmpl!: any;

  columns: ColumnDef<RoleDefinitionResponse>[] = [];

  // Signals for Local State
  readonly filters = signal<Record<string, any>>({});
  readonly createDrawerOpen = signal<boolean>(false);
  readonly editDrawerOpen = signal<boolean>(false);
  readonly saving = signal<boolean>(false);

  // Form Signals
  readonly newName = signal<string>('');
  readonly newCode = signal<string>('');
  readonly newDescription = signal<string>('');

  readonly editingRole = signal<RoleDefinitionResponse | null>(null);
  readonly editName = signal<string>('');
  readonly editDescription = signal<string>('');
  readonly editActive = signal<boolean>(true);

  readonly isCreateValid = computed(() => {
    return this.newName().trim().length > 0 && this.newCode().trim().length > 0;
  });

  filterCategories: FilterCategory[] = [
    {
      key: 'active',
      label: 'Status',
      expanded: true,
      options: [
        { label: 'Active', value: 'true' },
        { label: 'Inactive', value: 'false' }
      ]
    },
    {
      key: 'systemRole',
      label: 'Type',
      expanded: true,
      options: [
        { label: 'System Role', value: 'true' },
        { label: 'Custom Role', value: 'false' }
      ]
    }
  ];

  fetcher: PaginatedDataFetcher<RoleDefinitionResponse> = (req) => {
    return this.adminService.getRoleDefinitions(req.page, req.size, req.search || '').pipe(
      map(page => ({
        content: page.content,
        totalElements: page.totalElements,
        totalPages: page.totalPages,
        size: page.size,
        number: page.number
      } as PaginatedResponse<RoleDefinitionResponse>))
    );
  };

  constructor(
    private adminService: AdminService,
    private notificationService: NotificationService,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.columns = [
      { key: 'name', header: 'Role Name', sortable: true },
      { key: 'code', header: 'Code', sortable: true },
      { key: 'description', header: 'Description', cell: (r) => r.description || '—' },
      { key: 'active', header: 'Status', type: 'custom', template: this.statusTmpl },
      { key: 'systemRole', header: 'Type', cell: (r) => r.systemRole ? 'System' : 'Custom', type: 'badge',
        chipClass: (val) => val === 'System' ? 'system-type' : 'custom-type' },
      { key: 'permissions', header: 'Permissions', type: 'custom', template: this.permsTmpl },
      { key: 'actions', header: '', type: 'actions' }
    ];
  }

  onFilterChange(filters: Record<string, any>): void {
    this.filters.set({ ...filters });
  }

  // ── Create ──

  openCreateDrawer(): void {
    this.newName.set('');
    this.newCode.set('');
    this.newDescription.set('');
    this.createDrawerOpen.set(true);
  }

  saveNewRole(): void {
    if (!this.isCreateValid()) return;
    this.saving.set(true);
    const request: CreateRoleRequest = {
      name: this.newName().trim(),
      code: this.newCode().trim().toUpperCase(),
      description: this.newDescription().trim() || undefined
    };

    this.adminService.createRoleDefinition(request).subscribe({
      next: () => {
        this.notificationService.showSuccess('Role created successfully');
        this.createDrawerOpen.set(false);
        this.saving.set(false);
        this.paginatedTable.reload();
      },
      error: () => {
        this.saving.set(false);
      }
    });
  }

  // ── Edit ──

  openEditDrawer(role: RoleDefinitionResponse): void {
    this.editingRole.set(role);
    this.editName.set(role.name);
    this.editDescription.set(role.description || '');
    this.editActive.set(role.active);
    this.editDrawerOpen.set(true);
  }

  saveEditRole(): void {
    const role = this.editingRole();
    if (!role) return;
    this.saving.set(true);

    const request: UpdateRoleRequest = {
      name: this.editName().trim(),
      description: this.editDescription().trim(),
      active: this.editActive()
    };

    this.adminService.updateRoleDefinition(role.id, request).subscribe({
      next: () => {
        this.notificationService.showSuccess('Role updated successfully');
        this.editDrawerOpen.set(false);
        this.saving.set(false);
        this.paginatedTable.reload();
      },
      error: () => {
        this.saving.set(false);
      }
    });
  }

  // ── Toggle Active ──

  toggleActive(role: RoleDefinitionResponse): void {
    const newStatus = !role.active;
    this.adminService.updateRoleDefinition(role.id, { active: newStatus }).subscribe({
      next: () => {
        this.notificationService.showSuccess(`Role ${newStatus ? 'activated' : 'deactivated'}`);
        this.paginatedTable.reload();
      },
      error: () => {}
    });
  }

  // ── Delete ──

  deleteRole(role: RoleDefinitionResponse): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Role',
        message: `Are you sure you want to delete the role "${role.name}" (${role.code})? This action cannot be undone.`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
        color: 'warn',
        icon: 'warning'
      } as ConfirmDialogData
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (!confirmed) return;

      this.adminService.deleteRoleDefinition(role.id).subscribe({
        next: () => {
          this.notificationService.showSuccess('Role deleted successfully');
          this.paginatedTable.reload();
        },
        error: () => {}
      });
    });
  }
}
