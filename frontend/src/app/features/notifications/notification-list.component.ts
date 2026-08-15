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
import { MatCardModule } from '@angular/material/card';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { NotificationService, Notification } from './notification.service';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';

@Component({
  selector: 'app-notification-list',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatDividerModule,
    PageHeaderComponent
  ],
  template: `
    <div class="page-layout">
      <app-page-header
        title="Notifications"
        subtitle="View and manage your platform notifications."
        icon="notifications"
      ></app-page-header>

      <div *ngIf="notifications.length === 0" class="empty-state" role="status">
        <mat-icon class="empty-icon">notifications_none</mat-icon>
        <p>No notifications yet.</p>
      </div>

      <mat-card *ngIf="notifications.length > 0" class="notification-card">
        <mat-card-content>
          <mat-nav-list role="list" aria-label="Notification list">
            <ng-container *ngFor="let notification of notifications; let last = last">
              <mat-list-item class="notification-item"
                             [class.unread]="!notification.isRead"
                             (click)="onNotificationClick(notification)"
                             role="listitem"
                             [attr.aria-label]="notification.subject">
                <div matListItemIcon class="indicator-wrapper">
                  <span *ngIf="!notification.isRead" class="unread-dot" aria-label="Unread"></span>
                  <mat-icon>{{ getTypeIcon(notification.type) }}</mat-icon>
                </div>
                <div matListItemTitle class="notification-subject"
                     [class.bold]="!notification.isRead">
                  {{ notification.subject }}
                </div>
                <div matListItemLine class="notification-body">
                  {{ truncateBody(notification.body) }}
                </div>
                <div matListItemMeta class="notification-time">
                  {{ getRelativeTime(notification.sentAt) }}
                </div>
              </mat-list-item>

              <!-- Expanded content -->
              <div *ngIf="expandedId === notification.id" class="notification-expanded">
                <p class="expanded-body">{{ notification.body }}</p>
                <div class="expanded-meta">
                  <span class="meta-chip">{{ notification.type }}</span>
                  <span class="meta-chip">{{ notification.status }}</span>
                  <span class="meta-time">Sent: {{ notification.sentAt | date:'medium' }}</span>
                </div>
              </div>

              <mat-divider *ngIf="!last"></mat-divider>
            </ng-container>
          </mat-nav-list>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .empty-state {
      text-align: center;
      padding: 48px;
      color: #666;
    }
    .empty-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
      color: #bbb;
    }
    .notification-card {
      padding: 0;
    }
    .notification-item {
      cursor: pointer;
      transition: background-color 0.15s;
    }
    .notification-item:hover {
      background: rgba(0, 0, 0, 0.04);
    }
    .notification-item.unread {
      background: rgba(21, 101, 192, 0.04);
    }
    .indicator-wrapper {
      position: relative;
      display: flex;
      align-items: center;
    }
    .unread-dot {
      position: absolute;
      top: -2px;
      left: -4px;
      width: 8px;
      height: 8px;
      background: #1565c0;
      border-radius: 50%;
    }
    .notification-subject.bold {
      font-weight: 600;
    }
    .notification-body {
      color: #666;
      font-size: 0.85rem;
    }
    .notification-time {
      font-size: 0.75rem;
      color: #999;
      white-space: nowrap;
    }
    .notification-expanded {
      padding: 8px 24px 16px 72px;
      background: rgba(0, 0, 0, 0.02);
    }
    .expanded-body {
      margin: 0 0 8px 0;
      font-size: 0.9rem;
      line-height: 1.5;
    }
    .expanded-meta {
      display: flex;
      gap: 8px;
      align-items: center;
      flex-wrap: wrap;
    }
    .meta-chip {
      font-size: 0.7rem;
      padding: 2px 8px;
      background: #e0e0e0;
      border-radius: 12px;
      text-transform: uppercase;
    }
    .meta-time {
      font-size: 0.75rem;
      color: #999;
    }
  `]
})
export class NotificationListComponent implements OnInit {
  notifications: Notification[] = [];
  expandedId: string | null = null;

  constructor(private notificationService: NotificationService) {}

  ngOnInit(): void {
    this.notificationService.getNotifications().subscribe(data => {
      this.notifications = data || [];
    });
  }

  onNotificationClick(notification: Notification): void {
    if (this.expandedId === notification.id) {
      this.expandedId = null;
      return;
    }

    this.expandedId = notification.id;

    if (!notification.isRead) {
      this.notificationService.markAsRead(notification.id).subscribe({
        next: () => {
          notification.isRead = true;
          notification.status = 'READ';
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

  truncateBody(body: string): string {
    if (!body) return '';
    return body.length > 100 ? body.substring(0, 100) + '...' : body;
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
