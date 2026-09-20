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

import React, { useEffect, useRef } from 'react';
import katex from 'katex';
import { marked } from 'marked';
import 'katex/dist/katex.min.css';
// mhchem adds \ce{} (chemical equations) and \pu{} (physical units) to KaTeX.
// It ships inside the katex package — using ESM .mjs for proper Vite/esbuild bundler integration.
import 'katex/dist/contrib/mhchem.mjs';

// Module-level cache for lazy-loaded SmilesDrawer
let smilesDrawerPromise: Promise<any> | null = null;
function getSmilesDrawer(): Promise<any> {
  if (!smilesDrawerPromise) {
    smilesDrawerPromise = (async () => {
      const w = window as any;
      if (w.SmilesDrawer) return w.SmilesDrawer;
      if (w.__smilesDrawer) return w.__smilesDrawer;
      return await import('smiles-drawer');
    })();
  }
  return smilesDrawerPromise;
}

interface MathRendererProps {
  content: string;
  className?: string;
  inline?: boolean;
}

const NON_MATH_PATTERN =
  /\\{1,2}(begin|end)\\{(enumerate|itemize|document|figure|table|center)\\}|\\{1,2}item|\\{1,2}section|\\{1,2}subsection/;

// ─── Extraction & Placeholder helpers ──────────────────────────

interface SmilesItem {
  placeholder: string;
  svgHtml: string;
}

/**
 * Extracts <smiles ...>...</smiles> tags and replaces them with
 * `<svg data-smiles="...">` placeholder elements for SmilesDrawer SvgDrawer rendering.
 *
 * Addresses Issue #126 / #144: Supports width, height, theme, and title attributes.
 */
function extractSmilesBlocks(text: string): { processedText: string; smilesItems: SmilesItem[] } {
  const smilesItems: SmilesItem[] = [];
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
      const width = widthMatch ? parseInt(widthMatch[1], 10) : 260;
      const height = heightMatch ? parseInt(heightMatch[1], 10) : 200;
      const theme = themeMatch ? themeMatch[1] : 'light';

      const safeSmiles = (smiles || '').trim().replace(/"/g, '&quot;');
      const safeTitle = title ? title.replace(/"/g, '&quot;') : '';

      const captionHtml = title
        ? `<div class="chemical-structure-caption font-sans text-xs text-slate-500 dark:text-slate-400 mt-1 text-center font-medium">${escapeHtml(title)}</div>`
        : '';

      const svgHtml =
        `<span class="chemical-structure-container inline-flex flex-col items-center align-middle mx-1 my-0.5 p-1 rounded border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 shadow-xs" data-smiles-container="true">` +
        `<svg data-smiles="${safeSmiles}" data-theme="${theme}" data-title="${safeTitle}" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" class="smiles-drawer-svg block max-w-full"></svg>` +
        captionHtml +
        `</span>`;

      smilesItems.push({ placeholder, svgHtml });
      return placeholder;
    }
  );

  return { processedText, smilesItems };
}

/**
 * Extracts raw `<svg ...>...</svg>` blocks BEFORE LaTeX/Markdown processing
 * to protect inline chemical diagram SVGs from being mangled.
 */
function extractSvgBlocks(text: string): { processedText: string; svgTokens: Map<string, string> } {
  const svgTokens = new Map<string, string>();
  let idx = 0;
  const processedText = text.replace(/<svg[\s\S]*?<\/svg>/gi, (match) => {
    const placeholder = `%%%NAG_SVG_BLOCK_${idx++}%%%`;
    svgTokens.set(placeholder, match);
    return placeholder;
  });
  return { processedText, svgTokens };
}

// ─── LaTeX & Markdown preprocessing ────────────────────────────

function cleanLatexDocCommands(text: string): string {
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

function sanitizeLatex(latex: string): string {
  if (!latex) return '';
  let s = latex.trim();

  // 1. Repair tab characters that unescaped \t-commands
  s = s.replace(/\t(ext|an|imes|heta|au|o|herefore|ilde|extbf|extit)\b/g, '\\$1');

  // 2. Fix '\ ext' or '\  ext' or '\ text' -> '\text'
  s = s.replace(/\\+(\s*)ext(?=\{|\s|$|[0-9])/g, '\\text');

  // 3. Fix whitespace between backslash and command letters ('\ frac' -> '\frac')
  s = s.replace(/\\+\s+([a-zA-Z]+)/g, '\\$1');

  // 4. Deduplicate multiple backslashes before commands
  let prev: string;
  do {
    prev = s;
    s = s.replace(/\\{2,}([a-zA-Z]+|[{}_#$%&^~])/g, '\\$1');
  } while (s !== prev);

  // 5. Ensure bare % is escaped for KaTeX
  return s.replace(/(?<!\\)%/g, '\\%');
}

function renderKatex(latex: string, displayMode = false): string {
  const clean = sanitizeLatex(latex);
  if (!clean) return '';
  if (NON_MATH_PATTERN.test(clean)) {
    return `<span>${clean}</span>`;
  }
  try {
    return katex.renderToString(clean, {
      throwOnError: false,
      displayMode,
      output: 'htmlAndMathml',
      trust: false,
      strict: 'ignore',
    });
  } catch {
    return `<span class="math-render-error text-amber-600 font-mono text-xs">${clean}</span>`;
  }
}

function shouldDisplayBlock(cleanMatch: string, fullMatch: string, offset: number, fullStr: string): boolean {
  if (cleanMatch.includes('\\begin{align') || cleanMatch.includes('\\begin{equation')) return true;
  const before = fullStr.substring(0, offset);
  const after = fullStr.substring(offset + fullMatch.length);
  return /(?:^|\n)\s*$/.test(before) && /^\s*(?:\n|$)/.test(after);
}

function extractMathBlocks(text: string): { processedText: string; tokens: Map<string, string> } {
  const tokens = new Map<string, string>();
  let tokenIndex = 0;

  const createPlaceholder = (renderedHtml: string): string => {
    const placeholder = `%%%NAG_MATH_BLOCK_${tokenIndex++}%%%`;
    tokens.set(placeholder, renderedHtml);
    return placeholder;
  };

  // 1. Display Math: $$ ... $$
  text = text.replace(/\$\$([\s\S]*?)\$\$/g, (_match, math) => {
    const rendered = renderKatex(math, true);
    return createPlaceholder(rendered);
  });

  // 2. Display Math: \[ ... \]
  text = text.replace(/\\\[([\s\S]*?)\\\]/g, (_match, math) => {
    const rendered = renderKatex(math, true);
    return createPlaceholder(rendered);
  });

  // 3. Fenced math code blocks: ```math ... ``` or ```latex ... ```
  text = text.replace(/```(?:math|latex)\s*\n([\s\S]*?)```/g, (_match, math) => {
    const rendered = renderKatex(math, true);
    return createPlaceholder(rendered);
  });

  // 4. Inline Math: \( ... \)
  text = text.replace(/\\\(([\s\S]*?)\\\)/g, (_match, math) => {
    const rendered = renderKatex(math, false);
    return createPlaceholder(rendered);
  });

  // 5. Inline Math: $ ... $
  text = text.replace(/(^|[^\\])\$([^$\n\r]+?)\$(?!\$)/g, (match, prefix, math, offset, fullStr) => {
    const isBlock = shouldDisplayBlock(math, match, offset, fullStr);
    const rendered = renderKatex(math, isBlock);
    return (prefix || '') + createPlaceholder(rendered);
  });

  return { processedText: text, tokens };
}

function parseInlineMarkdown(text: string): string {
  if (!text) return '';
  return text
    .replace(/\*\*\*([^*\n\r]+?)\*\*\*/g, '<strong><em>$1</em></strong>')
    .replace(/___([^_\n\r]+?)___/g, '<strong><em>$1</em></strong>')
    .replace(/\*\*([^*\n\r]+?)\*\*/g, '<strong>$1</strong>')
    .replace(/__([^_\n\r]+?)__/g, '<strong>$1</strong>')
    .replace(/~~([^~\n\r]+?)~~/g, '<del>$1</del>')
    .replace(/`([^`\n\r]+?)`/g, '<code>$1</code>')
    .replace(/(^|[^*])\*([^*\n\r]+?)\*([^*]|$)/g, '$1<em>$2</em>$3')
    .replace(/(^|[^a-zA-Z0-9_])_([^_\n\r]+?)([^a-zA-Z0-9_]|$)/g, '$1<em>$2</em>$3');
}

function unescapeNewlines(text: string): string {
  return (text || '')
    .replace(/\\r\\n/g, '\n')
    .replace(/\\n(?![a-z])/g, '\n')
    .replace(/\\t(?![a-z])/g, '\t');
}

function decodeHtmlEntities(text: string): string {
  return (text || '')
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

function normalizeMarkdownTables(text: string): string {
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

function escapeHtml(str: string): string {
  return (str || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

// ─── Component ──────────────────────────────────────────────────

export const MathRenderer: React.FC<MathRendererProps> = ({
  content,
  className = '',
  inline = false,
}) => {
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!containerRef.current) return;
    const svgs = containerRef.current.querySelectorAll<SVGSVGElement>(
      'svg[data-smiles]:not([data-smiles-drawn])'
    );
    if (svgs.length === 0) return;

    let isMounted = true;

    getSmilesDrawer()
      .then((sd: any) => {
        if (!isMounted || !sd) return;
        const SvgDrawer = sd.SvgDrawer ?? sd.default?.SvgDrawer ?? sd.Drawer ?? sd.default;

        svgs.forEach((svgEl) => {
          if (svgEl.hasAttribute('data-smiles-drawn')) return;
          const smiles = svgEl.getAttribute('data-smiles');
          if (!smiles) return;

          const theme = svgEl.getAttribute('data-theme') || 'light';
          const w = parseInt(svgEl.getAttribute('width') || '260', 10);
          const h = parseInt(svgEl.getAttribute('height') || '200', 10);

          try {
            if (SvgDrawer) {
              const drawer = new SvgDrawer({
                width: w,
                height: h,
                compactDrawing: false,
              });

              if (typeof drawer.draw === 'function') {
                drawer.draw(smiles, svgEl, theme, false);
                svgEl.setAttribute('data-smiles-drawn', 'true');
              } else if (typeof drawer.parse === 'function') {
                drawer.parse(
                  smiles,
                  (tree: any) => {
                    drawer.draw(tree, svgEl, theme, false);
                    svgEl.setAttribute('data-smiles-drawn', 'true');
                  },
                  () => {
                    renderFallbackSvg(svgEl, smiles);
                  }
                );
              }
            }
          } catch {
            renderFallbackSvg(svgEl, smiles);
          }
        });
      })
      .catch(() => {
        svgs.forEach((svgEl) => {
          const smiles = svgEl.getAttribute('data-smiles') || '';
          renderFallbackSvg(svgEl, smiles);
        });
      });

    return () => {
      isMounted = false;
    };
  }, [content]);

  function renderFallbackSvg(svgEl: SVGSVGElement, smiles: string): void {
    svgEl.setAttribute('data-smiles-drawn', 'true');
    const w = parseInt(svgEl.getAttribute('width') || '260', 10);
    const h = parseInt(svgEl.getAttribute('height') || '200', 10);
    svgEl.innerHTML =
      `<rect width="${w}" height="${h}" fill="#f8fafc" stroke="#e2e8f0" rx="4"/>` +
      `<text x="${w / 2}" y="${h / 2}" font-family="monospace" font-size="12" fill="#64748b" text-anchor="middle" dominant-baseline="middle">${escapeHtml(smiles)}</text>`;
  }

  const renderContent = (): string => {
    if (!content || !content.trim()) return '';

    try {
      // 1. Unescape literal \n, \r\n, \t sequences if received as raw text
      let text = unescapeNewlines(content.trim());

      // 2. Decode common HTML entities
      text = decodeHtmlEntities(text);

      // 3. Extract SVG blocks as opaque placeholders FIRST (FIX #4)
      const { processedText: textWithoutSvg, svgTokens } = extractSvgBlocks(text);
      text = textWithoutSvg;

      // 4. Extract SMILES chemical structures <smiles ...>...</smiles> (FIX #126 / #144)
      const { processedText: textWithoutSmiles, smilesItems } = extractSmilesBlocks(text);
      text = textWithoutSmiles;

      // 5. Extract Math blocks ($$...$$, \[...\], \(...\), $...$) and replace with placeholders
      const { processedText: textWithoutMath, tokens: mathTokens } = extractMathBlocks(text);
      text = textWithoutMath;

      // 6. Normalize pipe tables
      if (!inline) {
        text = normalizeMarkdownTables(text);
      }

      // 7. Clean LaTeX document-level commands outside math blocks
      text = cleanLatexDocCommands(text);

      // 8. Pre-process inline markdown formatting
      text = parseInlineMarkdown(text);

      // 9. Parse Markdown using marked
      let parsedHtml = '';
      if (inline) {
        parsedHtml = marked.parseInline(text, {
          gfm: true,
          breaks: true,
        }) as string;
      } else {
        parsedHtml = marked.parse(text, {
          gfm: true,
          breaks: true,
          async: false,
        }) as string;
      }

      // 10. Re-insert LaTeX math placeholders
      mathTokens.forEach((renderedKatex, placeholder) => {
        parsedHtml = parsedHtml.split(placeholder).join(renderedKatex);
      });

      // 11. Re-insert SMILES svg placeholder blocks
      smilesItems.forEach((item) => {
        parsedHtml = parsedHtml.split(item.placeholder).join(item.svgHtml);
      });

      // 12. Re-insert raw SVG block placeholders
      svgTokens.forEach((svgContent, placeholder) => {
        parsedHtml = parsedHtml.split(placeholder).join(svgContent);
      });

      return parsedHtml;
    } catch {
      return `<span class="math-render-fallback">${escapeHtml(content)}</span>`;
    }
  };

  return (
    <div
      ref={containerRef}
      className={`math-renderer-host ${className}`}
      dangerouslySetInnerHTML={{ __html: renderContent() }}
    />
  );
};
