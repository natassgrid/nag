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

import { Component, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTabsModule } from '@angular/material/tabs';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatSlideToggleModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatTabsModule,
    PageHeaderComponent
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss']
})
export class SettingsComponent {
  // Security settings signals
  readonly enforceMfa = signal<boolean>(true);
  readonly sessionTimeoutMinutes = signal<number>(30);
  readonly maxLoginAttempts = signal<number>(5);
  readonly passwordExpiryDays = signal<number>(90);

  // Delivery & Exam settings signals
  readonly enableTamperDetection = signal<boolean>(true);
  readonly enforceKioskMode = signal<boolean>(true);
  readonly heartBeatIntervalSec = signal<number>(10);
  readonly autoSaveIntervalSec = signal<number>(15);

  // Notification settings signals
  readonly notifyOnFailedLogins = signal<boolean>(true);
  readonly notifyOnExamStart = signal<boolean>(true);
  readonly alertEmailRecipients = signal<string>('sec-ops@nag.gov.in, admin@nag.gov.in');

  readonly isSaving = signal<boolean>(false);

  constructor(private notificationService: NotificationService) {}

  saveSettings(): void {
    this.isSaving.set(true);
    setTimeout(() => {
      this.isSaving.set(false);
      this.notificationService.showSuccess('System settings updated successfully');
    }, 400);
  }

  resetToDefaults(): void {
    this.enforceMfa.set(true);
    this.sessionTimeoutMinutes.set(30);
    this.maxLoginAttempts.set(5);
    this.passwordExpiryDays.set(90);
    this.enableTamperDetection.set(true);
    this.enforceKioskMode.set(true);
    this.heartBeatIntervalSec.set(10);
    this.autoSaveIntervalSec.set(15);
    this.notifyOnFailedLogins.set(true);
    this.notifyOnExamStart.set(true);
    this.notificationService.showInfo('Reset settings to system defaults');
  }
}
