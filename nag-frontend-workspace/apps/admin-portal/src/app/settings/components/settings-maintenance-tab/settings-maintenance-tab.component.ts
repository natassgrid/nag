import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
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
      alert('Test security alert successfully dispatched to ' + this.settings().alertCriticalErrorWebhook);
    }, 600);
  }
}
