/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, MatIconModule, PageHeaderComponent],
  template: `
    <div class="page-layout">
      <app-page-header
        title="System Settings"
        subtitle="Manage system-wide configuration and platform preferences."
        icon="settings"
      ></app-page-header>

      <div class="placeholder">
        <mat-icon class="placeholder-icon">settings</mat-icon>
        <p>System settings coming soon.</p>
      </div>
    </div>
  `,
  styles: [`
    .placeholder {
      text-align: center;
      padding: 48px;
      color: #666;
    }
    .placeholder-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
      color: #bbb;
    }
  `]
})
export class SettingsComponent {}
