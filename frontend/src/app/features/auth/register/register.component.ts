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
import { MatSelectModule } from '@angular/material/select';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatStepperModule } from '@angular/material/stepper';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatStepperModule,
    RouterLink
  ],
  template: `
    <main class="register-container" role="main" aria-labelledby="register-heading">
      <div class="register-wrapper">
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

        <div class="register-card">
          <!-- Registration Step -->
          <ng-container *ngIf="!showOtpStep">
            <h2 id="register-heading" class="card-title">Create Account</h2>

            <form [formGroup]="registerForm" (ngSubmit)="onRegister()" aria-label="Registration form" class="register-form">
              <div class="form-group">
                <label class="input-label">Full Name</label>
                <input
                  formControlName="name"
                  type="text"
                  placeholder="Jane Doe"
                  autocomplete="name"
                  aria-required="true"
                  class="candidate-input"
                  [class.has-error]="registerForm.get('name')?.invalid && registerForm.get('name')?.touched"
                />
                <p class="input-error" *ngIf="registerForm.get('name')?.invalid && registerForm.get('name')?.touched">
                  Full name is required
                </p>
              </div>

              <div class="form-group">
                <label class="input-label">Email Address</label>
                <input
                  formControlName="email"
                  type="email"
                  placeholder="you@example.com"
                  autocomplete="email"
                  aria-required="true"
                  class="candidate-input"
                  [class.has-error]="registerForm.get('email')?.invalid && registerForm.get('email')?.touched"
                />
                <p class="input-error" *ngIf="registerForm.get('email')?.invalid && registerForm.get('email')?.touched">
                  Enter a valid email address
                </p>
              </div>

              <div class="form-group">
                <label class="input-label">Mobile Number</label>
                <input
                  formControlName="mobile"
                  type="tel"
                  autocomplete="tel"
                  placeholder="+919876543210"
                  aria-required="true"
                  class="candidate-input"
                  [class.has-error]="registerForm.get('mobile')?.invalid && registerForm.get('mobile')?.touched"
                />
                <p class="input-error" *ngIf="registerForm.get('mobile')?.invalid && registerForm.get('mobile')?.touched">
                  Enter a valid Indian mobile number (+91XXXXXXXXXX)
                </p>
              </div>

              <div class="form-group">
                <label class="input-label">Identity Document Type</label>
                <select
                  formControlName="identityDocType"
                  aria-required="true"
                  class="candidate-input candidate-select"
                  [class.has-error]="registerForm.get('identityDocType')?.invalid && registerForm.get('identityDocType')?.touched"
                >
                  <option value="" disabled selected>Select document</option>
                  <option value="AADHAAR">Aadhaar Card</option>
                  <option value="PAN">PAN Card</option>
                  <option value="VOTER_ID">Voter ID</option>
                  <option value="PASSPORT">Passport</option>
                </select>
                <p class="input-error" *ngIf="registerForm.get('identityDocType')?.invalid && registerForm.get('identityDocType')?.touched">
                  Document type is required
                </p>
              </div>

              <div class="form-group">
                <label class="input-label">Identity Document Number</label>
                <input
                  formControlName="identityDocNumber"
                  type="text"
                  placeholder="Enter document number"
                  aria-required="true"
                  class="candidate-input"
                  [class.has-error]="registerForm.get('identityDocNumber')?.invalid && registerForm.get('identityDocNumber')?.touched"
                />
                <p class="input-error" *ngIf="registerForm.get('identityDocNumber')?.invalid && registerForm.get('identityDocNumber')?.touched">
                  Document number is required
                </p>
              </div>

              <button
                mat-flat-button
                color="primary"
                type="submit"
                class="submit-button"
                [disabled]="registerForm.invalid || isLoading"
                aria-label="Submit registration"
              >
                <mat-spinner *ngIf="isLoading" diameter="20" class="button-spinner"></mat-spinner>
                <span *ngIf="!isLoading">Register Account</span>
              </button>
            </form>

            <div class="auth-links">
              <span class="auth-hint">Already have an account?</span>
              <a [routerLink]="['/auth/login']" class="login-link" aria-label="Already have an account? Sign in">
                Sign In
              </a>
            </div>
          </ng-container>

          <!-- OTP Verification Step -->
          <ng-container *ngIf="showOtpStep">
            <h2 id="register-heading" class="card-title">Verify OTP</h2>
            <p class="step-subtitle">Enter the 6-digit OTP sent to your registered mobile number</p>

            <form [formGroup]="otpForm" (ngSubmit)="onVerifyOtp()" aria-label="OTP verification form" class="register-form">
              <div class="form-group">
                <label class="input-label">OTP Code</label>
                <input
                  formControlName="otp"
                  type="text"
                  inputmode="numeric"
                  maxlength="6"
                  placeholder="123456"
                  autocomplete="one-time-code"
                  aria-required="true"
                  aria-label="6-digit One Time Password"
                  class="candidate-input"
                  [class.has-error]="otpForm.get('otp')?.invalid && otpForm.get('otp')?.touched"
                />
                <p class="input-error" *ngIf="otpForm.get('otp')?.invalid && otpForm.get('otp')?.touched">
                  Must be exactly 6 digits
                </p>
              </div>

              <button
                mat-flat-button
                color="primary"
                type="submit"
                class="submit-button"
                [disabled]="otpForm.invalid || isVerifying"
                aria-label="Verify OTP code"
              >
                <mat-spinner *ngIf="isVerifying" diameter="20" class="button-spinner"></mat-spinner>
                <span *ngIf="!isVerifying">Verify & Activate</span>
              </button>
            </form>

            <div class="auth-links">
              <a href="javascript:void(0)" (click)="backToRegister()" class="login-link"
                 aria-label="Go back to registration form">
                ← Back to registration
              </a>
            </div>
          </ng-container>
        </div>
      </div>
    </main>
  `
})
export class RegisterComponent {
  registerForm: FormGroup;
  otpForm: FormGroup;
  isLoading = false;
  isVerifying = false;
  showOtpStep = false;
  private registrationId = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    this.registerForm = this.fb.group({
      name: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      mobile: ['', [Validators.required, Validators.pattern(/^\+91\d{10}$/)]],
      identityDocType: ['', [Validators.required]],
      identityDocNumber: ['', [Validators.required]]
    });

    this.otpForm = this.fb.group({
      otp: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]]
    });
  }

  onRegister(): void {
    if (this.registerForm.invalid) return;

    this.isLoading = true;

    const formData = this.registerForm.value;
    this.authService.register(formData).subscribe({
      next: (res) => {
        this.isLoading = false;
        this.registrationId = res.registrationId;
        this.showOtpStep = true;
        this.snackBar.open('Registration successful! Please verify OTP.', 'OK', {
          duration: 4000,
          horizontalPosition: 'center',
          verticalPosition: 'bottom'
        });
      },
      error: (err) => {
        this.isLoading = false;
        const message = err.error?.message || 'Registration failed. Please try again.';
        this.snackBar.open(message, 'Dismiss', {
          duration: 5000,
          horizontalPosition: 'center',
          verticalPosition: 'bottom',
          panelClass: ['error-snackbar']
        });
      }
    });
  }

  onVerifyOtp(): void {
    if (this.otpForm.invalid) return;

    this.isVerifying = true;

    this.authService.verifyOtp({
      registrationId: this.registrationId,
      otp: this.otpForm.value.otp
    }).subscribe({
      next: () => {
        this.isVerifying = false;
        this.snackBar.open('Account verified successfully!', 'OK', {
          duration: 3000,
          horizontalPosition: 'center',
          verticalPosition: 'bottom'
        });
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.isVerifying = false;
        const message = err.error?.message || 'OTP verification failed. Please try again.';
        this.snackBar.open(message, 'Dismiss', {
          duration: 5000,
          horizontalPosition: 'center',
          verticalPosition: 'bottom',
          panelClass: ['error-snackbar']
        });
      }
    });
  }

  backToRegister(): void {
    this.showOtpStep = false;
    this.otpForm.reset();
  }
}
