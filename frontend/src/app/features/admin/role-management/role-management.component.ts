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
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Observable, map } from 'rxjs';
import {
  PaginatedTableComponent,
  PaginatedDataFetcher,
  PaginatedResponse,
  ColumnDef,
  FilterCategory
} from '../../../shared/components/paginated-table';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { RightDrawerComponent } from '../../../shared/components/right-drawer/right-drawer.component';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
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
    MatSnackBarModule,
    MatMenuModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatDialogModule,
    PaginatedTableComponent,
    PageHeaderComponent,
    RightDrawerComponent
  ],
  templateUrl: './role-management.component.html',
  styleUrls: ['./role-management.component.scss']
})
export class RoleManagementComponent {

  @ViewChild('paginatedTable') paginatedTable!: PaginatedTableComponent<RoleDefinitionResponse>;
  @ViewChild('statusTmpl', { static: true }) statusTmpl!: any;
  @ViewChild('permsTmpl', { static: true }) permsTmpl!: any;

  columns: ColumnDef<RoleDefinitionResponse>[] = [];
  filters: Record<string, any> = {};

  // Drawer States
  createDrawerOpen = false;
  editDrawerOpen = false;
  saving = false;

  // Create Form State
  newName = '';
  newCode = '';
  newDescription = '';

  // Edit Form State
  editingRole: RoleDefinitionResponse | null = null;
  editName = '';
  editDescription = '';
  editActive = true;

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
    private snackBar: MatSnackBar,
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
    this.filters = filters;
  }

  // ── Create ──

  openCreateDrawer(): void {
    this.newName = '';
    this.newCode = '';
    this.newDescription = '';
    this.createDrawerOpen = true;
  }

  saveNewRole(): void {
    this.saving = true;
    const request: CreateRoleRequest = {
      name: this.newName.trim(),
      code: this.newCode.trim().toUpperCase(),
      description: this.newDescription.trim() || undefined
    };

    this.adminService.createRoleDefinition(request).subscribe({
      next: () => {
        this.snackBar.open('Role created successfully', 'OK', { duration: 3000 });
        this.createDrawerOpen = false;
        this.saving = false;
        this.paginatedTable.reload();
      },
      error: (err) => {
        this.snackBar.open(err?.error?.message || 'Failed to create role', 'Dismiss', { duration: 4000 });
        this.saving = false;
      }
    });
  }

  // ── Edit ──

  openEditDrawer(role: RoleDefinitionResponse): void {
    this.editingRole = role;
    this.editName = role.name;
    this.editDescription = role.description || '';
    this.editActive = role.active;
    this.editDrawerOpen = true;
  }

  saveEditRole(): void {
    if (!this.editingRole) return;
    this.saving = true;

    const request: UpdateRoleRequest = {
      name: this.editName.trim(),
      description: this.editDescription.trim(),
      active: this.editActive
    };

    this.adminService.updateRoleDefinition(this.editingRole.id, request).subscribe({
      next: () => {
        this.snackBar.open('Role updated successfully', 'OK', { duration: 3000 });
        this.editDrawerOpen = false;
        this.saving = false;
        this.paginatedTable.reload();
      },
      error: (err) => {
        this.snackBar.open(err?.error?.message || 'Failed to update role', 'Dismiss', { duration: 4000 });
        this.saving = false;
      }
    });
  }

  // ── Toggle Active ──

  toggleActive(role: RoleDefinitionResponse): void {
    const newStatus = !role.active;
    this.adminService.updateRoleDefinition(role.id, { active: newStatus }).subscribe({
      next: () => {
        this.snackBar.open(`Role ${newStatus ? 'activated' : 'deactivated'}`, 'OK', { duration: 3000 });
        this.paginatedTable.reload();
      },
      error: (err) => {
        this.snackBar.open(err?.error?.message || 'Failed to update role status', 'Dismiss', { duration: 4000 });
      }
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
          this.snackBar.open('Role deleted successfully', 'OK', { duration: 3000 });
          this.paginatedTable.reload();
        },
        error: (err) => {
          this.snackBar.open(err?.error?.message || 'Failed to delete role', 'Dismiss', { duration: 4000 });
        }
      });
    });
  }
}
