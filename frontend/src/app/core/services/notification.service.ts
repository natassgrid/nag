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

import { Injectable, inject } from '@angular/core';
import { MatSnackBar, MatSnackBarConfig } from '@angular/material/snack-bar';

export type NotificationType = 'success' | 'error' | 'warning' | 'info';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private snackBar = inject(MatSnackBar);

  private defaultConfig: MatSnackBarConfig = {
    duration: 4000,
    horizontalPosition: 'right',
    verticalPosition: 'bottom'
  };

  showSuccess(message: string, action = 'OK', duration = 3000): void {
    this.snackBar.open(message, action, {
      ...this.defaultConfig,
      duration,
      panelClass: ['success-snackbar']
    });
  }

  showError(message: string, action = 'Dismiss', duration = 5000): void {
    this.snackBar.open(message, action, {
      ...this.defaultConfig,
      duration,
      panelClass: ['error-snackbar']
    });
  }

  showWarning(message: string, action = 'Dismiss', duration = 4000): void {
    this.snackBar.open(message, action, {
      ...this.defaultConfig,
      duration,
      panelClass: ['warning-snackbar']
    });
  }

  showInfo(message: string, action = 'Close', duration = 3000): void {
    this.snackBar.open(message, action, {
      ...this.defaultConfig,
      duration,
      panelClass: ['info-snackbar']
    });
  }
}
