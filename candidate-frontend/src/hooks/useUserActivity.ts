// src/hooks/useUserActivity.ts
// Monitors user activity, manages 15-minute inactivity auto-logout,
// and proactively refreshes JWT access tokens when expiring soon without disrupting exam sessions.

import { useEffect, useRef } from 'react';
import { tokenManager } from '../utils/tokenManager';
import { authService } from '../services/authService';

const INACTIVITY_TIMEOUT_MS = 15 * 60 * 1000; // 15 minutes
const RECENT_ACTIVITY_THRESHOLD_MS = 5 * 60 * 1000; // 5 minutes
const REFRESH_THRESHOLD_SECONDS = 120; // 2 minutes
const CHECK_INTERVAL_MS = 15 * 1000; // 15 seconds

export function useUserActivity(
  isAuthenticated: boolean,
  logout: () => Promise<void>
): void {
  const lastActiveRef = useRef<number>(Date.now());
  const isRefreshingRef = useRef<boolean>(false);

  useEffect(() => {
    if (!isAuthenticated) return;

    const handleUserActivity = (): void => {
      lastActiveRef.current = Date.now();
    };

    // Throttle event listeners
    let throttleTimeout: number | null = null;
    const throttledListener = (): void => {
      if (throttleTimeout === null) {
        handleUserActivity();
        throttleTimeout = window.setTimeout(() => {
          throttleTimeout = null;
        }, 10_000);
      }
    };

    const events = ['mousemove', 'keydown', 'click', 'scroll', 'touchstart'];
    events.forEach((event) => {
      window.addEventListener(event, throttledListener, { passive: true });
    });

    const intervalId = window.setInterval(async () => {
      if (!tokenManager.isAuthenticated() && !tokenManager.getRefreshToken()) {
        return;
      }

      const now = Date.now();
      const inactiveDuration = now - lastActiveRef.current;
      const isExamRoute = window.location.pathname.includes('/take-exam');

      // 1. Inactivity auto-logout (skipped while actively taking an exam)
      if (inactiveDuration >= INACTIVITY_TIMEOUT_MS && !isExamRoute) {
        void logout();
        return;
      }

      // 2. Proactive token refresh
      // Refresh if token lifetime <= 120 seconds AND (user active within last 5 mins OR in exam)
      const remainingLifetime = tokenManager.getTokenRemainingLifetime();
      const isRecentlyActive = inactiveDuration <= RECENT_ACTIVITY_THRESHOLD_MS;

      if (
        remainingLifetime > 0 &&
        remainingLifetime <= REFRESH_THRESHOLD_SECONDS &&
        (isRecentlyActive || isExamRoute) &&
        !isRefreshingRef.current
      ) {
        isRefreshingRef.current = true;
        try {
          const tokens = await authService.refreshToken();
          tokenManager.setTokens(
            tokens.accessToken,
            tokens.refreshToken,
            tokens.expiresIn,
            tokens.userId
          );
        } catch (err) {
          console.warn('Proactive token refresh failed:', err);
        } finally {
          isRefreshingRef.current = false;
        }
      }
    }, CHECK_INTERVAL_MS);

    return () => {
      if (throttleTimeout !== null) {
        window.clearTimeout(throttleTimeout);
      }
      window.clearInterval(intervalId);
      events.forEach((event) => {
        window.removeEventListener(event, throttledListener);
      });
    };
  }, [isAuthenticated, logout]);
}
