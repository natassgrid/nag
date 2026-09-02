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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.\n */

import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="empty-state-container">
      <div class="icon-circle">
        <mat-icon class="state-icon">{{ icon() }}</mat-icon>
      </div>
      <h3 class="state-title">{{ title() }}</h3>
      @if (description()) {
        <p class="state-desc">{{ description() }}</p>
      }
      <div class="state-content">
        <ng-content></ng-content>
      </div>
      @if (actionLabel()) {
        <button mat-flat-button color="primary" class="state-action-btn" (click)="actionClick.emit()">
          @if (actionIcon()) {
            <mat-icon>{{ actionIcon() }}</mat-icon>
          }
          {{ actionLabel() }}
        </button>
      }
    </div>
  `,
  styles: [`
    .empty-state-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 48px 24px;
      text-align: center;
      background: #FFFFFF;
      border: 1px dashed #CBD5E1;
      border-radius: 12px;
      margin: 16px 0;
    }

    .icon-circle {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 64px;
      height: 64px;
      border-radius: 50%;
      background-color: #EEF2FF;
      margin-bottom: 16px;
    }

    .state-icon {
      font-size: 32px;
      width: 32px;
      height: 32px;
      color: #4F46E5;
    }

    .state-title {
      font-size: 16px;
      font-weight: 600;
      color: #1E293B;
      margin: 0 0 8px 0;
    }

    .state-desc {
      font-size: 13.5px;
      color: #64748B;
      max-width: 420px;
      margin: 0 0 16px 0;
      line-height: 1.5;
    }

    .state-action-btn {
      margin-top: 8px;
    }
  `]
})
export class EmptyStateComponent {
  icon = input<string>('inbox');
  title = input<string>('No records found');
  description = input<string>('');
  actionLabel = input<string>('');
  actionIcon = input<string>('');

  actionClick = output<void>();
}
