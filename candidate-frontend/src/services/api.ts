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

// ── Request interceptor: attach bearer token ──────────────────────────────────
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = tokenManager.getAccessToken();
    if (token) {
      config.headers = config.headers ?? {};
      config.headers['Authorization'] = `Bearer ${token}`;
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

    if (error.response?.status === 401 && !originalRequest._retry) {
      const refreshToken = tokenManager.getRefreshToken();

      // If no refresh token, redirect to login
      if (!refreshToken) {
        tokenManager.clearTokens();
        window.location.href = '/login';
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
          `${BASE_URL}/api/v1/identity/auth/refresh`,
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
        window.location.href = '/login';
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
