import { Component, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatMenuModule } from '@angular/material/menu';
import { map } from 'rxjs/operators';
import { AdminService, UserAccountResponse } from '../services/admin.service';
import { RoleAssignDialogComponent, RoleAssignDialogData, RoleAssignDialogResult } from './role-assign-dialog.component';
import { UserCreateDialogComponent, UserCreateDialogResult } from './user-create-dialog.component';
import { UserEditDialogComponent, UserEditDialogData, UserEditDialogResult } from './user-edit-dialog.component';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import {
  PaginatedTableComponent,
  ColumnDef,
  PaginatedDataFetcher
} from '../../../shared/components/paginated-table';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDialogModule,
    MatSnackBarModule,
    MatMenuModule,
    PaginatedTableComponent
  ],
  template: `
    <section class="admin-container" role="main" aria-labelledby="user-mgmt-heading">
      <div class="page-header">
        <h1 id="user-mgmt-heading">User Management</h1>
        <button mat-raised-button color="primary" (click)="openCreateDialog()" aria-label="Create a new user">
          <mat-icon>person_add</mat-icon>
          Create User
        </button>
      </div>

      <!-- Reusable Paginated Table -->
      <app-paginated-table
        #paginatedTable
        title="Users List"
        [fetcher]="fetcher"
        [columns]="columns"
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
          <button mat-menu-item (click)="openEditDialog(user)">
            <mat-icon>edit</mat-icon>
            <span>Edit User</span>
          </button>
          <button mat-menu-item (click)="openRoleDialog(user)">
            <mat-icon>admin_panel_settings</mat-icon>
            <span>Manage Roles</span>
          </button>
          <button mat-menu-item (click)="deactivateUser(user)" [disabled]="user.accountStatus === 'DEACTIVATED'">
            <mat-icon color="warn">block</mat-icon>
            <span>Deactivate</span>
          </button>
        </mat-menu>
      </ng-template>

    </section>
  `,
  styles: [`
    .admin-container { padding: 24px; max-width: 1400px; margin: 0 auto; }
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
    .page-header h1 { margin: 0; }
    ::ng-deep .active-status { background-color: #c8e6c9 !important; color: #2e7d32 !important; }
    ::ng-deep .inactive-status { background-color: #ffcdd2 !important; color: #c62828 !important; }
    .mfa-on { color: #4caf50; }
    .mfa-off { color: #9e9e9e; }
    .role-chip { font-size: 11px; }
    .no-roles { color: #9e9e9e; font-style: italic; }
  `]
})
export class UserManagementComponent {

  @ViewChild('paginatedTable') paginatedTable!: PaginatedTableComponent<UserAccountResponse>;
  @ViewChild('mfaTmpl', { static: true }) mfaTmpl!: any;
  @ViewChild('rolesTmpl', { static: true }) rolesTmpl!: any;

  columns: ColumnDef<UserAccountResponse>[] = [];

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
    private dialog: MatDialog,
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

  reload(): void {
    this.paginatedTable?.reload();
  }

  openRoleDialog(user: UserAccountResponse): void {
    const dialogData: RoleAssignDialogData = {
      username: user.username,
      userId: user.id,
      currentRoles: user.roles
    };

    const dialogRef = this.dialog.open(RoleAssignDialogComponent, {
      width: '420px',
      data: dialogData
    });

    dialogRef.afterClosed().subscribe((result: RoleAssignDialogResult | undefined) => {
      if (result) {
        this.adminService.assignRole(user.id, result.role, result.action).subscribe({
          next: (res) => {
            this.snackBar.open(res.message, 'OK', { duration: 3000 });
            this.reload();
          },
          error: () => {
            this.snackBar.open('Role operation failed', 'Dismiss', { duration: 5000 });
          }
        });
      }
    });
  }

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(UserCreateDialogComponent, {
      width: '460px'
    });

    dialogRef.afterClosed().subscribe((result: UserCreateDialogResult | undefined) => {
      if (result) {
        this.adminService.createUser(result).subscribe({
          next: () => {
            this.snackBar.open('User created successfully', 'OK', { duration: 3000 });
            this.reload();
          },
          error: (err) => {
            const msg = err.error?.detail || err.error?.message || 'Failed to create user';
            this.snackBar.open(msg, 'Dismiss', { duration: 5000 });
          }
        });
      }
    });
  }

  openEditDialog(user: UserAccountResponse): void {
    const dialogData: UserEditDialogData = { user };

    const dialogRef = this.dialog.open(UserEditDialogComponent, {
      width: '460px',
      data: dialogData
    });

    dialogRef.afterClosed().subscribe((result: UserEditDialogResult | undefined) => {
      if (result && Object.keys(result).length > 0) {
        this.adminService.updateUser(user.id, result).subscribe({
          next: () => {
            this.snackBar.open('User updated successfully', 'OK', { duration: 3000 });
            this.reload();
          },
          error: (err) => {
            const msg = err.error?.detail || err.error?.message || 'Failed to update user';
            this.snackBar.open(msg, 'Dismiss', { duration: 5000 });
          }
        });
      }
    });
  }

  deactivateUser(user: UserAccountResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Deactivate User',
        message: `Are you sure you want to deactivate "${user.username}"? This will prevent them from logging in.`,
        confirmText: 'Deactivate',
        color: 'warn',
        icon: 'person_off'
      } as ConfirmDialogData
    });
    ref.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.adminService.deactivateUser(user.id).subscribe({
        next: () => {
          this.snackBar.open('User deactivated', 'OK', { duration: 3000 });
          this.reload();
        },
        error: () => {
          this.snackBar.open('Failed to deactivate user', 'Dismiss', { duration: 5000 });
        }
      });
    });
  }
}
