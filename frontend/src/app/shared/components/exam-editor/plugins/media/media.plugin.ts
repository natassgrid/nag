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
