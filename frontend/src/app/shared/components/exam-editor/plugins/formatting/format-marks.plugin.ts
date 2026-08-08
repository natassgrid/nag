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
