import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastContainerComponent } from './components/toast-container/toast-container.component';
import { ConfirmDialogComponent } from './components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'nag-global-notification',
  standalone: true,
  imports: [CommonModule, ToastContainerComponent, ConfirmDialogComponent],
  template: `
    <nag-toast-container />
    <nag-confirm-dialog />
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GlobalNotificationComponent {}
