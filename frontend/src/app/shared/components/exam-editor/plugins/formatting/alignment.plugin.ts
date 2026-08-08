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
