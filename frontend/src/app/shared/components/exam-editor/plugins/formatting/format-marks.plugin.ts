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
 * Plugin for inline text marks: bold, italic, underline, superscript, subscript.
 */
export class FormatMarksPlugin implements EditorPlugin {
  name = 'format-marks';
  priority = 10;

  toolbarButtons: ToolbarButton[] = [
    {
      id: 'bold',
      label: 'Bold',
      icon: 'format_bold',
      group: 'format',
      isToggle: true,
      shortcut: 'Ctrl+B',
      isActive: (_doc, _sel) => false, // Evaluated via context in component
      execute: (ctx: PluginContext) => ctx.toggleMark('bold')
    },
    {
      id: 'italic',
      label: 'Italic',
      icon: 'format_italic',
      group: 'format',
      isToggle: true,
      shortcut: 'Ctrl+I',
      isActive: (_doc, _sel) => false,
      execute: (ctx: PluginContext) => ctx.toggleMark('italic')
    },
    {
      id: 'underline',
      label: 'Underline',
      icon: 'format_underlined',
      group: 'format',
      isToggle: true,
      shortcut: 'Ctrl+U',
      isActive: (_doc, _sel) => false,
      execute: (ctx: PluginContext) => ctx.toggleMark('underline')
    },
    {
      id: 'superscript',
      label: 'Superscript',
      icon: 'superscript',
      group: 'format',
      isToggle: true,
      isActive: (_doc, _sel) => false,
      execute: (ctx: PluginContext) => ctx.toggleMark('superscript')
    },
    {
      id: 'subscript',
      label: 'Subscript',
      icon: 'subscript',
      group: 'format',
      isToggle: true,
      isActive: (_doc, _sel) => false,
      execute: (ctx: PluginContext) => ctx.toggleMark('subscript')
    }
  ];

  keyBindings: KeyBinding[] = [
    { hotkey: 'mod+b', handler: (ctx) => { ctx.toggleMark('bold'); return true; } },
    { hotkey: 'mod+i', handler: (ctx) => { ctx.toggleMark('italic'); return true; } },
    { hotkey: 'mod+u', handler: (ctx) => { ctx.toggleMark('underline'); return true; } }
  ];
}
