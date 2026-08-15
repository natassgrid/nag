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

import { EditorPlugin, ToolbarButton, KeyBinding, PluginContext } from '../editor-plugin';

/**
 * Plugin for numbered and bulleted lists.
 */
export class ListPlugin implements EditorPlugin {
  name = 'list';
  priority = 30;

  toolbarButtons: ToolbarButton[] = [
    {
      id: 'numbered-list',
      label: 'Numbered List',
      icon: 'format_list_numbered',
      group: 'list',
      isToggle: true,
      shortcut: 'Ctrl+Shift+7',
      isActive: (_doc, _sel) => false,
      execute: (ctx: PluginContext) => ctx.toggleBlock('numbered-list')
    },
    {
      id: 'bulleted-list',
      label: 'Bulleted List',
      icon: 'format_list_bulleted',
      group: 'list',
      isToggle: true,
      shortcut: 'Ctrl+Shift+8',
      isActive: (_doc, _sel) => false,
      execute: (ctx: PluginContext) => ctx.toggleBlock('bulleted-list')
    }
  ];

  keyBindings: KeyBinding[] = [
    { hotkey: 'mod+shift+7', handler: (ctx) => { ctx.toggleBlock('numbered-list'); return true; } },
    { hotkey: 'mod+shift+8', handler: (ctx) => { ctx.toggleBlock('bulleted-list'); return true; } }
  ];
}
