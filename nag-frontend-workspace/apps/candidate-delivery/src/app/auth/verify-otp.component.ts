import {
  Component,
  OnInit,
  OnDestroy,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router, ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '@nag-frontend-workspace/shared-data-access-auth';

@Component({
  selector: 'app-candidate-verify-otp',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './verify-otp.component.html',
  styleUrl: './verify-otp.component.scss',
})
export class VerifyOtpComponent implements OnInit, OnDestroy {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);

  userId = signal<string>('');
  email = signal<string>('');
  mobile = signal<string>('');
  otpCode = '';

  isPendingLogin = signal<boolean>(false);
  loading = signal<boolean>(false);
  resending = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  resendCountdown = signal<number>(60);
  private intervalTimer: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      if (params['userId']) this.userId.set(params['userId']);
      if (params['email']) this.email.set(params['email']);
      if (params['mobile']) this.mobile.set(params['mobile']);
      if (params['pending'] === 'true') this.isPendingLogin.set(true);

      if (params['otp']) {
        this.otpCode = params['otp'].trim();
        if (this.otpCode.length === 6) {
          // Auto-verify when arriving via direct email verification link
          this.handleVerifyOtp();
        }
      }
    });

    this.startResendTimer();
  }

  ngOnDestroy(): void {
    if (this.intervalTimer) {
      clearInterval(this.intervalTimer);
      this.intervalTimer = null;
    }
  }

  private startResendTimer(seconds = 60): void {
    this.resendCountdown.set(seconds);
    if (this.intervalTimer) clearInterval(this.intervalTimer);
    this.intervalTimer = setInterval(() => {
      if (this.resendCountdown() > 0) {
        this.resendCountdown.update((c) => c - 1);
      } else {
        if (this.intervalTimer) {
          clearInterval(this.intervalTimer);
          this.intervalTimer = null;
        }
      }
    }, 1000);
  }

  resendOtp(): void {
    if (this.resendCountdown() > 0 || this.resending()) {
      return;
    }

    this.resending.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const payload = {
      userId: this.userId() || undefined,
      email: this.email() || undefined,
    };

    this.authService.resendEmailOtp(payload).subscribe({
      next: (res) => {
        this.resending.set(false);
        this.successMessage.set(
          res?.message || 'A fresh 6-digit verification code has been dispatched to your email.'
        );
        this.startResendTimer(60);
      },
      error: (err) => {
        this.resending.set(false);
        const detail =
          err?.error?.detail ||
          err?.error?.message ||
          err?.message ||
          'Failed to resend verification code. Please try again.';
        this.errorMessage.set(detail);

        if (err?.error?.retryAfterSeconds) {
          this.startResendTimer(err.error.retryAfterSeconds);
        }
      },
    });
  }

  fillTestBypassOtp(): void {
    this.otpCode = '000000';
    this.handleVerifyOtp();
  }

  handleVerifyOtp(): void {
    if (this.otpCode.length !== 6) {
      this.errorMessage.set('Please enter a valid 6-digit OTP code.');
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const payload = {
      userId: this.userId() || undefined,
      email: this.email() || undefined,
      mobile: this.mobile() || undefined,
      otp: this.otpCode,
    };

    this.authService.verifyOtp(payload).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading.set(false);
        const detail =
          err?.error?.detail ||
          err?.error?.message ||
          err?.message ||
          'Invalid or expired verification code. Please try again.';
        this.errorMessage.set(detail);
      },
    });
  }
}
