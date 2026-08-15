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

import { ExamElement, ExamText, BlockType, MarkType } from '../models';

/**
 * Toolbar button definition exposed by a plugin.
 */
export interface ToolbarButton {
  /** Unique identifier */
  id: string;
  /** Tooltip label */
  label: string;
  /** Material icon name */
  icon: string;
  /** Button group for visual grouping in toolbar */
  group: 'format' | 'block' | 'list' | 'align' | 'indent' | 'media' | 'history' | 'color';
  /** Whether this is a toggle (active/inactive state) */
  isToggle?: boolean;
  /** Whether the button is currently active */
  isActive?: (document: ExamElement[], selection: EditorSelection | null) => boolean;
  /** Execute the action */
  execute: (context: PluginContext) => void;
  /** Optional dropdown items (e.g., heading levels, colors) */
  dropdown?: ToolbarDropdownItem[];
  /** Keyboard shortcut label for tooltip */
  shortcut?: string;
}

export interface ToolbarDropdownItem {
  id: string;
  label: string;
  icon?: string;
  color?: string;
  execute: (context: PluginContext) => void;
}

/**
 * Selection state passed to plugins.
 */
export interface EditorSelection {
  anchorPath: number[];
  anchorOffset: number;
  focusPath: number[];
  focusOffset: number;
  isCollapsed: boolean;
}

/**
 * Context provided to plugin actions for document manipulation.
 */
export interface PluginContext {
  /** Current document state */
  document: ExamElement[];
  /** Current selection */
  selection: EditorSelection | null;
  /** Apply a new document state */
  setDocument: (doc: ExamElement[]) => void;
  /** Set selection */
  setSelection: (sel: EditorSelection | null) => void;
  /** Focus the editor */
  focus: () => void;
  /** Insert a node at the current selection */
  insertNode: (node: ExamElement) => void;
  /** Toggle a mark on the current selection */
  toggleMark: (mark: MarkType, value?: string | boolean) => void;
  /** Check if a mark is active */
  isMarkActive: (mark: MarkType) => boolean;
  /** Get the current block type at selection */
  getActiveBlockType: () => BlockType | null;
  /** Toggle block type at selection */
  toggleBlock: (type: BlockType) => void;
  /** Set text alignment on current block */
  setAlignment: (align: 'left' | 'center' | 'right' | 'justify') => void;
  /** Get current alignment */
  getAlignment: () => string;
  /** Increase indent (max 3) */
  increaseIndent: () => void;
  /** Decrease indent (min 0) */
  decreaseIndent: () => void;
  /** Get current indent level */
  getIndentLevel: () => number;
  /** Insert an asset (media) by ID */
  insertAsset: (assetId: string, type: 'image' | 'audio' | 'video') => void;
}

/**
 * Keyboard shortcut binding.
 */
export interface KeyBinding {
  /** Hotkey pattern (e.g., 'mod+b', 'mod+shift+7') */
  hotkey: string;
  /** Handler returns true if the event was consumed */
  handler: (context: PluginContext) => boolean;
}

/**
 * Base interface for all editor plugins.
 *
 * Each plugin encapsulates a specific feature (formatting, media, etc.)
 * and exposes toolbar buttons, keyboard shortcuts, and rendering logic.
 */
export interface EditorPlugin {
  /** Unique plugin name */
  name: string;

  /** Plugin priority for ordering (lower = earlier). Default 100. */
  priority?: number;

  /** Toolbar buttons this plugin contributes */
  toolbarButtons?: ToolbarButton[];

  /** Keyboard shortcuts this plugin handles */
  keyBindings?: KeyBinding[];

  /**
   * Called when the plugin is registered.
   * Use for initialization logic.
   */
  onInit?: () => void;

  /**
   * Called when the plugin is destroyed.
   * Use for cleanup.
   */
  onDestroy?: () => void;
}
