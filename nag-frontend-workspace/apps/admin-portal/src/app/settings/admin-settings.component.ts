import {
  ChangeDetectionStrategy,
  Component,
  computed,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { PageHeaderComponent } from '@nag-frontend-workspace/shared-ui-components';
import {
  DEFAULT_SYSTEM_SETTINGS,
  SystemSettingsState,
} from './models/settings.model';
import {
  SettingsSecurityTabComponent,
  SettingsDeliveryTabComponent,
  SettingsDpiTabComponent,
  SettingsEvaluationTabComponent,
  SettingsMaintenanceTabComponent,
} from './components';

export type SettingsTabId =
  | 'security'
  | 'delivery'
  | 'dpi'
  | 'evaluation'
  | 'maintenance';

@Component({
  selector: 'app-admin-settings',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    PageHeaderComponent,
    SettingsSecurityTabComponent,
    SettingsDeliveryTabComponent,
    SettingsDpiTabComponent,
    SettingsEvaluationTabComponent,
    SettingsMaintenanceTabComponent,
  ],
  templateUrl: './admin-settings.component.html',
  styleUrl: './admin-settings.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminSettingsComponent {
  activeTab = signal<SettingsTabId>('security');
  saving = signal<boolean>(false);

  currentSettings = signal<SystemSettingsState>({ ...DEFAULT_SYSTEM_SETTINGS });
  savedSettings = signal<SystemSettingsState>({ ...DEFAULT_SYSTEM_SETTINGS });

  readonly tabs: { id: SettingsTabId; label: string; icon: string }[] = [
    { id: 'security', label: 'Security & Auth', icon: 'security' },
    { id: 'delivery', label: 'Exam Delivery & Kiosk', icon: 'devices' },
    { id: 'dpi', label: 'DPI & AI Infra', icon: 'hub' },
    { id: 'evaluation', label: 'Evaluation & Review', icon: 'fact_check' },
    { id: 'maintenance', label: 'Cluster & Alerts', icon: 'settings_suggest' },
  ];

  readonly isDirty = computed(() => {
    return (
      JSON.stringify(this.currentSettings()) !==
      JSON.stringify(this.savedSettings())
    );
  });

  onSettingsUpdate(updated: SystemSettingsState): void {
    this.currentSettings.set(updated);
  }

  resetDefaults(): void {
    if (confirm('Are you sure you want to reset all configurations to platform defaults?')) {
      this.currentSettings.set({ ...DEFAULT_SYSTEM_SETTINGS });
    }
  }

  saveSettings(): void {
    this.saving.set(true);
    setTimeout(() => {
      this.savedSettings.set({ ...this.currentSettings() });
      this.saving.set(false);
      alert('System configuration policies successfully saved and distributed across cluster nodes.');
    }, 600);
  }
}
