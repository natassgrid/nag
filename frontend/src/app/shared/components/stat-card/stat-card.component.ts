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

import { Component, ChangeDetectionStrategy, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-stat-card',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <mat-card class="stat-card" [ngClass]="['border-' + variant()]">
      <div class="stat-card-body">
        <div class="stat-info">
          <span class="stat-label">{{ label() }}</span>
          <div class="stat-value-row">
            <span class="stat-value">{{ value() }}</span>
            @if (unit()) {
              <span class="stat-unit">{{ unit() }}</span>
            }
          </div>
          @if (trendText()) {
            <div class="stat-trend" [ngClass]="trendDirection()">
              <mat-icon class="trend-icon">
                {{ trendDirection() === 'up' ? 'trending_up' : trendDirection() === 'down' ? 'trending_down' : 'remove' }}
              </mat-icon>
              <span>{{ trendText() }}</span>
            </div>
          }
        </div>
        <div class="stat-icon-wrapper" [ngClass]="['icon-bg-' + variant()]">
          <mat-icon class="stat-icon">{{ icon() }}</mat-icon>
        </div>
      </div>
    </mat-card>
  `,
  styles: [`
    .stat-card {
      padding: 16px 20px;
      border-radius: 12px;
      background: #FFFFFF;
      box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.08);
      border: 1px solid #E2E8F0;
      transition: transform 0.15s ease, box-shadow 0.15s ease;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 6px 16px 0 rgba(0, 0, 0, 0.08);
      }
    }

    .stat-card-body {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
    }

    .stat-info {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .stat-label {
      font-size: 13px;
      font-weight: 500;
      color: #64748B;
      text-transform: uppercase;
      letter-spacing: 0.03em;
    }

    .stat-value-row {
      display: flex;
      align-items: baseline;
      gap: 4px;
    }

    .stat-value {
      font-size: 26px;
      font-weight: 700;
      color: #0F172A;
      line-height: 1.2;
    }

    .stat-unit {
      font-size: 14px;
      font-weight: 500;
      color: #64748B;
    }

    .stat-trend {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 12px;
      font-weight: 600;
      margin-top: 4px;

      &.up {
        color: #059669;
      }
      &.down {
        color: #DC2626;
      }
      &.neutral {
        color: #64748B;
      }

      .trend-icon {
        font-size: 16px;
        width: 16px;
        height: 16px;
      }
    }

    .stat-icon-wrapper {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 48px;
      height: 48px;
      border-radius: 12px;

      .stat-icon {
        font-size: 24px;
        width: 24px;
        height: 24px;
      }
    }

    .icon-bg-primary {
      background-color: #EEF2FF;
      color: #4F46E5;
    }

    .icon-bg-accent {
      background-color: #FFFBEB;
      color: #D97706;
    }

    .icon-bg-success {
      background-color: #ECFDF5;
      color: #059669;
    }

    .icon-bg-warn {
      background-color: #FEF2F2;
      color: #DC2626;
    }

    .icon-bg-info {
      background-color: #F0F9FF;
      color: #0284C7;
    }
  `]
})
export class StatCardComponent {
  label = input.required<string>();
  value = input.required<string | number>();
  unit = input<string>('');
  icon = input<string>('analytics');
  variant = input<'primary' | 'accent' | 'success' | 'warn' | 'info'>('primary');
  trendText = input<string>('');
  trendDirection = input<'up' | 'down' | 'neutral'>('neutral');
}
