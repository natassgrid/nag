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
 * Plugin for indentation (increase/decrease, max 3 levels).
 */
export class IndentPlugin implements EditorPlugin {
  name = 'indent';
  priority = 50;

  toolbarButtons: ToolbarButton[] = [
    {
      id: 'indent-increase',
      label: 'Increase Indent',
      icon: 'format_indent_increase',
      group: 'indent',
      execute: (ctx: PluginContext) => ctx.increaseIndent()
    },
    {
      id: 'indent-decrease',
      label: 'Decrease Indent',
      icon: 'format_indent_decrease',
      group: 'indent',
      execute: (ctx: PluginContext) => ctx.decreaseIndent()
    }
  ];

  keyBindings: KeyBinding[] = [
    { hotkey: 'tab', handler: (ctx) => { ctx.increaseIndent(); return true; } },
    { hotkey: 'shift+tab', handler: (ctx) => { ctx.decreaseIndent(); return true; } }
  ];
}
