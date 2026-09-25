import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { AuthService } from '@nag-frontend-workspace/shared-data-access-auth';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    RouterLink,
    RouterLinkActive,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
  ],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private readonly router = inject(Router);
  readonly authService = inject(AuthService);

  isLoginPage(): boolean {
    const url = this.router.url || '';
    return url.includes('/login');
  }

  isRouteActive(prefix: string): boolean {
    const url = this.router.url || '';
    return url.startsWith(prefix);
  }

  handleLogout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
