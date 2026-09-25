import {
  Injectable,
  computed,
  inject,
  signal,
} from '@angular/core';
import {
  HttpClient,
  HttpInterceptorFn,
  HttpRequest,
  HttpHandlerFn,
  HttpErrorResponse,
  HttpEvent,
} from '@angular/common/http';
import { Router, CanActivateFn } from '@angular/router';
import { Observable, catchError, finalize, map, shareReplay, switchMap, tap, throwError } from 'rxjs';

export interface UserToken {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  roles: string[];
  userId: string;
}

export interface AuthUser {
  userId: string;
  userName: string;
  roles: string[];
  email?: string;
  tenantId?: string;
}

export interface TotpSetupData {
  secret: string;
  otpauthUri: string;
  issuer: string;
  username: string;
  backupCodes: string[];
}

export interface ValidateInviteData {
  valid: boolean;
  email: string;
  fullName: string;
  roles: string[];
  tenantId: string;
  expiresAt: string;
  message: string;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly TOKEN_KEY = 'exam_access_token';
  private readonly REFRESH_KEY = 'exam_refresh_token';
  private readonly USER_KEY = 'exam_user';
  private readonly TENANT_KEY = 'exam_tenant_id';

  readonly isAuthenticated = signal<boolean>(this.checkInitialAuth());
  readonly currentUser = signal<AuthUser | null>(this.getInitialUser());
  readonly userRoles = computed(() => this.currentUser()?.roles ?? []);
  readonly userName = computed(() => this.currentUser()?.userName ?? 'Guest');

  private refreshTokenInProgress$: Observable<UserToken> | null = null;

  getToken(): string | null {
    if (typeof localStorage === 'undefined') return null;
    return localStorage.getItem(this.TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    if (typeof localStorage === 'undefined') return null;
    return localStorage.getItem(this.REFRESH_KEY);
  }

  getTenantId(): string {
    if (typeof localStorage !== 'undefined') {
      const stored = localStorage.getItem(this.TENANT_KEY);
      if (stored) return stored;
    }
    return this.currentUser()?.tenantId || 'default';
  }

  setTenantId(tenantId: string): void {
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(this.TENANT_KEY, tenantId);
    }
    const current = this.currentUser();
    if (current) {
      const updated = { ...current, tenantId };
      this.currentUser.set(updated);
      localStorage.setItem(this.USER_KEY, JSON.stringify(updated));
    }
  }

  hasRole(role: string): boolean {
    return this.userRoles().includes(role);
  }

  hasAnyRole(roles: string[]): boolean {
    const current = this.userRoles();
    return roles.some((role) => current.includes(role));
  }

  storeTokens(tokenData: UserToken, fallbackUsername?: string): void {
    if (typeof localStorage === 'undefined' || !tokenData?.accessToken) return;

    localStorage.setItem(this.TOKEN_KEY, tokenData.accessToken);
    if (tokenData.refreshToken) {
      localStorage.setItem(this.REFRESH_KEY, tokenData.refreshToken);
    }

    const payload = this.decodeJwtPayload(tokenData.accessToken);
    const roles: string[] = payload?.realm_access?.roles || tokenData.roles || [];
    const userId: string = payload?.sub || tokenData.userId || '';

    let userName = fallbackUsername || payload?.preferred_username || payload?.name || payload?.email || userId;
    if (userName.includes('@')) {
      userName = userName.split('@')[0];
    }

    const userObj: AuthUser = {
      userId,
      userName,
      roles,
      email: payload?.email,
      tenantId: this.getTenantId(),
    };

    localStorage.setItem(this.USER_KEY, JSON.stringify(userObj));
    this.currentUser.set(userObj);
    this.isAuthenticated.set(true);
  }

  clearTokens(): void {
    if (typeof localStorage !== 'undefined') {
      localStorage.removeItem(this.TOKEN_KEY);
      localStorage.removeItem(this.REFRESH_KEY);
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
    const user = this.currentUser();
    if (user?.userId) {
      this.http.delete('/api/v1/identity/auth/logout').subscribe({
        error: () => {
          /* ignore */
        },
      });
    }
    this.clearTokens();
    this.router.navigate(['/login']);
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
        `/api/v1/identity/admin/invite/validate?token=${encodeURIComponent(token)}`
      )
      .pipe(map((res) => res.data));
  }

  acceptInvite(payload: {
    token: string;
    password: string;
    totpSecret: string;
    totpCode: string;
    backupCodes?: string[];
  }): Observable<UserToken> {
    return this.http
      .post<{ data: UserToken }>('/api/v1/identity/admin/invite/accept', payload)
      .pipe(
        map((res) => res.data),
        tap((token) => this.storeTokens(token))
      );
  }

  private checkInitialAuth(): boolean {
    if (typeof localStorage === 'undefined') return false;
    return !!localStorage.getItem(this.TOKEN_KEY);
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
 * and handling automatic 401 token refresh.
 */
export const authInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
): Observable<HttpEvent<unknown>> => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.getToken();

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
      if (error.status === 401 && !req.url.includes('/auth/token')) {
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
            return throwError(() => error);
          }),
          catchError((refreshErr) => {
            authService.clearTokens();
            router.navigate(['/login']);
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
 */
export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }
  return router.createUrlTree(['/login']);
};

/**
 * Functional Role Guard factory for role-restricted routes.
 */
export function roleGuard(allowedRoles: string[]): CanActivateFn {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    if (!authService.isAuthenticated()) {
      return router.createUrlTree(['/login']);
    }

    if (authService.hasAnyRole(allowedRoles)) {
      return true;
    }

    return router.createUrlTree(['/unauthorized']);
  };
}
