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

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatBadgeModule } from '@angular/material/badge';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { NotificationService, Notification } from '../../../features/notifications/notification.service';

@Component({
  selector: 'app-notification-panel',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatIconModule,
    MatButtonModule,
    MatBadgeModule,
    MatMenuModule,
    MatDividerModule
  ],
  template: `
    <button
      mat-icon-button
      [matMenuTriggerFor]="notificationMenu"
      aria-label="Notifications"
      class="notification-bell"
      (menuOpened)="onMenuOpened()"
    >
      <mat-icon
        [matBadge]="unreadCount > 0 ? unreadCount : null"
        matBadgeColor="warn"
        matBadgeSize="small"
      >notifications</mat-icon>
    </button>

    <mat-menu #notificationMenu="matMenu" class="notification-menu" xPosition="before">
      <div class="notification-header" (click)="$event.stopPropagation()">
        <span class="notification-title">Notifications</span>
        <span class="unread-badge" *ngIf="unreadCount > 0">{{ unreadCount }} new</span>
      </div>

      <mat-divider></mat-divider>

      <div class="notification-list" *ngIf="notifications.length > 0">
        <button
          mat-menu-item
          *ngFor="let notification of notifications.slice(0, 5)"
          class="notification-item"
          [class.unread]="!notification.isRead"
          (click)="markAsRead(notification)"
        >
          <mat-icon class="notification-type-icon">{{ getTypeIcon(notification.type) }}</mat-icon>
          <div class="notification-content">
            <span class="notification-subject">{{ notification.subject }}</span>
            <span class="notification-time">{{ getRelativeTime(notification.sentAt) }}</span>
          </div>
        </button>
      </div>

      <div class="empty-notifications" *ngIf="notifications.length === 0" (click)="$event.stopPropagation()">
        <mat-icon class="empty-icon">notifications_none</mat-icon>
        <span>No notifications</span>
      </div>

      <mat-divider></mat-divider>

      <a mat-menu-item routerLink="/notifications" class="view-all-link">
        <span>View all notifications</span>
        <mat-icon>arrow_forward</mat-icon>
      </a>
    </mat-menu>
  `,
  styles: [`
    .notification-bell {
      color: rgba(255, 255, 255, 0.9);
    }

    .notification-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 12px 16px;
    }

    .notification-title {
      font-weight: 600;
      font-size: 14px;
      color: #333;
    }

    .unread-badge {
      font-size: 11px;
      background: #536DFE;
      color: white;
      padding: 2px 8px;
      border-radius: 10px;
      font-weight: 500;
    }

    .notification-list {
      max-height: 320px;
      overflow-y: auto;
    }

    .notification-item {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      height: auto !important;
      min-height: 56px;
      padding: 8px 16px !important;
      white-space: normal !important;
      line-height: 1.4;
    }

    .notification-item.unread {
      background: rgba(83, 109, 254, 0.04);
    }

    .notification-type-icon {
      color: #666;
      margin-top: 2px;
      flex-shrink: 0;
    }

    .notification-content {
      display: flex;
      flex-direction: column;
      gap: 2px;
      overflow: hidden;
    }

    .notification-subject {
      font-size: 13px;
      color: #333;
      line-height: 1.3;
    }

    .notification-item.unread .notification-subject {
      font-weight: 600;
    }

    .notification-time {
      font-size: 11px;
      color: #999;
    }

    .empty-notifications {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 24px 16px;
      color: #999;
      gap: 8px;
    }

    .empty-icon {
      font-size: 32px;
      width: 32px;
      height: 32px;
      color: #ccc;
    }

    .view-all-link {
      display: flex;
      justify-content: space-between;
      align-items: center;
      color: #536DFE;
      font-weight: 500;
      font-size: 13px;
    }
  `]
})
export class NotificationPanelComponent implements OnInit {
  notifications: Notification[] = [];
  unreadCount = 0;

  constructor(private notificationService: NotificationService) {}

  ngOnInit(): void {
    this.loadNotifications();
  }

  onMenuOpened(): void {
    this.loadNotifications();
  }

  loadNotifications(): void {
    this.notificationService.getNotifications().subscribe(data => {
      this.notifications = data || [];
      this.unreadCount = this.notifications.filter(n => !n.isRead).length;
    });
  }

  markAsRead(notification: Notification): void {
    if (!notification.isRead) {
      this.notificationService.markAsRead(notification.id).subscribe({
        next: () => {
          notification.isRead = true;
          this.unreadCount = Math.max(0, this.unreadCount - 1);
        }
      });
    }
  }

  getTypeIcon(type: string): string {
    switch (type) {
      case 'EMAIL': return 'email';
      case 'IN_APP': return 'notifications';
      default: return 'mail_outline';
    }
  }

  getRelativeTime(dateStr: string): string {
    if (!dateStr) return '';
    const now = Date.now();
    const date = new Date(dateStr).getTime();
    const diff = now - date;

    const minutes = Math.floor(diff / 60000);
    if (minutes < 1) return 'Just now';
    if (minutes < 60) return `${minutes}m ago`;

    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}h ago`;

    const days = Math.floor(hours / 24);
    if (days < 7) return `${days}d ago`;

    return new Date(dateStr).toLocaleDateString();
  }
}
