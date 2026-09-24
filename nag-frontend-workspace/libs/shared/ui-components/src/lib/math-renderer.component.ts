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

  private render(): void {
    const rawContent = this.content();
    if (!rawContent || !rawContent.trim()) {
      this.renderedHtml.set('');
      return;
    }

    try {
      let text = rawContent.trim();

      // Render KaTeX display math $$...$$
      text = text.replace(/\$\$([\s\S]*?)\$\$/g, (match, formula) => {
        try {
          return `<div class="math-block">${katex.renderToString(formula.trim(), {
            displayMode: true,
            throwOnError: false,
          })}</div>`;
        } catch {
          return match;
        }
      });

      // Render KaTeX inline math $...$
      text = text.replace(/\$([^$\n]+?)\$/g, (match, formula) => {
        try {
          return katex.renderToString(formula.trim(), {
            displayMode: false,
            throwOnError: false,
          });
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
