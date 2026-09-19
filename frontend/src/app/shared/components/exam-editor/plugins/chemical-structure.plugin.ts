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

import { EditorPlugin, ToolbarButton, PluginContext } from './editor-plugin';
import { ChemicalStructureElement } from '../models';

/**
 * Chemical-structure plugin — Issue #126.
 *
 * Inserts a `chemical-structure` void node (SMILES notation) into the document.
 * The toolbar button opens the SMILES input modal (delegated to the host component
 * via `ctx.openSmilesInput()` extension). The modal shows:
 *   - A SMILES text input
 *   - Common quick-pick templates (benzene, cyclohexane, pyridine, …)
 *   - Real-time SmilesDrawer 2.0 SVG preview
 *
 * On confirm, `insertNode` places the `chemical-structure` void node.
 * Serialiser round-trips: chemical-structure ↔ <smiles>...</smiles>
 */
export class ChemicalStructurePlugin implements EditorPlugin {
  name = 'chemical-structure';
  priority = 25;

  toolbarButtons: ToolbarButton[] = [
    {
      id: 'insert-smiles',
      label: 'Insert Chemical Structure (SMILES)',
      icon: 'science',
      group: 'format',
      execute: (ctx: PluginContext) => {
        (ctx as any).openSmilesInput?.();
      }
    }
  ];

  /** Helper called by the host after the user confirms a SMILES string. */
  static createNode(
    smiles: string,
    title?: string,
    width = 250,
    height = 200,
    theme: 'light' | 'dark' = 'light'
  ): ChemicalStructureElement {
    return {
      type: 'chemical-structure',
      smiles: smiles.trim(),
      title: title?.trim() || undefined,
      width,
      height,
      theme,
      children: [{ text: '' }]
    };
  }

  /** Well-known SMILES quick-pick templates shown in the modal. */
  static readonly TEMPLATES: { label: string; smiles: string }[] = [
    { label: 'Benzene',        smiles: 'c1ccccc1' },
    { label: 'Cyclohexane',    smiles: 'C1CCCCC1' },
    { label: 'Pyridine',       smiles: 'c1ccncc1' },
    { label: 'Naphthalene',    smiles: 'c1ccc2ccccc2c1' },
    { label: 'Ethanol',        smiles: 'CCO' },
    { label: 'Acetic Acid',    smiles: 'CC(=O)O' },
    { label: 'Glucose',        smiles: 'OC[C@H]1OC(O)[C@H](O)[C@@H](O)[C@@H]1O' },
    { label: 'Aspirin',        smiles: 'CC(=O)Oc1ccccc1C(=O)O' },
    { label: 'Caffeine',       smiles: 'Cn1c(=O)c2c(ncn2C)n(c1=O)C' },
    { label: 'Alanine (L)',    smiles: 'N[C@@H](C)C(=O)O' },
  ];
}
