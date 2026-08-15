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

import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { tap, catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

function generateRequestId(): string {
  return crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(36).substring(2, 11)}`;
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.getToken();

  let headers = req.headers
    .set('X-Request-Id', generateRequestId())
    .set('X-Tenant-Id', 'exam-authority-1')
    .set('Accept-Language', navigator.language || 'en');

  if (token && token !== 'undefined' && token !== 'null') {
    // Don't send auth token on unauthenticated (public) endpoints
    const isUnauthenticated = req.url.includes('/identity/auth/') ||
                              req.url.includes('/identity/register') ||
                              req.url.includes('/identity/otp/') ||
                              req.url.includes('/actuator/');
    if (!isUnauthenticated) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }
  }

  const clonedReq = req.clone({ headers });
  return next(clonedReq).pipe(
    catchError(error => {
      if (error.status === 401) {
        // Token expired or invalid — clear session and redirect to login
        authService.logout();
        router.navigate(['/auth/login']);
      }
      return throwError(() => error);
    })
  );
};
