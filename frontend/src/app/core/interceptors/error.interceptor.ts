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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.\n */

import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { NotificationService } from '../services/notification.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const notificationService = inject(NotificationService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // 401 is handled by authInterceptor (redirects to login)
      // Check if caller requested to skip global error notification via custom header
      const skipGlobalNotification = req.headers.has('X-Skip-Error-Notification');

      if (!skipGlobalNotification && error.status !== 401) {
        let errorMessage = 'An unexpected error occurred. Please try again.';

        if (error.error) {
          if (typeof error.error === 'string') {
            errorMessage = error.error;
          } else if (error.error.message) {
            errorMessage = error.error.message;
          } else if (error.error.detail) {
            errorMessage = error.error.detail;
          } else if (error.error.error) {
            errorMessage = error.error.error;
          }
        }

        if (error.status === 0) {
          errorMessage = 'Unable to connect to the server. Please check your network connection.';
        } else if (error.status === 403) {
          errorMessage = 'Access denied. You do not have permission to perform this action.';
        } else if (error.status === 404) {
          errorMessage = error.error?.message || 'The requested resource was not found.';
        } else if (error.status >= 500) {
          errorMessage = error.error?.message || 'Server error. Our team has been notified.';
        }

        notificationService.showError(errorMessage);
      }

      return throwError(() => error);
    })
  );
};
