import { Component, OnInit, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatMenuModule } from '@angular/material/menu';
import { AdminService, UserAccountResponse } from '../services/admin.service';
import { RoleAssignDialogComponent, RoleAssignDialogData, RoleAssignDialogResult } from './role-assign-dialog.component';
import { UserCreateDialogComponent, UserCreateDialogResult } from './user-create-dialog.component';
import { UserEditDialogComponent, UserEditDialogData, UserEditDialogResult } from './user-edit-dialog.component';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDialogModule,
    MatCardModule,
    MatSnackBarModule,
    MatMenuModule
  ],
  template: `
    <section class="admin-container" role="main" aria-labelledby="user-mgmt-heading">
      <div class="page-header">
        <h1 id="user-mgmt-heading">User Management</h1>
        <button mat-raised-button color="primary" (click)="openCreateDialog()"
                aria-label="Create a new user">
          <mat-icon>person_add</mat-icon>
          Create User
        </button>
      </div>

      <!-- Search / Filter -->
      <mat-form-field appearance="outline" class="filter-field">
        <mat-label>Search users</mat-label>
        <input matInput (keyup)="applyFilter($event)" placeholder="Filter by username, status, or role"
               aria-label="Filter users">
        <mat-icon matSuffix>search</mat-icon>
      </mat-form-field>

      <!-- Users Table -->
      <mat-card class="users-table-card">
        <table mat-table [dataSource]="dataSource" matSort aria-label="Users list">

          <!-- Username Column -->
          <ng-container matColumnDef="username">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>Username</th>
            <td mat-cell *matCellDef="let user">{{ user.username }}</td>
          </ng-container>

          <!-- Status Column -->
          <ng-container matColumnDef="accountStatus">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>Status</th>
            <td mat-cell *matCellDef="let user"
                [class.active]="user.accountStatus === 'ACTIVE'"
                [class.inactive]="user.accountStatus !== 'ACTIVE'">
              {{ user.accountStatus }}
            </td>
          </ng-container>

          <!-- MFA Column -->
          <ng-container matColumnDef="mfaEnabled">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>MFA</th>
            <td mat-cell *matCellDef="let user">
              <mat-icon [class.mfa-on]="user.mfaEnabled" [class.mfa-off]="!user.mfaEnabled">
                {{ user.mfaEnabled ? 'verified_user' : 'no_encryption' }}
              </mat-icon>
            </td>
          </ng-container>

          <!-- Roles Column -->
          <ng-container matColumnDef="roles">
            <th mat-header-cell *matHeaderCellDef>Roles</th>
            <td mat-cell *matCellDef="let user">
              <mat-chip-set aria-label="User roles">
                <mat-chip *ngFor="let role of user.roles" class="role-chip">{{ role }}</mat-chip>
              </mat-chip-set>
              <span *ngIf="!user.roles?.length" class="no-roles">No roles</span>
            </td>
          </ng-container>

          <!-- Created Column -->
          <ng-container matColumnDef="createdAt">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>Created</th>
            <td mat-cell *matCellDef="let user">{{ user.createdAt | date:'medium' }}</td>
          </ng-container>

          <!-- Actions Column -->
          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef>Actions</th>
            <td mat-cell *matCellDef="let user">
              <button mat-icon-button [matMenuTriggerFor]="actionMenu"
                      aria-label="User actions menu">
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
                <button mat-menu-item (click)="deactivateUser(user)"
                        [disabled]="user.accountStatus === 'DEACTIVATED'">
                  <mat-icon color="warn">block</mat-icon>
                  <span>Deactivate</span>
                </button>
              </mat-menu>
            </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>

          <!-- No data row -->
          <tr class="mat-row" *matNoDataRow>
            <td class="mat-cell no-data" [attr.colspan]="displayedColumns.length">
              No users found matching "{{ filterValue }}"
            </td>
          </tr>
        </table>

        <mat-paginator [pageSizeOptions]="[10, 25, 50]" showFirstLastButtons
                       aria-label="Select page of users">
        </mat-paginator>
      </mat-card>
    </section>
  `,
  styles: [`
    .admin-container { padding: 24px; max-width: 1400px; margin: 0 auto; }
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
    .page-header h1 { margin: 0; }
    .filter-field { width: 100%; margin-bottom: 16px; }
    .users-table-card { overflow-x: auto; }
    table { width: 100%; }
    .active { color: #4caf50; font-weight: 500; }
    .inactive { color: #f44336; font-weight: 500; }
    .mfa-on { color: #4caf50; }
    .mfa-off { color: #9e9e9e; }
    .role-chip { font-size: 11px; }
    .no-roles { color: #9e9e9e; font-style: italic; }
    .no-data { text-align: center; padding: 24px; color: #9e9e9e; }
  `]
})
export class UserManagementComponent implements OnInit, AfterViewInit {
  displayedColumns = ['username', 'accountStatus', 'mfaEnabled', 'roles', 'createdAt', 'actions'];
  dataSource = new MatTableDataSource<UserAccountResponse>([]);
  filterValue = '';

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  constructor(
    private adminService: AdminService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
    this.dataSource.filterPredicate = (data: UserAccountResponse, filter: string) => {
      const searchStr = filter.toLowerCase();
      return data.username.toLowerCase().includes(searchStr)
        || data.accountStatus.toLowerCase().includes(searchStr)
        || data.roles.some(r => r.toLowerCase().includes(searchStr));
    };
  }

  loadUsers(): void {
    this.adminService.getUsers().subscribe({
      next: (users) => {
        this.dataSource.data = users;
      },
      error: (err) => {
        this.snackBar.open('Failed to load users', 'Dismiss', { duration: 5000 });
      }
    });
  }

  applyFilter(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.filterValue = value;
    this.dataSource.filter = value.trim().toLowerCase();
    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
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
            this.loadUsers();
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
            this.loadUsers();
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
            this.loadUsers();
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
    if (!confirm(`Are you sure you want to deactivate "${user.username}"? This will prevent them from logging in.`)) {
      return;
    }
    this.adminService.deactivateUser(user.id).subscribe({
      next: () => {
        this.snackBar.open('User deactivated', 'OK', { duration: 3000 });
        this.loadUsers();
      },
      error: () => {
        this.snackBar.open('Failed to deactivate user', 'Dismiss', { duration: 5000 });
      }
    });
  }
}
