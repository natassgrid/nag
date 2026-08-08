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
