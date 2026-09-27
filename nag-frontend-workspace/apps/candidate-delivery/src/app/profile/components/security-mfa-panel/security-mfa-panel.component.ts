import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import {
  AuthService,
  TotpSetupData,
} from '@nag-frontend-workspace/shared-data-access-auth';
import { QrCodeComponent } from '@nag-frontend-workspace/shared-ui-components';

@Component({
  selector: 'app-security-mfa-panel',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    QrCodeComponent,
  ],
  templateUrl: './security-mfa-panel.component.html',
  styleUrl: './security-mfa-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SecurityMfaPanelComponent implements OnInit {
  private readonly authService = inject(AuthService);

  readonly loading = signal<boolean>(false);
  readonly verifying = signal<boolean>(false);
  readonly disabling = signal<boolean>(false);
  readonly isMfaActive = signal<boolean>(false);

  readonly setupStep = signal<'idle' | 'enroll' | 'backup_codes'>('idle');
  readonly totpData = signal<TotpSetupData | null>(null);
  readonly verifyCode = signal<string>('');
  readonly errorMsg = signal<string | null>(null);
  readonly successMsg = signal<string | null>(null);
  readonly copiedSecret = signal<boolean>(false);

  ngOnInit(): void {
    const user = this.authService.currentUser();
    if (user?.userId) {
      this.authService.getVerificationStatus(user.userId).subscribe({
        next: (status) => {
          // Status check
        },
        error: () => {},
      });
    }
  }

  startEnrollment(): void {
    this.loading.set(true);
    this.errorMsg.set(null);
    this.successMsg.set(null);

    const user = this.authService.currentUser();
    this.authService.setupTotp(user?.username).subscribe({
      next: (data) => {
        this.totpData.set(data);
        this.setupStep.set('enroll');
        this.loading.set(false);
      },
      error: (err) => {
        this.errorMsg.set(
          err.error?.message || 'Failed to initiate 2FA setup. Please try again.'
        );
        this.loading.set(false);
      },
    });
  }

  confirmTotpSetup(): void {
    const code = this.verifyCode().trim();
    const data = this.totpData();
    if (!code || code.length !== 6) {
      this.errorMsg.set('Please enter a valid 6-digit authenticator OTP code.');
      return;
    }
    if (!data?.secret && !data?.secretKey) {
      this.errorMsg.set('Invalid setup state. Please retry.');
      return;
    }

    this.verifying.set(true);
    this.errorMsg.set(null);

    const secret = data.secret || data.secretKey || '';
    const user = this.authService.currentUser();

    this.authService
      .verifyTotpSetup({
        userId: user?.userId,
        secret,
        code,
        backupCodes: data.backupCodes,
      })
      .subscribe({
        next: () => {
          this.verifying.set(false);
          this.isMfaActive.set(true);
          this.setupStep.set('backup_codes');
          this.successMsg.set(
            'Two-factor authentication (TOTP) has been successfully activated!'
          );
        },
        error: (err) => {
          this.verifying.set(false);
          this.errorMsg.set(
            err.error?.message ||
              'Invalid authenticator code. Please check your app clock and try again.'
          );
        },
      });
  }

  disableMfa(): void {
    if (!confirm('Are you sure you want to disable Two-Factor Authentication (2FA)? Your account will be less secure.')) {
      return;
    }

    this.disabling.set(true);
    this.errorMsg.set(null);

    const user = this.authService.currentUser();
    this.authService.disableTotp(user?.userId).subscribe({
      next: () => {
        this.disabling.set(false);
        this.isMfaActive.set(false);
        this.setupStep.set('idle');
        this.totpData.set(null);
        this.successMsg.set('Two-factor authentication has been disabled.');
      },
      error: (err) => {
        this.disabling.set(false);
        this.errorMsg.set(
          err.error?.message || 'Failed to disable 2FA. Please try again.'
        );
      },
    });
  }

  copySecret(): void {
    const secret = this.totpData()?.secret || this.totpData()?.secretKey;
    if (secret && typeof navigator !== 'undefined') {
      navigator.clipboard.writeText(secret);
      this.copiedSecret.set(true);
      setTimeout(() => this.copiedSecret.set(false), 2500);
    }
  }

  downloadBackupCodes(): void {
    const codes = this.totpData()?.backupCodes || [];
    if (codes.length === 0) return;

    const content = [
      '========================================',
      'NAG PLATFORM - 2FA RECOVERY BACKUP CODES',
      '========================================',
      `Account: ${this.authService.currentUser()?.username || 'Candidate'}`,
      `Generated: ${new Date().toISOString()}`,
      '',
      'Keep these single-use recovery codes in a safe place.',
      'Each code can only be used once if you lose access to your authenticator app.',
      '',
      ...codes.map((c, i) => `${i + 1}. ${c}`),
      '========================================',
    ].join('\n');

    const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `nag-recovery-backup-codes-${Date.now()}.txt`;
    a.click();
    URL.revokeObjectURL(url);
  }

  finishSetup(): void {
    this.setupStep.set('idle');
    this.totpData.set(null);
    this.verifyCode.set('');
  }
}
