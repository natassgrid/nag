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
  template: `
    <div class="page-layout" role="main" aria-labelledby="role-mgmt-heading">
      <app-page-header
        title="Role Management"
        subtitle="Create, edit, and manage platform roles and their permissions."
        icon="admin_panel_settings"
      >
        <button mat-raised-button color="primary" (click)="openCreateDrawer()" aria-label="Create a new role">
          <mat-icon>add</mat-icon>
          Create Role
        </button>
      </app-page-header>

      <!-- Reusable Paginated Table -->
      <app-paginated-table
        #paginatedTable
        title="Roles"
        [fetcher]="fetcher"
        [columns]="columns"
        [filterCategories]="filterCategories"
        (filterChange)="onFilterChange($event)"
        [actionsTemplate]="actionsTmpl"
        searchPlaceholder="Search by name, code, or description..."
      ></app-paginated-table>

      <!-- Status Column Custom Template -->
      <ng-template #statusTmpl let-role>
        <span class="status-badge" [class.active]="role?.active" [class.inactive]="!role?.active">
          {{ role?.active ? 'Active' : 'Inactive' }}
        </span>
      </ng-template>

      <!-- Permissions Column Custom Template -->
      <ng-template #permsTmpl let-role>
        <mat-chip-set aria-label="Role permissions">
          <mat-chip *ngFor="let perm of (role?.permissions || []).slice(0, 3)" class="perm-chip">
            {{ perm.name }}
          </mat-chip>
          <mat-chip *ngIf="(role?.permissions || []).length > 3" class="perm-chip more-chip">
            +{{ role.permissions.length - 3 }} more
          </mat-chip>
        </mat-chip-set>
        <span *ngIf="!role?.permissions?.length" class="no-perms">No permissions</span>
      </ng-template>

      <!-- Actions Column Template -->
      <ng-template #actionsTmpl let-role>
        <button mat-icon-button [matMenuTriggerFor]="actionMenu" aria-label="Role actions menu">
          <mat-icon>more_vert</mat-icon>
        </button>
        <mat-menu #actionMenu="matMenu">
          <button mat-menu-item (click)="openEditDrawer(role)">
            <mat-icon>edit</mat-icon>
            <span>Edit Role</span>
          </button>
          <button mat-menu-item (click)="toggleActive(role)" *ngIf="!role.systemRole">
            <mat-icon>{{ role.active ? 'toggle_off' : 'toggle_on' }}</mat-icon>
            <span>{{ role.active ? 'Deactivate' : 'Activate' }}</span>
          </button>
          <button mat-menu-item (click)="deleteRole(role)" [disabled]="role.systemRole">
            <mat-icon color="warn">delete</mat-icon>
            <span>Delete Role</span>
          </button>
        </mat-menu>
      </ng-template>

      <!-- ── RIGHT COLLAPSIBLE DRAWER: CREATE ROLE ── -->
      <app-right-drawer
        [isOpen]="createDrawerOpen"
        title="Create New Role"
        subtitle="Define a new platform role with a unique code and description."
        (close)="createDrawerOpen = false"
      >
        <div drawer-body class="drawer-form-container">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Role Name</mat-label>
            <input matInput [(ngModel)]="newName" placeholder="e.g. Regional Admin" required />
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Role Code</mat-label>
            <input matInput [(ngModel)]="newCode" placeholder="e.g. REGIONAL_ADMIN" required />
            <mat-hint>Uppercase letters, numbers, and underscores only</mat-hint>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Description</mat-label>
            <textarea matInput [(ngModel)]="newDescription" rows="3"
                      placeholder="Describe what this role can do..."></textarea>
          </mat-form-field>
        </div>

        <div drawer-footer>
          <button mat-button (click)="createDrawerOpen = false">Cancel</button>
          <button mat-raised-button color="primary"
                  [disabled]="!newName || !newCode || saving"
                  (click)="saveNewRole()">
            Create Role
          </button>
        </div>
      </app-right-drawer>

      <!-- ── RIGHT COLLAPSIBLE DRAWER: EDIT ROLE ── -->
      <app-right-drawer
        [isOpen]="editDrawerOpen"
        [title]="'Edit Role: ' + (editingRole?.name || '')"
        subtitle="Update role name, description, or status."
        (close)="editDrawerOpen = false"
      >
        <div drawer-body class="drawer-form-container">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Role Code</mat-label>
            <input matInput [value]="editingRole?.code || ''" disabled />
            <mat-hint>Role code cannot be changed</mat-hint>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Role Name</mat-label>
            <input matInput [(ngModel)]="editName" required />
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Description</mat-label>
            <textarea matInput [(ngModel)]="editDescription" rows="3"></textarea>
          </mat-form-field>

          <div class="toggle-field">
            <mat-slide-toggle [(ngModel)]="editActive" color="primary">
              Active
            </mat-slide-toggle>
          </div>
        </div>

        <div drawer-footer>
          <button mat-button (click)="editDrawerOpen = false">Cancel</button>
          <button mat-raised-button color="primary" [disabled]="!editName || saving" (click)="saveEditRole()">
            Save Changes
          </button>
        </div>
      </app-right-drawer>

    </div>
  `,
  styles: [`
    .status-badge {
      padding: 4px 10px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 500;
    }
    .status-badge.active { background-color: #c8e6c9; color: #2e7d32; }
    .status-badge.inactive { background-color: #ffcdd2; color: #c62828; }
    .perm-chip { font-size: 11px; }
    .more-chip { background-color: #e3f2fd !important; color: #1565c0 !important; }
    .no-perms { color: #9e9e9e; font-style: italic; font-size: 13px; }
    .drawer-form-container { display: flex; flex-direction: column; gap: 16px; padding-top: 8px; }
    .full-width { width: 100%; }
    .toggle-field { padding: 8px 0; }
  `]
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
