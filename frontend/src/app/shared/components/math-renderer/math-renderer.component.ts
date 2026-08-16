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

import { Component, Input, OnChanges, SimpleChanges, ElementRef, SecurityContext } from '@angular/core';
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
 * Usage:
 *   <app-math-renderer [content]="questionContent"></app-math-renderer>
 */
@Component({
  selector: 'app-math-renderer',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './math-renderer.component.html',
  styleUrls: ['./math-renderer.component.scss']
})
export class MathRendererComponent implements OnChanges {
  /** The raw content string containing mixed text, $$LaTeX$$, and SVG. */
  @Input() content: string = '';

  /** The fully rendered HTML output (sanitized for SVG, KaTeX for math). */
  renderedHtml: SafeHtml = '';

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

    const segments = this.parseContent(this.content);
    const html = segments.map(seg => seg.rendered ?? seg.content).join('');

    // Bypass security to allow SVG and KaTeX HTML output
    this.renderedHtml = this.sanitizer.bypassSecurityTrustHtml(html);
  }

  /**
   * Parses content into segments, splitting on $$...$$ math blocks.
   * Non-math segments retain their original HTML/SVG content.
   */
  private parseContent(content: string): ContentSegment[] {
    const segments: ContentSegment[] = [];
    // Match $$...$$ (non-greedy, handles multiline)
    const mathRegex = /\$\$([\s\S]*?)\$\$/g;
    let lastIndex = 0;
    let match: RegExpExecArray | null;

    while ((match = mathRegex.exec(content)) !== null) {
      // Add any text/HTML/SVG before this math block
      if (match.index > lastIndex) {
        const htmlContent = content.substring(lastIndex, match.index);
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
    if (lastIndex < content.length) {
      const remaining = content.substring(lastIndex);
      segments.push({ type: 'html', content: remaining, rendered: remaining });
    }

    return segments;
  }

  /**
   * Renders a LaTeX string to HTML using KaTeX.
   * On error, returns the raw LaTeX wrapped in a styled error span.
   */
  private renderKatex(latex: string): string {
    try {
      return katex.renderToString(latex, {
        throwOnError: false,
        displayMode: true,
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
