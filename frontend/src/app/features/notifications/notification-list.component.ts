import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { NotificationService, Notification } from './notification.service';

@Component({
  selector: 'app-notification-list',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatDividerModule
  ],
  template: `
    <div class="notifications-container">
      <h1 class="page-title">Notifications</h1>

      <div *ngIf="isLoading" class="loading-container" role="status" aria-live="polite">
        <mat-spinner diameter="48"></mat-spinner>
        <p>Loading notifications...</p>
      </div>

      <div *ngIf="errorMessage && !isLoading" class="error-message" role="alert">
        <mat-icon>error_outline</mat-icon>
        <span>{{ errorMessage }}</span>
      </div>

      <div *ngIf="!isLoading && !errorMessage && notifications.length === 0" class="empty-state" role="status">
        <mat-icon class="empty-icon">notifications_none</mat-icon>
        <p>No notifications yet.</p>
      </div>

      <mat-card *ngIf="!isLoading && notifications.length > 0" class="notification-card">
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
    .notifications-container {
      padding: 24px;
      max-width: 800px;
      margin: 0 auto;
    }
    .page-title {
      font-size: 1.5rem;
      font-weight: 500;
      margin-bottom: 24px;
    }
    .loading-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 48px;
      gap: 16px;
    }
    .error-message {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 16px;
      color: #d32f2f;
      background: #fdecea;
      border-radius: 8px;
    }
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
  isLoading = true;
  errorMessage = '';
  expandedId: string | null = null;

  constructor(private notificationService: NotificationService) {}

  ngOnInit(): void {
    this.notificationService.getNotifications().subscribe({
      next: (data) => {
        this.notifications = data || [];
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Failed to load notifications.';
      }
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
        },
        error: () => {
          // Silently fail — still expand the notification
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
