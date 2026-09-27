import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { NotificationService } from '../../notification.service';
import { ToastMessage } from '../../notification.model';

@Component({
  selector: 'nag-toast-container',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './toast-container.component.html',
  styleUrl: './toast-container.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ToastContainerComponent {
  readonly notificationService = inject(NotificationService);
  readonly toasts = this.notificationService.toasts;

  getToastIcon(type: ToastMessage['type']): string {
    switch (type) {
      case 'success':
        return 'check_circle';
      case 'error':
        return 'error';
      case 'warning':
        return 'warning';
      case 'info':
      default:
        return 'info';
    }
  }

  getToastClasses(type: ToastMessage['type']): string {
    switch (type) {
      case 'success':
        return 'bg-white border-emerald-300 text-slate-800 shadow-lg shadow-emerald-500/10';
      case 'error':
        return 'bg-white border-rose-300 text-slate-800 shadow-lg shadow-rose-500/10';
      case 'warning':
        return 'bg-white border-amber-300 text-slate-800 shadow-lg shadow-amber-500/10';
      case 'info':
      default:
        return 'bg-white border-indigo-300 text-slate-800 shadow-lg shadow-indigo-500/10';
    }
  }

  getIconClasses(type: ToastMessage['type']): string {
    switch (type) {
      case 'success':
        return 'text-emerald-600 bg-emerald-50';
      case 'error':
        return 'text-rose-600 bg-rose-50';
      case 'warning':
        return 'text-amber-600 bg-amber-50';
      case 'info':
      default:
        return 'text-indigo-600 bg-indigo-50';
    }
  }

  getBarClasses(type: ToastMessage['type']): string {
    switch (type) {
      case 'success':
        return 'bg-emerald-500';
      case 'error':
        return 'bg-rose-500';
      case 'warning':
        return 'bg-amber-500';
      case 'info':
      default:
        return 'bg-indigo-500';
    }
  }
}
