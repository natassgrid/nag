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
  Input,
  Output,
  EventEmitter,
  forwardRef,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  OnInit,
  OnDestroy,
  OnChanges,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, FormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatDialogModule } from '@angular/material/dialog';

import {
  ExamDocument,
  ExamElement,
  ExamText,
  EMPTY_DOCUMENT,
  MarkType,
  BlockType,
  LIST_TYPES,
  VOID_TYPES,
  TextAlignment,
  HIGHLIGHT_COLORS,
  TEXT_COLORS
} from './models';
import { PluginRegistry } from './plugins/plugin-registry';
import { PluginContext, EditorSelection } from './plugins/editor-plugin';
import { EditorToolbarComponent } from './editor-toolbar.component';
import { EditorContentComponent } from './editor-content.component';
import { FormatMarksPlugin } from './plugins/formatting/format-marks.plugin';
import { HeadingPlugin } from './plugins/formatting/heading.plugin';
import { ListPlugin } from './plugins/formatting/list.plugin';
import { AlignmentPlugin } from './plugins/formatting/alignment.plugin';
import { IndentPlugin } from './plugins/formatting/indent.plugin';
import { ColorPlugin } from './plugins/formatting/color.plugin';
import { MediaPlugin } from './plugins/media/media.plugin';
import { MathInlinePlugin } from './plugins/math-inline.plugin';
import { ChemicalStructurePlugin } from './plugins/chemical-structure.plugin';
import { MathInputDialogComponent } from './math-input-dialog.component';
import { SmilesInputDialogComponent } from './smiles-input-dialog.component';
import { serialiseDocument, deserialiseContent } from './utils/serializer';
import { sanitizeClipboardData } from './utils/clipboard-sanitizer';
import { validateDocument } from './utils/document-validator';

/** The three display modes supported by the editor. */
export type ExamEditorMode = 'full' | 'inline' | 'readonly';

function matchHotkey(hotkey: string, event: KeyboardEvent): boolean {
  const parts = hotkey.toLowerCase().split('+');
  const key = parts[parts.length - 1];
  const needsMod = parts.includes('mod') || parts.includes('ctrl') || parts.includes('cmd');
  const needsShift = parts.includes('shift');
  const needsAlt = parts.includes('alt');

  const hasMod = event.ctrlKey || event.metaKey;
  if (needsMod && !hasMod) return false;
  if (!needsMod && hasMod) return false;
  if (needsShift !== event.shiftKey) return false;
  if (needsAlt !== event.altKey) return false;

  return event.key.toLowerCase() === key.toLowerCase();
}

/**
 * Main rich-text editor component for examination content.
 *
 * Replaces the legacy Quill-based ExamEditorComponent.
 * Implements Angular's ControlValueAccessor so it works with Reactive Forms
 * and with plain [value]/(valueChange) bindings.
 */
@Component({
  selector: 'exam-editor',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    EditorToolbarComponent,
    EditorContentComponent
  ],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ExamEditorComponent),
      multi: true
    },
    PluginRegistry
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './exam-editor.component.html',
  styleUrls: ['./exam-editor.component.scss']
})
export class ExamEditorComponent implements ControlValueAccessor, OnInit, OnDestroy, OnChanges {

  @ViewChild(EditorContentComponent) contentComponent?: EditorContentComponent;

  @Input() placeholder = 'Start typing...';
  @Input() disabled = false;
  @Input() mode: ExamEditorMode = 'full';

  /** Optional additional plugin instances to register beyond the defaults. */
  @Input() plugins: any[] = [];

  @Input()
  set value(val: any) {
    const newDoc = deserialiseContent(val);
    if (JSON.stringify(newDoc) !== JSON.stringify(this.document)) {
      this.document = newDoc;
      this.undoStack = [];
      this.redoStack = [];
      this.cdr.markForCheck();
      this.contentComponent?.renderDocument();
    }
  }

  @Output() valueChange = new EventEmitter<string>();
  @Output() ready = new EventEmitter<void>();

  document: ExamDocument = EMPTY_DOCUMENT;
  selection: EditorSelection | null = null;

  private undoStack: ExamDocument[] = [];
  private redoStack: ExamDocument[] = [];
  private readonly maxHistory = 50;

  private onChange: (val: string) => void = () => {};
  private onTouched: () => void = () => {};

  constructor(
    public registry: PluginRegistry,
    private cdr: ChangeDetectorRef,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.registerDefaultPlugins();
    for (const plugin of this.plugins) {
      this.registry.register(plugin);
    }
    this.ready.emit();
  }

  ngOnDestroy(): void {
    this.registry.destroy();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['plugins'] && !changes['plugins'].firstChange) {
      this.registerDefaultPlugins();
      for (const p of this.plugins) {
        this.registry.register(p);
      }
      this.cdr.markForCheck();
    }
  }

  private registerDefaultPlugins(): void {
    this.registry.destroy();
    this.registry.register(new FormatMarksPlugin());
    this.registry.register(new HeadingPlugin());
    this.registry.register(new ListPlugin());
    this.registry.register(new AlignmentPlugin());
    this.registry.register(new IndentPlugin());
    this.registry.register(new ColorPlugin());
    this.registry.register(new MediaPlugin());
    this.registry.register(new MathInlinePlugin());
    this.registry.register(new ChemicalStructurePlugin());
  }

  // ─── ControlValueAccessor ───

  writeValue(obj: any): void {
    this.document = deserialiseContent(obj);
    this.undoStack = [];
    this.redoStack = [];
    this.cdr.markForCheck();
    this.contentComponent?.renderDocument();
  }

  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    this.cdr.markForCheck();
  }

  // ─── Document mutation events from EditorContentComponent ───

  onDocumentChange(newDoc: ExamDocument): void {
    if (this.mode === 'readonly') return;

    // Push to undo stack
    if (this.undoStack.length >= this.maxHistory) {
      this.undoStack.shift();
    }
    this.undoStack.push(JSON.parse(JSON.stringify(this.document)));
    this.redoStack = []; // clear redo on new edit

    this.document = newDoc;
    const markdown = serialiseDocument(newDoc);
    this.onChange(markdown);
    this.valueChange.emit(markdown);
    this.cdr.markForCheck();
  }

  onSelectionChange(sel: EditorSelection | null): void {
    this.selection = sel;
    this.cdr.markForCheck();
  }

  onPaste(event: ClipboardEvent): void {
    if (this.mode === 'readonly') return;
    event.preventDefault();
    const data = event.clipboardData;
    if (!data) return;
    const nodes = sanitizeClipboardData(data);
    if (!nodes || nodes.length === 0) return;
    this.insertNodes(nodes);
  }

  onKeydown(event: KeyboardEvent): void {
    if (this.mode === 'readonly') return;

    // Apply plugin key bindings
    const keyBindings = this.registry.getKeyBindings();
    for (const binding of keyBindings) {
      if (matchHotkey(binding.hotkey, event)) {
        event.preventDefault();
        binding.handler(this.pluginContext);
        break;
      }
    }

    // Undo / Redo
    if (matchHotkey('mod+z', event)) { event.preventDefault(); this.undo(); }
    if (matchHotkey('mod+shift+z', event) || matchHotkey('mod+y', event)) { event.preventDefault(); this.redo(); }
  }

  onBlur(): void {
    this.onTouched();
  }

  // ─── Undo / Redo ───

  undo(): void {
    if (this.undoStack.length === 0) return;
    this.redoStack.push(JSON.parse(JSON.stringify(this.document)));
    this.document = this.undoStack.pop()!;
    const markdown = serialiseDocument(this.document);
    this.onChange(markdown);
    this.valueChange.emit(markdown);
    this.cdr.markForCheck();
    this.contentComponent?.renderDocument();
  }

  redo(): void {
    if (this.redoStack.length === 0) return;
    this.undoStack.push(JSON.parse(JSON.stringify(this.document)));
    this.document = this.redoStack.pop()!;
    const markdown = serialiseDocument(this.document);
    this.onChange(markdown);
    this.valueChange.emit(markdown);
    this.cdr.markForCheck();
    this.contentComponent?.renderDocument();
  }

  // ─── Public API ───

  export(): ExamDocument { return JSON.parse(JSON.stringify(this.document)); }

  validate() { return validateDocument(this.document); }

  load(doc: ExamDocument): void {
    this.document = doc;
    this.cdr.markForCheck();
    this.contentComponent?.renderDocument();
  }

  insertAsset(assetId: string, type: 'image' | 'audio' | 'video'): void {
    const node: ExamElement = { type, assetId, children: [{ text: '' }] } as any;
    this.insertNodes([node]);
  }

  // ─── Plugin Context ───

  get pluginContext(): PluginContext & {
    undo: () => void;
    redo: () => void;
    openMathInput: () => void;
    openSmilesInput: () => void;
    openAssetPicker: (type: 'image' | 'audio' | 'video') => void;
  } {
    return {
      document: this.document,
      selection: this.selection,
      setDocument: (doc) => {
        this.onDocumentChange(doc);
        this.contentComponent?.renderDocument();
      },
      setSelection: (sel) => { this.selection = sel; this.cdr.markForCheck(); },
      focus: () => { this.contentComponent?.editorArea?.nativeElement.focus(); },
      insertNode: (node) => { this.insertNodes([node]); },
      toggleMark: (mark, value) => { this.toggleMark(mark, value); },
      isMarkActive: (mark) => this.isMarkActive(mark),
      getActiveBlockType: () => this.getActiveBlockType(),
      toggleBlock: (type) => { this.toggleBlock(type); },
      setAlignment: (align) => { this.setAlignment(align); },
      getAlignment: () => this.getAlignment(),
      increaseIndent: () => { this.changeIndent(1); },
      decreaseIndent: () => { this.changeIndent(-1); },
      getIndentLevel: () => this.getIndentLevel(),
      insertAsset: (id, type) => { this.insertAsset(id, type); },
      undo: () => this.undo(),
      redo: () => this.redo(),
      openMathInput: () => this.openMathInput(),
      openSmilesInput: () => this.openSmilesInput(),
      openAssetPicker: (type) => {
        (this as any).assetPickerRequest?.emit(type);
      }
    };
  }

  // ─── Math input dialog ───

  openMathInput(existing?: { latex: string; display?: boolean }): void {
    const ref = this.dialog.open(MathInputDialogComponent, {
      width: '560px',
      data: existing || {}
    });
    ref.afterClosed().subscribe((result: { latex: string; display: boolean } | null) => {
      if (!result) return;
      const node = MathInlinePlugin.createNode(result.latex, result.display);
      this.insertNodes([node]);
    });
  }

  // ─── SMILES input dialog ───

  openSmilesInput(existing?: { smiles: string; title?: string }): void {
    const ref = this.dialog.open(SmilesInputDialogComponent, {
      width: '480px',
      data: existing || {}
    });
    ref.afterClosed().subscribe((result: { smiles: string; title?: string } | null) => {
      if (!result) return;
      const node = ChemicalStructurePlugin.createNode(result.smiles, result.title);
      this.insertNodes([node]);
    });
  }

  // ─── Document Manipulation Helpers ───

  private insertNodes(nodes: ExamElement[]): void {
    const newDoc = [...this.document];
    const last = newDoc[newDoc.length - 1];
    const hasTrailingEmpty = last?.type === 'paragraph' &&
      last.children.length === 1 && (last.children[0] as ExamText).text === '';
    if (hasTrailingEmpty) newDoc.pop();
    newDoc.push(...nodes);
    newDoc.push({ type: 'paragraph', children: [{ text: '' }] });
    this.onDocumentChange(newDoc);
    this.contentComponent?.renderDocument();
  }

  private toggleMark(mark: MarkType, value?: string | boolean): void {
    const execCmdMap: Partial<Record<MarkType, string>> = {
      bold: 'bold',
      italic: 'italic',
      underline: 'underline',
      superscript: 'superscript',
      subscript: 'subscript'
    };

    if (execCmdMap[mark] && this.contentComponent) {
      this.contentComponent.execFormatCommand(execCmdMap[mark]!);
      return;
    }

    if (mark === 'color' && typeof value === 'string' && this.contentComponent) {
      const col = TEXT_COLORS.find(c => c.key === value);
      if (col) {
        this.contentComponent.execFormatCommand('foreColor', col.hex);
        return;
      }
    }

    if (mark === 'highlight' && typeof value === 'string' && this.contentComponent) {
      const hl = HIGHLIGHT_COLORS.find(c => c.key === value);
      if (hl) {
        this.contentComponent.execFormatCommand('hiliteColor', hl.hex);
        return;
      }
    }

    // Fallback: AST toggle
    const isActive = this.isMarkActive(mark);
    const newDoc = this.applyMarkToDocument(this.document, mark, isActive ? undefined : (value ?? true));
    this.onDocumentChange(newDoc);
    this.contentComponent?.renderDocument();
  }

  private applyMarkToDocument(doc: ExamDocument, mark: MarkType, value: any): ExamDocument {
    return doc.map(el => this.applyMarkToElement(el, mark, value));
  }

  private applyMarkToElement(el: ExamElement, mark: MarkType, value: any): ExamElement {
    if (VOID_TYPES.includes(el.type)) return el;
    if (LIST_TYPES.includes(el.type)) {
      return { ...el, children: (el as any).children.map((c: any) => this.applyMarkToElement(c, mark, value)) } as any;
    }
    const children = ((el as any).children as ExamText[]).map(child => {
      if ('text' in child) {
        const copy: any = { ...child };
        if (value === undefined || value === false) {
          delete copy[mark];
        } else {
          copy[mark] = value;
        }
        return copy;
      }
      return child;
    });
    return { ...el, children } as any;
  }

  private isMarkActive(mark: MarkType): boolean {
    try {
      if (document.queryCommandState(mark)) {
        return true;
      }
    } catch {
      // ignore
    }
    const firstPara = this.document.find(el => el.type === 'paragraph');
    if (!firstPara || !firstPara.children.length) return false;
    const firstText = firstPara.children[0] as ExamText;
    return !!(firstText && (firstText as any)[mark]);
  }

  private getActiveBlockType(): BlockType {
    const first = this.document[0];
    return (first?.type as BlockType) || 'paragraph';
  }

  private toggleBlock(type: BlockType): void {
    if (this.contentComponent) {
      if (type === 'numbered-list') {
        this.contentComponent.execFormatCommand('insertOrderedList');
        return;
      }
      if (type === 'bulleted-list') {
        this.contentComponent.execFormatCommand('insertUnorderedList');
        return;
      }
      const tagMap: Partial<Record<BlockType, string>> = {
        'heading-one': '<h1>',
        'heading-two': '<h2>',
        'heading-three': '<h3>',
        'paragraph': '<p>'
      };
      if (tagMap[type]) {
        this.contentComponent.execFormatCommand('formatBlock', tagMap[type]);
        return;
      }
    }

    // Fallback: AST toggle
    const current = this.getActiveBlockType();
    const newType = current === type ? 'paragraph' : type;
    const newDoc = this.document.map(el => {
      if (LIST_TYPES.includes(el.type) || VOID_TYPES.includes(el.type)) return el;
      return { ...el, type: newType } as ExamElement;
    }) as ExamDocument;
    this.onDocumentChange(newDoc);
    this.contentComponent?.renderDocument();
  }

  private setAlignment(align: TextAlignment): void {
    if (this.contentComponent) {
      const alignCmdMap: Record<TextAlignment, string> = {
        left: 'justifyLeft',
        center: 'justifyCenter',
        right: 'justifyRight',
        justify: 'justifyFull'
      };
      if (alignCmdMap[align]) {
        this.contentComponent.execFormatCommand(alignCmdMap[align]);
        return;
      }
    }

    const newDoc = this.document.map(el => ({ ...el, align } as ExamElement)) as ExamDocument;
    this.onDocumentChange(newDoc);
    this.contentComponent?.renderDocument();
  }

  private getAlignment(): TextAlignment {
    return (this.document[0] as any)?.align || 'left';
  }

  private changeIndent(delta: number): void {
    if (this.contentComponent) {
      if (delta > 0) {
        this.contentComponent.execFormatCommand('indent');
      } else {
        this.contentComponent.execFormatCommand('outdent');
      }
      return;
    }

    const current = this.getIndentLevel();
    const next = Math.max(0, Math.min(8, current + delta));
    const newDoc = this.document.map(el => ({ ...el, indent: next } as ExamElement)) as ExamDocument;
    this.onDocumentChange(newDoc);
    this.contentComponent?.renderDocument();
  }

  private getIndentLevel(): number {
    return (this.document[0] as any)?.indent || 0;
  }
}
