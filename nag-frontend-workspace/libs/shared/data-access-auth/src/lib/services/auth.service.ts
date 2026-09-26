import {
  Injectable,
  inject,
  signal,
  computed,
} from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, throwError } from 'rxjs';
import { map, tap, finalize, shareReplay } from 'rxjs/operators';
import {
  UserToken,
  AuthUser,
  TotpSetupData,
  ValidateInviteData,
} from '../models/auth.model';

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
    if (this.currentUser) {
      this.currentUser.set(null);
    }
    if (this.isAuthenticated) {
      this.isAuthenticated.set(false);
    }
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
    // Synchronously clear local credentials to eliminate client-side race conditions
    this.clearTokens();

    if (token) {
      // Notify backend to invalidate refresh token in the background
      this.http
        .delete('/api/v1/identity/auth/logout', {
          headers: {
            Authorization: `Bearer ${token}`,
            'X-Tenant-Id': this.getTenantId(),
          },
        })
        .pipe(
          finalize(() => {
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
      if (typeof window !== 'undefined' && !window.location.pathname.includes('/login')) {
        window.location.href = '/login';
      } else {
        this.router.navigate(['/login']);
      }
    }
  }

  resendEmailOtp(payload: { userId?: string; email?: string }): Observable<any> {
    return this.http.post<{ status?: string; message?: string }>(
      '/api/v1/identity/resend/email-otp',
      payload
    );
  }

  verifyOtp(payload: {
    registrationId?: string;
    userId?: string;
    email?: string;
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
        tap((token) => {
          if (token && (token.accessToken || token.userId)) {
            this.storeTokens(token, payload.email);
          }
        })
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
      localStorage.removeItem(this.TOKEN_KEY);
      localStorage.removeItem(this.REFRESH_TOKEN_KEY);
      localStorage.removeItem(this.USER_KEY);
      return false;
    }

    const isExpired = payload.exp * 1000 < Date.now();
    if (isExpired) {
      localStorage.removeItem(this.TOKEN_KEY);
      localStorage.removeItem(this.REFRESH_TOKEN_KEY);
      localStorage.removeItem(this.USER_KEY);
      return false;
    }

    return true;
  }

  private getInitialUser(): AuthUser | null {
    if (typeof localStorage === 'undefined') return null;
    const userStr = localStorage.getItem(this.USER_KEY);
    if (!userStr) return null;
    try {
      return JSON.parse(userStr);
    } catch {
      return null;
    }
  }

  private decodeJwtPayload(token: string): any {
    try {
      const parts = token.split('.');
      if (parts.length < 2) return null;
      const base64Url = parts[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(
        atob(base64)
          .split('')
          .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join('')
      );
      return JSON.parse(jsonPayload);
    } catch {
      return null;
    }
  }
}
