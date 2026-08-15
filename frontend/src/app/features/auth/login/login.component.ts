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

import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    RouterLink
  ],
  template: `
    <main class="login-container" role="main" aria-labelledby="login-heading">
      <mat-card class="login-card" appearance="outlined">
        <mat-card-header class="login-header">
          <div class="app-branding">
            <mat-icon class="app-logo" aria-hidden="true">school</mat-icon>
            <h1 id="login-heading" class="app-title">Exam Platform</h1>
          </div>
          <p class="login-subtitle">Sign in to your account</p>
        </mat-card-header>

        <mat-card-content>
          <form [formGroup]="loginForm" (ngSubmit)="onSubmit()" aria-label="Login form">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Username</mat-label>
              <input matInput formControlName="username"
                     type="text"
                     autocomplete="username"
                     aria-required="true"
                     [attr.aria-invalid]="loginForm.get('username')?.invalid && loginForm.get('username')?.touched">
              <mat-icon matPrefix>person</mat-icon>
              <mat-error *ngIf="loginForm.get('username')?.hasError('required')">
                Username is required
              </mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Password</mat-label>
              <input matInput formControlName="password"
                     [type]="hidePassword ? 'password' : 'text'"
                     autocomplete="current-password"
                     aria-required="true"
                     [attr.aria-invalid]="loginForm.get('password')?.invalid && loginForm.get('password')?.touched">
              <mat-icon matPrefix>lock</mat-icon>
              <button mat-icon-button matSuffix type="button"
                      (click)="hidePassword = !hidePassword"
                      [attr.aria-label]="hidePassword ? 'Show password' : 'Hide password'">
                <mat-icon>{{ hidePassword ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
              <mat-error *ngIf="loginForm.get('password')?.hasError('required')">
                Password is required
              </mat-error>
            </mat-form-field>

            <button mat-raised-button color="primary" type="submit"
                    class="login-button full-width"
                    [disabled]="loginForm.invalid || isLoading"
                    aria-label="Sign in to your account">
              <mat-spinner *ngIf="isLoading" diameter="20" class="button-spinner"></mat-spinner>
              <span *ngIf="!isLoading">Sign In</span>
            </button>
          </form>

          <div class="auth-links">
            <a [routerLink]="['/auth/register']" aria-label="Create a new account">
              Don't have an account? Register
            </a>
          </div>
        </mat-card-content>
      </mat-card>
    </main>
  `,
  styles: [`
    .login-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      padding: 16px;
      background: linear-gradient(135deg, #e8eaf6 0%, #fafafa 100%);
    }

    .login-card {
      max-width: 400px;
      width: 100%;
      padding: 32px 24px;
      border-radius: 12px;
      box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08), 0 2px 8px rgba(0, 0, 0, 0.04);
    }

    .login-header {
      display: flex;
      flex-direction: column;
      align-items: center;
      margin-bottom: 24px;
    }

    .app-branding {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
    }

    .app-logo {
      font-size: 48px;
      width: 48px;
      height: 48px;
      color: #3f51b5;
    }

    .app-title {
      margin: 0;
      font-size: 24px;
      font-weight: 600;
      color: #3f51b5;
      letter-spacing: -0.5px;
    }

    .login-subtitle {
      margin: 8px 0 0;
      font-size: 14px;
      color: #666;
    }

    .full-width {
      width: 100%;
    }

    mat-form-field.full-width {
      margin-bottom: 8px;
    }

    .login-button {
      height: 48px;
      font-size: 16px;
      font-weight: 500;
      margin-top: 8px;
      border-radius: 8px;
    }

    .button-spinner {
      display: inline-block;
    }

    ::ng-deep .login-button .mat-mdc-button-persistent-ripple {
      border-radius: 8px;
    }

    .auth-links {
      margin-top: 24px;
      text-align: center;
    }

    .auth-links a {
      color: #3f51b5;
      text-decoration: none;
      font-size: 14px;
    }

    .auth-links a:hover {
      text-decoration: underline;
    }
  `]
})
export class LoginComponent {
  loginForm: FormGroup;
  hidePassword = true;
  isLoading = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    this.loginForm = this.fb.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required]]
    });
  }

  onSubmit(): void {
    if (this.loginForm.invalid) return;

    this.isLoading = true;

    const { username, password } = this.loginForm.value;

    this.authService.login({ username, password }).subscribe({
      next: () => {
        this.isLoading = false;
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.isLoading = false;
        const message = err.error?.message || 'Login failed. Please check your credentials.';
        this.snackBar.open(message, 'Dismiss', {
          duration: 5000,
          horizontalPosition: 'center',
          verticalPosition: 'bottom',
          panelClass: ['error-snackbar']
        });
      }
    });
  }
}
