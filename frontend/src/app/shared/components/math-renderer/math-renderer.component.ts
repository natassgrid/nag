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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

import {
  Component,
  HostBinding,
  Input,
  OnChanges,
  AfterViewChecked,
  SimpleChanges,
  ViewEncapsulation,
  ChangeDetectionStrategy,
  ElementRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import katex from 'katex';
import { marked } from 'marked';
// mhchem adds \ce{} (chemical equations) and \pu{} (physical units) to KaTeX.
// It ships inside the katex package — no extra npm dependency needed.
import 'katex/dist/contrib/mhchem.js';

@Component({
  selector: 'app-math-renderer',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './math-renderer.component.html',
  styleUrls: ['./math-renderer.component.scss'],
  encapsulation: ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MathRendererComponent implements OnChanges, AfterViewChecked {
  @Input() content: string | null = '';
  @Input() inline: boolean = false;

  @HostBinding('class.math-renderer-host') hostClass = true;

  renderedHtml: SafeHtml = '';

  private pendingSmilesRender = false;

  // SmilesDrawer module (lazy-loaded)
  private smilesDrawerModule: any = null;
  private smilesDrawerLoading = false;

  // Non-math LaTeX commands that should be treated as text/HTML
  private nonMathPattern =
    /\\{1,2}(begin|end)\\{(enumerate|itemize|document|figure|table|center)\\}|\\{1,2}item|\\{1,2}section|\\{1,2}subsection/;

  constructor(private sanitizer: DomSanitizer, private el: ElementRef) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['content'] || changes['inline']) {
      this.render();
    }
  }

  /**
   * After each view update, find unrendered SMILES svg elements
   * and draw them using SmilesDrawer 2.0 SvgDrawer if pending.
   */
  ngAfterViewChecked(): void {
    if (!this.pendingSmilesRender) return;
    const svgs = this.el.nativeElement.querySelectorAll('svg[data-smiles]:not([data-smiles-drawn])');
    if (svgs.length === 0) return;
    this.pendingSmilesRender = false;
    this.drawSmilesSvgs(svgs);
  }

  private render(): void {
    if (!this.content || !this.content.trim()) {
      this.renderedHtml = '';
      return;
    }

    try {
      // 1. Unescape literal \n, \r\n, \t sequences if received as raw text
      let text = this.unescapeNewlines(this.content.trim());

      // 2. Decode common HTML entities that might surround math
      text = this.decodeHtmlEntities(text);

      // 3. Extract SVG blocks as opaque placeholders FIRST (FIX #4)
      const { processedText: textWithoutSvg, tokens: svgTokens } = this.extractSvgBlocks(text);
      text = textWithoutSvg;

      // 3b. Extract <smiles>...</smiles> blocks — convert to svg placeholders (Issue #126 / #144)
      const { processedText: textWithoutSmiles, smilesItems } = this.extractSmilesBlocks(text);
      text = textWithoutSmiles;

      // 4. Extract and render all LaTeX math expressions to placeholders
      const { processedText: textWithoutMath, tokens: mathTokens } = this.extractAndRenderMath(text);
      text = textWithoutMath;

      // 5. Normalize pipe tables
      if (!this.inline) {
        text = this.normalizeMarkdownTables(text);
      }

      // 6. Clean LaTeX document-level commands outside math blocks
      text = this.cleanLatexDocCommands(text);

      // 7. Pre-process inline markdown formatting
      text = this.parseInlineMarkdown(text);

      // 8. Parse Markdown using marked
      let parsedHtml = '';
      if (this.inline) {
        parsedHtml = marked.parseInline(text, {
          gfm: true,
          breaks: true
        }) as string;
      } else {
        parsedHtml = marked.parse(text, {
          gfm: true,
          breaks: true,
          async: false
        }) as string;
      }

      // 9. Re-insert LaTeX math placeholders
      mathTokens.forEach((renderedKatex, placeholder) => {
        parsedHtml = parsedHtml.split(placeholder).join(renderedKatex);
      });

      // 10. Re-insert SMILES placeholders & mark for svg drawing
      if (smilesItems.length > 0) {
        smilesItems.forEach(({ placeholder, svgHtml }) => {
          parsedHtml = parsedHtml.split(placeholder).join(svgHtml);
        });
        this.pendingSmilesRender = true;
      }

      // 11. Re-insert SVG block placeholders LAST
      svgTokens.forEach((svgContent, placeholder) => {
        parsedHtml = parsedHtml.split(placeholder).join(svgContent);
      });

      this.renderedHtml = this.sanitizer.bypassSecurityTrustHtml(parsedHtml);
    } catch {
      this.renderedHtml = this.sanitizer.bypassSecurityTrustHtml(
        `<span class="math-render-fallback">${this.escapeHtml(this.content || '')}</span>`
      );
    }
  }

  private shouldDisplayBlock(cleanMatch: string, fullMatch: string, offset: number, fullStr: string): boolean {
    if (cleanMatch.includes('\\begin{align') || cleanMatch.includes('\\begin{equation')) {
      return true;
    }
    const before = fullStr.substring(0, offset);
    const after = fullStr.substring(offset + fullMatch.length);
    const isLineStart = /(?:^|\n)\s*$/.test(before);
    const isLineEnd = /^\s*(?:\n|$)/.test(after);
    return isLineStart && isLineEnd;
  }

  private extractAndRenderMath(text: string): { processedText: string; tokens: Map<string, string> } {
    const tokens = new Map<string, string>();
    let tokenIdx = 0;

    const createPlaceholder = (rendered: string): string => {
      const placeholder = `%%%NAG_MATH_BLOCK_${tokenIdx++}%%%`;
      tokens.set(placeholder, rendered);
      return placeholder;
    };

    // 1. Display Math: $$ ... $$
    text = text.replace(/\\\\?\${2}([\s\S]*?)\\\\?\${2}/g, (_, math) => {
      const rendered = this.renderKatex(math, true);
      return createPlaceholder(rendered);
    });

    // 2. Display Math: \[ ... \]
    text = text.replace(/\\{1,4}\[([\s\S]*?)\\\\{1,4}\]/g, (_, math) => {
      const rendered = this.renderKatex(math, true);
      return createPlaceholder(rendered);
    });

    // 3. LaTeX environments: \begin{...}...\end{...}
    const envRegex = /\\{1,4}begin\{(matrix|pmatrix|bmatrix|vmatrix|Vmatrix|cases|align|align\*|aligned|equation|equation\*|gather|gather\*)\}([\s\S]*?)\\{1,4}end\{\1\}/g;
    text = text.replace(envRegex, (fullMatch, _env, _inner, offset, fullStr) => {
      const cleanMatch = fullMatch.replace(/\\{2,}/g, '\\');
      const isDisplay = this.shouldDisplayBlock(cleanMatch, fullMatch, offset, fullStr);
      const rendered = this.renderKatex(cleanMatch, isDisplay);
      return createPlaceholder(rendered);
    });

    // 4. Inline Math: \( ... \)
    text = text.replace(/\\{1,4}\(([\s\S]*?)\\{1,4}\)/g, (_, math) => {
      const rendered = this.renderKatex(math, false);
      return createPlaceholder(rendered);
    });

    // 5. Inline Math: $ ... $
    text = text.replace(/(^|[^\\])\$([^$\n\r]+?)\$(?!\$)/g, (match, prefix, math) => {
      const rendered = this.renderKatex(math, false);
      return (prefix || '') + createPlaceholder(rendered);
    });

    return { processedText: text, tokens };
  }

  private parseInlineMarkdown(text: string): string {
    if (!text) return '';
    return text
      .replace(/\*\*\*([^*\n\r]+?)\*\*\*/g, '<strong><em>$1</em></strong>')
      .replace(/___([^_\n\r]+?)___/g, '<strong><em>$1</em></strong>')
      .replace(/\*\*([^*\n\r]+?)\*\*/g, '<strong>$1</strong>')
      .replace(/__([^_\n\r]+?)__/g, '<strong>$1</strong>')
      .replace(/~~([^~\n\r]+?)~~/g, '<del>$1</del>')
      .replace(/`([^`\n\r]+?)`/g, '<code>$1</code>')
      .replace(/(^|[^*])\*([^*\n\r]+?)\*([^*]|$)/g, '$1<em>$2</em>$3')
      .replace(/(^|[^a-zA-Z0-9_])_([^_\n\r]+?)_([^a-zA-Z0-9_]|$)/g, '$1<em>$2</em>$3');
  }

  private unescapeNewlines(text: string): string {
    if (typeof text !== 'string') return '';
    return text
      .replace(/\\r\\n/g, '\n')
      .replace(/\\n(?![a-z])/g, '\n')
      .replace(/\\t(?![a-z])/g, '\t');
  }

  private decodeHtmlEntities(text: string): string {
    if (!text) return '';
    return text
      .replace(/&nbsp;/g, ' ')
      .replace(/&amp;/g, '&')
      .replace(/&lt;/g, '<')
      .replace(/&gt;/g, '>')
      .replace(/&quot;/g, '"')
      .replace(/&#39;/g, "'")
      .replace(/&rsquo;/g, "'")
      .replace(/&lsquo;/g, "'")
      .replace(/&rdquo;/g, '"')
      .replace(/&ldquo;/g, '"')
      .trim();
  }

  private normalizeMarkdownTables(text: string): string {
    if (!text.includes('|')) return text;

    const lines = text.split('\n');
    let inTable = false;
    let tableHeaderCols = 0;
    const newLines: string[] = [];

    for (let i = 0; i < lines.length; i++) {
      const line = lines[i].trim();
      if (line.startsWith('|') && line.endsWith('|') && line.length > 2) {
        if (!inTable) {
          inTable = true;
          const colCount = line.split('|').length - 2;
          tableHeaderCols = colCount;
          newLines.push(line);
          const nextLine = (lines[i + 1] || '').trim();
          if (!nextLine.startsWith('|') || !nextLine.includes('-')) {
            newLines.push('|' + Array(Math.max(1, tableHeaderCols)).fill('---').join('|') + '|');
          }
        } else {
          newLines.push(line);
        }
      } else {
        inTable = false;
        newLines.push(lines[i]);
      }
    }

    return newLines.join('\n');
  }

  private cleanLatexDocCommands(text: string): string {
    if (!text) return '';
    return text
      .replace(/\\{1,2}begin\{enumerate\}/gi, '')
      .replace(/\\{1,2}end\{enumerate\}/gi, '')
      .replace(/\\{1,2}begin\{itemize\}/gi, '')
      .replace(/\\{1,2}end\{itemize\}/gi, '')
      .replace(/\\{1,2}item\s*/gi, '\n- ')
      .replace(/\\{1,2}textbf\{([^}]*)\}/gi, '**$1**')
      .replace(/\\{1,2}textit\{([^}]*)\}/gi, '*$1*')
      .replace(/\\\\(\s|$)/g, '\n$1');
  }

  private sanitizeLatex(latex: string): string {
    if (!latex || typeof latex !== 'string') return '';
    let s = latex.trim();
    let previous: string;
    do {
      previous = s;
      s = s.replace(/\\{2,}([a-zA-Z]+|[{}_#$%&^~])/g, '\\$1');
    } while (s !== previous);
    // Ensure bare % is escaped for KaTeX
    s = s.replace(/(?<!\\)%/g, '\\%');
    return s;
  }

  private extractSvgBlocks(text: string): { processedText: string; tokens: Map<string, string> } {
    const tokens = new Map<string, string>();
    let idx = 0;
    const processedText = text.replace(/<svg[\s\S]*?<\/svg>/gi, (match) => {
      const placeholder = `%%%NAG_SVG_BLOCK_${idx++}%%%`;
      tokens.set(placeholder, match);
      return placeholder;
    });
    return { processedText, tokens };
  }

  private renderKatex(latex: string, displayMode = false): string {
    const trimmed = this.sanitizeLatex(latex);
    if (!trimmed) return '';

    if (this.nonMathPattern.test(trimmed)) {
      return `<span>${this.escapeHtml(trimmed)}</span>`;
    }

    try {
      return katex.renderToString(trimmed, {
        throwOnError: false,
        displayMode,
        output: 'htmlAndMathml',
        trust: false,
        strict: 'ignore'
      });
    } catch {
      return `<span class="math-render-error text-amber-600 font-mono text-xs">${this.escapeHtml(trimmed)}</span>`;
    }
  }

  /**
   * Extracts <smiles ...>...</smiles> blocks and replaces them with
   * `<svg data-smiles="...">` placeholder elements for SmilesDrawer SvgDrawer rendering.
   *
   * Addresses Issue #126 / #144: Supports width, height, theme, and title attributes.
   */
  private extractSmilesBlocks(text: string): {
    processedText: string;
    smilesItems: { placeholder: string; svgHtml: string }[];
  } {
    const smilesItems: { placeholder: string; svgHtml: string }[] = [];
    let idx = 0;

    const processedText = text.replace(
      /<smiles(?:\s+([^>]*?))?>([\s\S]*?)<\/smiles>(?:\s*<!--\s*(.*?)\s*-->)?/gi,
      (_match, rawAttrs: string | undefined, smiles: string, commentTitle: string | undefined) => {
        const placeholder = `%%%NAG_SMILES_BLOCK_${idx++}%%%`;
        const attrs = rawAttrs || '';
        const titleMatch = attrs.match(/title="([^"]*)"/i);
        const widthMatch = attrs.match(/width="(\d+)"/i);
        const heightMatch = attrs.match(/height="(\d+)"/i);
        const themeMatch = attrs.match(/theme="(light|dark)"/i);

        const title = titleMatch ? titleMatch[1] : (commentTitle?.trim() || undefined);
        const width = widthMatch ? widthMatch[1] : '260';
        const height = heightMatch ? heightMatch[1] : '200';
        const theme = themeMatch ? themeMatch[1] : 'light';

        const escapedSmiles = smiles.trim().replace(/"/g, '&quot;');
        const titleHtml = title
          ? `<div class="smiles-caption">${this.escapeHtml(title.trim())}</div>`
          : '';
        const svgHtml =
          `<span class="smiles-block smiles-block--${theme}">`
          + `<svg width="${width}" height="${height}" data-smiles="${escapedSmiles}" data-theme="${theme}" class="smiles-svg"></svg>`
          + titleHtml
          + `</span>`;
        smilesItems.push({ placeholder, svgHtml });
        return placeholder;
      }
    );

    return { processedText, smilesItems };
  }

  /**
   * Draw SMILES structures onto SVG elements after they are in the DOM.
   * Uses SmilesDrawer 2.0 SvgDrawer with lazy dynamic import.
   */
  private drawSmilesSvgs(svgs: NodeListOf<SVGElement>): void {
    this.loadSmilesDrawer().then(sdModule => {
      if (!sdModule) return;
      const SmilesDrawer = sdModule.default ?? sdModule;
      const SvgDrawer = SmilesDrawer.SvgDrawer ?? sdModule.SvgDrawer;
      if (!SvgDrawer) return;

      svgs.forEach((svg) => {
        const smiles = svg.getAttribute('data-smiles') || '';
        const theme = svg.getAttribute('data-theme') || 'light';
        const width = parseInt(svg.getAttribute('width') || '260', 10);
        const height = parseInt(svg.getAttribute('height') || '200', 10);
        if (!smiles) return;

        try {
          svg.setAttribute('data-smiles-drawn', '1');
          const drawer = new SvgDrawer({ width, height, compactDrawing: false });

          if (typeof SmilesDrawer.parse === 'function') {
            SmilesDrawer.parse(
              smiles,
              (tree: any) => {
                drawer.draw(tree, svg, theme, null, false);
              },
              () => {
                this.fallbackSmilesSvg(svg, smiles);
              }
            );
          } else if (SmilesDrawer.Parser?.parse) {
            const tree = SmilesDrawer.Parser.parse(smiles);
            drawer.draw(tree, svg, theme, null, false);
          }
        } catch {
          this.fallbackSmilesSvg(svg, smiles);
        }
      });
    });
  }

  private fallbackSmilesSvg(svg: SVGElement, smiles: string): void {
    while (svg.firstChild) {
      svg.removeChild(svg.firstChild);
    }
    const textNode = document.createElementNS('http://www.w3.org/2000/svg', 'text');
    textNode.setAttribute('x', '10');
    textNode.setAttribute('y', '30');
    textNode.setAttribute('font-family', 'monospace');
    textNode.setAttribute('font-size', '12');
    textNode.setAttribute('fill', '#d32f2f');
    textNode.textContent = smiles;
    svg.appendChild(textNode);
  }

  /** Lazy-load smiles-drawer module and cache it. */
  private async loadSmilesDrawer(): Promise<any> {
    if (this.smilesDrawerModule) return this.smilesDrawerModule;
    if (this.smilesDrawerLoading) return null;
    this.smilesDrawerLoading = true;
    try {
      this.smilesDrawerModule = await import('smiles-drawer');
    } catch {
      this.smilesDrawerModule = null;
    }
    this.smilesDrawerLoading = false;
    return this.smilesDrawerModule;
  }

  private escapeHtml(str: string): string {
    return str
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }
}
