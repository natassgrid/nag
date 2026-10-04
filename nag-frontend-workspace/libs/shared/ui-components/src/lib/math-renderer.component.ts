import {
  Component,
  ChangeDetectionStrategy,
  ElementRef,
  OnChanges,
  SimpleChanges,
  inject,
  input,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import katex from 'katex';
import { marked } from 'marked';

@Component({
  selector: 'nag-math-renderer',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './math-renderer.component.html',
  styleUrl: './math-renderer.component.scss',
})
export class MathRendererComponent implements OnChanges {
  content = input<string | null>('');
  inline = input<boolean>(false);

  renderedHtml = signal<SafeHtml>('');

  private sanitizer = inject(DomSanitizer);
  private el = inject(ElementRef);

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['content'] || changes['inline']) {
      this.render();
    }
  }

  /**
   * Some stored questions have double-escaped LaTeX commands (e.g. `\\sec`), which KaTeX
   * would treat as a line break followed by plain text. Collapse `\\<letter>` to
   * `\<letter>` unless the formula is a real multi-row environment (`\begin{...}`).
   */
  private normalizeFormula(formula: string): string {
    const trimmed = formula.trim();
    if (trimmed.includes('\\begin')) {
      return trimmed;
    }
    return trimmed.replace(/\\\\(?=[a-zA-Z])/g, '\\');
  }

  private toKatex(formula: string, displayMode: boolean): string {
    return katex.renderToString(this.normalizeFormula(formula), {
      displayMode,
      throwOnError: false,
    });
  }

  private render(): void {
    const rawContent = this.content();
    if (!rawContent || !rawContent.trim()) {
      this.renderedHtml.set('');
      return;
    }

    try {
      let text = rawContent.trim();
      const forceInline = this.inline();

      // Render KaTeX display math \[...\]
      text = text.replace(/\\\[([\s\S]*?)\\\]/g, (match, formula) => {
        try {
          return `<div class="math-block">${this.toKatex(formula, true)}</div>`;
        } catch {
          return match;
        }
      });

      // Render KaTeX math $$...$$
      // Treated as display (block) math only when it is multi-line or stands alone on
      // its own line(s). A $$...$$ embedded within a sentence (or any $$...$$ when the
      // renderer is in inline mode) is rendered inline so it flows with the text.
      text = text.replace(
        /\$\$([\s\S]*?)\$\$/g,
        (match, formula: string, offset: number, whole: string) => {
          try {
            const before = whole.slice(0, offset);
            const after = whole.slice(offset + match.length);
            const standalone =
              (before === '' || /\n\s*$/.test(before)) &&
              (after === '' || /^\s*\n/.test(after));
            const displayMode =
              !forceInline && (formula.includes('\n') || standalone);
            const html = this.toKatex(formula, displayMode);
            return displayMode ? `<div class="math-block">${html}</div>` : html;
          } catch {
            return match;
          }
        }
      );

      // Render KaTeX inline math \(...\)
      text = text.replace(/\\\(([\s\S]*?)\\\)/g, (match, formula) => {
        try {
          return this.toKatex(formula, false);
        } catch {
          return match;
        }
      });

      // Render KaTeX inline math $...$
      text = text.replace(/\$([^$\n]+?)\$/g, (match, formula) => {
        try {
          return this.toKatex(formula, false);
        } catch {
          return match;
        }
      });

      // Render Markdown via marked
      const parsedHtml = marked.parse(text) as string;
      this.renderedHtml.set(
        this.sanitizer.bypassSecurityTrustHtml(parsedHtml)
      );
    } catch {
      this.renderedHtml.set(
        this.sanitizer.bypassSecurityTrustHtml(rawContent)
      );
    }
  }
}
