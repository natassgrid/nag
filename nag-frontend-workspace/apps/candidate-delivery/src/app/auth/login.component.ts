import {
  Component,
  inject,
  signal,
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
export class LoginComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  username = '';
  password = '';
  resetEmail = '';

  loading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  showPassword = signal<boolean>(false);
  showForgotPassword = signal<boolean>(false);

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
          const detail =
            err?.error?.message ||
            err?.error?.detail ||
            'Authentication failed. Please check your credentials.';
          this.errorMessage.set(detail);
        },
      });
  }

  handleForgotPassword(): void {
    if (!this.resetEmail.trim()) return;
    alert(`Reset OTP dispatched to ${this.resetEmail}. Check your inbox.`);
    this.showForgotPassword.set(false);
  }
}
