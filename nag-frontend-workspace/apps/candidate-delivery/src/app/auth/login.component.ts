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
  selector: 'app-candidate-login',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  username = '';
  password = '';
  resetEmail = '';

  loading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  showPassword = signal<boolean>(false);
  showForgotPassword = signal<boolean>(false);

  ngOnInit(): void {
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/dashboard']);
    }
  }

  handleLogin(): void {
    if (!this.username.trim() || !this.password.trim()) {
      this.errorMessage.set('Please enter both your email/mobile and password.');
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    this.authService
      .login({
        username: this.username.trim(),
        password: this.password,
      })
      .subscribe({
        next: () => {
          this.loading.set(false);
          this.router.navigate(['/dashboard']);
        },
        error: (err) => {
          this.loading.set(false);

          // Intercept unverified accounts and redirect to verification flow
          const errData = err?.error;
          if (
            errData?.pendingVerification ||
            errData?.title === 'Account Not Verified' ||
            (errData?.detail && errData.detail.toLowerCase().includes('not yet verified'))
          ) {
            const userId = errData?.userId || '';
            const email = errData?.email || this.username.trim();
            this.router.navigate(['/verify-otp'], {
              queryParams: {
                userId,
                email,
                pending: 'true',
              },
            });
            return;
          }

          const detail =
            errData?.message ||
            errData?.detail ||
            err?.message ||
            'Invalid credentials. Please verify your email/mobile and password.';
          this.errorMessage.set(detail);
        },
      });
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
      alert('Password reset instructions have been sent to your registered email.');
    }, 1000);
  }

  togglePasswordVisibility(): void {
    this.showPassword.update((val) => !val);
  }
}
