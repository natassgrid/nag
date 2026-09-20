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
 * GNU标识 Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService, TotpSetupData, ValidateInviteData } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-accept-invite',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './accept-invite.component.html',
  styleUrls: ['./accept-invite.component.scss']
})
export class AcceptInviteComponent implements OnInit {

  inviteToken = signal<string>('');
  isValidating = signal<boolean>(true);
  validationError = signal<string | null>(null);
  inviteData = signal<ValidateInviteData | null>(null);
  totpData = signal<TotpSetupData | null>(null);

  isSubmitting = signal<boolean>(false);
  hidePassword = signal<boolean>(true);
  hideConfirmPassword = signal<boolean>(true);
  copiedSecret = signal<boolean>(false);
  copiedBackupCodes = signal<boolean>(false);

  form!: FormGroup;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]],
      totpCode: ['', [Validators.required, Validators.pattern('^[0-9]{6}$')]]
    }, {
      validators: this.passwordMatchValidator
    });

    this.route.queryParams.subscribe(params => {
      const token = params['token'];
      if (!token) {
        this.isValidating.set(false);
        this.validationError.set('No invitation token found in link. Please use the complete link provided in your invitation email.');
        return;
      }

      this.inviteToken.set(token);
      this.validateAndLoadSetup(token);
    });
  }

  private passwordMatchValidator(g: FormGroup) {
    const pwd = g.get('password')?.value;
    const cpwd = g.get('confirmPassword')?.value;
    return pwd === cpwd ? null : { mismatch: true };
  }

  validateAndLoadSetup(token: string): void {
    this.isValidating.set(true);
    this.validationError.set(null);

    this.authService.validateInvite(token).subscribe({
      next: (invite) => {
        this.inviteData.set(invite);
        this.loadTotpSetup();
      },
      error: (err) => {
        this.isValidating.set(false);
        const msg = err.error?.message || err.error?.error || 'Invalid or expired invitation link.';
        this.validationError.set(msg);
      }
    });
  }

  loadTotpSetup(): void {
    this.authService.setupTotp().subscribe({
      next: (setup) => {
        this.totpData.set(setup);
        this.isValidating.set(false);
      },
      error: (err) => {
        this.isValidating.set(false);
        this.notificationService.showError('Could not initialize 2FA setup. Please refresh the page.');
      }
    });
  }

  copySecret(): void {
    const secret = this.totpData()?.secret;
    if (!secret) return;
    navigator.clipboard.writeText(secret).then(() => {
      this.copiedSecret.set(true);
      setTimeout(() => this.copiedSecret.set(false), 2500);
    });
  }

  copyBackupCodes(): void {
    const codes = this.totpData()?.backupCodes;
    if (!codes || !codes.length) return;
    navigator.clipboard.writeText(codes.join('\n')).then(() => {
      this.copiedBackupCodes.set(true);
      setTimeout(() => this.copiedBackupCodes.set(false), 2500);
    });
  }

  downloadBackupCodes(): void {
    const codes = this.totpData()?.backupCodes;
    if (!codes || !codes.length) return;
    const blob = new Blob([
      'National Assessment Grid (NAG) - Admin 2FA Emergency Backup Codes\n',
      'Account: ' + (this.inviteData()?.email || '') + '\n',
      'Generated: ' + new Date().toISOString() + '\n\n',
      codes.join('\n') + '\n\n',
      'Keep these backup codes safe and confidential.'
    ], { type: 'text/plain' });

    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'nag-admin-backup-codes.txt';
    a.click();
    window.URL.revokeObjectURL(url);
  }

  getQrCodeUrl(): string {
    const uri = this.totpData()?.otpauthUri;
    if (!uri) return '';
    return `https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=${encodeURIComponent(uri)}`;
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const token = this.inviteToken();
    const totp = this.totpData();
    if (!token || !totp) return;

    this.isSubmitting.set(true);
    const { password, totpCode } = this.form.value;

    this.authService.acceptInvite({
      token,
      password,
      totpSecret: totp.secret,
      totpCode,
      backupCodes: totp.backupCodes
    }).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.notificationService.showSuccess('Account activated and 2FA configured successfully!');
        this.router.navigate(['/admin']);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        const msg = err.error?.message || err.error?.error || 'Failed to accept invitation. Please check your 6-digit TOTP code.';
        this.notificationService.showError(msg);
      }
    });
  }
}
