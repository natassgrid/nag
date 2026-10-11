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
  TotpVerifySetupRequest,
  TotpStatusData,
  ValidateInviteData,
  VerificationStatusData,
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
    return roles.some((role) => this.hasRole(role));
  }

  isSuperAdmin(): boolean {
    return this.hasRole('SUPER_ADMIN');
  }

  isQuestionAuthor(): boolean {
    return this.hasRole('QUESTION_AUTHOR');
  }

  isExamController(): boolean {
    return this.hasRole('EXAM_CONTROLLER');
  }

  isCandidate(): boolean {
    return this.hasRole('CANDIDATE');
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

  storeTokens(token: UserToken): void {
    if (typeof localStorage === 'undefined') return;

    if (token.accessToken) {
      localStorage.setItem(this.TOKEN_KEY, token.accessToken);
    }
    if (token.refreshToken) {
      localStorage.setItem(this.REFRESH_TOKEN_KEY, token.refreshToken);
    }

    const payload = token.accessToken ? this.decodeJwtPayload(token.accessToken) : null;
    const resolvedRoles: string[] =
      token.roles ||
      (payload && payload.realm_access && payload.realm_access.roles) ||
      (payload && payload.roles) ||
      [];

    const resolvedUserId: string =
      token.userId ||
      (payload && payload.sub) ||
      (payload && payload.userId) ||
      'user-unknown';

    const resolvedUsername: string =
      (payload && (payload.preferred_username || payload.username || payload.email)) ||
      'Authenticated User';

    const resolvedPreferredLanguage: string | undefined =
      (payload && (payload.preferred_language || payload.preferredLanguage)) ||
      undefined;

    const user: AuthUser = {
      userId: resolvedUserId,
      username: resolvedUsername,
      roles: resolvedRoles,
      preferredLanguage: resolvedPreferredLanguage,
    };

    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
    this.currentUser.set(user);
    this.isAuthenticated.set(true);
  }

  updatePreferredLanguage(lang: string): void {
    const current = this.currentUser();
    if (current) {
      const updated: AuthUser = { ...current, preferredLanguage: lang };
      this.currentUser.set(updated);
      if (typeof localStorage !== 'undefined') {
        localStorage.setItem(this.USER_KEY, JSON.stringify(updated));
      }
    }
  }

  login(credentials: {
    username: string;
    password?: string;
    totpCode?: string;
  }): Observable<UserToken> {
    return this.http
      .post<{ data?: UserToken } & UserToken>(
        '/api/v1/identity/auth/login',
        credentials
      )
      .pipe(
        map((response) => response.data || (response as UserToken)),
        tap((token) => this.storeTokens(token))
      );
  }

  refreshToken(): Observable<UserToken> {
    if (this.refreshTokenInProgress$) {
      return this.refreshTokenInProgress$;
    }

    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
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

  changePassword(payload: { currentPassword?: string; oldPassword?: string; newPassword: string }): Observable<void> {
    const body = {
      currentPassword: payload.currentPassword || payload.oldPassword,
      newPassword: payload.newPassword,
    };
    return this.http
      .post<{ status?: string }>('/api/v1/identity/auth/change-password', body)
      .pipe(map(() => void 0));
  }

  resendEmailOtp(payload: { userId?: string; email?: string }): Observable<any> {
    return this.http.post<{ status?: string; message?: string }>(
      '/api/v1/identity/resend/email-otp',
      payload
    );
  }

  resendSmsOtp(payload: { userId: string }): Observable<any> {
    return this.http.post<{ status?: string; message?: string }>(
      '/api/v1/identity/resend/sms-otp',
      payload
    );
  }

  verifyEmail(payload: { userId: string; otp: string }): Observable<VerificationStatusData> {
    return this.http
      .post<{ data: VerificationStatusData }>('/api/v1/identity/verify/email', payload)
      .pipe(map((res) => res.data));
  }

  verifyMobile(payload: { userId: string; otp: string }): Observable<VerificationStatusData> {
    return this.http
      .post<{ data: VerificationStatusData }>('/api/v1/identity/verify/mobile', payload)
      .pipe(map((res) => res.data));
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
        map((response) => response.data || (response as UserToken)),
        tap((token) => this.storeTokens(token))
      );
  }

  getVerificationStatus(userId: string): Observable<VerificationStatusData> {
    return this.http
      .get<{ status?: string; data: VerificationStatusData }>(
        `/api/v1/identity/verification-status?userId=${encodeURIComponent(userId)}`
      )
      .pipe(map((res) => res.data));
  }

  setupTotp(username?: string): Observable<TotpSetupData> {
    const url = username
      ? `/api/v1/identity/auth/2fa/setup?username=${encodeURIComponent(username)}`
      : '/api/v1/identity/auth/2fa/setup';
    return this.http
      .post<{ data: TotpSetupData }>(url, {})
      .pipe(map((res) => res.data));
  }

  getTotpStatus(userId?: string): Observable<TotpStatusData> {
    const url = userId
      ? `/api/v1/identity/auth/2fa/status?userId=${encodeURIComponent(userId)}`
      : '/api/v1/identity/auth/2fa/status';
    return this.http
      .get<{ data: TotpStatusData }>(url)
      .pipe(map((res) => res.data));
  }

  verifyTotpSetup(payload: TotpVerifySetupRequest): Observable<void> {
    return this.http
      .post<{ status?: string }>('/api/v1/identity/auth/2fa/verify-setup', payload)
      .pipe(map(() => void 0));
  }

  disableTotp(userId?: string): Observable<void> {
    const url = userId
      ? `/api/v1/identity/auth/2fa/disable?userId=${encodeURIComponent(userId)}`
      : '/api/v1/identity/auth/2fa/disable';
    return this.http
      .post<{ status?: string }>(url, {})
      .pipe(map(() => void 0));
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
    totpSecret?: string;
    totpCode?: string;
    backupCodes?: string[];
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

  clearTokens(): void {
    if (typeof localStorage !== 'undefined') {
      localStorage.removeItem(this.TOKEN_KEY);
      localStorage.removeItem(this.REFRESH_TOKEN_KEY);
      localStorage.removeItem(this.USER_KEY);
    }
    this.currentUser.set(null);
    this.isAuthenticated.set(false);
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
