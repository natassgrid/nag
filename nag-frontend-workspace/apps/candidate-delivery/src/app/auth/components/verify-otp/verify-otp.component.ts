import {
  Component,
  OnInit,
  OnDestroy,
  inject,
  signal,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '@nag-frontend-workspace/shared-data-access-auth';
import { AuthBrandHeaderComponent } from '../auth-brand-header/auth-brand-header.component';
import { AuthFlowService } from '../../services/auth-flow.service';

@Component({
  selector: 'app-candidate-verify-otp',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    AuthBrandHeaderComponent,
  ],
  templateUrl: './verify-otp.component.html',
  styleUrl: './verify-otp.component.scss',
})
export class VerifyOtpComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
  private readonly authFlowService = inject(AuthFlowService);

  activeTab = signal<'email' | 'mobile'>('email');

  userId = signal<string>('');
  email = signal<string>('');
  mobile = signal<string>('');
  otpCode = '';

  // Mobile Quota & Masking
  smsQuotaRemaining = signal<number>(3);
  maxWeeklyQuota = 3;

  isPendingLogin = signal<boolean>(false);
  loading = signal<boolean>(false);
  resending = signal<boolean>(false);
  sendingSms = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  resendCountdown = signal<number>(30);
  private intervalTimer: ReturnType<typeof setInterval> | null = null;

  readonly maskedPhone = computed(() => {
    const raw = this.mobile().trim();
    if (!raw) return '+91 ******0000';
    const digits = raw.replace(/\D/g, '');
    if (digits.length >= 10) {
      const last4 = digits.slice(-4);
      return `+91 ******${last4}`;
    }
    return `+91 ******${digits.slice(-2)}`;
  });

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      if (params['userId']) this.userId.set(params['userId']);
      if (params['email']) this.email.set(params['email']);
      if (params['mobile']) this.mobile.set(params['mobile']);
      if (params['pending'] === 'true') this.isPendingLogin.set(true);
      if (params['tab'] === 'mobile' || params['channel'] === 'sms') {
        this.activeTab.set('mobile');
      }

      if (params['otp']) {
        this.otpCode = params['otp'].trim();
        if (this.otpCode.length === 6) {
          // Auto-verify when arriving via direct email verification link
          this.handleVerifyOtp();
        }
      }

      if (this.userId()) {
        this.fetchVerificationStatus();
      }
    });

    this.startResendTimer(30);
  }

  ngOnDestroy(): void {
    if (this.intervalTimer) {
      clearInterval(this.intervalTimer);
      this.intervalTimer = null;
    }
  }

  switchTab(tab: 'email' | 'mobile'): void {
    if (this.activeTab() === tab) return;
    this.activeTab.set(tab);
    this.clearError();
    this.otpCode = '';
  }

  fetchVerificationStatus(): void {
    if (!this.userId()) return;
    this.authService.getVerificationStatus(this.userId()).subscribe({
      next: (status) => {
        if (status.remainingSmsQuota !== undefined) {
          this.smsQuotaRemaining.set(status.remainingSmsQuota);
        }
        if (status.maskedMobile) {
          this.mobile.set(status.maskedMobile);
        }
      },
      error: () => {
        // Fallback to default initial quota
      },
    });
  }

  startResendTimer(seconds = 30): void {
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

  clearError(): void {
    this.errorMessage.set(null);
  }

  clearInput(): void {
    this.otpCode = '';
    this.clearError();
  }

  resendOtp(): void {
    if (this.resendCountdown() > 0 || this.resending()) {
      return;
    }

    if (this.activeTab() === 'mobile') {
      this.sendMobileSmsOtp();
      return;
    }

    this.resending.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.otpCode = '';

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
        this.startResendTimer(30);
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

  sendMobileSmsOtp(): void {
    if (this.smsQuotaRemaining() <= 0) {
      this.errorMessage.set(
        'Weekly SMS quota exhausted (0/3 remaining). Please verify via Email OTP or retry next week.'
      );
      return;
    }

    this.sendingSms.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.otpCode = '';

    const payload = {
      userId: this.userId() || this.email() || 'cand-001',
    };

    this.authService.resendSmsOtp(payload).subscribe({
      next: (res) => {
        this.sendingSms.set(false);
        this.smsQuotaRemaining.update((q) => Math.max(0, q - 1));
        this.successMessage.set(
          res?.message || `SMS OTP dispatched to ${this.maskedPhone()}. (${this.smsQuotaRemaining()}/${this.maxWeeklyQuota} SMS remaining this week)`
        );
        this.startResendTimer(30);
      },
      error: (err) => {
        this.sendingSms.set(false);
        if (err?.status === 429) {
          this.smsQuotaRemaining.set(0);
          this.errorMessage.set(
            'Weekly SMS limit reached (429: Too Many Requests). Please switch to Email verification.'
          );
        } else {
          const detail =
            err?.error?.detail ||
            err?.error?.message ||
            err?.message ||
            'Failed to dispatch SMS OTP. Please check mobile connection or verify with Email.';
          this.errorMessage.set(detail);
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
        this.authFlowService.navigateToDashboard();
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
