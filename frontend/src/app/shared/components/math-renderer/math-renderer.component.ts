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

import { Component, Input, OnChanges, SimpleChanges, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import katex from 'katex';
import { marked } from 'marked';

/**
 * MathRendererComponent renders mixed content containing:
 * - Markdown text formatting (headings, lists, bold, italics, code, tables)
 * - LaTeX math expressions wrapped in $$...$$, $...$, \(...\), or \[...\]
 * - Newline representations including literal \n and GFM breaks
 * - ASCII/pipe matrices and tables
 * - Inline SVG elements
 *
 * Usage:
 *   <app-math-renderer [content]="questionContent"></app-math-renderer>
 *   <app-math-renderer [content]="optionText" [inline]="true"></app-math-renderer>
 */
@Component({
  selector: 'app-math-renderer',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './math-renderer.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./math-renderer.component.scss']
})
export class MathRendererComponent implements OnChanges {
  /** The raw content string containing mixed text, Markdown, LaTeX, and SVG. */
  @Input() content: string = '';

  /** When true, renders inline without wrapping in <p> tags. */
  @Input() inline: boolean = false;

  /** The fully rendered HTML output. */
  renderedHtml: SafeHtml = '';

  /** Non-math LaTeX commands that should be rendered as plain text/HTML. */
  private static readonly NON_MATH_PATTERN = /\\(begin|end)\\{(enumerate|itemize|document|figure|table|center)\\}|\\item|\\textbf|\\textit|\\section|\\subsection/;

  constructor(private sanitizer: DomSanitizer) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['content'] || changes['inline']) {
      this.renderContent();
    }
  }

  private renderContent(): void {
    if (!this.content) {
      this.renderedHtml = '';
      return;
    }

    try {
      // 1. Unescape literal \n or \r\n sequences if received as raw text
      let text = this.unescapeNewlines(this.content);

      // 2. Decode HTML entities
      text = this.decodeHtmlEntities(text);

      // 3. Format ASCII / pipe matrices and tables missing separator rows
      text = this.formatMarkdownTables(text);

      // 4. Normalize alternate LaTeX math delimiters to $$...$$
      text = this.normalizeMathDelimiters(text);

      // 5. Clean LaTeX document commands
      text = this.cleanLatexDocCommands(text);

      // 6. Convert single-dollar $math$ into $$math$$
      text = text.replace(/(^|[^$\\\\])\$([^$\n\r]+?)\$([^$]|$)/g, '$1$$$$$2$$$$$3');

      // 7. Extract math blocks and replace with unique placeholders
      const mathTokens: { placeholder: string; rendered: string }[] = [];
      let tokenIndex = 0;

      const mathRegex = /\$\$([\s\S]*?)\$\$/g;
      text = text.replace(mathRegex, (_, latexContent) => {
        const placeholder = `%%%NAG_MATH_BLOCK_${tokenIndex++}%%%`;
        const rendered = this.renderKatex(latexContent);
        mathTokens.push({ placeholder, rendered });
        return placeholder;
      });

      // 8. Transform inline Markdown constructs (bold, italic, strike, code)
      // so they render properly even inside HTML block tags (<p>, <div>) produced by rich editors
      text = this.parseInlineMarkdown(text);

      // 9. Parse Markdown using marked
      let parsedHtml: string;
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

      // 10. Secondary pass for any inline markdown in HTML blocks passed through by marked
      parsedHtml = this.parseInlineMarkdown(parsedHtml);

      // 11. Reinsert rendered KaTeX math blocks
      for (const token of mathTokens) {
        parsedHtml = parsedHtml.split(token.placeholder).join(token.rendered);
      }

      this.renderedHtml = this.sanitizer.bypassSecurityTrustHtml(parsedHtml);
    } catch (e) {
      console.warn('Failed to parse Markdown / Math content:', e);
      this.renderedHtml = this.sanitizer.bypassSecurityTrustHtml(this.content);
    }
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
      // Italic: *text* (when not part of a math block or token)
      .replace(/(^|[^*])\*([^*\n\r]+?)\*([^*]|$)/g, '$1<em>$2</em>$3')
      // Italic: _text_ (when surrounded by non-alphanumeric boundaries)
      .replace(/(^|[^a-zA-Z0-9_])_([^_\n\r]+?)_([^a-zA-Z0-9_]|$)/g, '$1<em>$2</em>$3');
  }

  private unescapeNewlines(text: string): string {
    if (typeof text !== 'string') return '';
    return text
      .replace(/\\r\\n/g, '\n')
      .replace(/\\n/g, '\n')
      .replace(/\\t/g, '\t');
  }

  private decodeHtmlEntities(content: string): string {
    return content
      .replace(/&nbsp;/g, ' ')
      .replace(/&amp;/g, '&')
      .replace(/&lt;/g, '<')
      .replace(/&gt;/g, '>')
      .replace(/&quot;/g, '"')
      .replace(/&#39;/g, "'")
      .replace(/&ldquo;/g, '\u201c')
      .replace(/&rdquo;/g, '\u201d')
      .replace(/&lsquo;/g, '\u2018')
      .replace(/&rsquo;/g, '\u2019')
      .trim();
  }

  private formatMarkdownTables(text: string): string {
    const lines = text.split('\n');
    const resultLines: string[] = [];
    let i = 0;

    while (i < lines.length) {
      const line = lines[i];
      const trimmed = line.trim();

      const isPipeRow = trimmed.startsWith('|') && trimmed.endsWith('|') && trimmed.length > 2;

      if (isPipeRow) {
        const tableBlock: string[] = [line];
        let j = i + 1;
        while (j < lines.length && lines[j].trim().startsWith('|') && lines[j].trim().endsWith('|')) {
          tableBlock.push(lines[j]);
          j++;
        }

        if (tableBlock.length >= 1) {
          const hasDelimiter = tableBlock.some(row => /^\s*\|(\s*:?-+:?\s*\|)+\s*$/.test(row.trim()));
          if (!hasDelimiter) {
            const colCount = tableBlock[0].trim().split('|').filter(c => c.trim().length > 0).length;
            if (colCount > 0) {
              const delimiterRow = '|' + ' --- |'.repeat(colCount);
              tableBlock.splice(1, 0, delimiterRow);
            }
          }
          resultLines.push(...tableBlock);
          i = j;
          continue;
        }
      }

      resultLines.push(line);
      i++;
    }

    return resultLines.join('\n');
  }

  private normalizeMathDelimiters(content: string): string {
    return content
      .replace(/\\\(([\s\S]*?)\\\)/g, '$$$$$1$$$$')
      .replace(/\\\[([\s\S]*?)\\\]/g, '$$$$$1$$$$');
  }

  private cleanLatexDocCommands(content: string): string {
    return content
      .replace(/\\begin\{enumerate\}/g, '')
      .replace(/\\end\{enumerate\}/g, '')
      .replace(/\\begin\{itemize\}/g, '')
      .replace(/\\end\{itemize\}/g, '')
      .replace(/\\item\s*/g, '\n- ')
      .replace(/\\textbf\{([^}]*)\}/g, '**$1**')
      .replace(/\\textit\{([^}]*)\}/g, '*$1*')
      .replace(/\\\\(\s|$)/g, '\n$1');
  }

  private sanitizeLatex(latex: string): string {
    let s = latex.trim();
    s = s.replace(/\\*%/g, '\\%');
    return s;
  }

  private renderKatex(latex: string): string {
    const trimmed = this.sanitizeLatex(latex);
    if (!trimmed) {
      return '';
    }

    if (MathRendererComponent.NON_MATH_PATTERN.test(trimmed)) {
      return `<span class="math-as-text">${this.escapeHtml(trimmed)}</span>`;
    }

    try {
      return katex.renderToString(trimmed, {
        throwOnError: false,
        displayMode: false,
        output: 'htmlAndMathml',
        strict: 'ignore'
      });
    } catch (e) {
      return `<span class="math-render-error" title="Failed to render LaTeX">${this.escapeHtml(trimmed)}</span>`;
    }
  }

  private escapeHtml(text: string): string {
    return text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }
}
