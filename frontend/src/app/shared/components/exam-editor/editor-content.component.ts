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
  ChangeDetectionStrategy,
  ElementRef,
  ViewChild,
  AfterViewInit,
  OnChanges,
  OnDestroy,
  SimpleChanges,
  NgZone
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ExamDocument,
  ExamElement,
  ExamText,
  VOID_TYPES,
  HIGHLIGHT_COLORS,
  TEXT_COLORS,
  MathInlineElement,
  ChemicalStructureElement
} from './models';
import { MATH_SENTINEL, SMILES_SENTINEL, decodeSentinel } from './utils/serializer';
import { EditorSelection } from './plugins';
import { EditorAssetService } from './services';

/**
 * Renders the document content as a contenteditable area.
 * Handles user input, selection tracking, and DOM-to-model sync.
 */
@Component({
  selector: 'editor-content',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './editor-content.component.html',
  styleUrls: ['./editor-content.component.scss']
})
export class EditorContentComponent implements AfterViewInit, OnChanges, OnDestroy {

  @ViewChild('editorArea') editorArea!: ElementRef<HTMLDivElement>;

  @Input() document: ExamDocument = [];
  @Input() placeholder = 'Start typing...';
  @Input() disabled = false;
  @Input() selection: EditorSelection | null = null;

  @Output() documentChange = new EventEmitter<ExamDocument>();
  @Output() selectionChange = new EventEmitter<EditorSelection | null>();
  @Output() paste = new EventEmitter<ClipboardEvent>();
  @Output() focused = new EventEmitter<void>();
  @Output() blurred = new EventEmitter<void>();
  @Output() keydown = new EventEmitter<KeyboardEvent>();

  private isRendering = false;
  private isInternalChange = false;
  private savedRange: Range | null = null;
  private selectionChangeHandler = () => this.onSelectionChange();

  constructor(private assetService: EditorAssetService, private ngZone: NgZone) {}

  ngAfterViewInit(): void {
    this.renderDocument();
    try {
      document.execCommand('defaultParagraphSeparator', false, 'p');
    } catch {
      // ignore
    }
    // Listen for selection changes at document level for reliable tracking
    document.addEventListener('selectionchange', this.selectionChangeHandler);
  }

  ngOnDestroy(): void {
    document.removeEventListener('selectionchange', this.selectionChangeHandler);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['document'] && !changes['document'].firstChange) {
      if (this.isInternalChange) {
        // Change originated from user input or execCommand - DOM is already correct, skip re-render
        this.isInternalChange = false;
        return;
      }
      this.renderDocument();
    }
  }

  isEmpty(): boolean {
    if (!this.document || this.document.length === 0) return true;
    if (this.document.length === 1) {
      const first = this.document[0];
      if (first.type === 'paragraph' && first.children.length === 1) {
        return (first.children[0] as ExamText).text === '';
      }
    }
    return false;
  }

  onInput(_event: Event): void {
    if (this.isRendering) return;
    // Parse DOM back to document model
    const newDoc = this.parseDomToDocument();
    if (newDoc) {
      this.isInternalChange = true;
      this.documentChange.emit(newDoc);
    }
  }

  onFocused(): void {
    this.focused.emit();
    this.trackSelection();
  }

  private onSelectionChange(): void {
    const editorEl = this.editorArea?.nativeElement;
    if (!editorEl) return;
    const sel = window.getSelection();
    if (!sel || sel.rangeCount === 0) return;
    // Only track if selection is within our editor
    if (!editorEl.contains(sel.anchorNode)) return;
    this.ngZone.run(() => this.trackSelection());
  }

  trackSelection(): void {
    const sel = window.getSelection();
    if (!sel || sel.rangeCount === 0) {
      this.selectionChange.emit(null);
      return;
    }

    const range = sel.getRangeAt(0);
    const editorEl = this.editorArea?.nativeElement;
    if (!editorEl || !editorEl.contains(range.startContainer)) {
      this.selectionChange.emit(null);
      return;
    }

    this.savedRange = range.cloneRange();

    const anchorPath = this.getNodePath(range.startContainer, editorEl);
    const focusPath = this.getNodePath(range.endContainer, editorEl);

    this.selectionChange.emit({
      anchorPath,
      anchorOffset: range.startOffset,
      focusPath,
      focusOffset: range.endOffset,
      isCollapsed: range.collapsed
    });
  }

  // ─── Execution & Formatting Helper ───

  /**
   * Executes a native browser formatting command (e.g. 'bold', 'italic', etc.)
   * on the current DOM selection, then synchronizes the DOM back to the AST document model.
   */
  public execFormatCommand(cmd: string, value: string | undefined = undefined): boolean {
    const editorEl = this.editorArea?.nativeElement;
    if (!editorEl) return false;

    try {
      document.execCommand('styleWithCSS', false, 'false');
    } catch {
      // ignore
    }

    const sel = window.getSelection();
    const isInside = sel && sel.rangeCount > 0 && editorEl.contains(sel.getRangeAt(0).commonAncestorContainer);

    if (!isInside && this.savedRange) {
      sel?.removeAllRanges();
      sel?.addRange(this.savedRange);
    } else if (!isInside) {
      editorEl.focus();
    }

    const success = document.execCommand(cmd, false, value);
    this.onInput(new Event('input'));
    this.trackSelection();
    return success;
  }

  /**
   * Synchronizes the DOM back into an ExamDocument AST model and emits the change.
   */
  public syncDocument(): ExamDocument | null {
    const newDoc = this.parseDomToDocument();
    if (newDoc) {
      this.isInternalChange = true;
      this.documentChange.emit(newDoc);
    }
    return newDoc;
  }

  // ─── Rendering ───

  public renderDocument(): void {
    if (!this.editorArea) return;
    this.isRendering = true;
    const el = this.editorArea.nativeElement;
    el.innerHTML = this.documentToHtml(this.document);
    this.isRendering = false;
    this.renderSmilesCanvases(el);
  }

  /**
   * Render any SMILES 2D structure canvases present in the rendered DOM.
   */
  private renderSmilesCanvases(container: HTMLElement): void {
    const canvases = container.querySelectorAll<HTMLCanvasElement>('canvas.smiles-canvas');
    if (!canvases || canvases.length === 0) return;

    canvases.forEach(canvas => {
      const smiles = canvas.getAttribute('data-smiles');
      if (!smiles) return;
      try {
        const sd: any = (window as any)['SmilesDrawer'] || (window as any)['__smilesDrawer'];
        if (sd) {
          const drawer = new (sd.Drawer || sd.SvgDrawer || sd)({
            width: canvas.width,
            height: canvas.height
          });
          if (typeof drawer.draw === 'function') {
            drawer.draw(smiles, canvas, 'light', false);
          } else if (typeof drawer.drawToCanvas === 'function') {
            drawer.drawToCanvas(smiles, canvas, 'light');
          }
        } else {
          // Fallback text rendering
          const ctx = canvas.getContext('2d');
          if (ctx) {
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            ctx.font = '12px monospace';
            ctx.fillStyle = '#424242';
            ctx.fillText(smiles, 8, canvas.height / 2);
          }
        }
      } catch {
        // Fallback text on error
        const ctx = canvas.getContext('2d');
        if (ctx) {
          ctx.clearRect(0, 0, canvas.width, canvas.height);
          ctx.font = '12px monospace';
          ctx.fillStyle = '#616161';
          ctx.fillText(smiles, 8, canvas.height / 2);
        }
      }
    });
  }

  private documentToHtml(doc: ExamDocument): string {
    return doc.map(element => this.elementToHtml(element)).join('');
  }

  private elementToHtml(element: ExamElement): string {
    const align = (element as any).align;
    const indent = (element as any).indent ?? 0;
    const style = this.buildBlockStyle(align, indent);

    switch (element.type) {
      case 'paragraph':
        return `<p${style}>${this.childrenToHtml(element.children)}</p>`;
      case 'heading-one':
        return `<h1${style}>${this.childrenToHtml(element.children)}</h1>`;
      case 'heading-two':
        return `<h2${style}>${this.childrenToHtml(element.children)}</h2>`;
      case 'heading-three':
        return `<h3${style}>${this.childrenToHtml(element.children)}</h3>`;
      case 'numbered-list':
        return `<ol>${element.children.map(c => this.elementToHtml(c as any)).join('')}</ol>`;
      case 'bulleted-list':
        return `<ul>${element.children.map(c => this.elementToHtml(c as any)).join('')}</ul>`;
      case 'list-item':
        return `<li${style}>${this.childrenToHtml(element.children)}</li>`;
      case 'image':
        const imgSrc = this.assetService.getDownloadUrl((element as any).assetId);
        const alt = (element as any).alt || 'Image';
        return `<div class="media-block" contenteditable="false"><img src="${imgSrc}" alt="${alt}" /></div>`;
      case 'audio':
        const audioSrc = this.assetService.getDownloadUrl((element as any).assetId);
        return `<div class="media-block" contenteditable="false"><audio controls src="${audioSrc}"></audio></div>`;
      case 'video':
        const videoSrc = this.assetService.getDownloadUrl((element as any).assetId);
        return `<div class="media-block" contenteditable="false"><video controls src="${videoSrc}"></video></div>`;
      case 'math-inline': {
        const math = element as MathInlineElement;
        // Render via KaTeX at display time; void node is non-editable
        const latexEscaped = math.latex.replace(/"/g, '&quot;');
        const displayClass = math.display ? 'math-void--display' : 'math-void--inline';
        return `<span class="math-void ${displayClass}" contenteditable="false" data-latex="${latexEscaped}" data-type="math-inline">`
          + this.renderKatexSafe(math.latex, math.display ?? false)
          + `</span>`;
      }
      case 'chemical-structure': {
        const chem = element as ChemicalStructureElement;
        const smilesEscaped = chem.smiles.replace(/"/g, '&quot;');
        const titleHtml = chem.title ? `<div class="chem-caption">${this.escapeHtml(chem.title)}</div>` : '';
        const w = chem.width ?? 250;
        const h = chem.height ?? 200;
        return `<span class="chem-void" contenteditable="false" data-smiles="${smilesEscaped}" data-type="chemical-structure">`
          + `<canvas class="smiles-canvas" width="${w}" height="${h}" data-smiles="${smilesEscaped}" data-theme="${chem.theme ?? 'light'}"></canvas>`
          + titleHtml
          + `</span>`;
      }
      default:
        return `<p>${this.childrenToHtml((element as any).children)}</p>`;
    }
  }

  private childrenToHtml(children: ExamText[]): string {
    return children.map(child => this.textToHtml(child)).join('');
  }

  private textToHtml(text: ExamText): string {
    // Check for inline math/smiles sentinels (from serializer parseInlineLine)
    const sentinel = decodeSentinel(text.text || '');
    if (sentinel) {
      if (sentinel.kind === 'math') {
        return `<span class="math-void math-void--inline" contenteditable="false" data-latex="${sentinel.payload.replace(/"/g, '&quot;')}" data-type="math-inline">`
          + this.renderKatexSafe(sentinel.payload, false)
          + `</span>`;
      }
      if (sentinel.kind === 'smiles') {
        return `<span class="chem-void" contenteditable="false" data-smiles="${sentinel.payload.replace(/"/g, '&quot;')}" data-type="chemical-structure">`
          + `<canvas class="smiles-canvas" width="250" height="200" data-smiles="${sentinel.payload.replace(/"/g, '&quot;')}" data-theme="light"></canvas>`
          + `</span>`;
      }
    }

    let html = this.escapeHtml(text.text || '');
    if (!html) html = '<br>';

    if (text.bold) html = `<strong>${html}</strong>`;
    if (text.italic) html = `<em>${html}</em>`;
    if (text.underline) html = `<u>${html}</u>`;
    if (text.superscript) html = `<sup>${html}</sup>`;
    if (text.subscript) html = `<sub>${html}</sub>`;

    let spanStyles = '';
    if (text.highlight) {
      const color = HIGHLIGHT_COLORS.find(c => c.key === text.highlight);
      if (color) spanStyles += `background-color:${color.hex};`;
    }
    if (text.color) {
      const color = TEXT_COLORS.find(c => c.key === text.color);
      if (color) spanStyles += `color:${color.hex};`;
    }
    if (spanStyles) {
      html = `<span style="${spanStyles}">${html}</span>`;
    }

    return html;
  }

  /**
   * Render a LaTeX string to KaTeX HTML, with graceful error fallback.
   * Imported lazily to avoid issues if katex is not yet available.
   */
  private renderKatexSafe(latex: string, displayMode: boolean): string {
    try {
      // katex is already a dependency of the frontend package.json
      const katex: any = (window as any)['katex'];
      if (!katex) {
        // Fallback: show raw latex in a code span
        return `<code class="math-raw">${this.escapeHtml(latex)}</code>`;
      }
      return katex.renderToString(latex, {
        throwOnError: false,
        displayMode,
        output: 'htmlAndMathml',
        trust: false,
        strict: 'ignore'
      });
    } catch {
      return `<code class="math-raw">${this.escapeHtml(latex)}</code>`;
    }
  }

  private buildBlockStyle(align?: string, indent?: number): string {
    const styles: string[] = [];
    if (align && align !== 'left') styles.push(`text-align:${align}`);
    if (indent && indent > 0) styles.push(`margin-left:${indent * 2}em`);
    return styles.length > 0 ? ` style="${styles.join(';')}"` : '';
  }

  private escapeHtml(text: string): string {
    return text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  // ─── DOM Parsing ───

  private parseDomToDocument(): ExamDocument | null {
    const el = this.editorArea?.nativeElement;
    if (!el) return null;

    const doc: ExamElement[] = [];
    for (let i = 0; i < el.childNodes.length; i++) {
      const node = el.childNodes[i];
      const element = this.parseDomNode(node);
      if (element) doc.push(element);
    }

    return doc.length > 0 ? doc : [{ type: 'paragraph', children: [{ text: '' }] }];
  }

  private parseDomNode(node: Node): ExamElement | null {
    if (node.nodeType === Node.TEXT_NODE) {
      return { type: 'paragraph', children: [{ text: node.textContent || '' }] };
    }

    if (node.nodeType !== Node.ELEMENT_NODE) return null;

    const el = node as HTMLElement;
    const tag = el.tagName.toLowerCase();

    const align = (el.style.textAlign as any) || undefined;
    const marginLeft = el.style.marginLeft;
    let indent: number | undefined = undefined;
    if (marginLeft && marginLeft.endsWith('em')) {
      const emVal = parseFloat(marginLeft);
      if (!isNaN(emVal) && emVal > 0) {
        indent = Math.round(emVal / 2);
      }
    }

    switch (tag) {
      case 'p':
        return { type: 'paragraph', align, indent, children: this.parseInlineChildren(el) } as any;
      case 'h1':
        return { type: 'heading-one', align, children: this.parseInlineChildren(el) };
      case 'h2':
        return { type: 'heading-two', align, children: this.parseInlineChildren(el) };
      case 'h3':
        return { type: 'heading-three', align, children: this.parseInlineChildren(el) };
      case 'ol':
        return { type: 'numbered-list', children: this.parseListItems(el) } as any;
      case 'ul':
        return { type: 'bulleted-list', children: this.parseListItems(el) } as any;
      case 'li':
        return { type: 'list-item', indent, children: this.parseInlineChildren(el) };
      case 'div':
        // Media blocks or fallback to paragraph
        if (el.classList.contains('media-block')) return null; // Preserve as-is
        return { type: 'paragraph', align, indent, children: this.parseInlineChildren(el) } as any;
      default:
        return { type: 'paragraph', align, indent, children: this.parseInlineChildren(el) } as any;
    }
  }

  private parseInlineChildren(el: HTMLElement): ExamText[] {
    const texts: ExamText[] = [];
    for (let i = 0; i < el.childNodes.length; i++) {
      const child = el.childNodes[i];
      texts.push(...this.parseInlineNode(child, {}));
    }
    return texts.length > 0 ? texts : [{ text: '' }];
  }

  private parseInlineNode(node: Node, marks: Partial<ExamText>): ExamText[] {
    if (node.nodeType === Node.TEXT_NODE) {
      return [{ text: node.textContent || '', ...marks }];
    }

    if (node.nodeType !== Node.ELEMENT_NODE) return [];

    const el = node as HTMLElement;
    const tag = el.tagName.toLowerCase();
    const newMarks = { ...marks };

    // Void math/chem spans - reconstruct as sentinels so the document model is stable
    const nodeType = el.getAttribute('data-type');
    if (nodeType === 'math-inline') {
      const latex = el.getAttribute('data-latex') || '';
      return [{ text: `\x00math\x00${latex}\x00` }];
    }
    if (nodeType === 'chemical-structure') {
      const smiles = el.getAttribute('data-smiles') || '';
      return [{ text: `\x00smiles\x00${smiles}\x00` }];
    }

    // Check HTML tag formatting
    if (tag === 'strong' || tag === 'b') newMarks.bold = true;
    if (tag === 'em' || tag === 'i') newMarks.italic = true;
    if (tag === 'u') newMarks.underline = true;
    if (tag === 'sup') newMarks.superscript = true;
    if (tag === 'sub') newMarks.subscript = true;
    if (tag === 'br') return [{ text: '\n', ...marks }];

    // Check CSS styles for formatting (Chrome execCommand with styles or spans)
    const fw = el.style.fontWeight;
    if (fw === 'bold' || fw === '700' || fw === 'bolder' || parseInt(fw, 10) >= 600) {
      newMarks.bold = true;
    } else if (fw === 'normal' || fw === '400') {
      delete newMarks.bold;
    }

    const fs = el.style.fontStyle;
    if (fs === 'italic' || fs === 'oblique') {
      newMarks.italic = true;
    } else if (fs === 'normal') {
      delete newMarks.italic;
    }

    const td = el.style.textDecoration || el.style.textDecorationLine;
    if (td && td.includes('underline')) {
      newMarks.underline = true;
    } else if (td && td.includes('none')) {
      delete newMarks.underline;
    }

    const va = el.style.verticalAlign;
    if (va === 'super') {
      newMarks.superscript = true;
    } else if (va === 'sub') {
      newMarks.subscript = true;
    }

    // Check inline styling for highlight/color
    if (el.style.backgroundColor) {
      const bg = el.style.backgroundColor;
      const matched = HIGHLIGHT_COLORS.find(c => c.hex.toLowerCase() === bg.toLowerCase());
      if (matched) newMarks.highlight = matched.key;
    }
    if (el.style.color) {
      const col = el.style.color;
      const matched = TEXT_COLORS.find(c => c.hex.toLowerCase() === col.toLowerCase());
      if (matched) newMarks.color = matched.key;
    }

    const results: ExamText[] = [];
    for (let i = 0; i < el.childNodes.length; i++) {
      results.push(...this.parseInlineNode(el.childNodes[i], newMarks));
    }
    return results;
  }

  private parseListItems(el: HTMLElement): ExamElement[] {
    const items: ExamElement[] = [];
    for (let i = 0; i < el.children.length; i++) {
      const li = el.children[i];
      if (li.tagName.toLowerCase() === 'li') {
        items.push({ type: 'list-item', children: this.parseInlineChildren(li as HTMLElement) });
      }
    }
    return items;
  }

  private getNodePath(node: Node, root: HTMLElement): number[] {
    const path: number[] = [];
    let current: Node | null = node;

    // Walk up to find the block-level child of root
    while (current && current !== root) {
      const parent: Node | null = current.parentNode;
      if (parent === root) {
        path.unshift(Array.from(root.childNodes).indexOf(current as ChildNode));
        break;
      }
      current = parent;
    }

    return path;
  }
}
