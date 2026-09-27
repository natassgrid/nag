import {
  ChangeDetectionStrategy,
  Component,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { NotificationService } from '@nag-frontend-workspace/shared-ui-components';
import { SystemSettingsState } from '../../models/settings.model';

@Component({
  selector: 'nag-settings-maintenance-tab',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatIconModule,
    MatButtonModule,
    MatSlideToggleModule,
  ],
  templateUrl: './settings-maintenance-tab.component.html',
  styleUrl: './settings-maintenance-tab.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SettingsMaintenanceTabComponent {
  private readonly notificationService = inject(NotificationService);

  readonly settings = input.required<SystemSettingsState>();
  readonly settingsChange = output<SystemSettingsState>();

  readonly testingWebhook = signal<boolean>(false);

  update<K extends keyof SystemSettingsState>(key: K, val: SystemSettingsState[K]): void {
    this.settingsChange.emit({
      ...this.settings(),
      [key]: val,
    });
  }

  testWebhook(): void {
    this.testingWebhook.set(true);
    setTimeout(() => {
      this.testingWebhook.set(false);
      this.notificationService.success(
        'Webhook Dispatched',
        `Test security alert successfully dispatched to ${this.settings().alertCriticalErrorWebhook}`
      );
    }, 600);
  }
}
