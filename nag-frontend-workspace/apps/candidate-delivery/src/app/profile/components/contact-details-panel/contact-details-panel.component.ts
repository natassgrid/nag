import {
  ChangeDetectionStrategy,
  Component,
  OnDestroy,
  inject,
  model,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '@nag-frontend-workspace/shared-data-access-auth';
import { CandidateProfile } from '../../models';

@Component({
  selector: 'nag-contact-details-panel',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatIconModule],
  templateUrl: './contact-details-panel.component.html',
  styleUrl: './contact-details-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ContactDetailsPanelComponent implements OnDestroy {
  private readonly authService = inject(AuthService);

  readonly profile = model.required<CandidateProfile>();

  // Mobile Revalidation Modal State
  readonly isMobileModalOpen = signal<boolean>(false);
  readonly mobileOtp = signal<string>('');
  readonly mobileLoading = signal<boolean>(false);
  readonly mobileResending = signal<boolean>(false);
  readonly mobileError = signal<string | null>(null);
  readonly mobileSuccess = signal<string | null>(null);
  readonly mobileResendCountdown = signal<number>(0);
  private mobileTimer: ReturnType<typeof setInterval> | null = null;

  // Email Update Modal State
  readonly isEmailModalOpen = signal<boolean>(false);
  readonly newEmail = signal<string>('');
  readonly emailOtp = signal<string>('');
  readonly emailStep = signal<'input' | 'otp'>('input');
  readonly emailLoading = signal<boolean>(false);
  readonly emailResending = signal<boolean>(false);
  readonly emailError = signal<string | null>(null);
  readonly emailSuccess = signal<string | null>(null);
  readonly emailResendCountdown = signal<number>(0);
  private emailTimer: ReturnType<typeof setInterval> | null = null;

  ngOnDestroy(): void {
    this.clearMobileTimer();
    this.clearEmailTimer();
  }

  // ----------------------------------------------------
  // Mobile Revalidation Flows
  // ----------------------------------------------------
  openMobileModal(): void {
    this.isMobileModalOpen.set(true);
    this.mobileOtp.set('');
    this.mobileError.set(null);
    this.mobileSuccess.set(null);
    this.triggerSendMobileOtp();
  }

  closeMobileModal(): void {
    this.isMobileModalOpen.set(false);
    this.clearMobileTimer();
  }

  triggerSendMobileOtp(): void {
    const userId = this.profile().candidateId;
    if (!userId || userId === 'NAG-CAN-NEW' || userId === 'user-unknown') {
      this.mobileError.set('Please save your profile first before verifying your mobile number.');
      return;
    }

    this.mobileResending.set(true);
    this.mobileError.set(null);

    this.authService.resendSmsOtp({ userId }).subscribe({
      next: () => {
        this.mobileResending.set(false);
        this.mobileSuccess.set('Verification code dispatched to your mobile number.');
        this.startMobileCountdown(60);
      },
      error: (err) => {
        this.mobileResending.set(false);
        const detail = err?.error?.detail || err?.error?.message || err?.message || 'Failed to send SMS OTP.';
        this.mobileError.set(detail);
      },
    });
  }

  verifyMobileOtp(): void {
    const otp = this.mobileOtp().trim();
    if (otp.length !== 6) {
      this.mobileError.set('Please enter the complete 6-digit code.');
      return;
    }

    const userId = this.profile().candidateId;
    this.mobileLoading.set(true);
    this.mobileError.set(null);

    this.authService.verifyMobile({ userId, otp }).subscribe({
      next: () => {
        this.mobileLoading.set(false);
        this.mobileSuccess.set('Mobile number verified successfully!');
        this.profile.update((curr) => ({
          ...curr,
          mobileVerified: true,
        }));
        setTimeout(() => {
          this.closeMobileModal();
        }, 1200);
      },
      error: (err) => {
        this.mobileLoading.set(false);
        const detail = err?.error?.detail || err?.error?.message || err?.message || 'Invalid or expired OTP code.';
        this.mobileError.set(detail);
      },
    });
  }

  fillTestMobileOtp(): void {
    this.mobileOtp.set('000000');
    this.verifyMobileOtp();
  }

  private startMobileCountdown(seconds = 60): void {
    this.mobileResendCountdown.set(seconds);
    this.clearMobileTimer();
    this.mobileTimer = setInterval(() => {
      if (this.mobileResendCountdown() > 0) {
        this.mobileResendCountdown.update((c) => c - 1);
      } else {
        this.clearMobileTimer();
      }
    }, 1000);
  }

  private clearMobileTimer(): void {
    if (this.mobileTimer) {
      clearInterval(this.mobileTimer);
      this.mobileTimer = null;
    }
  }

  // ----------------------------------------------------
  // Email Update & Verification Flows
  // ----------------------------------------------------
  openEmailModal(): void {
    this.isEmailModalOpen.set(true);
    this.newEmail.set('');
    this.emailOtp.set('');
    this.emailStep.set('input');
    this.emailError.set(null);
    this.emailSuccess.set(null);
  }

  closeEmailModal(): void {
    this.isEmailModalOpen.set(false);
    this.clearEmailTimer();
  }

  requestEmailUpdateOtp(): void {
    const targetEmail = this.newEmail().trim().toLowerCase();
    if (!targetEmail || !targetEmail.includes('@') || !targetEmail.includes('.')) {
      this.emailError.set('Please enter a valid email address.');
      return;
    }

    if (targetEmail === this.profile().email?.toLowerCase()?.trim()) {
      this.emailError.set('New email must be different from current email.');
      return;
    }

    const userId = this.profile().candidateId;
    this.emailResending.set(true);
    this.emailError.set(null);

    this.authService.resendEmailOtp({ userId, email: targetEmail }).subscribe({
      next: () => {
        this.emailResending.set(false);
        this.emailStep.set('otp');
        this.emailSuccess.set(`A 6-digit verification code has been sent to ${targetEmail}.`);
        this.startEmailCountdown(60);
      },
      error: (err) => {
        this.emailResending.set(false);
        const detail = err?.error?.detail || err?.error?.message || err?.message || 'Failed to dispatch email verification code.';
        this.emailError.set(detail);
      },
    });
  }

  verifyEmailUpdateOtp(): void {
    const otp = this.emailOtp().trim();
    if (otp.length !== 6) {
      this.emailError.set('Please enter a valid 6-digit OTP code.');
      return;
    }

    const userId = this.profile().candidateId;
    const targetEmail = this.newEmail().trim().toLowerCase();

    this.emailLoading.set(true);
    this.emailError.set(null);

    this.authService.verifyEmail({ userId, otp }).subscribe({
      next: () => {
        this.emailLoading.set(false);
        this.emailSuccess.set('Email updated and verified successfully!');
        this.profile.update((curr) => ({
          ...curr,
          email: targetEmail,
          emailVerified: true,
        }));
        setTimeout(() => {
          this.closeEmailModal();
        }, 1200);
      },
      error: (err) => {
        this.emailLoading.set(false);
        const detail = err?.error?.detail || err?.error?.message || err?.message || 'Invalid or expired OTP code.';
        this.emailError.set(detail);
      },
    });
  }

  fillTestEmailOtp(): void {
    this.emailOtp.set('000000');
    this.verifyEmailUpdateOtp();
  }

  private startEmailCountdown(seconds = 60): void {
    this.emailResendCountdown.set(seconds);
    this.clearEmailTimer();
    this.emailTimer = setInterval(() => {
      if (this.emailResendCountdown() > 0) {
        this.emailResendCountdown.update((c) => c - 1);
      } else {
        this.clearEmailTimer();
      }
    }, 1000);
  }

  private clearEmailTimer(): void {
    if (this.emailTimer) {
      clearInterval(this.emailTimer);
      this.emailTimer = null;
    }
  }
}
