// src/services/notificationService.ts
// Wraps notification-service REST calls and manages the SSE connection.

import { api, unwrap } from './api';
import { tokenManager } from '../utils/tokenManager';
import type { NotificationDto, Page } from '../types/api';

const BASE = '/api/v1/notifications';
const rawApiUrl = (import.meta.env.VITE_API_URL as string | undefined)?.trim();
const API_URL = rawApiUrl
  ? rawApiUrl.replace(/\/+$/, '').replace(/\/api$/, '')
  : '';

export const notificationService = {
  /** Fetch paginated notifications for the authenticated user. */
  async getNotifications(page = 0, size = 20): Promise<Page<NotificationDto>> {
    return unwrap(await api.get(BASE, { params: { page, size } }));
  },

  /** Mark a notification as read. */
  async markAsRead(notificationId: string): Promise<NotificationDto> {
    return (await api.patch<NotificationDto>(`${BASE}/${notificationId}/read`)).data;
  },

  /**
   * Opens an SSE connection to receive real-time push notifications.
   * Returns an EventSource instance — the caller must close it on unmount.
   *
   * Note: EventSource doesn't support custom headers, so the JWT is passed
   * as a query parameter. The backend must support ?token= for SSE auth.
   * Alternatively, use a BFF that forwards cookies.
   */
  openStream(onNotification: (notification: NotificationDto) => void): EventSource {
    const token = tokenManager.getAccessToken();
    const url = `${API_URL}${BASE}/stream${token ? `?token=${encodeURIComponent(token)}` : ''}`;

    const eventSource = new EventSource(url, { withCredentials: false });

    eventSource.addEventListener('notification', (event: MessageEvent) => {
      try {
        const notification: NotificationDto = JSON.parse(event.data as string);
        onNotification(notification);
      } catch {
        console.error('Failed to parse SSE notification', event.data);
      }
    });

    eventSource.onerror = () => {
      // EventSource auto-reconnects by default; log for monitoring
      console.warn('SSE connection error — browser will retry automatically');
    };

    return eventSource;
  },
};
