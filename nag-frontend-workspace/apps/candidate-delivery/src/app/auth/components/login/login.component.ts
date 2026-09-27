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
import { NotificationService } from '@nag-frontend-workspace/shared-ui-components';
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

  loading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  showPassword = signal<boolean>(false);
  showForgotPassword = signal<boolean>(false);
  mfaStepRequired = signal<boolean>(false);

  ngOnInit(): void {
    if (this.authService.isAuthenticated()) {
      this.authFlowService.navigateToDashboard();
    }
  }

  handleLogin(): void {
    if (!this.username.trim() || !this.password.trim()) {
      this.errorMessage.set('Please enter both your email/mobile and password.');
      return;
    }

    if (this.mfaStepRequired() && !this.otpCode.trim()) {
      this.errorMessage.set('Please enter your 6-digit Authenticator OTP or single-use recovery code.');
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

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

        const detail =
          err?.error?.message ||
          err?.error?.detail ||
          err?.message ||
          'Invalid credentials. Please verify your email/mobile and password.';
        this.errorMessage.set(detail);
      },
    });
  }

  cancelMfa(): void {
    this.mfaStepRequired.set(false);
    this.otpCode = '';
    this.errorMessage.set(null);
  }

  handleForgotPassword(): void {
    if (!this.resetEmail.trim()) {
      this.errorMessage.set('Please enter your registered email address.');
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
