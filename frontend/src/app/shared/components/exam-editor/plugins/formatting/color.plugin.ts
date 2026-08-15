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

import { EditorPlugin, ToolbarButton, PluginContext, ToolbarDropdownItem } from '../editor-plugin';
import { HIGHLIGHT_COLORS, TEXT_COLORS } from '../../models';

/**
 * Plugin for text highlight and text color from a limited palette.
 */
export class ColorPlugin implements EditorPlugin {
  name = 'color';
  priority = 60;

  toolbarButtons: ToolbarButton[] = [
    {
      id: 'text-highlight',
      label: 'Highlight',
      icon: 'highlight',
      group: 'color',
      execute: () => {},
      dropdown: HIGHLIGHT_COLORS.map(c => ({
        id: `highlight-${c.key}`,
        label: c.label,
        color: c.hex,
        execute: (ctx: PluginContext) => ctx.toggleMark('highlight', c.key)
      }))
    },
    {
      id: 'text-color',
      label: 'Text Color',
      icon: 'format_color_text',
      group: 'color',
      execute: () => {},
      dropdown: TEXT_COLORS.map(c => ({
        id: `color-${c.key}`,
        label: c.label,
        color: c.hex,
        execute: (ctx: PluginContext) => ctx.toggleMark('color', c.key)
      }))
    }
  ];
}
