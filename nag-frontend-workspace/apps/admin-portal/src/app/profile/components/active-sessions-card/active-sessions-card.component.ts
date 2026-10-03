import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { NotificationService } from '@nag-frontend-workspace/shared-ui-components';
import { ActiveSessionInfo } from '../../profile.model';

@Component({
  selector: 'nag-active-sessions-card',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule, MatTooltipModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './active-sessions-card.component.html',
  styleUrl: './active-sessions-card.component.scss',
})
export class ActiveSessionsCardComponent {
  private readonly notificationService = inject(NotificationService);

  readonly sessions = input.required<ActiveSessionInfo[]>();
  readonly revoking = input<boolean>(false);

  readonly revokeSession = output<string>();
  readonly revokeOtherSessions = output<void>();

  async onRevokeOther(): Promise<void> {
    const confirmed = await this.notificationService.confirm({
      title: 'Terminate Other Active Sessions',
      message:
        'Are you sure you want to invalidate all other active browser sessions? Only this current session will remain authenticated.',
      confirmText: 'Terminate All Others',
      cancelText: 'Cancel',
      type: 'danger',
    });

    if (confirmed) {
      this.revokeOtherSessions.emit();
    }
  }

  async onRevokeSingle(sessionId: string, device: string): Promise<void> {
    const confirmed = await this.notificationService.confirm({
      title: 'Terminate Session',
      message: `Are you sure you want to terminate the active session on "${device}"?`,
      confirmText: 'Terminate',
      cancelText: 'Cancel',
      type: 'danger',
    });

    if (confirmed) {
      this.revokeSession.emit(sessionId);
    }
  }

  getDeviceIcon(session: ActiveSessionInfo): string {
    const text = (session.device + ' ' + session.os).toLowerCase();
    if (text.includes('mobile') || text.includes('android') || text.includes('ios') || text.includes('phone')) {
      return 'smartphone';
    }
    if (text.includes('tablet') || text.includes('ipad')) {
      return 'tablet_mac';
    }
    return 'computer';
  }
}
