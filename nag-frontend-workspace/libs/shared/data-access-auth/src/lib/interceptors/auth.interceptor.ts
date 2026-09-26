import { inject } from '@angular/core';
import {
  HttpInterceptorFn,
  HttpRequest,
  HttpHandlerFn,
  HttpEvent,
  HttpErrorResponse,
} from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, throwError } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';

/**
 * Functional HTTP Interceptor for adding Authorization Bearer & X-Tenant-Id headers
 * and handling automatic 401 token refresh or redirect to login.
 */
export const authInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
): Observable<HttpEvent<unknown>> => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.getToken();

  const redirectToLogin = () => {
    authService.clearTokens();
    if (typeof window !== 'undefined') {
      if (!window.location.pathname.includes('/login')) {
        window.location.href = '/login';
      }
    } else {
      router.navigate(['/login']);
    }
  };

  const setHeaders: Record<string, string> = {};
  if (token && !req.headers.has('Authorization')) {
    setHeaders['Authorization'] = `Bearer ${token}`;
  }
  if (!req.headers.has('X-Tenant-Id')) {
    setHeaders['X-Tenant-Id'] = authService.getTenantId();
  }

  const modifiedReq =
    Object.keys(setHeaders).length > 0 ? req.clone({ setHeaders }) : req;

  return next(modifiedReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // 1. If 401 occurs during initial login credential submission, let component display invalid credentials message
      if (
        error.status === 401 &&
        (req.url.endsWith('/auth/token') || req.url.includes('/auth/token?'))
      ) {
        return throwError(() => error);
      }

      // 2. If 401 occurs during token refresh or logout, immediately clear tokens and redirect to login
      if (
        error.status === 401 &&
        (req.url.includes('/auth/token/refresh') || req.url.includes('/auth/logout'))
      ) {
        redirectToLogin();
        return throwError(() => error);
      }

      // 3. For any other API call returning 401:
      if (error.status === 401) {
        const refreshToken = authService.getRefreshToken();

        // If no refresh token exists, immediately move to login
        if (!refreshToken) {
          redirectToLogin();
          return throwError(() => error);
        }

        // If refresh token exists, attempt refresh
        return authService.refreshToken().pipe(
          switchMap((newToken) => {
            if (newToken?.accessToken) {
              const retryReq = req.clone({
                setHeaders: {
                  ...setHeaders,
                  Authorization: `Bearer ${newToken.accessToken}`,
                },
              });
              return next(retryReq);
            }
            redirectToLogin();
            return throwError(() => error);
          }),
          catchError((refreshErr) => {
            redirectToLogin();
            return throwError(() => refreshErr);
          })
        );
      }

      return throwError(() => error);
    })
  );
};
