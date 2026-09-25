import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '@nag-frontend-workspace/shared-data-access-auth';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterModule, MatButtonModule, MatIconModule],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private readonly router = inject(Router);
  readonly authService = inject(AuthService);

  isExamRuntime(): boolean {
    const url = this.router.url;
    return (
      url.includes('/delivery') ||
      url.includes('/login') ||
      url.includes('/register') ||
      url.includes('/verify-otp')
    );
  }

  handleLogout(): void {
    this.authService.logout();
  }
}
