import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  signal,
  computed,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  QrCodeComponent,
  NotificationService,
} from '@nag-frontend-workspace/shared-ui-components';
import { AdminUserProfile, ChangePasswordPayload, TotpSetupResult } from '../../profile.model';

@Component({
  selector: 'nag-security-mfa-card',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
    QrCodeComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './security-mfa-card.component.html',
  styleUrl: './security-mfa-card.component.scss',
})
export class SecurityMfaCardComponent {
  private readonly notificationService = inject(NotificationService);

  readonly profile = input.required<AdminUserProfile>();
  readonly totpSetupData = input<TotpSetupResult | null>(null);
  readonly isChangingPassword = input<boolean>(false);
  readonly isProcessingTotp = input<boolean>(false);

  readonly passwordChange = output<ChangePasswordPayload>();
  readonly initTotpSetup = output<void>();
  readonly verifyTotp = output<{ secret: string; code: string; backupCodes?: string[] }>();
  readonly disableTotp = output<void>();

  // Password State
  readonly currentPassword = signal<string>('');
  readonly newPassword = signal<string>('');
  readonly confirmPassword = signal<string>('');
  readonly showCurrentPassword = signal<boolean>(false);
  readonly showNewPassword = signal<boolean>(false);
  readonly showConfirmPassword = signal<boolean>(false);

  // TOTP Setup Wizard State
  readonly isSetupWizardOpen = signal<boolean>(false);
  readonly totpVerificationCode = signal<string>('');
  readonly copiedSecret = signal<boolean>(false);
  readonly copiedCodes = signal<boolean>(false);

  // Password Strength Indicators
  readonly hasMinLength = computed(() => this.newPassword().length >= 8);
  readonly hasUppercase = computed(() => /[A-Z]/.test(this.newPassword()));
  readonly hasLowercase = computed(() => /[a-z]/.test(this.newPassword()));
  readonly hasNumber = computed(() => /[0-9]/.test(this.newPassword()));
  readonly hasSpecial = computed(() => /[^A-Za-z0-9]/.test(this.newPassword()));

  readonly passwordScore = computed(() => {
    let score = 0;
    if (this.hasMinLength()) score++;
    if (this.hasUppercase()) score++;
    if (this.hasLowercase()) score++;
    if (this.hasNumber()) score++;
    if (this.hasSpecial()) score++;
    return score;
  });

  readonly isPasswordMatch = computed(() => {
    return (
      this.newPassword().length > 0 &&
      this.newPassword() === this.confirmPassword()
    );
  });

  readonly isPasswordFormValid = computed(() => {
    return (
      this.currentPassword().length > 0 &&
      this.passwordScore() >= 4 &&
      this.isPasswordMatch()
    );
  });

  onPasswordSubmit(): void {
    if (!this.isPasswordFormValid()) {
      this.notificationService.warning('Please satisfy all password complexity and matching requirements.');
      return;
    }

    this.passwordChange.emit({
      currentPassword: this.currentPassword(),
      newPassword: this.newPassword(),
      confirmPassword: this.confirmPassword(),
    });
  }

  resetPasswordForm(): void {
    this.currentPassword.set('');
    this.newPassword.set('');
    this.confirmPassword.set('');
  }

  startTotpSetup(): void {
    this.isSetupWizardOpen.set(true);
    this.totpVerificationCode.set('');
    this.initTotpSetup.emit();
  }

  cancelTotpSetup(): void {
    this.isSetupWizardOpen.set(false);
    this.totpVerificationCode.set('');
  }

  onVerifyTotp(): void {
    const code = this.totpVerificationCode().trim();
    if (!code || code.length !== 6) {
      this.notificationService.warning('Please enter a valid 6-digit Authenticator verification code.');
      return;
    }
    const data = this.totpSetupData();
    if (!data || !data.secret) {
      this.notificationService.error('TOTP Setup data is missing. Please restart setup.');
      return;
    }

    this.verifyTotp.emit({
      secret: data.secret,
      code,
      backupCodes: data.backupCodes,
    });
  }

  async copySecret(secret: string): Promise<void> {
    try {
      await navigator.clipboard.writeText(secret);
      this.copiedSecret.set(true);
      this.notificationService.success('Secret key copied to clipboard.');
      setTimeout(() => this.copiedSecret.set(false), 3000);
    } catch {
      this.notificationService.error('Failed to copy secret key.');
    }
  }

  async copyBackupCodes(codes: string[]): Promise<void> {
    try {
      await navigator.clipboard.writeText(codes.join('\n'));
      this.copiedCodes.set(true);
      this.notificationService.success('Backup recovery codes copied.');
      setTimeout(() => this.copiedCodes.set(false), 3000);
    } catch {
      this.notificationService.error('Failed to copy recovery codes.');
    }
  }

  async onDisableTotp(): Promise<void> {
    const confirmed = await this.notificationService.confirm({
      title: 'Disable Two-Factor Authentication',
      message: 'Are you sure you want to disable 2FA (TOTP)? Your administrative account security level will be reduced.',
      confirmText: 'Disable 2FA',
      cancelText: 'Keep Enabled',
      type: 'danger',
    });

    if (confirmed) {
      this.disableTotp.emit();
    }
  }
}
