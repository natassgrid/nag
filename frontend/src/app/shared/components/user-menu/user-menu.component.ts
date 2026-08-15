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

import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';

@Component({
  selector: 'app-user-menu',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    MatDividerModule
  ],
  template: `
    <button
      mat-button
      [matMenuTriggerFor]="userMenu"
      class="user-menu-trigger"
      aria-label="User menu"
    >
      <mat-icon class="avatar-icon">account_circle</mat-icon>
      <span class="user-display-name">{{ userName }}</span>
      <mat-icon class="dropdown-arrow">arrow_drop_down</mat-icon>
    </button>

    <mat-menu #userMenu="matMenu" xPosition="before">
      <div class="user-menu-header" (click)="$event.stopPropagation()">
        <mat-icon class="menu-avatar">account_circle</mat-icon>
        <div class="menu-user-info">
          <span class="menu-user-name">{{ userName }}</span>
          <span class="menu-user-role">{{ primaryRole }}</span>
        </div>
      </div>

      <mat-divider></mat-divider>

      <a mat-menu-item routerLink="/profile">
        <mat-icon>person</mat-icon>
        <span>Profile</span>
      </a>

      <a mat-menu-item routerLink="/preferences">
        <mat-icon>settings</mat-icon>
        <span>Preferences</span>
      </a>

      <mat-divider></mat-divider>

      <button mat-menu-item (click)="onSignOut()">
        <mat-icon>logout</mat-icon>
        <span>Sign Out</span>
      </button>
    </mat-menu>
  `,
  styles: [`
    .user-menu-trigger {
      display: flex;
      align-items: center;
      gap: 6px;
      color: rgba(255, 255, 255, 0.9);
      text-transform: none;
      font-weight: 400;
      padding: 4px 8px;
      border-radius: 24px;
      min-width: auto;
    }

    .user-menu-trigger:hover {
      background: rgba(255, 255, 255, 0.1);
    }

    .avatar-icon {
      font-size: 28px;
      width: 28px;
      height: 28px;
    }

    .user-display-name {
      font-size: 14px;
      max-width: 120px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .dropdown-arrow {
      font-size: 18px;
      width: 18px;
      height: 18px;
      margin-left: -4px;
    }

    .user-menu-header {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px 16px;
    }

    .menu-avatar {
      font-size: 40px;
      width: 40px;
      height: 40px;
      color: #536DFE;
    }

    .menu-user-info {
      display: flex;
      flex-direction: column;
    }

    .menu-user-name {
      font-weight: 600;
      font-size: 14px;
      color: #333;
    }

    .menu-user-role {
      font-size: 12px;
      color: #888;
      text-transform: capitalize;
    }
  `]
})
export class UserMenuComponent {
  @Input() userName = 'User';
  @Input() primaryRole = '';
  @Output() signOut = new EventEmitter<void>();

  onSignOut(): void {
    this.signOut.emit();
  }
}
