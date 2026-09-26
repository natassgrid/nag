import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { SystemSettingsState } from '../../models/settings.model';

@Component({
  selector: 'nag-settings-dpi-tab',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, MatSlideToggleModule],
  templateUrl: './settings-dpi-tab.component.html',
  styleUrl: './settings-dpi-tab.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SettingsDpiTabComponent {
  readonly settings = input.required<SystemSettingsState>();
  readonly settingsChange = output<SystemSettingsState>();

  update<K extends keyof SystemSettingsState>(key: K, val: SystemSettingsState[K]): void {
    this.settingsChange.emit({
      ...this.settings(),
      [key]: val,
    });
  }
}
