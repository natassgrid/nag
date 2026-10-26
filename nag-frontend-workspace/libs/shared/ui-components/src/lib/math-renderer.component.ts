import {
  Component,
  ChangeDetectionStrategy,
  ElementRef,
  OnChanges,
  SimpleChanges,
  computed,
  inject,
  input,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import katex from 'katex';
import 'katex/dist/contrib/mhchem.mjs';
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
  content = input<string | null | undefined>('');
  inline = input<boolean>(false);

  private sanitizer = inject(DomSanitizer);
  private el = inject(ElementRef);

  readonly renderedHtml = computed<SafeHtml>(() => {
    return this.renderToSafeHtml(this.content(), this.inline());
  });

  ngOnChanges(_changes: SimpleChanges): void {
    // Signal-based computed renderedHtml automatically tracks inputs reactively
  }

  /**
   * Repairs control character artifacts from JSON unescaping
   * (e.g. form-feed for \frac, tab for \times, \theta, \tau, etc.)
   */
  private repairEscapedArtifacts(text: string): string {
    return text
      .replace(/\x0crac/g, '\\frac')
      .replace(/\x0corall/g, '\\forall')
      .replace(/\x0clat/g, '\\flat')
      .replace(/\t+imes/g, '\\times')
      .replace(/\t+heta/g, '\\theta')
      .replace(/\t+au/g, '\\tau')
      .replace(/\t+ext(?=[^a-zA-Z]|$)/g, '\\text')
      .replace(/\t+an(?=[^a-zA-Z]|$)/g, '\\tan')
      .replace(/\t+o(?=[^a-zA-Z]|$)/g, '\\to')
      .replace(/\r+ho/g, '\\rho')
      .replace(/\r+ight/g, '\\right')
      .replace(/\x08eta/g, '\\beta')
      .replace(/\x08egin/g, '\\begin')
      .replace(/\x08ar(?=[^a-zA-Z]|$)/g, '\\bar')
      .replace(/\x08ullet/g, '\\bullet')
      .replace(/\x08oxed/g, '\\boxed');
  }

  /**
   * Normalizes formulas before passing them to KaTeX:
   * 1. Fixes common JSON unescape artifacts (e.g. form-feed `\frac`, tab `\times`).
   * 2. Normalizes escaped braces in `\ce\{...\}`, `\ce\{...}`, `\\ce\{...}` to standard `\ce{...}`.
   * 3. Normalizes bare precipitate/gas arrows (`v` and `^`) in `\ce` to `(v)` and `(^)`.
   * 4. Collapses double-escaped backslashes `\\<letter>`, `\\%`, and LaTeX command spaces/punctuation
   *    unless the formula is a multi-row environment (`\begin{...}`).
   */
  private normalizeFormula(formula: string): string {
    let trimmed = this.repairEscapedArtifacts(formula.trim());

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

  /**
   * Extracts and renders standalone \ce{...} or \ce\{...\} blocks that appear in text
   * outside standard $...$ or $$...$$ delimiters.
   */
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

  private renderToSafeHtml(
    rawContent: string | null | undefined,
    forceInline: boolean
  ): SafeHtml {
    if (!rawContent || !rawContent.trim()) {
      return '';
    }

    try {
      let text = this.repairEscapedArtifacts(rawContent.trim());

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

      return this.sanitizer.bypassSecurityTrustHtml(parsedHtml);
    } catch {
      return this.sanitizer.bypassSecurityTrustHtml(rawContent);
    }
  }
}
