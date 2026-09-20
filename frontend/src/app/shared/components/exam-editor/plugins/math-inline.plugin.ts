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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

import { EditorPlugin, ToolbarButton, PluginContext } from './editor-plugin';
import { MathInlineElement } from '../models';

/**
 * Math-inline plugin — Issue #25 & #143.
 *
 * Inserts a first-class `math-inline` void node into the document.
 * The toolbar button opens a modal (delegated to the host component via
 * `ctx.openMathInput()` extension) where the user types LaTeX (math, physics, chemistry)
 * and sees a live KaTeX + mhchem preview. On confirm, `insertNode` places the void node.
 *
 * Serialiser round-trips: math-inline ↔ $$latex$$ (per AGENTS.md convention).
 */
export class MathInlinePlugin implements EditorPlugin {
  name = 'math-inline';
  priority = 20;

  toolbarButtons: ToolbarButton[] = [
    {
      id: 'insert-math',
      label: 'Insert Formula / Equation ($$...$$)',
      icon: 'functions',
      group: 'format',
      execute: (ctx: PluginContext) => {
        // Delegates to the host component's openMathInput() hook
        (ctx as any).openMathInput?.();
      }
    }
  ];

  /**
   * Helper called by the host after the user confirms a LaTeX string.
   * Creates a math-inline void node and inserts it.
   */
  static createNode(latex: string, display = false): MathInlineElement {
    return {
      type: 'math-inline',
      latex: latex.trim(),
      display,
      children: [{ text: '' }]
    };
  }
}
