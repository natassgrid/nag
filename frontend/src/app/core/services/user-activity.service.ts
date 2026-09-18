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

import { Injectable, NgZone, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { fromEvent, merge, Subscription, interval } from 'rxjs';
import { throttleTime } from 'rxjs/operators';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class UserActivityService implements OnDestroy {
  /** Inactivity timeout in milliseconds (15 minutes) */
  private readonly INACTIVITY_TIMEOUT_MS = 15 * 60 * 1000;

  /** Max allowed inactive time for proactive refresh (5 minutes) */
  private readonly RECENT_ACTIVITY_THRESHOLD_MS = 5 * 60 * 1000;

  /** Token threshold to trigger proactive refresh (2 minutes in seconds) */
  private readonly TOKEN_REFRESH_THRESHOLD_SECONDS = 120;

  /** Check interval in milliseconds (15 seconds) */
  private readonly CHECK_INTERVAL_MS = 15 * 1000;

  private lastActiveTimestamp = Date.now();
  private eventSubscription?: Subscription;
  private timerSubscription?: Subscription;
  private isRefreshing = false;

  constructor(
    private authService: AuthService,
    private router: Router,
    private ngZone: NgZone
  ) {}

  /**
   * Initializes user activity monitoring and periodic proactive refresh checks.
   */
  public init(): void {
    if (this.eventSubscription) {
      return;
    }

    this.recordActivity();

    // Listen to user interaction outside NgZone to prevent unnecessary Angular change detection cycles
    this.ngZone.runOutsideAngular(() => {
      const mousemove$ = fromEvent(window, 'mousemove');
      const keydown$ = fromEvent(window, 'keydown');
      const click$ = fromEvent(window, 'click');
      const scroll$ = fromEvent(window, 'scroll');
      const touchstart$ = fromEvent(window, 'touchstart');

      this.eventSubscription = merge(mousemove$, keydown$, click$, scroll$, touchstart$)
        .pipe(throttleTime(10000))
        .subscribe(() => {
          this.recordActivity();
        });

      // Periodic check for inactivity timeout and proactive token refresh
      this.timerSubscription = interval(this.CHECK_INTERVAL_MS).subscribe(() => {
        this.checkSessionStatus();
      });
    });
  }

  public recordActivity(): void {
    this.lastActiveTimestamp = Date.now();
  }

  public getLastActiveTimestamp(): number {
    return this.lastActiveTimestamp;
  }

  private checkSessionStatus(): void {
    if (!this.authService.hasToken()) {
      return;
    }

    const now = Date.now();
    const inactiveDuration = now - this.lastActiveTimestamp;

    // 1. Inactivity auto-logout check (15 minutes)
    if (inactiveDuration >= this.INACTIVITY_TIMEOUT_MS) {
      this.ngZone.run(() => {
        this.authService.logout();
        this.router.navigate(['/auth/login'], {
          queryParams: { sessionExpired: 'inactive' }
        });
      });
      return;
    }

    // 2. Proactive refresh check
    // If token expiring within 2 minutes AND user was active within last 5 minutes
    const remainingSeconds = this.authService.getTokenRemainingLifetimeSeconds();
    const isRecentActive = inactiveDuration <= this.RECENT_ACTIVITY_THRESHOLD_MS;

    if (remainingSeconds > 0 && remainingSeconds <= this.TOKEN_REFRESH_THRESHOLD_SECONDS && isRecentActive && !this.isRefreshing) {
      this.isRefreshing = true;
      this.ngZone.run(() => {
        this.authService.refreshToken().subscribe({
          next: () => {
            this.isRefreshing = false;
          },
          error: () => {
            this.isRefreshing = false;
          }
        });
      });
    }
  }

  ngOnDestroy(): void {
    if (this.eventSubscription) {
      this.eventSubscription.unsubscribe();
    }
    if (this.timerSubscription) {
      this.timerSubscription.unsubscribe();
    }
  }
}
