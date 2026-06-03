import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDialogModule } from '@angular/material/dialog';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { HttpClient } from '@angular/common/http';

interface User {
  userId: string;
  name: string;
  email: string;
  roles: string[];
  status: 'ACTIVE' | 'DEACTIVATED';
  createdAt: string;
}

const AVAILABLE_ROLES = [
  'Super_Admin', 'Security_Admin', 'Question_Author', 'Reviewer',
  'Approver', 'Exam_Controller', 'Translator', 'Evaluator', 'Auditor', 'Candidate'
];

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDialogModule,
    MatCardModule,
    MatChipsModule
  ],
  template: `
    <section class="admin-container" role="main" aria-labelledby="user-mgmt-heading">
      <h1 id="user-mgmt-heading">User Management</h1>

      <!-- Create User Form -->
      <mat-card class="create-user-card">
        <mat-card-header>
          <mat-card-title>{{ editingUser ? 'Edit User' : 'Create User' }}</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="userForm" (ngSubmit)="onSubmit()" aria-label="User form">
            <div class="form-row">
              <mat-form-field appearance="outline">
                <mat-label>Name</mat-label>
                <input matInput formControlName="name" aria-required="true">
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>Email</mat-label>
                <input matInput formControlName="email" type="email" aria-required="true">
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>Roles</mat-label>
                <mat-select formControlName="roles" multiple aria-required="true">
                  <mat-option *ngFor="let role of availableRoles" [value]="role">{{ role }}</mat-option>
                </mat-select>
              </mat-form-field>
            </div>

            <div class="form-actions">
              <button mat-raised-button color="primary" type="submit"
                      [disabled]="userForm.invalid"
                      [attr.aria-label]="editingUser ? 'Update user' : 'Create user'">
                {{ editingUser ? 'Update' : 'Create' }}
              </button>
              <button mat-stroked-button type="button" *ngIf="editingUser"
                      (click)="cancelEdit()" aria-label="Cancel editing">
                Cancel
              </button>
            </div>
          </form>
        </mat-card-content>
      </mat-card>

      <!-- Users Table -->
      <mat-card class="users-table-card">
        <table mat-table [dataSource]="users" aria-label="Users list">
          <ng-container matColumnDef="name">
            <th mat-header-cell *matHeaderCellDef>Name</th>
            <td mat-cell *matCellDef="let user">{{ user.name }}</td>
          </ng-container>

          <ng-container matColumnDef="email">
            <th mat-header-cell *matHeaderCellDef>Email</th>
            <td mat-cell *matCellDef="let user">{{ user.email }}</td>
          </ng-container>

          <ng-container matColumnDef="roles">
            <th mat-header-cell *matHeaderCellDef>Roles</th>
            <td mat-cell *matCellDef="let user">
              <mat-chip-set aria-label="User roles">
                <mat-chip *ngFor="let role of user.roles">{{ role }}</mat-chip>
              </mat-chip-set>
            </td>
          </ng-container>

          <ng-container matColumnDef="status">
            <th mat-header-cell *matHeaderCellDef>Status</th>
            <td mat-cell *matCellDef="let user"
                [class.active]="user.status === 'ACTIVE'"
                [class.deactivated]="user.status === 'DEACTIVATED'">
              {{ user.status }}
            </td>
          </ng-container>

          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef>Actions</th>
            <td mat-cell *matCellDef="let user">
              <button mat-icon-button (click)="editUser(user)" aria-label="Edit user">
                <mat-icon>edit</mat-icon>
              </button>
              <button mat-icon-button color="warn"
                      (click)="toggleStatus(user)"
                      [attr.aria-label]="user.status === 'ACTIVE' ? 'Deactivate user' : 'Activate user'">
                <mat-icon>{{ user.status === 'ACTIVE' ? 'block' : 'check_circle' }}</mat-icon>
              </button>
            </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
        </table>
      </mat-card>
    </section>
  `,
  styles: [`
    .admin-container { padding: var(--spacing-lg); max-width: 1200px; margin: 0 auto; }
    .create-user-card { margin-bottom: var(--spacing-lg); }
    .form-row { display: flex; gap: var(--spacing-md); flex-wrap: wrap; }
    .form-row mat-form-field { flex: 1; min-width: 200px; }
    .form-actions { display: flex; gap: var(--spacing-md); margin-top: var(--spacing-sm); }
    .users-table-card { overflow-x: auto; }
    table { width: 100%; }
    .active { color: var(--color-success); font-weight: 500; }
    .deactivated { color: var(--color-error); font-weight: 500; }
  `]
})
export class UserManagementComponent implements OnInit {
  users: User[] = [];
  userForm: FormGroup;
  editingUser: User | null = null;
  availableRoles = AVAILABLE_ROLES;
  displayedColumns = ['name', 'email', 'roles', 'status', 'actions'];

  constructor(private fb: FormBuilder, private http: HttpClient) {
    this.userForm = this.fb.group({
      name: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      roles: [[], Validators.required]
    });
  }

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.http.get<User[]>('/api/v1/admin/users').subscribe({
      next: (users) => this.users = users,
      error: () => {}
    });
  }

  onSubmit(): void {
    if (this.userForm.invalid) return;

    const payload = this.userForm.value;
    if (this.editingUser) {
      this.http.put(`/api/v1/admin/users/${this.editingUser.userId}`, payload).subscribe({
        next: () => { this.loadUsers(); this.cancelEdit(); }
      });
    } else {
      this.http.post('/api/v1/admin/users', payload).subscribe({
        next: () => { this.loadUsers(); this.userForm.reset(); }
      });
    }
  }

  editUser(user: User): void {
    this.editingUser = user;
    this.userForm.patchValue({ name: user.name, email: user.email, roles: user.roles });
  }

  cancelEdit(): void {
    this.editingUser = null;
    this.userForm.reset();
  }

  toggleStatus(user: User): void {
    const action = user.status === 'ACTIVE' ? 'deactivate' : 'activate';
    this.http.post(`/api/v1/admin/users/${user.userId}/${action}`, {}).subscribe({
      next: () => this.loadUsers()
    });
  }
}
