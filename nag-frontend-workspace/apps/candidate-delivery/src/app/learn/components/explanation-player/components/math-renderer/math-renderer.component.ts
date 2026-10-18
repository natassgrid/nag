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
import 'katex/dist/contrib/mhchem.mjs';
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
  content = input<string | null | undefined>('');
  inline = input<boolean>(false);

  renderedHtml = signal<SafeHtml>('');

  private readonly sanitizer = inject(DomSanitizer);

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['content'] || changes['inline']) {
      this.render();
    }
  }

  private normalizeFormula(formula: string): string {
    let trimmed = formula.trim();

    // Repair control character artifacts from JSON unescaping
    trimmed = trimmed.replace(/\x0crac/g, '\\frac').replace(/\t+imes/g, '\\times');

    // Normalize \ce\{...\} or \\ce\{...\} to standard \ce{...}
    trimmed = trimmed.replace(/\\\\?ce\s*\\\{([\s\S]*?)\\\}/g, (_, inner) => {
      return `\\ce{${inner.replace(/\\\{/g, '{').replace(/\\\}/g, '}')}}`;
    });

    // In mhchem, bare ^ or v for gas/precipitate should be formatted as (^) or (v)
    trimmed = trimmed.replace(/(\\ce\{[^{}]*?)\s+\^(?=[}\s])/g, '$1 (^) ');
    trimmed = trimmed.replace(/(\\ce\{[^{}]*?)\s+v(?=[}\s])/g, '$1 (v) ');

    if (trimmed.includes('\\begin')) {
      return trimmed;
    }
    return trimmed.replace(/\\\\(?=[a-zA-Z% ,;!])/g, '\\');
  }

  private toKatex(formula: string, displayMode: boolean): string {
    return katex.renderToString(this.normalizeFormula(formula), {
      displayMode,
      throwOnError: false,
    });
  }

  private replaceStandaloneCe(
    text: string,
    createToken: (html: string, isBlock: boolean) => string
  ): string {
    let result = '';
    let i = 0;
    while (i < text.length) {
      const sub = text.slice(i);
      const match = sub.match(/^(?:\\\\|\\)ce\s*(\\\{|\??\{)/);
      if (match) {
        const isEscaped = match[1].includes('\\');
        let depth = 1;
        const startIdx = i + match[0].length;
        let j = startIdx;
        while (j < text.length && depth > 0) {
          if (text[j] === '\\') {
            if (isEscaped && text.slice(j, j + 2) === '\\}') {
              depth--;
              if (depth === 0) {
                j += 2;
                break;
              }
              j += 2;
              continue;
            }
            if (isEscaped && text.slice(j, j + 2) === '\\{') {
              depth++;
              j += 2;
              continue;
            }
            j += 2;
            continue;
          }
          if (text[j] === '{') {
            depth++;
          } else if (text[j] === '}') {
            depth--;
            if (depth === 0) {
              j++;
              break;
            }
          }
          j++;
        }
        const rawInner = text.slice(startIdx, isEscaped ? j - 2 : j - 1);
        const cleanInner = rawInner.replace(/\\\{/g, '{').replace(/\\\}/g, '}');
        try {
          const rendered = this.toKatex(`\\ce{${cleanInner}}`, false);
          result += createToken(rendered, false);
        } catch {
          result += text.slice(i, j);
        }
        i = j;
      } else {
        result += text[i];
        i++;
      }
    }
    return result;
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

      // Repair control character artifacts from JSON unescaping
      text = text.replace(/\x0crac/g, '\\frac').replace(/\t+imes/g, '\\times');

      const mathTokens = new Map<string, string>();
      let tokenCounter = 0;

      const createToken = (html: string, isBlock: boolean): string => {
        const token = `@@@KATEX_${isBlock ? 'BLOCK' : 'INLINE'}_${tokenCounter++}@@@`;
        mathTokens.set(token, html);
        return isBlock ? `\n\n${token}\n\n` : token;
      };

      // 1. Render KaTeX display math \[...\]
      text = text.replace(/\\\[([\s\S]*?)\\\]/g, (_, formula) => {
        try {
          const html = `<div class="math-block">${this.toKatex(formula, true)}</div>`;
          return createToken(html, true);
        } catch {
          return _;
        }
      });

      // 2. Render KaTeX math $$...$$
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
            const html = displayMode
              ? `<div class="math-block">${this.toKatex(formula, true)}</div>`
              : this.toKatex(formula, false);
            return createToken(html, displayMode);
          } catch {
            return match;
          }
        }
      );

      // 3. Render KaTeX inline math \(...\)
      text = text.replace(/\\\(([\s\S]*?)\\\)/g, (_, formula) => {
        try {
          return createToken(this.toKatex(formula, false), false);
        } catch {
          return _;
        }
      });

      // 4. Render KaTeX inline math $...$
      text = text.replace(/\$([^$\n]+?)\$/g, (_, formula) => {
        try {
          return createToken(this.toKatex(formula, false), false);
        } catch {
          return _;
        }
      });

      // 5. Render standalone chemistry \ce{...} or \ce\{...\} outside math delimiters
      text = this.replaceStandaloneCe(text, createToken);

      // 6. Render parenthesized formulas: ( r = \frac{...}{...} )
      text = text.replace(
        /\(\s*([a-zA-Z0-9_{}\s+\-*/=^]+?\\[a-zA-Z]+[^()]*?)\s*\)/g,
        (_, formula) => {
          try {
            return `(${createToken(this.toKatex(formula, false), false)})`;
          } catch {
            return _;
          }
        }
      );

      // 7. Handle bare formulas without delimiters (e.g. cheat sheet items "[1 - x] = \frac{1}{100}"
      //    or finalAnswer "4.5 \times 10^3 \text{ J}")
      if (tokenCounter === 0 && /\\[a-zA-Z]+/.test(text)) {
        const lines = text.split('\n');
        const transformedLines = lines.map((line) => {
          const trimmed = line.trim();
          if (trimmed && /\\[a-zA-Z]+/.test(trimmed)) {
            const prefixMatch = trimmed.match(/^([^:\\]+:\s*)(.*?\\[a-zA-Z].*)$/);
            if (prefixMatch) {
              try {
                return `${prefixMatch[1]}${createToken(
                  this.toKatex(prefixMatch[2], false),
                  false
                )}`;
              } catch {
                return line;
              }
            } else {
              try {
                const isBlock = !forceInline;
                const html = isBlock
                  ? `<div class="math-block">${this.toKatex(trimmed, true)}</div>`
                  : this.toKatex(trimmed, false);
                return createToken(html, isBlock);
              } catch {
                return line;
              }
            }
          }
          return line;
        });
        text = transformedLines.join('\n');
      }

      // 8. Render Markdown via marked
      let parsedHtml = marked.parse(text) as string;

      // 9. Reinsert rendered KaTeX HTML tokens
      for (const [token, katexHtml] of mathTokens.entries()) {
        const blockP = new RegExp(`<p>\\s*${token}\\s*<\\/p>`, 'g');
        if (blockP.test(parsedHtml)) {
          parsedHtml = parsedHtml.replace(blockP, katexHtml);
        } else {
          parsedHtml = parsedHtml.split(token).join(katexHtml);
        }
      }

      this.renderedHtml.set(this.sanitizer.bypassSecurityTrustHtml(parsedHtml));
    } catch {
      this.renderedHtml.set(this.sanitizer.bypassSecurityTrustHtml(rawContent));
    }
  }
}
