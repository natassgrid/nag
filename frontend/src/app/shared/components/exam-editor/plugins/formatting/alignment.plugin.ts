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

import { EditorPlugin, ToolbarButton, KeyBinding, PluginContext, ToolbarDropdownItem } from '../editor-plugin';

/**
 * Plugin for text alignment (left, center, right, justify).
 */
export class AlignmentPlugin implements EditorPlugin {
  name = 'alignment';
  priority = 40;

  toolbarButtons: ToolbarButton[] = [
    {
      id: 'align-left',
      label: 'Align Left',
      icon: 'format_align_left',
      group: 'align',
      isToggle: true,
      isActive: (_doc, _sel) => false,
      execute: (ctx: PluginContext) => ctx.setAlignment('left')
    },
    {
      id: 'align-center',
      label: 'Align Center',
      icon: 'format_align_center',
      group: 'align',
      isToggle: true,
      isActive: (_doc, _sel) => false,
      execute: (ctx: PluginContext) => ctx.setAlignment('center')
    },
    {
      id: 'align-right',
      label: 'Align Right',
      icon: 'format_align_right',
      group: 'align',
      isToggle: true,
      isActive: (_doc, _sel) => false,
      execute: (ctx: PluginContext) => ctx.setAlignment('right')
    },
    {
      id: 'align-justify',
      label: 'Justify',
      icon: 'format_align_justify',
      group: 'align',
      isToggle: true,
      isActive: (_doc, _sel) => false,
      execute: (ctx: PluginContext) => ctx.setAlignment('justify')
    }
  ];

  keyBindings: KeyBinding[] = [
    { hotkey: 'mod+shift+l', handler: (ctx) => { ctx.setAlignment('left'); return true; } },
    { hotkey: 'mod+shift+e', handler: (ctx) => { ctx.setAlignment('center'); return true; } },
    { hotkey: 'mod+shift+r', handler: (ctx) => { ctx.setAlignment('right'); return true; } },
    { hotkey: 'mod+shift+j', handler: (ctx) => { ctx.setAlignment('justify'); return true; } }
  ];
}
