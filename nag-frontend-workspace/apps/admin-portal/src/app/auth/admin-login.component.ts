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
  showPassword = signal<boolean>(false);
  mfaStepRequired = signal<boolean>(false);

  ngOnInit(): void {
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/dashboard']);
    }
  }

  handleLogin(): void {
    if (!this.username.trim() || !this.password.trim()) {
      this.errorMessage.set('Please enter officer ID and access credentials.');
      return;
    }

    if (this.mfaStepRequired() && !this.otpCode.trim()) {
      this.errorMessage.set('Please enter your 6-digit Authenticator OTP code.');
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

        const detail =
          err?.error?.message ||
          err?.error?.detail ||
          err?.message ||
          'Authentication failed. Please verify your officer credentials.';
        this.errorMessage.set(detail);
      },
    });
  }

  cancelMfa(): void {
    this.mfaStepRequired.set(false);
    this.otpCode = '';
    this.errorMessage.set(null);
  }

  togglePasswordVisibility(): void {
    this.showPassword.update((val) => !val);
  }
}
