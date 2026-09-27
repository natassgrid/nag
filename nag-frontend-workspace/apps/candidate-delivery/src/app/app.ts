import { Component, inject, signal, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, NavigationEnd } from '@angular/router';
import { filter, Subscription } from 'rxjs';
import { AuthService } from '@nag-frontend-workspace/shared-data-access-auth';
import { GlobalNotificationComponent } from '@nag-frontend-workspace/shared-ui-components';
import {
  CandidateHeaderComponent,
  CandidateDrawerComponent,
} from './layout';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    GlobalNotificationComponent,
    CandidateHeaderComponent,
    CandidateDrawerComponent,
  ],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App implements OnInit, OnDestroy {
  private readonly router = inject(Router);
  readonly authService = inject(AuthService);

  readonly isDrawerOpen = signal<boolean>(false);
  private navSub?: Subscription;

  ngOnInit(): void {
    this.navSub = this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe(() => {
        this.closeDrawer();
      });
  }

  ngOnDestroy(): void {
    this.navSub?.unsubscribe();
  }

  isExamRuntime(): boolean {
    const url = this.router.url || '';
    return (
      url.includes('/delivery') ||
      url.includes('/login') ||
      url.includes('/register') ||
      url.includes('/verify-otp')
    );
  }

  toggleDrawer(): void {
    this.isDrawerOpen.update((v) => !v);
  }

  closeDrawer(): void {
    this.isDrawerOpen.set(false);
  }

  handleLogout(): void {
    this.authService.logout();
  }
}
