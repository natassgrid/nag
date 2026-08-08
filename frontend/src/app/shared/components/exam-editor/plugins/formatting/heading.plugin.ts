import { EditorPlugin, ToolbarButton, KeyBinding, PluginContext, ToolbarDropdownItem } from '../editor-plugin';
import { BlockType } from '../../models';

/**
 * Plugin for heading blocks (H1, H2, H3) and paragraph.
 */
export class HeadingPlugin implements EditorPlugin {
  name = 'heading';
  priority = 20;

  toolbarButtons: ToolbarButton[] = [
    {
      id: 'block-type',
      label: 'Block Type',
      icon: 'title',
      group: 'block',
      isActive: (_doc, _sel) => false,
      execute: () => {},
      dropdown: [
        {
          id: 'paragraph',
          label: 'Paragraph',
          icon: 'subject',
          execute: (ctx: PluginContext) => ctx.toggleBlock('paragraph')
        },
        {
          id: 'heading-one',
          label: 'Heading 1',
          icon: 'looks_one',
          execute: (ctx: PluginContext) => ctx.toggleBlock('heading-one')
        },
        {
          id: 'heading-two',
          label: 'Heading 2',
          icon: 'looks_two',
          execute: (ctx: PluginContext) => ctx.toggleBlock('heading-two')
        },
        {
          id: 'heading-three',
          label: 'Heading 3',
          icon: 'looks_3',
          execute: (ctx: PluginContext) => ctx.toggleBlock('heading-three')
        }
      ]
    }
  ];

  keyBindings: KeyBinding[] = [
    { hotkey: 'mod+alt+1', handler: (ctx) => { ctx.toggleBlock('heading-one'); return true; } },
    { hotkey: 'mod+alt+2', handler: (ctx) => { ctx.toggleBlock('heading-two'); return true; } },
    { hotkey: 'mod+alt+3', handler: (ctx) => { ctx.toggleBlock('heading-three'); return true; } },
    { hotkey: 'mod+alt+0', handler: (ctx) => { ctx.toggleBlock('paragraph'); return true; } }
  ];
}
