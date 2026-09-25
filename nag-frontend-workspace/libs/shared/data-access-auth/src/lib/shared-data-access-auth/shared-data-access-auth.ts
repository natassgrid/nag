import {
  Injectable,
  inject,
  signal,
  computed,
} from '@angular/core';
import {
  HttpClient,
  HttpInterceptorFn,
  HttpRequest,
  HttpHandlerFn,
  HttpEvent,
  HttpErrorResponse,
} from '@angular/common/http';
import { Router, CanActivateFn, UrlTree } from '@angular/router';
import { Observable, throwError } from 'rxjs';
import { catchError, map, tap, switchMap, finalize, shareReplay } from 'rxjs/operators';

export interface UserToken {
  accessToken: string;
  refreshToken?: string;
  expiresIn?: number;
  tokenType?: string;
  roles?: string[];
  userId?: string;
}

export interface AuthUser {
  userId: string;
  username: string;
  roles: string[];
}

export interface TotpSetupData {
  secretKey: string;
  qrCodeUrl: string;
}

export interface ValidateInviteData {
  invitationId: string;
  email: string;
  fullName: string;
  assignedRoles: string[];
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly TOKEN_KEY = 'nag_access_token';
  private readonly REFRESH_TOKEN_KEY = 'nag_refresh_token';
  private readonly USER_KEY = 'nag_auth_user';
  private readonly TENANT_KEY = 'nag_tenant_id';

  readonly currentUser = signal<AuthUser | null>(this.getInitialUser());
  readonly isAuthenticated = signal<boolean>(this.checkInitialAuth());
  readonly userRoles = computed(() => this.currentUser()?.roles ?? []);
  readonly userName = computed(() => this.currentUser()?.username ?? 'Guest');

  private refreshTokenInProgress$: Observable<UserToken> | null = null;

  hasRole(role: string): boolean {
    return this.userRoles().includes(role);
  }

  hasAnyRole(roles: string[]): boolean {
    const current = this.userRoles();
    return roles.some((r) => current.includes(r));
  }

  getToken(): string | null {
    if (typeof localStorage === 'undefined') return null;
    return localStorage.getItem(this.TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    if (typeof localStorage === 'undefined') return null;
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  getTenantId(): string {
    if (typeof localStorage === 'undefined') return 'default';
    return localStorage.getItem(this.TENANT_KEY) || 'default';
  }

  setTenantId(tenantId: string): void {
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(this.TENANT_KEY, tenantId);
    }
  }

  storeTokens(token: UserToken, username?: string): void {
    if (typeof localStorage === 'undefined') return;

    if (token.accessToken) {
      localStorage.setItem(this.TOKEN_KEY, token.accessToken);
    }
    if (token.refreshToken) {
      localStorage.setItem(this.REFRESH_TOKEN_KEY, token.refreshToken);
    }

    const payload = token.accessToken
      ? this.decodeJwtPayload(token.accessToken)
      : null;

    const user: AuthUser = {
      userId: token.userId || payload?.sub || 'user-unknown',
      username: username || payload?.preferred_username || payload?.sub || 'user',
      roles:
        token.roles ||
        payload?.realm_access?.roles ||
        payload?.roles ||
        [],
    };

    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
    this.currentUser.set(user);
    this.isAuthenticated.set(true);
  }

  clearTokens(): void {
    if (typeof localStorage !== 'undefined') {
      localStorage.removeItem(this.TOKEN_KEY);
      localStorage.removeItem(this.REFRESH_TOKEN_KEY);
      localStorage.removeItem(this.USER_KEY);
    }
    this.currentUser.set(null);
    this.isAuthenticated.set(false);
  }

  login(credentials: {
    username: string;
    password: string;
    otpCode?: string;
    deviceFingerprint?: string;
  }): Observable<UserToken> {
    this.clearTokens();
    const payload = { ...credentials };
    return this.http
      .post<{ status?: string; data?: UserToken } & UserToken>(
        '/api/v1/identity/auth/token',
        payload
      )
      .pipe(
        map((res) => res.data || (res as UserToken)),
        tap((token) => this.storeTokens(token, credentials.username))
      );
  }

  refreshToken(): Observable<UserToken> {
    if (this.refreshTokenInProgress$) {
      return this.refreshTokenInProgress$;
    }

    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      this.clearTokens();
      return throwError(() => new Error('No refresh token available'));
    }

    this.refreshTokenInProgress$ = this.http
      .post<{ status?: string; data?: UserToken } & UserToken>(
        '/api/v1/identity/auth/token/refresh',
        { refreshToken }
      )
      .pipe(
        map((response) => response.data || (response as UserToken)),
        tap((token) => this.storeTokens(token)),
        finalize(() => {
          this.refreshTokenInProgress$ = null;
        }),
        shareReplay(1)
      );

    return this.refreshTokenInProgress$;
  }

  logout(): void {
    const token = this.getToken();
    if (token) {
      // Send DELETE /auth/logout while the token is still active
      this.http
        .delete('/api/v1/identity/auth/logout', {
          headers: {
            Authorization: `Bearer ${token}`,
            'X-Tenant-Id': this.getTenantId(),
          },
        })
        .pipe(
          finalize(() => {
            this.clearTokens();
            if (typeof window !== 'undefined' && !window.location.pathname.includes('/login')) {
              window.location.href = '/login';
            } else {
              this.router.navigate(['/login']);
            }
          })
        )
        .subscribe({
          error: () => {
            /* ignore network/backend errors on logout */
          },
        });
    } else {
      this.clearTokens();
      if (typeof window !== 'undefined' && !window.location.pathname.includes('/login')) {
        window.location.href = '/login';
      } else {
        this.router.navigate(['/login']);
      }
    }
  }

  verifyOtp(payload: {
    registrationId?: string;
    userId?: string;
    mobile?: string;
    otp: string;
  }): Observable<UserToken> {
    return this.http
      .post<{ status?: string; data?: UserToken } & UserToken>(
        '/api/v1/identity/otp/verify',
        payload
      )
      .pipe(
        map((res) => res.data || (res as UserToken)),
        tap((token) => this.storeTokens(token))
      );
  }

  setupTotp(): Observable<TotpSetupData> {
    return this.http
      .post<{ data: TotpSetupData }>('/api/v1/identity/auth/2fa/setup', {})
      .pipe(map((res) => res.data));
  }

  validateInvite(token: string): Observable<ValidateInviteData> {
    return this.http
      .get<{ data: ValidateInviteData }>(
        `/api/v1/identity/invitations/validate?token=${encodeURIComponent(token)}`
      )
      .pipe(map((res) => res.data));
  }

  acceptInvite(payload: {
    token: string;
    password?: string;
  }): Observable<UserToken> {
    return this.http
      .post<{ data: UserToken }>('/api/v1/identity/invitations/accept', payload)
      .pipe(
        map((res) => res.data),
        tap((token) => this.storeTokens(token))
      );
  }

  private checkInitialAuth(): boolean {
    if (typeof localStorage === 'undefined') return false;
    const token = localStorage.getItem(this.TOKEN_KEY);
    if (!token) return false;

    // Validate JWT structure and expiry
    const payload = this.decodeJwtPayload(token);
    if (!payload || !payload.exp) {
      this.clearTokens();
      return false;
    }

    const nowSec = Math.floor(Date.now() / 1000);
    if (payload.exp <= nowSec) {
      this.clearTokens();
      return false;
    }

    return true;
  }

  private getInitialUser(): AuthUser | null {
    if (typeof localStorage === 'undefined') return null;
    const raw = localStorage.getItem(this.USER_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as AuthUser;
    } catch {
      return null;
    }
  }

  private decodeJwtPayload(token: string): any {
    try {
      const parts = token.split('.');
      if (parts.length !== 3) return null;
      let base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
      const pad = base64.length % 4;
      if (pad === 2) base64 += '==';
      else if (pad === 3) base64 += '=';
      const jsonStr = decodeURIComponent(
        atob(base64)
          .split('')
          .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join('')
      );
      return JSON.parse(jsonStr);
    } catch {
      return null;
    }
  }
}

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

  const modifiedReq = Object.keys(setHeaders).length > 0
    ? req.clone({ setHeaders })
    : req;

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
