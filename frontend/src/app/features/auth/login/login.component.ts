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

import { Component, ChangeDetectionStrategy } from '@angular/core';
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
  templateUrl: './login.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {
  loginForm: FormGroup;
  hidePassword = true;
  isLoading = false;
  mfaRequired = false;
  mfaPromptMessage = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    this.loginForm = this.fb.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required]],
      otpCode: ['']
    });
  }

  resetMfa(): void {
    this.mfaRequired = false;
    this.mfaPromptMessage = '';
    this.loginForm.get('otpCode')?.clearValidators();
    this.loginForm.get('otpCode')?.reset();
    this.loginForm.get('otpCode')?.updateValueAndValidity();
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;

    const { username, password, otpCode } = this.loginForm.value;

    this.authService.login({ username, password, otpCode: this.mfaRequired ? otpCode : undefined }).subscribe({
      next: () => {
        this.isLoading = false;
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.isLoading = false;

        // Check if backend responded with MFA / Step-up authentication challenge (403 Forbidden)
        const isMfa = err.status === 403 && (
          err.error?.mfaRequired === true ||
          err.error?.title === 'MFA Required' ||
          (typeof err.error?.detail === 'string' && err.error?.detail.toLowerCase().includes('otp'))
        );

        if (isMfa) {
          this.mfaRequired = true;
          this.mfaPromptMessage = err.error?.detail || 'Step-up authentication required. Please provide the 6-digit OTP sent to your registered mobile.';
          this.loginForm.get('otpCode')?.setValidators([Validators.required, Validators.pattern('^[0-9]{6}$')]);
          this.loginForm.get('otpCode')?.updateValueAndValidity();

          this.snackBar.open(this.mfaPromptMessage, 'OK', {
            duration: 7000,
            horizontalPosition: 'center',
            verticalPosition: 'bottom',
            panelClass: ['info-snackbar']
          });
          return;
        }

        const message = err.error?.detail || err.error?.message || 'Login failed. Please check your credentials.';
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
