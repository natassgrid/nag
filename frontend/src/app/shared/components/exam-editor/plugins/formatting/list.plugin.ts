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
