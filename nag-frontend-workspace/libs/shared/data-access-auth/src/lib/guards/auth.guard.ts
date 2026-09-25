import { inject } from '@angular/core';
import { Router, CanActivateFn, UrlTree } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Functional Route Guard for protected routes.
 * Returns true if authenticated, or redirects to /login via UrlTree.
 */
export const authGuard: CanActivateFn = (): boolean | UrlTree => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  return router.createUrlTree(['/login']);
};

/**
 * Functional Root Guard for root ('') and wildcard ('**') paths.
 * Directly routes authenticated users to /dashboard and unauthenticated users to /login.
 */
export const rootGuard: CanActivateFn = (): UrlTree => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return router.createUrlTree(['/dashboard']);
  }
  return router.createUrlTree(['/login']);
};
