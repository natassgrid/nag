// src/utils/tokenManager.ts
// Manages JWT access and refresh tokens in localStorage.
// For production, prefer httpOnly cookies via a BFF pattern.

const ACCESS_TOKEN_KEY = 'nag_access_token';
const REFRESH_TOKEN_KEY = 'nag_refresh_token';
const USER_ID_KEY = 'nag_user_id';
const EXPIRES_AT_KEY = 'nag_token_expires_at';

const UUID_REGEX = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/;

export const tokenManager = {
  setTokens(accessToken: string, refreshToken?: string, expiresIn?: number, userId?: string): void {
    const expiresAt = Date.now() + (expiresIn || 3600) * 1000;
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    if (refreshToken) {
      localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
    }
    localStorage.setItem(EXPIRES_AT_KEY, String(expiresAt));

    // Resolve userId from argument or JWT payload claims (must be valid UUID)
    let resolvedId = userId;
    if (!resolvedId || !UUID_REGEX.test(resolvedId)) {
      const payload = tokenManager.decodePayload();
      const sub = (payload?.sub as string) || '';
      const uid = (payload?.userId as string) || (payload?.id as string) || '';
      resolvedId = UUID_REGEX.test(uid) ? uid : UUID_REGEX.test(sub) ? sub : '';
    }
    if (resolvedId && UUID_REGEX.test(resolvedId)) {
      localStorage.setItem(USER_ID_KEY, resolvedId);
    }
  },

  getAccessToken(): string | null {
    return localStorage.getItem(ACCESS_TOKEN_KEY);
  },

  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  },

  getUserId(): string | null {
    const id = localStorage.getItem(USER_ID_KEY);
    if (id && UUID_REGEX.test(id)) {
      return id;
    }
    // Fallback: decode directly from JWT access token payload
    const payload = tokenManager.decodePayload();
    const uid = (payload?.userId as string) || (payload?.id as string) || '';
    const sub = (payload?.sub as string) || '';
    const valid = UUID_REGEX.test(uid) ? uid : UUID_REGEX.test(sub) ? sub : null;
    if (valid) {
      localStorage.setItem(USER_ID_KEY, valid);
      return valid;
    }
    return null;
  },

  isAccessTokenExpired(): boolean {
    const expiresAt = localStorage.getItem(EXPIRES_AT_KEY);
    if (!expiresAt) return true;
    // Treat as expired 30s before actual expiry (buffer for refresh)
    return Date.now() > Number(expiresAt) - 30_000;
  },

  isAuthenticated(): boolean {
    const token = tokenManager.getAccessToken();
    return !!token && !tokenManager.isAccessTokenExpired();
  },

  clearTokens(): void {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(USER_ID_KEY);
    localStorage.removeItem(EXPIRES_AT_KEY);
    // Also clear legacy mock keys from the prototype
    localStorage.removeItem('nag_candidate_auth');
    localStorage.removeItem('nag_candidate_verified');
    localStorage.removeItem('nag_candidate_user');
    localStorage.removeItem('nag_candidate_otp_sent');
  },

  /**
   * Decodes the JWT payload without verifying the signature.
   * Verification happens server-side; this is for reading claims on the client.
   */
  decodePayload(): Record<string, unknown> | null {
    const token = tokenManager.getAccessToken();
    if (!token) return null;
    try {
      const base64Url = token.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      return JSON.parse(atob(base64));
    } catch {
      return null;
    }
  },
};
