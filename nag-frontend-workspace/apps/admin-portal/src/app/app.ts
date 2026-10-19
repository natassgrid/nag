import { Component, inject, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { AuthService } from '@nag-frontend-workspace/shared-data-access-auth';
import { GlobalNotificationComponent } from '@nag-frontend-workspace/shared-ui-components';

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
    MatDividerModule,
    GlobalNotificationComponent,
  ],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private readonly router = inject(Router);
  readonly authService = inject(AuthService);

  readonly mobileMenuOpen = signal<boolean>(false);

  toggleMobileMenu(): void {
    this.mobileMenuOpen.set(!this.mobileMenuOpen());
  }

  closeMobileMenu(): void {
    this.mobileMenuOpen.set(false);
  }

  readonly userInitials = computed(() => {
    const name = this.authService.userName();
    if (!name || name === 'Guest') return 'AD';
    const parts = name.trim().split(/[\s@._-]+/);
    if (parts.length >= 2 && parts[0] && parts[1]) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
  });

  isLoginPage(): boolean {
    const url = this.router.url || '';
    return url.includes('/login');
  }

  isRouteActive(prefix: string): boolean {
    const url = this.router.url || '';
    return url.startsWith(prefix);
  }

  isCurriculumActive(): boolean {
    return (
      this.isRouteActive('/questions/taxonomy') ||
      this.isRouteActive('/questions/blueprints') ||
      this.isRouteActive('/examinations') ||
      this.isRouteActive('/evaluation')
    );
  }

  isQuestionBankActive(): boolean {
    return (
      this.isRouteActive('/questions') &&
      !this.isRouteActive('/questions/taxonomy') &&
      !this.isRouteActive('/questions/blueprints')
    );
  }

  handleLogout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
