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

import { Injectable } from '@angular/core';
import { EditorPlugin, ToolbarButton, KeyBinding } from './editor-plugin';

/**
 * Central registry for editor plugins.
 *
 * Manages plugin lifecycle, aggregates toolbar buttons and key bindings
 * from all registered plugins, and provides ordered access.
 */
@Injectable({ providedIn: 'root' })
export class PluginRegistry {

  private plugins: EditorPlugin[] = [];

  /**
   * Register a plugin. Plugins are sorted by priority (lower first).
   */
  register(plugin: EditorPlugin): void {
    if (this.plugins.find(p => p.name === plugin.name)) {
      console.warn(`[PluginRegistry] Plugin '${plugin.name}' already registered, skipping.`);
      return;
    }
    this.plugins.push(plugin);
    this.plugins.sort((a, b) => (a.priority ?? 100) - (b.priority ?? 100));
    plugin.onInit?.();
  }

  /**
   * Register multiple plugins at once.
   */
  registerAll(plugins: EditorPlugin[]): void {
    plugins.forEach(p => this.register(p));
  }

  /**
   * Unregister a plugin by name.
   */
  unregister(name: string): void {
    const index = this.plugins.findIndex(p => p.name === name);
    if (index >= 0) {
      this.plugins[index].onDestroy?.();
      this.plugins.splice(index, 1);
    }
  }

  /**
   * Get all registered plugins.
   */
  getPlugins(): EditorPlugin[] {
    return [...this.plugins];
  }

  /**
   * Get a plugin by name.
   */
  getPlugin(name: string): EditorPlugin | undefined {
    return this.plugins.find(p => p.name === name);
  }

  /**
   * Collect all toolbar buttons from all plugins, ordered by plugin priority.
   */
  getToolbarButtons(): ToolbarButton[] {
    return this.plugins.flatMap(p => p.toolbarButtons ?? []);
  }

  /**
   * Collect all key bindings from all plugins.
   */
  getKeyBindings(): KeyBinding[] {
    return this.plugins.flatMap(p => p.keyBindings ?? []);
  }

  /**
   * Get toolbar buttons grouped by their group field.
   */
  getToolbarGroups(): Map<string, ToolbarButton[]> {
    const groups = new Map<string, ToolbarButton[]>();
    for (const button of this.getToolbarButtons()) {
      const group = groups.get(button.group) ?? [];
      group.push(button);
      groups.set(button.group, group);
    }
    return groups;
  }

  /**
   * Clear all plugins. Call on destroy.
   */
  clear(): void {
    this.plugins.forEach(p => p.onDestroy?.());
    this.plugins = [];
  }
}
