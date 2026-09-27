import {
  ChangeDetectionStrategy,
  Component,
  HostListener,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { NotificationService } from '../../notification.service';

@Component({
  selector: 'nag-confirm-dialog',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  templateUrl: './confirm-dialog.component.html',
  styleUrl: './confirm-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConfirmDialogComponent {
  readonly notificationService = inject(NotificationService);
  readonly confirmState = this.notificationService.confirmState;

  @HostListener('window:keydown.escape')
  onEscape(): void {
    if (this.confirmState()) {
      this.notificationService.resolveConfirm(false);
    }
  }

  onConfirm(): void {
    this.notificationService.resolveConfirm(true);
  }

  onCancel(): void {
    this.notificationService.resolveConfirm(false);
  }

  getIconName(type?: 'danger' | 'warning' | 'info'): string {
    switch (type) {
      case 'danger':
        return 'warning';
      case 'warning':
        return 'help_outline';
      case 'info':
      default:
        return 'info';
    }
  }

  getIconColorClasses(type?: 'danger' | 'warning' | 'info'): string {
    switch (type) {
      case 'danger':
        return 'text-rose-600 bg-rose-50';
      case 'warning':
        return 'text-amber-600 bg-amber-50';
      case 'info':
      default:
        return 'text-indigo-600 bg-indigo-50';
    }
  }

  getConfirmButtonClasses(type?: 'danger' | 'warning' | 'info'): string {
    switch (type) {
      case 'danger':
        return '!bg-rose-600 !text-white hover:!bg-rose-700';
      case 'warning':
        return '!bg-amber-600 !text-white hover:!bg-amber-700';
      case 'info':
      default:
        return '!bg-indigo-600 !text-white hover:!bg-indigo-700';
    }
  }
}
