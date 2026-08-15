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

import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { PluginRegistry } from './plugins/plugin-registry';
import { PluginContext, ToolbarButton, EditorSelection } from './plugins';
import { ExamDocument, ExamElement, MarkType } from './models';

/**
 * Editor toolbar displaying buttons from all registered plugins.
 * Buttons are grouped visually with dividers between groups.
 */
@Component({
  selector: 'editor-toolbar',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatMenuModule,
    MatDividerModule
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="editor-toolbar" role="toolbar" aria-label="Text formatting">
      <ng-container *ngFor="let group of toolbarGroups; let last = last">
        <ng-container *ngFor="let button of group.buttons">

          <!-- Button with dropdown -->
          <ng-container *ngIf="button.dropdown; else simpleButton">
            <button
              mat-icon-button
              [matMenuTriggerFor]="menu"
              [matTooltip]="button.label"
              [attr.aria-label]="button.label"
              class="toolbar-btn"
            >
              <mat-icon>{{ button.icon }}</mat-icon>
            </button>
            <mat-menu #menu="matMenu">
              <button
                mat-menu-item
                *ngFor="let item of button.dropdown"
                (click)="executeDropdownItem(item)"
                [attr.aria-label]="item.label"
              >
                <mat-icon *ngIf="item.icon">{{ item.icon }}</mat-icon>
                <span
                  *ngIf="item.color"
                  class="color-swatch"
                  [style.background-color]="item.color"
                ></span>
                <span>{{ item.label }}</span>
              </button>
            </mat-menu>
          </ng-container>

          <!-- Simple toggle/action button -->
          <ng-template #simpleButton>
            <button
              mat-icon-button
              [matTooltip]="button.label + (button.shortcut ? ' (' + button.shortcut + ')' : '')"
              [attr.aria-label]="button.label"
              [class.active]="isButtonActive(button)"
              (click)="executeButton(button)"
              class="toolbar-btn"
            >
              <mat-icon>{{ button.icon }}</mat-icon>
            </button>
          </ng-template>

        </ng-container>

        <mat-divider *ngIf="!last" vertical class="toolbar-divider"></mat-divider>
      </ng-container>

      <!-- Undo / Redo -->
      <mat-divider vertical class="toolbar-divider"></mat-divider>
      <button
        mat-icon-button
        matTooltip="Undo (Ctrl+Z)"
        aria-label="Undo"
        (click)="onUndo()"
        class="toolbar-btn"
      >
        <mat-icon>undo</mat-icon>
      </button>
      <button
        mat-icon-button
        matTooltip="Redo (Ctrl+Shift+Z)"
        aria-label="Redo"
        (click)="onRedo()"
        class="toolbar-btn"
      >
        <mat-icon>redo</mat-icon>
      </button>
    </div>
  `,
  styles: [`
    .editor-toolbar {
      display: flex;
      align-items: center;
      gap: 2px;
      padding: 4px 8px;
      border-bottom: 1px solid #e0e0e0;
      background: #fafafa;
      flex-wrap: wrap;
    }
    .toolbar-btn {
      width: 32px;
      height: 32px;
      line-height: 32px;
    }
    .toolbar-btn.active {
      background-color: #e3f2fd;
      color: #1976d2;
      border-radius: 4px;
    }
    .toolbar-btn mat-icon {
      font-size: 20px;
      width: 20px;
      height: 20px;
    }
    .toolbar-divider {
      height: 24px;
      margin: 0 4px;
    }
    .color-swatch {
      display: inline-block;
      width: 16px;
      height: 16px;
      border-radius: 3px;
      margin-right: 8px;
      border: 1px solid #ccc;
      vertical-align: middle;
    }
  `]
})
export class EditorToolbarComponent {

  @Input() pluginRegistry!: PluginRegistry;
  @Input() context!: PluginContext;
  @Input() document: ExamDocument = [];
  @Input() selection: EditorSelection | null = null;

  get toolbarGroups(): { name: string; buttons: ToolbarButton[] }[] {
    const groupMap = this.pluginRegistry.getToolbarGroups();
    const groups: { name: string; buttons: ToolbarButton[] }[] = [];
    const order = ['format', 'block', 'list', 'align', 'indent', 'color', 'media'];
    for (const name of order) {
      const buttons = groupMap.get(name);
      if (buttons && buttons.length > 0) {
        groups.push({ name, buttons });
      }
    }
    return groups;
  }

  isButtonActive(button: ToolbarButton): boolean {
    if (!button.isToggle) return false;
    if (button.isActive) {
      return button.isActive(this.document, this.selection);
    }
    // Fallback: check via context for marks
    if (['bold', 'italic', 'underline', 'superscript', 'subscript'].includes(button.id)) {
      return this.context?.isMarkActive(button.id as MarkType) ?? false;
    }
    // Check block type
    if (button.id === this.context?.getActiveBlockType()) {
      return true;
    }
    // Check alignment
    if (button.id.startsWith('align-')) {
      const align = button.id.replace('align-', '');
      return this.context?.getAlignment() === align;
    }
    return false;
  }

  executeButton(button: ToolbarButton): void {
    if (this.context) {
      button.execute(this.context);
    }
  }

  executeDropdownItem(item: { execute: (ctx: PluginContext) => void }): void {
    if (this.context) {
      item.execute(this.context);
    }
  }

  onUndo(): void {
    // Emit to parent — parent calls undo()
    (this.context as any).undo?.();
  }

  onRedo(): void {
    (this.context as any).redo?.();
  }
}
