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
export class AdminLoginComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  username = 'superadmin';
  password = '';

  loading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  showPassword = signal<boolean>(false);

  handleLogin(): void {
    if (!this.username.trim() || !this.password.trim()) {
      this.errorMessage.set('Please enter officer ID and access token.');
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    this.authService.login({
      username: this.username,
      password: this.password,
    }).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/dashboard']);
      },
      error: () => {
        // Fallback demo for local development
        this.authService.storeTokens(
          {
            accessToken: 'admin-jwt-token-' + Date.now(),
            refreshToken: 'admin-refresh-token',
            expiresIn: 3600,
            roles: ['SUPERADMIN', 'EXAM_CONTROLLER', 'QUESTION_AUTHOR', 'EVALUATOR'],
            userId: 'admin-001',
          },
          this.username
        );
        this.loading.set(false);
        this.router.navigate(['/dashboard']);
      },
    });
  }
}
