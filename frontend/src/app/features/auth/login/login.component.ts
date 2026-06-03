import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
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
    MatCheckboxModule,
    RouterLink
  ],
  template: `
    <main id="main-content" class="auth-container" role="main" aria-labelledby="login-heading">
      <mat-card class="auth-card">
        <mat-card-header>
          <mat-card-title id="login-heading">Sign In</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="loginForm" (ngSubmit)="onSubmit()" aria-label="Login form">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Username or Email</mat-label>
              <input matInput formControlName="username"
                     type="text"
                     autocomplete="username"
                     aria-required="true"
                     [attr.aria-invalid]="loginForm.get('username')?.invalid && loginForm.get('username')?.touched">
              <mat-error *ngIf="loginForm.get('username')?.hasError('required')">
                Username is required
              </mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Password</mat-label>
              <input matInput formControlName="password"
                     [type]="hidePassword ? 'password' : 'text'"
                     autocomplete="current-password"
                     aria-required="true">
              <button mat-icon-button matSuffix type="button"
                      (click)="hidePassword = !hidePassword"
                      [attr.aria-label]="hidePassword ? 'Show password' : 'Hide password'">
                <mat-icon>{{ hidePassword ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
              <mat-error *ngIf="loginForm.get('password')?.hasError('required')">
                Password is required
              </mat-error>
            </mat-form-field>

            <!-- MFA OTP Input -->
            <mat-form-field appearance="outline" class="full-width" *ngIf="showMfa">
              <mat-label>MFA Code (6 digits)</mat-label>
              <input matInput formControlName="mfaCode"
                     type="text"
                     inputmode="numeric"
                     maxlength="6"
                     autocomplete="one-time-code"
                     aria-label="Multi-factor authentication code">
              <mat-error *ngIf="loginForm.get('mfaCode')?.hasError('pattern')">
                Must be 6 digits
              </mat-error>
            </mat-form-field>

            <div class="form-actions">
              <button mat-raised-button color="primary" type="submit"
                      [disabled]="loginForm.invalid || isLoading"
                      aria-label="Sign in to your account">
                {{ isLoading ? 'Signing in...' : 'Sign In' }}
              </button>

              <button mat-stroked-button type="button"
                      (click)="loginWithWebAuthn()"
                      aria-label="Sign in with WebAuthn passkey">
                <mat-icon>fingerprint</mat-icon>
                WebAuthn
              </button>
            </div>

            <div *ngIf="errorMessage" class="error-message" role="alert" aria-live="assertive">
              {{ errorMessage }}
            </div>
          </form>

          <div class="auth-links">
            <a [routerLink]="['/auth/register']" aria-label="Create a new account">
              Create Account
            </a>
          </div>

          <div class="accessibility-toggle">
            <mat-checkbox (change)="toggleHighContrast($event.checked)"
                          aria-label="Enable high contrast mode">
              High Contrast Mode
            </mat-checkbox>
          </div>
        </mat-card-content>
      </mat-card>
    </main>
  `,
  styles: [`
    .auth-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      padding: var(--spacing-md);
    }
    .auth-card {
      max-width: 420px;
      width: 100%;
      padding: var(--spacing-lg);
    }
    .full-width { width: 100%; margin-bottom: var(--spacing-sm); }
    .form-actions {
      display: flex;
      gap: var(--spacing-md);
      margin-top: var(--spacing-md);
      flex-wrap: wrap;
    }
    .error-message {
      color: var(--color-error);
      margin-top: var(--spacing-md);
      font-weight: 500;
    }
    .auth-links { margin-top: var(--spacing-lg); text-align: center; }
    .accessibility-toggle { margin-top: var(--spacing-md); }
  `]
})
export class LoginComponent {
  loginForm: FormGroup;
  hidePassword = true;
  showMfa = false;
  isLoading = false;
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required]],
      mfaCode: ['', [Validators.pattern(/^\d{6}$/)]]
    });
  }

  onSubmit(): void {
    if (this.loginForm.invalid) return;

    this.isLoading = true;
    this.errorMessage = '';

    const { username, password, mfaCode } = this.loginForm.value;

    this.authService.login({ username, password, mfaCode: mfaCode || undefined }).subscribe({
      next: () => {
        this.router.navigate(['/exam']);
      },
      error: (err) => {
        this.isLoading = false;
        if (err.status === 403 && err.error?.mfaRequired) {
          this.showMfa = true;
          this.errorMessage = 'Please enter your MFA code.';
        } else if (err.status === 429) {
          this.errorMessage = 'Too many attempts. Please try again later.';
        } else {
          this.errorMessage = err.error?.message || 'Login failed. Please check your credentials.';
        }
      }
    });
  }

  loginWithWebAuthn(): void {
    // WebAuthn/FIDO2 authentication flow placeholder
    // Would invoke navigator.credentials.get() with publicKey options
    this.errorMessage = 'WebAuthn authentication not yet configured for this device.';
  }

  toggleHighContrast(enabled: boolean): void {
    document.body.classList.toggle('high-contrast', enabled);
  }
}
