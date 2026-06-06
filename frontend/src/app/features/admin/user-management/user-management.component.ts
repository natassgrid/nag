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
import { AdminService, UserAccountResponse } from '../services/admin.service';
import { RoleAssignDialogComponent, RoleAssignDialogData, RoleAssignDialogResult } from './role-assign-dialog.component';

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
    MatSnackBarModule
  ],
  template: `
    <section class="admin-container" role="main" aria-labelledby="user-mgmt-heading">
      <h1 id="user-mgmt-heading">User Management</h1>

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
              <button mat-raised-button color="primary" (click)="openRoleDialog(user)"
                      aria-label="Assign or revoke role for user">
                <mat-icon>admin_panel_settings</mat-icon>
                Assign Role
              </button>
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
}
