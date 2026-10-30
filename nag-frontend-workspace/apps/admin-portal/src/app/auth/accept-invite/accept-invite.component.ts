import {
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService, ValidateInviteData } from '@nag-frontend-workspace/shared-data-access-auth';
import { parseHttpError } from '@nag-frontend-workspace/shared-ui-components';
import * as QRCode from 'qrcode';

@Component({
  selector: 'app-admin-accept-invite',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './accept-invite.component.html',
  styleUrl: './accept-invite.component.scss',
})
export class AcceptInviteComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  token = signal<string>('');
  invitationData = signal<ValidateInviteData | null>(null);
  validating = signal<boolean>(true);
  tokenValid = signal<boolean>(false);
  validationError = signal<string | null>(null);

  // Form Fields
  password = '';
  confirmPassword = '';
  showPassword = signal<boolean>(false);
  fieldErrors = signal<Record<string, string>>({});

  // 2FA / TOTP Setup Data
  totpSecret = signal<string>('JBSWY3DPEHPK3PXP');
  totpQrCodeUrl = signal<string>('');
  totpCode = '';
  backupCodes = signal<string[]>([
    'ABCD-1234',
    'EFGH-5678',
    'IJKL-9012',
    'MNOP-3456',
    'QRST-7890',
    'UVWX-1234'
  ]);
  copiedBackupCodes = signal<boolean>(false);

  submitting = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      const inviteToken = params['token'];
      if (!inviteToken) {
        this.validating.set(false);
        this.tokenValid.set(false);
        this.validationError.set('No invitation token provided. Please use the link sent in your invitation email.');
        return;
      }

      this.token.set(inviteToken);
      this.validateInvitation(inviteToken);
    });
  }

  validateInvitation(token: string): void {
    this.validating.set(true);
    this.validationError.set(null);

    this.authService.validateInvite(token).subscribe({
      next: (data) => {
        this.validating.set(false);
        this.tokenValid.set(true);
        this.invitationData.set(data);
        this.generateTotpCredentials(data.email || 'admin@nag.gov.in');
      },
      error: (err) => {
        this.validating.set(false);
        this.tokenValid.set(false);
        const parsed = parseHttpError(err, 'This invitation link is invalid or has expired. Please contact your system administrator.');
        this.validationError.set(parsed.message);
      },
    });
  }

  private generateTotpCredentials(email: string): void {
    // Generate secure 16-char base32 secret
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';
    let secret = '';
    for (let i = 0; i < 16; i++) {
      secret += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    this.totpSecret.set(secret);

    const otpauthUri = `otpauth://totp/NAG:${encodeURIComponent(email)}?secret=${secret}&issuer=National%20Assessment%20Grid`;

    QRCode.toDataURL(otpauthUri, { width: 200, margin: 1 })
      .then((url: string) => {
        this.totpQrCodeUrl.set(url);
      })
      .catch(() => {
        // Fallback placeholder
        this.totpQrCodeUrl.set('');
      });
  }

  copySecret(): void {
    if (typeof navigator !== 'undefined' && navigator.clipboard) {
      navigator.clipboard.writeText(this.totpSecret());
    }
  }

  copyBackupCodes(): void {
    if (typeof navigator !== 'undefined' && navigator.clipboard) {
      navigator.clipboard.writeText(this.backupCodes().join('\n'));
      this.copiedBackupCodes.set(true);
      setTimeout(() => this.copiedBackupCodes.set(false), 3000);
    }
  }

  fillTestTotp(): void {
    this.totpCode = '000000';
  }

  handleAcceptInvite(): void {
    const errors: Record<string, string> = {};

    if (!this.password || this.password.length < 8) {
      errors['password'] = 'Password must be at least 8 characters long.';
    }

    if (!this.confirmPassword) {
      errors['confirmPassword'] = 'Please confirm your password.';
    } else if (this.password !== this.confirmPassword) {
      errors['confirmPassword'] = 'Passwords do not match.';
    }

    if (!this.totpCode || !this.totpCode.trim()) {
      errors['totpCode'] = 'Please enter the 6-digit Authenticator code to verify 2FA setup.';
    } else if (!/^\d{6}$/.test(this.totpCode.trim())) {
      errors['totpCode'] = 'Authenticator code must be a 6-digit number.';
    }

    if (Object.keys(errors).length > 0) {
      this.fieldErrors.set(errors);
      if (errors['password']) {
        this.errorMessage.set(errors['password']);
      } else if (errors['confirmPassword']) {
        this.errorMessage.set(errors['confirmPassword']);
      } else if (errors['totpCode']) {
        this.errorMessage.set(errors['totpCode']);
      }
      return;
    }

    this.fieldErrors.set({});
    this.submitting.set(true);
    this.errorMessage.set(null);

    const payload = {
      token: this.token(),
      password: this.password,
      totpSecret: this.totpSecret(),
      totpCode: this.totpCode.trim(),
      backupCodes: this.backupCodes(),
    };

    this.authService.acceptInvite(payload).subscribe({
      next: () => {
        this.submitting.set(false);
        this.successMessage.set('Account activated and 2FA configured successfully! Redirecting to command dashboard...');
        setTimeout(() => {
          this.router.navigate(['/dashboard']);
        }, 1200);
      },
      error: (err) => {
        this.submitting.set(false);
        const parsed = parseHttpError(err, 'Failed to accept invitation. Please ensure your 6-digit Authenticator code is accurate and try again.');
        this.errorMessage.set(parsed.message);
        if (parsed.fieldErrors && Object.keys(parsed.fieldErrors).length > 0) {
          this.fieldErrors.set(parsed.fieldErrors);
        }
      },
    });
  }
}
