/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap, map } from 'rxjs';

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

  getUserName(): string | null {
    const user = localStorage.getItem(this.USER_KEY);
    if (!user) return null;
    try {
      return JSON.parse(user).userName || null;
    } catch {
      return null;
    }
  }

  hasToken(): boolean {
    return !!this.getToken();
  }

  isTokenExpired(): boolean {
    const token = this.getToken();
    if (!token) return true;
    try {
      const payload = this.decodeJwtPayload(token);
      if (!payload || !payload.exp) return true;
      // exp is in seconds, Date.now() in milliseconds
      return Date.now() >= payload.exp * 1000;
    } catch {
      return true;
    }
  }

  hasRole(role: string): boolean {
    return this.getUserRoles().includes(role);
  }

  hasAnyRole(roles: string[]): boolean {
    const userRoles = this.getUserRoles();
    return roles.some(role => userRoles.includes(role));
  }

  storeTokens(tokenData: UserToken): void {
    if (tokenData && tokenData.accessToken) {
      localStorage.setItem(this.TOKEN_KEY, tokenData.accessToken);
      localStorage.setItem(this.REFRESH_KEY, tokenData.refreshToken);

      // Extract roles and userId from JWT payload
      const payload = this.decodeJwtPayload(tokenData.accessToken);
      const roles = payload?.realm_access?.roles || tokenData.roles || [];
      const userId = payload?.sub || payload?.preferred_username || tokenData.userId || '';
      const userName = payload?.name || payload?.preferred_username || tokenData.userId || '';

      localStorage.setItem(this.USER_KEY, JSON.stringify({ roles, userId, userName }));
      this.isAuthenticatedSubject.next(true);
    }
  }

  private decodeJwtPayload(token: string): any {
    try {
      const parts = token.split('.');
      if (parts.length !== 3) return null;
      const payload = atob(parts[1].replace(/-/g, '+').replace(/_/g, '/'));
      return JSON.parse(payload);
    } catch {
      return null;
    }
  }

  clearTokens(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.isAuthenticatedSubject.next(false);
  }

  login(credentials: { username: string; password: string; mfaCode?: string }): Observable<UserToken> {
    this.clearTokens();
    return this.http.post<{ status: string; data: UserToken }>('/api/v1/identity/auth/token', credentials)
      .pipe(
        map(response => response.data),
        tap(token => this.storeTokens(token))
      );
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
