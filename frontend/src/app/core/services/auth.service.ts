import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';

export interface UserToken {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  roles: string[];
  userId: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly TOKEN_KEY = 'exam_access_token';
  private readonly REFRESH_KEY = 'exam_refresh_token';
  private readonly USER_KEY = 'exam_user';

  private isAuthenticatedSubject = new BehaviorSubject<boolean>(this.hasToken());

  isAuthenticated$ = this.isAuthenticatedSubject.asObservable();

  constructor(private http: HttpClient) {}

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_KEY);
  }

  getUserRoles(): string[] {
    const user = localStorage.getItem(this.USER_KEY);
    if (!user) return [];
    try {
      return JSON.parse(user).roles || [];
    } catch {
      return [];
    }
  }

  getUserId(): string | null {
    const user = localStorage.getItem(this.USER_KEY);
    if (!user) return null;
    try {
      return JSON.parse(user).userId || null;
    } catch {
      return null;
    }
  }

  hasToken(): boolean {
    return !!this.getToken();
  }

  hasRole(role: string): boolean {
    return this.getUserRoles().includes(role);
  }

  hasAnyRole(roles: string[]): boolean {
    const userRoles = this.getUserRoles();
    return roles.some(role => userRoles.includes(role));
  }

  storeTokens(tokenData: UserToken): void {
    localStorage.setItem(this.TOKEN_KEY, tokenData.accessToken);
    localStorage.setItem(this.REFRESH_KEY, tokenData.refreshToken);
    localStorage.setItem(this.USER_KEY, JSON.stringify({
      roles: tokenData.roles,
      userId: tokenData.userId
    }));
    this.isAuthenticatedSubject.next(true);
  }

  clearTokens(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.isAuthenticatedSubject.next(false);
  }

  login(credentials: { username: string; password: string; mfaCode?: string }): Observable<UserToken> {
    return this.http.post<UserToken>('/api/v1/identity/auth/token', credentials)
      .pipe(tap(token => this.storeTokens(token)));
  }

  register(data: { name: string; email: string; mobile: string; identityDocType: string; identityDocNumber: string }): Observable<{ registrationId: string }> {
    return this.http.post<{ registrationId: string }>('/api/v1/identity/register', data);
  }

  verifyOtp(data: { registrationId: string; otp: string }): Observable<UserToken> {
    return this.http.post<UserToken>('/api/v1/identity/otp/verify', data)
      .pipe(tap(token => this.storeTokens(token)));
  }

  logout(): void {
    this.clearTokens();
  }
}
