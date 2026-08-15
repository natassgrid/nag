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
  template: `
    <div class="page-layout" role="main" aria-labelledby="user-mgmt-heading">
      <app-page-header
        title="User Management"
        subtitle="Create, edit, and manage platform user accounts and roles."
        icon="people"
      >
        <button mat-raised-button color="primary" (click)="openCreateDrawer()" aria-label="Create a new user">
          <mat-icon>person_add</mat-icon>
          Create User
        </button>
      </app-page-header>

      <!-- Reusable Paginated Table -->
      <app-paginated-table
        #paginatedTable
        title="Users List"
        [fetcher]="fetcher"
        [columns]="columns"
        [filterCategories]="filterCategories"
        (filterChange)="onFilterChange($event)"
        [actionsTemplate]="actionsTmpl"
        searchPlaceholder="Filter by username, status, or role..."
      ></app-paginated-table>

      <!-- MFA Column Custom Template -->
      <ng-template #mfaTmpl let-user>
        <mat-icon [class.mfa-on]="user?.mfaEnabled" [class.mfa-off]="!user?.mfaEnabled">
          {{ user?.mfaEnabled ? 'verified_user' : 'no_encryption' }}
        </mat-icon>
      </ng-template>

      <!-- Roles Column Custom Template -->
      <ng-template #rolesTmpl let-user>
        <mat-chip-set aria-label="User roles">
          <mat-chip *ngFor="let role of user?.roles" class="role-chip">{{ role }}</mat-chip>
        </mat-chip-set>
        <span *ngIf="!user?.roles?.length" class="no-roles">No roles</span>
      </ng-template>

      <!-- Actions Column Template -->
      <ng-template #actionsTmpl let-user>
        <button mat-icon-button [matMenuTriggerFor]="actionMenu" aria-label="User actions menu">
          <mat-icon>more_vert</mat-icon>
        </button>
        <mat-menu #actionMenu="matMenu">
          <button mat-menu-item (click)="openEditDrawer(user)">
            <mat-icon>edit</mat-icon>
            <span>Edit User</span>
          </button>
          <button mat-menu-item (click)="openRoleDrawer(user)">
            <mat-icon>admin_panel_settings</mat-icon>
            <span>Manage Roles</span>
          </button>
          <button mat-menu-item (click)="deactivateUser(user)" [disabled]="user.accountStatus === 'DEACTIVATED'">
            <mat-icon color="warn">block</mat-icon>
            <span>Deactivate</span>
          </button>
        </mat-menu>
      </ng-template>

      <!-- ── RIGHT COLLAPSIBLE DRAWER: CREATE USER ── -->
      <app-right-drawer
        [isOpen]="createDrawerOpen"
        title="Create New User"
        subtitle="Add a new administrative user or candidate account to the platform."
        (close)="createDrawerOpen = false"
      >
        <div drawer-body class="drawer-form-container">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Full Name</mat-label>
            <input matInput [(ngModel)]="newFullName" placeholder="e.g. John Doe" required />
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Email Address</mat-label>
            <input matInput type="email" [(ngModel)]="newEmail" placeholder="e.g. jdoe@example.com" required />
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Password</mat-label>
            <input matInput type="password" [(ngModel)]="newPassword" placeholder="Minimum 6 characters" required />
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Initial Roles</mat-label>
            <mat-select [(ngModel)]="newRoles" multiple>
              <mat-option *ngFor="let role of availableRoles" [value]="role">{{ role }}</mat-option>
            </mat-select>
          </mat-form-field>
        </div>

        <div drawer-footer>
          <button mat-button (click)="createDrawerOpen = false">Cancel</button>
          <button mat-raised-button color="primary" [disabled]="!newFullName.trim() || !newEmail.trim() || !newPassword.trim() || saving" (click)="saveCreateUser()">
            Create User
          </button>
        </div>
      </app-right-drawer>

      <!-- ── RIGHT COLLAPSIBLE DRAWER: EDIT USER ── -->
      <app-right-drawer
        [isOpen]="editDrawerOpen"
        [title]="'Edit User: ' + (editingUser?.username || '')"
        subtitle="Update user status and multi-factor authentication settings."
        (close)="editDrawerOpen = false"
      >
        <div drawer-body class="drawer-form-container">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Account Status</mat-label>
            <mat-select [(ngModel)]="editStatus">
              <mat-option value="ACTIVE">Active</mat-option>
              <mat-option value="DEACTIVATED">Deactivated</mat-option>
            </mat-select>
          </mat-form-field>

          <div class="checkbox-field">
            <mat-checkbox [(ngModel)]="editMfaEnabled" color="primary">Enable Multi-Factor Authentication (MFA)</mat-checkbox>
          </div>
        </div>

        <div drawer-footer>
          <button mat-button (click)="editDrawerOpen = false">Cancel</button>
          <button mat-raised-button color="primary" [disabled]="saving" (click)="saveEditUser()">
            Save Changes
          </button>
        </div>
      </app-right-drawer>

      <!-- ── RIGHT COLLAPSIBLE DRAWER: MANAGE ROLES ── -->
      <app-right-drawer
        [isOpen]="roleDrawerOpen"
        [title]="'Manage Roles: ' + (roleUser?.username || '')"
        subtitle="Assign or revoke platform permissions and access roles."
        (close)="roleDrawerOpen = false"
      >
        <div drawer-body class="drawer-form-container">
          <p class="section-subtext">Current Roles: <strong>{{ (roleUser?.roles || []).join(', ') || 'None' }}</strong></p>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Target Role</mat-label>
            <mat-select [(ngModel)]="selectedRole">
              <mat-option *ngFor="let r of availableRoles" [value]="r">{{ r }}</mat-option>
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Action</mat-label>
            <mat-select [(ngModel)]="roleAction">
              <mat-option value="ASSIGN">Assign Role</mat-option>
              <mat-option value="REVOKE">Revoke Role</mat-option>
            </mat-select>
          </mat-form-field>
        </div>

        <div drawer-footer>
          <button mat-button (click)="roleDrawerOpen = false">Cancel</button>
          <button mat-raised-button color="primary" [disabled]="!selectedRole || saving" (click)="saveRoleChange()">
            Apply Role Change
          </button>
        </div>
      </app-right-drawer>

    </div>
  `,
  styles: [`
    .mfa-on { color: #4caf50; }
    .mfa-off { color: #9e9e9e; }
    .role-chip { font-size: 11px; }
    .no-roles { color: #9e9e9e; font-style: italic; }
    .drawer-form-container { display: flex; flex-direction: column; gap: 16px; padding-top: 8px; }
    .full-width { width: 100%; }
    .checkbox-field { padding: 4px 0; }
    .section-subtext { font-size: 14px; color: #616161; margin-bottom: 12px; }
    ::ng-deep .active-status { background-color: #c8e6c9 !important; color: #2e7d32 !important; }
    ::ng-deep .inactive-status { background-color: #ffcdd2 !important; color: #c62828 !important; }
  `]
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
