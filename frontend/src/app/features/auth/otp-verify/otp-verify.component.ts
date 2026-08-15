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

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-otp-verify',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatCardModule,
    MatCheckboxModule
  ],
  template: `
    <main id="main-content" class="auth-container" role="main" aria-labelledby="otp-heading">
      <mat-card class="auth-card">
        <mat-card-header>
          <mat-card-title id="otp-heading">Verify OTP</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <p id="otp-instructions">Enter the 6-digit OTP sent to your registered mobile number.</p>

          <form [formGroup]="otpForm" (ngSubmit)="onSubmit()" aria-describedby="otp-instructions" aria-label="OTP verification form">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>OTP Code</mat-label>
              <input matInput formControlName="otp"
                     type="text"
                     inputmode="numeric"
                     maxlength="6"
                     autocomplete="one-time-code"
                     aria-required="true"
                     aria-label="6-digit One Time Password">
              <mat-error *ngIf="otpForm.get('otp')?.hasError('required')">OTP is required</mat-error>
              <mat-error *ngIf="otpForm.get('otp')?.hasError('pattern')">Must be exactly 6 digits</mat-error>
            </mat-form-field>

            <div class="form-actions">
              <button mat-raised-button color="primary" type="submit"
                      [disabled]="otpForm.invalid || isLoading"
                      aria-label="Verify OTP code">
                {{ isLoading ? 'Verifying...' : 'Verify' }}
              </button>

              <button mat-stroked-button type="button"
                      (click)="resendOtp()"
                      [disabled]="resendCooldown > 0"
                      aria-label="Resend OTP code">
                {{ resendCooldown > 0 ? 'Resend in ' + resendCooldown + 's' : 'Resend OTP' }}
              </button>
            </div>

            <div *ngIf="errorMessage" class="error-message" role="alert" aria-live="assertive">
              {{ errorMessage }}
            </div>
            <div *ngIf="successMessage" class="success-message" role="status" aria-live="polite">
              {{ successMessage }}
            </div>
          </form>

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
    .auth-card { max-width: 400px; width: 100%; padding: var(--spacing-lg); }
    .full-width { width: 100%; margin-bottom: var(--spacing-sm); }
    .form-actions { display: flex; gap: var(--spacing-md); margin-top: var(--spacing-md); flex-wrap: wrap; }
    .error-message { color: var(--color-error); margin-top: var(--spacing-md); font-weight: 500; }
    .success-message { color: var(--color-success); margin-top: var(--spacing-md); font-weight: 500; }
    .accessibility-toggle { margin-top: var(--spacing-lg); }
  `]
})
export class OtpVerifyComponent implements OnInit {
  otpForm: FormGroup;
  isLoading = false;
  errorMessage = '';
  successMessage = '';
  resendCooldown = 0;
  private registrationId = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.otpForm = this.fb.group({
      otp: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]]
    });
  }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.registrationId = params['registrationId'] || '';
    });
    this.startResendCooldown();
  }

  onSubmit(): void {
    if (this.otpForm.invalid) return;

    this.isLoading = true;
    this.errorMessage = '';

    this.authService.verifyOtp({
      registrationId: this.registrationId,
      otp: this.otpForm.value.otp
    }).subscribe({
      next: () => {
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'OTP verification failed. Please try again.';
      }
    });
  }

  resendOtp(): void {
    this.successMessage = 'OTP has been resent to your mobile number.';
    this.startResendCooldown();
  }

  private startResendCooldown(): void {
    this.resendCooldown = 60;
    const interval = setInterval(() => {
      this.resendCooldown--;
      if (this.resendCooldown <= 0) {
        clearInterval(interval);
      }
    }, 1000);
  }

  toggleHighContrast(enabled: boolean): void {
    document.body.classList.toggle('high-contrast', enabled);
  }
}
