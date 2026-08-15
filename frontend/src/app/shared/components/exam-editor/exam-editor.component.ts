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

import {
  Component,
  ChangeDetectionStrategy,
  Input,
  Output,
  EventEmitter,
  OnInit,
  OnDestroy,
  ElementRef,
  ViewChild,
  forwardRef,
  ChangeDetectorRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import {
  ExamDocument,
  ExamElement,
  ExamText,
  BlockType,
  MarkType,
  EMPTY_DOCUMENT,
  VOID_TYPES,
  LIST_TYPES,
  SCHEMA_LIMITS,
  HIGHLIGHT_COLORS,
  TEXT_COLORS
} from './models';
import { PluginContext, EditorSelection, KeyBinding } from './plugins';
import { PluginRegistry } from './plugins/plugin-registry';
import { FormatMarksPlugin, HeadingPlugin, ListPlugin, AlignmentPlugin, IndentPlugin, ColorPlugin } from './plugins/formatting';
import { MediaPlugin } from './plugins/media';
import { sanitizeClipboardData } from './utils/clipboard-sanitizer';
import { validateDocument, ValidationResult } from './utils/document-validator';
import { EditorToolbarComponent } from './editor-toolbar.component';
import { EditorContentComponent } from './editor-content.component';

/**
 * Secure Slate.js-based rich text editor for examination content.
 *
 * Usage:
 * ```html
 * <exam-editor
 *   [value]="document"
 *   (valueChange)="onDocumentChanged($event)"
 *   [placeholder]="'Enter question content...'"
 * ></exam-editor>
 * ```
 *
 * Also supports Reactive Forms via ControlValueAccessor:
 * ```html
 * <exam-editor formControlName="content"></exam-editor>
 * ```
 */
@Component({
  selector: 'exam-editor',
  standalone: true,
  imports: [
    CommonModule,
    MatSnackBarModule,
    EditorToolbarComponent,
    EditorContentComponent
  ],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ExamEditorComponent),
      multi: true
    }
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './exam-editor.component.html',
  styleUrls: ['./exam-editor.component.scss']
})
export class ExamEditorComponent implements OnInit, OnDestroy, ControlValueAccessor {

  @Input() placeholder = 'Start typing...';
  @Input() disabled = false;

  @Input()
  set value(doc: ExamDocument) {
    if (doc && doc !== this.document) {
      this.document = doc;
      this.pushToHistory();
    }
  }

  @Output() valueChange = new EventEmitter<ExamDocument>();

  document: ExamDocument = [...EMPTY_DOCUMENT];
  selection: EditorSelection | null = null;
  focused = false;

  // Undo/Redo history
  private history: ExamDocument[] = [];
  private historyIndex = -1;
  private maxHistory = 100;

  // ControlValueAccessor callbacks
  private onChange: (value: ExamDocument) => void = () => {};
  private onTouched: () => void = () => {};

  pluginContext!: PluginContext;

  constructor(
    public pluginRegistry: PluginRegistry,
    private cdr: ChangeDetectorRef,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.initPlugins();
    this.buildPluginContext();
    this.pushToHistory();
  }

  ngOnDestroy(): void {
    this.pluginRegistry.clear();
  }

  // ─── ControlValueAccessor ────────────────────────────────────────────────

  writeValue(value: ExamDocument): void {
    this.document = value ?? [...EMPTY_DOCUMENT];
    this.history = [JSON.parse(JSON.stringify(this.document))];
    this.historyIndex = 0;
    this.cdr.markForCheck();
  }

  registerOnChange(fn: (value: ExamDocument) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    this.cdr.markForCheck();
  }

  // ─── Public API ──────────────────────────────────────────────────────────

  /** Load a document into the editor */
  load(document: ExamDocument): void {
    this.document = document;
    this.history = [JSON.parse(JSON.stringify(document))];
    this.historyIndex = 0;
    this.emitChange();
  }

  /** Export the current document */
  export(): ExamDocument {
    return JSON.parse(JSON.stringify(this.document));
  }

  /** Validate the document against the schema */
  validate(): ValidationResult {
    return validateDocument(this.document);
  }

  /** Insert an asset at the current selection */
  insertAsset(assetId: string, type: 'image' | 'audio' | 'video' = 'image'): void {
    this.pluginContext.insertAsset(assetId, type);
  }

  /** Undo */
  undo(): void {
    if (this.historyIndex > 0) {
      this.historyIndex--;
      this.document = JSON.parse(JSON.stringify(this.history[this.historyIndex]));
      this.emitChange();
      this.cdr.markForCheck();
    }
  }

  /** Redo */
  redo(): void {
    if (this.historyIndex < this.history.length - 1) {
      this.historyIndex++;
      this.document = JSON.parse(JSON.stringify(this.history[this.historyIndex]));
      this.emitChange();
      this.cdr.markForCheck();
    }
  }

  // ─── Event Handlers ──────────────────────────────────────────────────────

  onContentChange(doc: ExamDocument): void {
    this.document = doc;
    this.pushToHistory();
    this.emitChange();
    this.cdr.markForCheck();
  }

  onSelectionChange(sel: EditorSelection | null): void {
    this.selection = sel;
    this.cdr.markForCheck();
  }

  onPaste(event: ClipboardEvent): void {
    event.preventDefault();
    if (!event.clipboardData) return;

    const nodes = sanitizeClipboardData(event.clipboardData);
    if (!nodes || nodes.length === 0) {
      this.snackBar.open('Paste rejected: unsupported content', 'OK', { duration: 3000 });
      return;
    }

    // Insert pasted paragraphs at current position
    this.insertNodes(nodes);
  }

  onKeyDown(event: KeyboardEvent): void {
    // Check plugin key bindings
    const bindings = this.pluginRegistry.getKeyBindings();
    for (const binding of bindings) {
      if (this.matchHotkey(binding.hotkey, event)) {
        event.preventDefault();
        if (binding.handler(this.pluginContext)) {
          return;
        }
      }
    }

    // Built-in undo/redo
    if (this.isHotkey('mod+z', event)) {
      event.preventDefault();
      this.undo();
      return;
    }
    if (this.isHotkey('mod+shift+z', event) || this.isHotkey('mod+y', event)) {
      event.preventDefault();
      this.redo();
      return;
    }
  }

  onFocus(): void {
    this.focused = true;
    this.cdr.markForCheck();
  }

  onBlur(): void {
    this.focused = false;
    this.onTouched();
    this.cdr.markForCheck();
  }

  // ─── Private ─────────────────────────────────────────────────────────────

  private initPlugins(): void {
    this.pluginRegistry.registerAll([
      new FormatMarksPlugin(),
      new HeadingPlugin(),
      new ListPlugin(),
      new AlignmentPlugin(),
      new IndentPlugin(),
      new ColorPlugin(),
      new MediaPlugin()
    ]);
  }

  private buildPluginContext(): void {
    this.pluginContext = {
      get document() { return []; }, // Will be overridden by getter
      get selection() { return null; },
      setDocument: (doc: ExamDocument) => {
        this.document = doc;
        this.pushToHistory();
        this.emitChange();
        this.cdr.markForCheck();
      },
      setSelection: (sel: EditorSelection | null) => {
        this.selection = sel;
        this.cdr.markForCheck();
      },
      focus: () => { /* delegated to content component */ },
      insertNode: (node: ExamElement) => {
        this.insertNodes([node]);
      },
      toggleMark: (mark: MarkType, value?: string | boolean) => {
        this.toggleMarkOnSelection(mark, value);
      },
      isMarkActive: (mark: MarkType) => {
        return this.isMarkActiveAtSelection(mark);
      },
      getActiveBlockType: () => {
        return this.getBlockTypeAtSelection();
      },
      toggleBlock: (type: BlockType) => {
        this.toggleBlockType(type);
      },
      setAlignment: (align: 'left' | 'center' | 'right' | 'justify') => {
        this.setBlockAlignment(align);
      },
      getAlignment: () => {
        return this.getBlockAlignment();
      },
      increaseIndent: () => {
        this.changeIndent(1);
      },
      decreaseIndent: () => {
        this.changeIndent(-1);
      },
      getIndentLevel: () => {
        return this.getCurrentIndent();
      },
      insertAsset: (assetId: string, type: 'image' | 'audio' | 'video') => {
        const node: ExamElement = {
          type,
          assetId,
          children: [{ text: '' }]
        } as any;
        this.insertNodes([node]);
      }
    };

    // Override getters to always return current state
    Object.defineProperty(this.pluginContext, 'document', {
      get: () => this.document
    });
    Object.defineProperty(this.pluginContext, 'selection', {
      get: () => this.selection
    });
  }

  private emitChange(): void {
    this.valueChange.emit(this.document);
    this.onChange(this.document);
  }

  private pushToHistory(): void {
    const snapshot = JSON.parse(JSON.stringify(this.document));
    // Trim future if we're not at the end
    if (this.historyIndex < this.history.length - 1) {
      this.history = this.history.slice(0, this.historyIndex + 1);
    }
    this.history.push(snapshot);
    if (this.history.length > this.maxHistory) {
      this.history.shift();
    }
    this.historyIndex = this.history.length - 1;
  }

  // ─── Document Manipulation ───────────────────────────────────────────────

  private insertNodes(nodes: ExamElement[]): void {
    if (!this.selection) {
      // Append to end
      this.document = [...this.document, ...nodes];
    } else {
      // Insert at selection path
      const idx = this.selection.anchorPath[0] ?? this.document.length;
      const newDoc = [...this.document];
      newDoc.splice(idx + 1, 0, ...nodes);
      this.document = newDoc;
    }
    this.pushToHistory();
    this.emitChange();
    this.cdr.markForCheck();
  }

  private toggleMarkOnSelection(mark: MarkType, value?: string | boolean): void {
    if (!this.selection) return;
    const { anchorPath, anchorOffset, focusPath, focusOffset } = this.selection;
    const blockIdx = anchorPath[0];
    if (blockIdx == null || blockIdx >= this.document.length) return;

    const block = this.document[blockIdx];
    if (VOID_TYPES.includes(block.type)) return;

    const newDoc = JSON.parse(JSON.stringify(this.document)) as ExamDocument;
    const targetBlock = newDoc[blockIdx];
    const children = targetBlock.children as ExamText[];

    for (const child of children) {
      if ('text' in child) {
        const currentValue = (child as any)[mark];
        if (value !== undefined) {
          (child as any)[mark] = currentValue === value ? undefined : value;
        } else {
          (child as any)[mark] = !currentValue;
        }
      }
    }

    this.document = newDoc;
    this.pushToHistory();
    this.emitChange();
    this.cdr.markForCheck();
  }

  private isMarkActiveAtSelection(mark: MarkType): boolean {
    if (!this.selection) return false;
    const blockIdx = this.selection.anchorPath[0];
    if (blockIdx == null || blockIdx >= this.document.length) return false;

    const block = this.document[blockIdx];
    if (VOID_TYPES.includes(block.type)) return false;

    const children = block.children as ExamText[];
    return children.some(child => 'text' in child && !!(child as any)[mark]);
  }

  private getBlockTypeAtSelection(): BlockType | null {
    if (!this.selection) return null;
    const blockIdx = this.selection.anchorPath[0];
    if (blockIdx == null || blockIdx >= this.document.length) return null;
    return this.document[blockIdx].type;
  }

  private toggleBlockType(type: BlockType): void {
    if (!this.selection) return;
    const blockIdx = this.selection.anchorPath[0];
    if (blockIdx == null || blockIdx >= this.document.length) return;

    const newDoc = JSON.parse(JSON.stringify(this.document)) as ExamDocument;
    const current = newDoc[blockIdx];

    if (LIST_TYPES.includes(type as BlockType)) {
      if (current.type === type) {
        // Unwrap list to paragraphs
        const items = (current as any).children as any[];
        const paragraphs = items.map((item: any) => ({
          type: 'paragraph' as const,
          children: item.children
        }));
        newDoc.splice(blockIdx, 1, ...paragraphs);
      } else {
        // Wrap current block in list
        const listItem = { type: 'list-item' as const, children: current.children };
        newDoc[blockIdx] = { type: type as any, children: [listItem] } as any;
      }
    } else {
      // Simple block type toggle
      if (current.type === type) {
        (newDoc[blockIdx] as any).type = 'paragraph';
      } else {
        (newDoc[blockIdx] as any).type = type;
      }
    }

    this.document = newDoc;
    this.pushToHistory();
    this.emitChange();
    this.cdr.markForCheck();
  }

  private setBlockAlignment(align: 'left' | 'center' | 'right' | 'justify'): void {
    if (!this.selection) return;
    const blockIdx = this.selection.anchorPath[0];
    if (blockIdx == null || blockIdx >= this.document.length) return;

    const newDoc = JSON.parse(JSON.stringify(this.document)) as ExamDocument;
    (newDoc[blockIdx] as any).align = align;

    this.document = newDoc;
    this.pushToHistory();
    this.emitChange();
    this.cdr.markForCheck();
  }

  private getBlockAlignment(): string {
    if (!this.selection) return 'left';
    const blockIdx = this.selection.anchorPath[0];
    if (blockIdx == null || blockIdx >= this.document.length) return 'left';
    return (this.document[blockIdx] as any).align || 'left';
  }

  private changeIndent(delta: number): void {
    if (!this.selection) return;
    const blockIdx = this.selection.anchorPath[0];
    if (blockIdx == null || blockIdx >= this.document.length) return;

    const newDoc = JSON.parse(JSON.stringify(this.document)) as ExamDocument;
    const current = (newDoc[blockIdx] as any).indent ?? 0;
    const newIndent = Math.max(0, Math.min(SCHEMA_LIMITS.maxIndent, current + delta));
    (newDoc[blockIdx] as any).indent = newIndent;

    this.document = newDoc;
    this.pushToHistory();
    this.emitChange();
    this.cdr.markForCheck();
  }

  private getCurrentIndent(): number {
    if (!this.selection) return 0;
    const blockIdx = this.selection.anchorPath[0];
    if (blockIdx == null || blockIdx >= this.document.length) return 0;
    return (this.document[blockIdx] as any).indent ?? 0;
  }

  // ─── Hotkey Matching ─────────────────────────────────────────────────────

  private matchHotkey(hotkey: string, event: KeyboardEvent): boolean {
    return this.isHotkey(hotkey, event);
  }

  private isHotkey(hotkey: string, event: KeyboardEvent): boolean {
    const parts = hotkey.toLowerCase().split('+');
    const key = parts[parts.length - 1];
    const requireMod = parts.includes('mod');
    const requireShift = parts.includes('shift');
    const requireAlt = parts.includes('alt');

    const modKey = navigator.platform.includes('Mac') ? event.metaKey : event.ctrlKey;

    if (requireMod && !modKey) return false;
    if (!requireMod && modKey) return false;
    if (requireShift && !event.shiftKey) return false;
    if (!requireShift && event.shiftKey && key !== 'tab') return false;
    if (requireAlt && !event.altKey) return false;
    if (!requireAlt && event.altKey) return false;

    const eventKey = event.key.toLowerCase();
    if (key === 'tab') return eventKey === 'tab';
    if (key.length === 1) return eventKey === key;
    return eventKey === key;
  }
}
