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
