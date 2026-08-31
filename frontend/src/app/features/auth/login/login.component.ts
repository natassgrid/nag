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
      <div class="login-wrapper">
        <!-- Candidate-style NAG Logo -->
        <div class="brand-header">
          <div class="brand-badge-container">
            <div class="brand-icon-box">
              <mat-icon class="brand-icon" aria-hidden="true">menu_book</mat-icon>
            </div>
            <div class="brand-text">
              <h1 class="brand-title">NAG</h1>
              <p class="brand-subtitle">National Assessment Grid</p>
            </div>
          </div>
          <p class="portal-tag">Administration Portal</p>
        </div>

        <!-- Glassmorphic Card -->
        <div class="login-card">
          <h2 id="login-heading" class="card-title">Sign in to your account</h2>

          <form [formGroup]="loginForm" (ngSubmit)="onSubmit()" aria-label="Login form" class="login-form">
            <div class="form-group">
              <label class="input-label" for="username-input">Email or Mobile Number</label>
              <input
                id="username-input"
                formControlName="username"
                type="text"
                autocomplete="username"
                placeholder="you@example.com or 9876543210"
                aria-required="true"
                [attr.aria-invalid]="loginForm.get('username')?.invalid && loginForm.get('username')?.touched"
                class="candidate-input"
                [class.has-error]="loginForm.get('username')?.invalid && loginForm.get('username')?.touched"
              />
              <p class="input-error" *ngIf="loginForm.get('username')?.invalid && loginForm.get('username')?.touched">
                Username or email is required
              </p>
            </div>

            <div class="form-group">
              <label class="input-label" for="password-input">Password</label>
              <div class="password-input-wrapper">
                <input
                  id="password-input"
                  formControlName="password"
                  [type]="hidePassword ? 'password' : 'text'"
                  autocomplete="current-password"
                  placeholder="••••••••"
                  aria-required="true"
                  [attr.aria-invalid]="loginForm.get('password')?.invalid && loginForm.get('password')?.touched"
                  class="candidate-input password-input"
                  [class.has-error]="loginForm.get('password')?.invalid && loginForm.get('password')?.touched"
                />
                <button
                  type="button"
                  (click)="hidePassword = !hidePassword"
                  class="password-toggle-btn"
                  [attr.aria-label]="hidePassword ? 'Show password' : 'Hide password'"
                >
                  <mat-icon class="toggle-icon">{{ hidePassword ? 'visibility_off' : 'visibility' }}</mat-icon>
                </button>
              </div>
              <p class="input-error" *ngIf="loginForm.get('password')?.invalid && loginForm.get('password')?.touched">
                Password is required
              </p>
            </div>

            <button
              mat-flat-button
              color="primary"
              type="submit"
              class="login-button"
              [disabled]="loginForm.invalid || isLoading"
              aria-label="Sign in to your account"
            >
              <mat-spinner *ngIf="isLoading" diameter="20" class="button-spinner"></mat-spinner>
              <span *ngIf="!isLoading" class="btn-content">
                <mat-icon class="btn-icon">login</mat-icon>
                Sign In
              </span>
            </button>
          </form>

          <div class="auth-links">
            <span class="auth-hint">Don't have an account?</span>
            <a [routerLink]="['/auth/register']" class="register-link" aria-label="Register here">
              Register here
            </a>
          </div>
        </div>
      </div>
    </main>
  `
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
