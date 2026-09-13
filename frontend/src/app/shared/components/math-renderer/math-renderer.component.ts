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
  HostBinding,
  Input,
  OnChanges,
  SimpleChanges,
  ViewEncapsulation,
  ChangeDetectionStrategy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import katex from 'katex';
import { marked } from 'marked';

@Component({
  selector: 'app-math-renderer',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './math-renderer.component.html',
  styleUrls: ['./math-renderer.component.scss'],
  encapsulation: ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MathRendererComponent implements OnChanges {
  @Input() content: string | null = '';
  @Input() inline: boolean = false;
  @Input() className: string = '';

  // FIX #6: bind the 'inline-mode' class to :host when inline=true so the
  // SCSS :host.inline-mode rule takes effect and the host becomes display:inline.
  @HostBinding('class.inline-mode') get isInlineMode(): boolean {
    return this.inline;
  }

  renderedHtml: SafeHtml = '';

  // Non-math LaTeX commands that should be treated as text/HTML
  private nonMathPattern =
    /\\{1,2}(begin|end)\{(enumerate|itemize|document|figure|table|center)\}|\\{1,2}item|\\{1,2}section|\\{1,2}subsection/;

  constructor(private sanitizer: DomSanitizer) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['content'] || changes['inline']) {
      this.render();
    }
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
      // Prevents \\-to-newline and markdown processing from corrupting SVG attributes/paths
      const { processedText: textWithoutSvg, tokens: svgTokens } = this.extractSvgBlocks(text);
      text = textWithoutSvg;

      // 4. Extract and render all LaTeX math expressions to placeholders
      // This protects math formulas containing \\, _, *, &, etc. from being corrupted by Markdown or doc cleaners
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

      // 9. Secondary pass for any inline markdown in HTML blocks passed through by marked
      parsedHtml = this.parseInlineMarkdown(parsedHtml);

      // 10. Reinsert rendered KaTeX math blocks
      for (const [placeholder, rendered] of mathTokens.entries()) {
        parsedHtml = parsedHtml.split(placeholder).join(rendered);
      }

      // 11. Reinsert SVG blocks (FIX #4)
      for (const [placeholder, svgBlock] of svgTokens.entries()) {
        parsedHtml = parsedHtml.split(placeholder).join(svgBlock);
      }

      this.renderedHtml = this.sanitizer.bypassSecurityTrustHtml(parsedHtml);
    } catch (e) {
      console.warn('Failed to parse Markdown / Math content:', e);
      this.renderedHtml = this.sanitizer.bypassSecurityTrustHtml(this.content);
    }
  }

  /**
   * FIX #3 — Numbered-list / inline context:
   * A formula on a line that starts with a list marker (e.g. "1.", "I.", "-", "*")
   * followed by other text should never become a display block — it must stay inline
   * to avoid breaking statement list flow.
   */
  private shouldDisplayBlock(
    math: string,
    fullMatch: string,
    offset: number,
    fullStr: string
  ): boolean {
    if (this.inline) return false;

    // Check if directly wrapped in parentheses or brackets e.g. ($$math$$)
    const charBefore = offset > 0 ? fullStr[offset - 1] : '';
    const charAfter = offset + fullMatch.length < fullStr.length ? fullStr[offset + fullMatch.length] : '';
    const isEnclosedInParens = (charBefore === '(' || charBefore === '[') && (charAfter === ')' || charAfter === ']');
    if (isEnclosedInParens) {
      return false;
    }

    const textBefore = fullStr.slice(0, offset);
    const lastNewlineBefore = textBefore.lastIndexOf('\n');
    const linePrefix = lastNewlineBefore === -1 ? textBefore : textBefore.slice(lastNewlineBefore + 1);

    // FIX #3: list item lines (numbered, lettered, roman numerals, bullet) always stay inline
    const listItemPrefix = /^\s*(\d+\.|[IVXLC]+\.|[A-Za-z]\.|[-*•])\s/;
    if (listItemPrefix.test(linePrefix)) {
      return false;
    }

    const textAfter = fullStr.slice(offset + fullMatch.length);
    const nextNewlineAfter = textAfter.indexOf('\n');
    const lineSuffix = nextNewlineAfter === -1 ? textAfter : textAfter.slice(0, nextNewlineAfter);

    const hasSurroundingText = linePrefix.trim() !== '' || lineSuffix.trim() !== '';

    // Check if it is a single variable token (e.g. "A", "L", "\neg L", "x") embedded in running text
    const trimmedMath = math.trim();
    const isSingleVariableToken = /^(\\[a-zA-Z]+\s+)?[a-zA-Z0-9_]{1,3}$/.test(trimmedMath);

    if (isSingleVariableToken && hasSurroundingText) {
      return false;
    }

    return true;
  }

  /**
   * Identifies all LaTeX math expressions (display, environments, bracketed, parenthesis, and inline dollars)
   * and renders them into KaTeX HTML, substituting placeholders to protect the formulas.
   *
   * FIX #2 — Delimiter variations:
   * Accept 1–4 backslashes before ( ) [ ] delimiters so \\(, \\\(, \\\\( all match correctly.
   */
  private extractAndRenderMath(text: string): { processedText: string; tokens: Map<string, string> } {
    const tokens = new Map<string, string>();
    let tokenIndex = 0;

    const createPlaceholder = (rendered: string): string => {
      const placeholder = `%%%NAG_MATH_BLOCK_${tokenIndex++}%%%`;
      tokens.set(placeholder, rendered);
      return placeholder;
    };

    // 1. Math in $$ ... $$ (display mode when standalone, inline mode when in running text)
    text = text.replace(/\$\$([\s\S]*?)\$\$/g, (fullMatch, math, offset, fullStr) => {
      const isDisplay = this.shouldDisplayBlock(math, fullMatch, offset, fullStr);
      const rendered = this.renderKatex(math, isDisplay);
      return createPlaceholder(rendered);
    });

    // 2. Math in \[ ... \] — accept 1–4 leading/trailing backslashes (FIX #2)
    text = text.replace(/\\{1,4}\[([\s\S]*?)\\{1,4}\]/g, (fullMatch, math, offset, fullStr) => {
      const isDisplay = this.shouldDisplayBlock(math, fullMatch, offset, fullStr);
      const rendered = this.renderKatex(math, isDisplay);
      return createPlaceholder(rendered);
    });

    // 3. LaTeX environments: \begin{...}...\end{...} — accept 1–4 backslashes
    const envRegex = /\\{1,4}begin\{(matrix|pmatrix|bmatrix|vmatrix|Vmatrix|cases|align|align\*|aligned|equation|equation\*|gather|gather\*)\}([\s\S]*?)\\{1,4}end\{\1\}/g;
    text = text.replace(envRegex, (fullMatch, _env, _inner, offset, fullStr) => {
      // Normalize all over-escaped backslashes before passing to KaTeX
      const cleanMatch = fullMatch.replace(/\\{2,}/g, '\\');
      const isDisplay = this.shouldDisplayBlock(cleanMatch, fullMatch, offset, fullStr);
      const rendered = this.renderKatex(cleanMatch, isDisplay);
      return createPlaceholder(rendered);
    });

    // 4. Inline Math: \( ... \) — accept 1–4 leading/trailing backslashes (FIX #2)
    text = text.replace(/\\{1,4}\(([\s\S]*?)\\{1,4}\)/g, (_, math) => {
      const rendered = this.renderKatex(math, false);
      return createPlaceholder(rendered);
    });

    // 5. Inline Math: $ ... $ (avoid escaped \$ and ensure non-empty)
    text = text.replace(/(^|[^\\])\$([^\$\n\r]+?)\$(?!\$)/g, (match, prefix, math) => {
      const rendered = this.renderKatex(math, false);
      return (prefix || '') + createPlaceholder(rendered);
    });

    return { processedText: text, tokens };
  }

  private parseInlineMarkdown(text: string): string {
    if (!text) return '';
    return text
      // Bold + Italic: ***text*** or ___text___
      .replace(/\*\*\*([^\*\n\r]+?)\*\*\*/g, '<strong><em>$1</em></strong>')
      .replace(/___([^_\n\r]+?)___/g, '<strong><em>$1</em></strong>')
      // Bold: **text** or __text__
      .replace(/\*\*([^\*\n\r]+?)\*\*/g, '<strong>$1</strong>')
      .replace(/__([^_\n\r]+?)__/g, '<strong>$1</strong>')
      // Strikethrough: ~~text~~
      .replace(/~~([^~\n\r]+?)~~/g, '<del>$1</del>')
      // Inline code: `code`
      .replace(/`([^`\n\r]+?)`/g, '<code>$1</code>')
      // Italic: *text*
      .replace(/(^|[^*])\*([^*\\n\r]+?)\*([^*]|$)/g, '$1<em>$2</em>$3')
      // Italic: _text_
      .replace(/(^|[^a-zA-Z0-9_])_([^_\n\r]+?)_([^a-zA-Z0-9_]|$)/g, '$1<em>$2</em>$3');
  }

  private unescapeNewlines(text: string): string {
    if (typeof text !== 'string') return '';
    return text
      .replace(/\\r\\n/g, '\n')
      .replace(/\\n/g, '\n')
      .replace(/\\t/g, '\t');
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

  /**
   * Sanitizes LaTeX expression before passing to KaTeX.
   *
   * FIX #1 — Multi-escaped backslashes:
   * Iteratively collapse over-escaped backslash chains (e.g. \\\\det → \\det → \det)
   * until the string is stable, so every over-escaped chain is fully reduced regardless
   * of how many levels of serialization introduced the extra escapes.
   */
  private sanitizeLatex(latex: string): string {
    if (!latex || typeof latex !== 'string') return '';
    let s = latex.trim();
    let previous: string;
    do {
      previous = s;
      s = s.replace(/\\{2,}([a-zA-Z]+|[{}_#$%&^~])/g, '\\$1');
    } while (s !== previous);
    // Ensure bare % is escaped for KaTeX (% is a LaTeX comment character)
    s = s.replace(/(?<!\\)%/g, '\\%');
    return s;
  }

  /**
   * Extracts inline SVG blocks as opaque placeholders so that subsequent markdown
   * and LaTeX processing steps cannot corrupt SVG attribute values or path data.
   *
   * FIX #4 — SVG + Markdown + LaTeX integration
   */
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

  private escapeHtml(str: string): string {
    return str
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }
}
