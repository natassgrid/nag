import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  NotificationService,
  PageHeaderComponent,
} from '@nag-frontend-workspace/shared-ui-components';
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
import { AdminSettingsService } from './services/admin-settings.service';

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
export class AdminSettingsComponent implements OnInit {
  private readonly settingsService = inject(AdminSettingsService);
  private readonly notificationService = inject(NotificationService);

  activeTab = signal<SettingsTabId>('security');
  loading = signal<boolean>(false);
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

  ngOnInit(): void {
    this.loadSettings();
  }

  loadSettings(): void {
    this.loading.set(true);
    this.settingsService.getSettings().subscribe({
      next: (data) => {
        this.currentSettings.set({ ...data });
        this.savedSettings.set({ ...data });
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.notificationService.error('Failed to load system settings');
      },
    });
  }

  onSettingsUpdate(updated: SystemSettingsState): void {
    this.currentSettings.set(updated);
  }

  async resetDefaults(): Promise<void> {
    const confirmed = await this.notificationService.confirm({
      title: 'Reset Configuration to Defaults?',
      message:
        'Are you sure you want to reset all configurations to platform defaults? Any unsaved changes across tabs will be reverted.',
      confirmText: 'Reset Defaults',
      cancelText: 'Cancel',
      type: 'warning',
    });

    if (confirmed) {
      this.saving.set(true);
      this.settingsService.resetDefaults().subscribe({
        next: (defaults) => {
          this.currentSettings.set({ ...defaults });
          this.savedSettings.set({ ...defaults });
          this.saving.set(false);
          this.notificationService.success(
            'Configurations Reset',
            'System configurations have been reset to platform defaults.'
          );
        },
        error: () => {
          this.saving.set(false);
          this.notificationService.error(
            'Reset Failed',
            'Failed to reset system configuration policies.'
          );
        },
      });
    }
  }

  saveSettings(): void {
    this.saving.set(true);
    this.settingsService.saveSettings(this.currentSettings()).subscribe({
      next: (saved) => {
        this.savedSettings.set({ ...saved });
        this.saving.set(false);
        this.notificationService.success(
          'Settings Saved',
          'System configuration policies successfully saved and distributed across cluster nodes.'
        );
      },
      error: () => {
        this.saving.set(false);
        this.notificationService.error(
          'Save Failed',
          'Failed to save system settings.'
        );
      },
    });
  }
}
