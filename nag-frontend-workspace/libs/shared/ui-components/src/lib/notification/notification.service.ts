import { Injectable, signal } from '@angular/core';
import { ConfirmDialogOptions, ToastMessage, ToastType } from './notification.model';

@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  private readonly _toasts = signal<ToastMessage[]>([]);
  readonly toasts = this._toasts.asReadonly();

  private readonly _confirmState = signal<{
    options: ConfirmDialogOptions;
    resolve: (val: boolean) => void;
  } | null>(null);
  readonly confirmState = this._confirmState.asReadonly();

  show(type: ToastType, title: string, message?: string, duration = 4000): string {
    const id = `toast-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`;
    const toast: ToastMessage = {
      id,
      type,
      title,
      message,
      duration,
      createdAt: Date.now(),
    };

    this._toasts.update((current) => [...current, toast]);

    if (duration > 0) {
      setTimeout(() => {
        this.removeToast(id);
      }, duration);
    }

    return id;
  }

  success(title: string, message?: string, duration = 4000): string {
    return this.show('success', title, message, duration);
  }

  error(title: string, message?: string, duration = 6000): string {
    return this.show('error', title, message, duration);
  }

  warning(title: string, message?: string, duration = 5000): string {
    return this.show('warning', title, message, duration);
  }

  info(title: string, message?: string, duration = 4000): string {
    return this.show('info', title, message, duration);
  }

  removeToast(id: string): void {
    this._toasts.update((current) => current.filter((t) => t.id !== id));
  }

  confirm(options: ConfirmDialogOptions): Promise<boolean> {
    return new Promise<boolean>((resolve) => {
      this._confirmState.set({
        options: {
          confirmText: 'Confirm',
          cancelText: 'Cancel',
          type: 'danger',
          ...options,
        },
        resolve,
      });
    });
  }

  resolveConfirm(result: boolean): void {
    const current = this._confirmState();
    if (current) {
      current.resolve(result);
      this._confirmState.set(null);
    }
  }
}
