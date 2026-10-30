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
import { parseHttpError } from '@nag-frontend-workspace/shared-ui-components';

@Component({
  selector: 'app-admin-login',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './admin-login.component.html',
  styleUrl: './admin-login.component.scss',
})
export class AdminLoginComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  username = 'superadmin';
  password = '';
  otpCode = '';

  loading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  fieldErrors = signal<Record<string, string>>({});
  showPassword = signal<boolean>(false);
  mfaStepRequired = signal<boolean>(false);

  ngOnInit(): void {
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/dashboard']);
    }
  }

  handleLogin(): void {
    const errors: Record<string, string> = {};

    if (!this.mfaStepRequired()) {
      if (!this.username || !this.username.trim()) {
        errors['username'] = 'Officer ID / Email is required.';
      }
      if (!this.password || !this.password.trim()) {
        errors['password'] = 'Access token / Password is required.';
      }

      if (Object.keys(errors).length > 0) {
        this.fieldErrors.set(errors);
        this.errorMessage.set('Please enter officer ID and access credentials.');
        return;
      }
    } else {
      if (!this.otpCode || !this.otpCode.trim()) {
        errors['otpCode'] = 'Please enter your 6-digit Authenticator OTP code.';
      } else if (this.otpCode.trim().length < 6 || this.otpCode.trim().length > 16) {
        errors['otpCode'] = 'Authenticator OTP code must be between 6 and 16 characters.';
      }

      if (Object.keys(errors).length > 0) {
        this.fieldErrors.set(errors);
        this.errorMessage.set('Please enter your 6-digit Authenticator OTP code.');
        return;
      }
    }

    this.fieldErrors.set({});
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
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading.set(false);

        const msg = err?.error?.message || err?.error?.detail || err?.message || '';
        if (
          err.status === 403 &&
          (msg.toLowerCase().includes('mfa') ||
           msg.toLowerCase().includes('2fa') ||
           msg.toLowerCase().includes('authenticator') ||
           msg.toLowerCase().includes('otp'))
        ) {
          this.mfaStepRequired.set(true);
          this.errorMessage.set('Command Clearance: Two-Factor Authentication required. Enter the 6-digit code from your Authenticator app.');
          return;
        }

        const parsed = parseHttpError(err, 'Authentication failed. Please verify your officer credentials.');
        this.errorMessage.set(parsed.message);
        if (parsed.fieldErrors && Object.keys(parsed.fieldErrors).length > 0) {
          this.fieldErrors.set(parsed.fieldErrors);
        }
      },
    });
  }

  cancelMfa(): void {
    this.mfaStepRequired.set(false);
    this.otpCode = '';
    this.errorMessage.set(null);
    this.fieldErrors.set({});
  }

  togglePasswordVisibility(): void {
    this.showPassword.update((val) => !val);
  }
}
