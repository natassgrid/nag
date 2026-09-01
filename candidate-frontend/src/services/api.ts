// src/services/api.ts
// Central Axios instance with request/response interceptors.
// Attaches JWT bearer token and X-Tenant-Id to every request.
// Auto-refreshes token on 401 response.

import axios, { type AxiosInstance, type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios';
import { tokenManager } from '../utils/tokenManager';

const rawBaseUrl = (import.meta.env.VITE_API_URL as string | undefined)?.trim();
const BASE_URL = rawBaseUrl
  ? rawBaseUrl.replace(/\/+$/, '').replace(/\/api$/, '')
  : '';
const TENANT_ID = import.meta.env.VITE_TENANT_ID ?? 'default';

// Track whether a token refresh is already in progress to avoid race conditions
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (error: unknown) => void;
}> = [];

function processQueue(error: unknown, token: string | null = null): void {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token!);
    }
  });
  failedQueue = [];
}

export const api: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 15_000,
  headers: {
    'Content-Type': 'application/json',
    'X-Tenant-Id': TENANT_ID,
  },
});

// ── Request interceptor: attach bearer token only to protected endpoints ──────
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const url = config.url || '';
    // Public/unauthenticated endpoints that MUST NOT include old or expired Bearer tokens
    const isPublicAuthEndpoint =
      url.includes('/identity/auth/token') ||
      url.includes('/identity/auth/refresh') ||
      url.includes('/identity/auth/forgot-password') ||
      url.includes('/identity/auth/reset-password') ||
      url.includes('/identity/register') ||
      url.includes('/identity/otp/') ||
      url.includes('/actuator/');

    if (!isPublicAuthEndpoint) {
      const token = tokenManager.getAccessToken();
      if (token && token !== 'undefined' && token !== 'null') {
        config.headers = config.headers ?? {};
        config.headers['Authorization'] = `Bearer ${token}`;
      }
    } else {
      if (config.headers?.['Authorization']) {
        delete config.headers['Authorization'];
      }
    }
    return config;
  },
  (error) => Promise.reject(error),
);

// ── Response interceptor: handle 401 with silent refresh ──────────────────────
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config as AxiosRequestConfig & { _retry?: boolean };

    // Do NOT intercept 401s on auth/login/registration endpoints — let the caller handle and display the error
    const url = originalRequest?.url || '';
    const isAuthEndpoint =
      url.includes('/identity/auth/token') ||
      url.includes('/identity/auth/refresh') ||
      url.includes('/identity/auth/forgot-password') ||
      url.includes('/identity/auth/reset-password') ||
      url.includes('/identity/register') ||
      url.includes('/identity/otp/');

    if (error.response?.status === 401 && !isAuthEndpoint && !originalRequest._retry) {
      const refreshToken = tokenManager.getRefreshToken();

      // If no refresh token, clear tokens and redirect only if not already on /login
      if (!refreshToken) {
        tokenManager.clearTokens();
        if (window.location.pathname !== '/login' && window.location.pathname !== '/register') {
          window.location.href = '/login';
        }
        return Promise.reject(error);
      }

      if (isRefreshing) {
        // Queue this request until the token is refreshed
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then((token) => {
          originalRequest.headers = {
            ...(originalRequest.headers ?? {}),
            Authorization: `Bearer ${token}`,
          };
          return api(originalRequest);
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const { data } = await axios.post(
          `${BASE_URL}/api/v1/identity/auth/refresh` as string,
          { refreshToken },
          { headers: { 'X-Tenant-Id': TENANT_ID } },
        );

        const { accessToken, refreshToken: newRefresh, expiresIn, userId } = data.data;
        tokenManager.setTokens(accessToken, newRefresh, expiresIn, userId);
        processQueue(null, accessToken);

        originalRequest.headers = {
          ...(originalRequest.headers ?? {}),
          Authorization: `Bearer ${accessToken}`,
        };
        return api(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError, null);
        tokenManager.clearTokens();
        if (window.location.pathname !== '/login') {
          window.location.href = '/login';
        }
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  },
);

/** Helper to extract the `data` field from an ApiResponse<T> body. */
export function unwrap<T>(response: { data: { data: T } }): T {
  return response.data.data;
}
