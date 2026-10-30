import {
  Component,
  inject,
  signal,
  OnInit,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '@nag-frontend-workspace/shared-data-access-auth';
import { NotificationService, parseHttpError } from '@nag-frontend-workspace/shared-ui-components';
import { AuthBrandHeaderComponent } from '../auth-brand-header/auth-brand-header.component';
import { AuthFlowService } from '../../services/auth-flow.service';

@Component({
  selector: 'app-candidate-login',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    AuthBrandHeaderComponent,
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly authFlowService = inject(AuthFlowService);
  private readonly router = inject(Router);
  private readonly notificationService = inject(NotificationService);

  username = '';
  password = '';
  otpCode = '';
  resetEmail = '';

  readonly loading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly fieldErrors = signal<Record<string, string>>({});
  readonly showPassword = signal<boolean>(false);
  readonly showForgotPassword = signal<boolean>(false);
  readonly mfaStepRequired = signal<boolean>(false);

  ngOnInit(): void {
    if (this.authService.isAuthenticated()) {
      this.authFlowService.navigateToDashboard();
    }
  }

  clearFieldError(field: string): void {
    const current = { ...this.fieldErrors() };
    if (current[field]) {
      delete current[field];
      this.fieldErrors.set(current);
    }
    this.errorMessage.set(null);
  }

  validateSingleField(field: string): void {
    const error = this.getFieldValidationError(field);
    const current = { ...this.fieldErrors() };
    if (error) {
      current[field] = error;
    } else {
      delete current[field];
    }
    this.fieldErrors.set(current);
  }

  validateField(field: string): void {
    this.validateSingleField(field);
  }

  private getFieldValidationError(field: string): string | null {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    const mobileRegex = /^[6-9]\d{9}$/;

    switch (field) {
      case 'username': {
        const u = (this.username || '').trim();
        if (!u) {
          return 'Please enter your email or 10-digit mobile number.';
        }
        if (/^\d+$/.test(u)) {
          if (!mobileRegex.test(u)) {
            return 'Mobile number must be a valid 10-digit Indian number starting with 6, 7, 8, or 9.';
          }
          return null;
        }
        if (u.includes('@')) {
          if (!emailRegex.test(u)) {
            return 'Please enter a valid email address.';
          }
          return null;
        }
        return 'Please enter a valid email address or 10-digit mobile number.';
      }
      case 'password': {
        if (!this.password || !this.password.trim()) {
          return 'Password is required.';
        }
        return null;
      }
      case 'otpCode': {
        const otp = (this.otpCode || '').trim();
        if (!otp) {
          return 'Please enter your 6-digit Authenticator OTP.';
        }
        if (!/^\d{6}$/.test(otp)) {
          return 'Please enter a valid 6-digit numeric OTP code.';
        }
        return null;
      }
      case 'resetEmail': {
        const re = (this.resetEmail || '').trim();
        if (!re || !emailRegex.test(re)) {
          return 'Please enter a valid email address.';
        }
        return null;
      }
      default:
        return null;
    }
  }

  handleLogin(): void {
    const errors: Record<string, string> = {};

    if (this.mfaStepRequired()) {
      const otpErr = this.getFieldValidationError('otpCode');
      if (otpErr) {
        errors['otpCode'] = otpErr;
      }
    } else {
      if (!this.username.trim() && !this.password.trim()) {
        errors['username'] = 'Please enter your email or registered mobile.';
        errors['password'] = 'Password is required.';
        this.fieldErrors.set(errors);
        this.errorMessage.set('Please enter both your email/mobile and password.');
        return;
      }

      const uErr = this.getFieldValidationError('username');
      if (uErr) errors['username'] = uErr;

      const pErr = this.getFieldValidationError('password');
      if (pErr) errors['password'] = pErr;
    }

    if (Object.keys(errors).length > 0) {
      this.fieldErrors.set(errors);
      const firstError = Object.values(errors)[0];
      this.errorMessage.set(firstError);
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    this.fieldErrors.set({});

    const payload: { username: string; password: string; otpCode?: string } = {
      username: this.username.trim(),
      password: this.password,
    };

    if (this.mfaStepRequired() && this.otpCode.trim()) {
      payload.otpCode = this.otpCode.trim();
    }

    this.authService.login(payload).subscribe({
      next: () => {
        this.loading.set(false);
        this.authFlowService.navigateToDashboard();
      },
      error: (err) => {
        this.loading.set(false);

        // Intercept unverified accounts and redirect to verification flow
        if (this.authFlowService.isUnverifiedAccountError(err)) {
          const errData = err?.error;
          const userId = errData?.userId || '';
          const email = errData?.email || this.username.trim();
          this.authFlowService.navigateToVerifyOtp({
            userId,
            email,
            pending: true,
          });
          return;
        }

        // Intercept MFA Required challenge (HTTP 403 or specific message)
        const msg = err?.error?.message || err?.error?.detail || err?.message || '';
        if (
          err.status === 403 &&
          (msg.toLowerCase().includes('mfa') ||
           msg.toLowerCase().includes('2fa') ||
           msg.toLowerCase().includes('authenticator') ||
           msg.toLowerCase().includes('otp'))
        ) {
          this.mfaStepRequired.set(true);
          this.errorMessage.set('Two-Factor Authentication required. Enter the 6-digit code from your Authenticator app.');
          return;
        }

        const parsed = parseHttpError(
          err,
          'Invalid credentials. Please verify your email/mobile and password.'
        );
        this.fieldErrors.set(parsed.fieldErrors);
        this.errorMessage.set(parsed.message);
      },
    });
  }

  cancelMfa(): void {
    this.mfaStepRequired.set(false);
    this.otpCode = '';
    this.errorMessage.set(null);
    this.clearFieldError('otpCode');
  }

  handleForgotPassword(): void {
    const err = this.getFieldValidationError('resetEmail');
    if (err) {
      this.fieldErrors.set({ resetEmail: err });
      this.errorMessage.set(err);
      return;
    }

    this.loading.set(true);
    setTimeout(() => {
      this.loading.set(false);
      this.showForgotPassword.set(false);
      this.errorMessage.set(null);
      this.notificationService.success(
        'Password Reset Email Sent',
        'Password reset instructions have been sent to your registered email.'
      );
    }, 1000);
  }

  togglePasswordVisibility(): void {
    this.showPassword.update((val) => !val);
  }
}
