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

import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatBadgeModule } from '@angular/material/badge';
import { MatButtonModule } from '@angular/material/button';

interface Notification {
  id: string;
  title: string;
  message: string;
  timestamp: string;
  read: boolean;
  type: 'info' | 'success' | 'warning' | 'error';
}

@Component({
  selector: 'app-notification-panel',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatListModule,
    MatIconModule,
    MatBadgeModule,
    MatButtonModule
  ],
  templateUrl: './notification-panel.component.html',
  styleUrls: ['./notification-panel.component.scss']
})
export class NotificationPanelComponent implements OnInit, OnDestroy {
  notifications: Notification[] = [];
  unreadCount = 0;
  private eventSource: EventSource | null = null;

  ngOnInit(): void {
    this.connectSSE();
  }

  ngOnDestroy(): void {
    this.eventSource?.close();
  }

  private connectSSE(): void {
    try {
      this.eventSource = new EventSource('/api/v1/notifications/stream');

      this.eventSource.onmessage = (event) => {
        const notification: Notification = JSON.parse(event.data);
        this.notifications.unshift(notification);
        this.updateUnreadCount();
      };

      this.eventSource.onerror = () => {
        // Reconnect after delay
        this.eventSource?.close();
        setTimeout(() => this.connectSSE(), 5000);
      };
    } catch {
      // SSE not supported or failed to connect
    }
  }

  markRead(notification: Notification): void {
    notification.read = true;
    this.updateUnreadCount();
  }

  private updateUnreadCount(): void {
    this.unreadCount = this.notifications.filter(n => !n.read).length;
  }

  getIcon(type: string): string {
    switch (type) {
      case 'success': return 'check_circle';
      case 'warning': return 'warning';
      case 'error': return 'error';
      default: return 'info';
    }
  }
}
