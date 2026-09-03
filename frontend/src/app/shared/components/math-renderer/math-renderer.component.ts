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

import { Component, Input, OnChanges, SimpleChanges, ElementRef, SecurityContext, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import katex from 'katex';

/**
 * Represents a parsed segment of mixed content.
 * Each segment is either a math block (rendered via KaTeX) or raw HTML/text/SVG.
 */
interface ContentSegment {
  type: 'math' | 'html';
  content: string;
  rendered?: string;
}

/**
 * MathRendererComponent renders mixed content containing:
 * - Plain text / HTML
 * - LaTeX math expressions wrapped in $$...$$
 * - Inline SVG elements
 *
 * Also handles LLM-generated content that incorrectly uses LaTeX document
 * commands (\begin{enumerate}, \item, etc.) by converting them to HTML.
 *
 * Usage:
 *   <app-math-renderer [content]="questionContent"></app-math-renderer>
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
  /** The raw content string containing mixed text, $$LaTeX$$, and SVG. */
  @Input() content: string = '';

  /** The fully rendered HTML output (sanitized for SVG, KaTeX for math). */
  renderedHtml: SafeHtml = '';

  /** Non-math LaTeX commands that should be rendered as plain text/HTML. */
  private static readonly NON_MATH_PATTERN = /\\(begin|end)\{(enumerate|itemize|document|figure|table|center)\}|\\item|\\textbf|\\textit|\\section|\\subsection/;

  constructor(private sanitizer: DomSanitizer) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['content']) {
      this.renderContent();
    }
  }

  private renderContent(): void {
    if (!this.content) {
      this.renderedHtml = '';
      return;
    }

    // Decode HTML entities (rich text editors convert spaces to &nbsp; etc.)
    const decoded = this.decodeHtmlEntities(this.content);
    // Normalize \( ... \) and \[ ... \] delimiters to $$...$$ so all sources render consistently
    const normalized = this.normalizeMathDelimiters(decoded);
    // Pre-clean: convert LaTeX document commands to readable HTML
    const cleaned = this.cleanLatexDocCommands(normalized);
    const segments = this.parseContent(cleaned);
    const html = segments.map(seg => seg.rendered ?? seg.content).join('');

    // Bypass security to allow SVG and KaTeX HTML output
    this.renderedHtml = this.sanitizer.bypassSecurityTrustHtml(html);
  }

  /**
   * Decodes HTML entities that rich text editors introduce.
   * Converts &nbsp; to space, &amp; to &, &lt;/&gt; to angle brackets, etc.
   * Also strips wrapping <p> tags that editors add around content.
   */
  private decodeHtmlEntities(content: string): string {
    return content
      .replace(/&nbsp;/g, ' ')
      .replace(/&amp;/g, '&')
      .replace(/&lt;/g, '<')
      .replace(/&gt;/g, '>')
      .replace(/&quot;/g, '"')
      .replace(/&#39;/g, "'")
      .trim();
  }

  /**
   * Normalizes alternate LaTeX math delimiters to the canonical $$...$$ form.
   * LLMs and external sources frequently emit inline \( ... \) or display
   * \[ ... \] delimiters. Converting them here means the parser only needs to
   * handle $$...$$, and content renders consistently regardless of its origin.
   * Existing $$...$$ spans are left untouched.
   */
  private normalizeMathDelimiters(content: string): string {
    return content
      .replace(/\\\(([\s\S]*?)\\\)/g, '$$$$$1$$$$')
      .replace(/\\\[([\s\S]*?)\\\]/g, '$$$$$1$$$$');
  }

  /**
   * Converts LaTeX document-structure commands to readable HTML.
   * Handles cases where LLMs output \begin{enumerate}, \item etc. as content.
   */
  private cleanLatexDocCommands(content: string): string {
    return content
      .replace(/\\begin\{enumerate\}/g, '')
      .replace(/\\end\{enumerate\}/g, '')
      .replace(/\\begin\{itemize\}/g, '')
      .replace(/\\end\{itemize\}/g, '')
      .replace(/\\item\s*/g, '<br>\u2022 ')
      .replace(/\\textbf\{([^}]*)\}/g, '<strong>$1</strong>')
      .replace(/\\textit\{([^}]*)\}/g, '<em>$1</em>')
      .replace(/\\\\(\s)/g, '<br>$1');
  }

  /**
   * Parses content into segments, splitting on $$...$$ math blocks.
   * Non-math segments retain their original HTML/SVG content.
   */
  private parseContent(content: string): ContentSegment[] {
    let cleanContent = content;
    if (!cleanContent) return [];

    // Convert single-dollar $math$ to $$math$$ (enclosed in $$...$$)
    cleanContent = cleanContent.replace(/(^|[^$])\$([^$\n]+)\$([^$]|$)/g, '$1$$$$$2$$$$$3');

    // Handle unmatched $$ delimiters (e.g. stray $$ at start of non-LaTeX questions)
    const matches = cleanContent.match(/\$\$/g);
    if (matches && matches.length % 2 !== 0) {
      if (cleanContent.startsWith('$$')) {
        cleanContent = cleanContent.substring(2);
      } else if (cleanContent.endsWith('$$')) {
        cleanContent = cleanContent.substring(0, cleanContent.length - 2);
      }
    }

    const segments: ContentSegment[] = [];
    // Match $$...$$ (non-greedy, handles multiline)
    const mathRegex = /\$\$([\s\S]*?)\$\$/g;
    let lastIndex = 0;
    let match: RegExpExecArray | null;

    while ((match = mathRegex.exec(cleanContent)) !== null) {
      // Add any text/HTML/SVG before this math block
      if (match.index > lastIndex) {
        const htmlContent = cleanContent.substring(lastIndex, match.index);
        segments.push({ type: 'html', content: htmlContent, rendered: htmlContent });
      }

      // Render the math block with KaTeX
      const latex = match[1];
      segments.push({
        type: 'math',
        content: latex,
        rendered: this.renderKatex(latex)
      });

      lastIndex = match.index + match[0].length;
    }

    // Add any remaining content after the last math block
    if (lastIndex < cleanContent.length) {
      const remaining = cleanContent.substring(lastIndex);
      segments.push({ type: 'html', content: remaining, rendered: remaining });
    }

    return segments;
  }

  /**
   * Renders a LaTeX string to HTML using KaTeX.
   * If the content contains non-math LaTeX commands (enumerate, item, etc.),
   * it is displayed as plain text instead of attempting math rendering.
   */
  private renderKatex(latex: string): string {
    if (!latex || !latex.trim()) {
      return '';
    }

    // If it contains document-structure LaTeX commands, display as plain text
    if (MathRendererComponent.NON_MATH_PATTERN.test(latex)) {
      return `<span class="math-as-text">${this.escapeHtml(latex)}</span>`;
    }

    try {
      return katex.renderToString(latex.trim(), {
        throwOnError: false,
        displayMode: false,
        output: 'html'
      });
    } catch (e) {
      return `<span class="math-render-error" title="Failed to render LaTeX">${this.escapeHtml(latex)}</span>`;
    }
  }

  /** Escapes HTML special characters for safe display in error fallback. */
  private escapeHtml(text: string): string {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }
}
