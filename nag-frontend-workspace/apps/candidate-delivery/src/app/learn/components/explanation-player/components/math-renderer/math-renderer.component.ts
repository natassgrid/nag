import {
  Component,
  ChangeDetectionStrategy,
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
  selector: 'nag-explanation-math-renderer',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div [innerHTML]="renderedHtml()" [class.inline-block]="inline()" [class.block]="!inline()"></div>`,
  styles: [
    `
      :host {
        display: block;
        width: 100%;
      }
      .math-block {
        overflow-x: auto;
        padding: 0.5rem 0;
        text-align: center;
      }
    `,
  ],
})
export class MathRendererComponent implements OnChanges {
  content = input<string | null>('');
  inline = input<boolean>(false);

  renderedHtml = signal<SafeHtml>('');

  private readonly sanitizer = inject(DomSanitizer);

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['content'] || changes['inline']) {
      this.render();
    }
  }

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

      // Display math \[...\]
      text = text.replace(/\\\[([\s\S]*?)\\\]/g, (match, formula) => {
        try {
          return `<div class="math-block">${this.toKatex(formula, true)}</div>`;
        } catch {
          return match;
        }
      });

      // $$...$$
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

      // Inline math \(...\)
      text = text.replace(/\\\(([\s\S]*?)\\\)/g, (match, formula) => {
        try {
          return this.toKatex(formula, false);
        } catch {
          return match;
        }
      });

      // Inline math $...$
      text = text.replace(/\$([^$\n]+?)\$/g, (match, formula) => {
        try {
          return this.toKatex(formula, false);
        } catch {
          return match;
        }
      });

      const parsedHtml = marked.parse(text) as string;
      this.renderedHtml.set(this.sanitizer.bypassSecurityTrustHtml(parsedHtml));
    } catch {
      this.renderedHtml.set(this.sanitizer.bypassSecurityTrustHtml(rawContent));
    }
  }
}
