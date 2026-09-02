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

import { Component, ChangeDetectionStrategy, input } from '@angular/core';
import { CommonModule } from '@angular/common';

export type StatusVariant = 'success' | 'warn' | 'error' | 'info' | 'neutral' | 'primary' | 'secondary';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="status-badge" [ngClass]="['badge-' + computedVariant(), customClass()]">
      @if (showDot()) {
        <span class="badge-dot"></span>
      }
      <ng-content></ng-content>
      @if (label()) {
        {{ label() }}
      }
    </span>
  `,
  styles: [`
    .status-badge {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      font-size: 11.5px;
      font-weight: 600;
      padding: 3px 10px;
      border-radius: 9999px;
      line-height: 1.3;
      white-space: nowrap;
      text-transform: capitalize;
      letter-spacing: 0.02em;
    }

    .badge-dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background-color: currentColor;
    }

    .badge-success {
      background-color: #ECFDF5;
      color: #047857;
      border: 1px solid #A7F3D0;
    }

    .badge-warn {
      background-color: #FFFBEB;
      color: #B45309;
      border: 1px solid #FDE68A;
    }

    .badge-error {
      background-color: #FEF2F2;
      color: #B91C1C;
      border: 1px solid #FECACA;
    }

    .badge-info {
      background-color: #F0F9FF;
      color: #0369A1;
      border: 1px solid #BAE6FD;
    }

    .badge-primary {
      background-color: #EEF2FF;
      color: #4338CA;
      border: 1px solid #C7D2FE;
    }

    .badge-neutral {
      background-color: #F1F5F9;
      color: #475569;
      border: 1px solid #E2E8F0;
    }
  `]
})
export class StatusBadgeComponent {
  label = input<string>();
  variant = input<StatusVariant>('neutral');
  customClass = input<string>('');
  showDot = input<boolean>(true);

  computedVariant(): StatusVariant {
    const v = this.variant();
    return v;
  }
}
