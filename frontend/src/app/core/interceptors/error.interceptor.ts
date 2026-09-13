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

        if (error.status === 413) {
          errorMessage = 'The uploaded file exceeds the maximum allowed size (100 MB). Please upload a smaller file.';
        } else if (error.status === 415) {
          errorMessage = 'The selected file type is not supported.';
        } else if (error.status === 0) {
          errorMessage = 'Unable to connect to the server. Please check your network connection.';
        } else if (error.status === 403) {
          errorMessage = 'Access denied. You do not have permission to perform this action.';
        } else if (error.status === 404) {
          errorMessage = (typeof error.error?.message === 'string' && error.error.message) || 'The requested resource was not found.';
        } else if (error.status >= 500) {
          errorMessage = (typeof error.error?.message === 'string' && error.error.message)
            ? error.error.message
            : 'Server error. Our team has been notified.';
        } else if (error.error) {
          if (typeof error.error === 'string') {
            errorMessage = error.error;
          } else if (error.error.message && typeof error.error.message === 'string') {
            errorMessage = error.error.message;
          } else if (error.error.detail && typeof error.error.detail === 'string') {
            errorMessage = error.error.detail;
          } else if (error.error.error && typeof error.error.error === 'string') {
            errorMessage = error.error.error;
          }
        }

        // Clean any HTML markup from error message
        if (typeof errorMessage === 'string' && /<[a-z][\s\S]*>/i.test(errorMessage)) {
          const titleMatch = errorMessage.match(/<title[^>]*>(.*?)<\/title>/i);
          const h1Match = errorMessage.match(/<h1[^>]*>(.*?)<\/h1>/i);
          if (h1Match && h1Match[1] && !h1Match[1].toLowerCase().includes('error')) {
            errorMessage = h1Match[1].replace(/\s+/g, ' ').trim();
          } else if (titleMatch && titleMatch[1]) {
            errorMessage = titleMatch[1].replace(/\s+/g, ' ').trim();
          } else {
            errorMessage = errorMessage.replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim();
          }
        }

        notificationService.showError(errorMessage);
      }

      return throwError(() => error);
    })
  );
};
