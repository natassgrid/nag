// src/services/authService.ts
// Wraps all identity-service REST calls.

import { api, unwrap } from './api';
import { tokenManager } from '../utils/tokenManager';
import type {
  AuthTokenRequest,
  AuthTokenResponse,
  ChangePasswordRequest,
  ForgotPasswordRequest,
  OtpResendRequest,
  OtpVerifyRequest,
  RegistrationRequest,
  RegistrationResponse,
  ResetPasswordRequest,
} from '../types/api';

const BASE = '/api/v1/identity';

export const authService = {
  /** Register a new candidate account. Returns userId for OTP verification. */
  async register(request: RegistrationRequest): Promise<RegistrationResponse> {
    return unwrap(await api.post(`${BASE}/register`, request));
  },

  /** Verify email/mobile OTP and activate the account. Returns JWT tokens. */
  async verifyOtp(request: OtpVerifyRequest): Promise<AuthTokenResponse> {
    return unwrap(await api.post(`${BASE}/otp/verify`, request));
  },

  /** Re-send OTP to the candidate's registered email and mobile. */
  async resendOtp(request: OtpResendRequest): Promise<void> {
    await api.post(`${BASE}/otp/resend`, request);
  },

  /** Authenticate with username/password. Returns JWT tokens. */
  async login(request: AuthTokenRequest): Promise<AuthTokenResponse> {
    return unwrap(await api.post(`${BASE}/auth/token`, request));
  },

  /** Silent token refresh using the stored refresh token. */
  async refreshToken(): Promise<AuthTokenResponse> {
    const refreshToken = tokenManager.getRefreshToken();
    if (!refreshToken) throw new Error('No refresh token available');
    return unwrap(await api.post(`${BASE}/auth/refresh`, { refreshToken }));
  },

  /** Revoke refresh token and clear local storage on logout. */
  async logout(): Promise<void> {
    const refreshToken = tokenManager.getRefreshToken();
    if (refreshToken) {
      try {
        await api.delete(`${BASE}/auth/logout`, { data: { refreshToken } });
      } catch {
        // Best-effort — clear tokens regardless
      }
    }
    tokenManager.clearTokens();
  },

  /** Change password for the authenticated candidate. */
  async changePassword(request: ChangePasswordRequest): Promise<void> {
    await api.post(`${BASE}/auth/change-password`, request);
  },

  /** Initiate forgot-password flow — sends OTP to registered email. */
  async forgotPassword(request: ForgotPasswordRequest): Promise<void> {
    await api.post(`${BASE}/auth/forgot-password`, request);
  },

  /** Complete password reset after OTP verification. */
  async resetPassword(request: ResetPasswordRequest): Promise<void> {
    await api.post(`${BASE}/auth/reset-password`, request);
  },
};
