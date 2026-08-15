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

import { EditorPlugin, ToolbarButton, PluginContext } from '../editor-plugin';

/**
 * Plugin for embedding media assets (image, audio, video) via Asset Service.
 *
 * Media is referenced by assetId only — no Base64, no external URLs,
 * no inline binary data. The Asset Service resolves metadata and URLs.
 */
export class MediaPlugin implements EditorPlugin {
  name = 'media';
  priority = 70;

  toolbarButtons: ToolbarButton[] = [
    {
      id: 'insert-image',
      label: 'Insert Image',
      icon: 'image',
      group: 'media',
      execute: (ctx: PluginContext) => {
        // The component will open an asset picker dialog
        // and call ctx.insertAsset(assetId, 'image') on selection
        (ctx as any).openAssetPicker?.('image');
      }
    },
    {
      id: 'insert-audio',
      label: 'Insert Audio',
      icon: 'audiotrack',
      group: 'media',
      execute: (ctx: PluginContext) => {
        (ctx as any).openAssetPicker?.('audio');
      }
    },
    {
      id: 'insert-video',
      label: 'Insert Video',
      icon: 'videocam',
      group: 'media',
      execute: (ctx: PluginContext) => {
        (ctx as any).openAssetPicker?.('video');
      }
    }
  ];
}
