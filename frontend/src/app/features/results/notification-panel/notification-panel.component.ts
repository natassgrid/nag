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
  template: `
    <mat-card class="notification-panel" role="region" aria-labelledby="notification-heading">
      <mat-card-header>
        <mat-card-title id="notification-heading">
          <mat-icon [matBadge]="unreadCount" matBadgeColor="warn"
                    [matBadgeHidden]="unreadCount === 0">notifications</mat-icon>
          Notifications
        </mat-card-title>
      </mat-card-header>
      <mat-card-content>
        <mat-nav-list aria-label="Notification list" role="log" aria-live="polite">
          <mat-list-item *ngFor="let notification of notifications"
                         [class.unread]="!notification.read"
                         (click)="markRead(notification)"
                         role="listitem"
                         [attr.aria-label]="notification.title + ': ' + notification.message">
            <mat-icon matListItemIcon [class]="'type-' + notification.type">
              {{ getIcon(notification.type) }}
            </mat-icon>
            <div matListItemTitle>{{ notification.title }}</div>
            <div matListItemLine>{{ notification.message }}</div>
            <div matListItemMeta>{{ notification.timestamp | date:'short' }}</div>
          </mat-list-item>
        </mat-nav-list>

        <div *ngIf="notifications.length === 0" class="empty-state" role="status">
          No notifications yet.
        </div>
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    .notification-panel { max-width: 360px; width: 100%; max-height: 500px; overflow-y: auto; }
    .unread { font-weight: 500; background: rgba(21, 101, 192, 0.04); }
    .type-info { color: var(--color-primary); }
    .type-success { color: var(--color-success); }
    .type-warning { color: var(--color-warning); }
    .type-error { color: var(--color-error); }
    .empty-state { text-align: center; padding: var(--spacing-lg); color: var(--color-text-secondary); }
    @media (max-width: 320px) {
      .notification-panel { max-width: 100%; }
    }
  `]
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
