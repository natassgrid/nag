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
  templateUrl: './editor-toolbar.component.html',
  styleUrls: ['./editor-toolbar.component.scss']
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
