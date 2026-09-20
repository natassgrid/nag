/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Candidate Frontend
 * Copyright (C) 2025 NAG Contributors
 */

import React, { useEffect, useMemo, useRef, useState, useCallback } from 'react';
import katex from 'katex';
import { marked } from 'marked';
import 'katex/dist/contrib/mhchem.js';
import ImageZoomModal from './ImageZoomModal';

interface MathRendererProps {
  /** The raw markdown + math content to render */
  content: string | null | undefined;
  /** Additional CSS class names for styling */
  className?: string;
  /** When true, renders as an inline <span>; otherwise as a <div> block */
  inline?: boolean;
}

// Non-math LaTeX commands that should be treated as text/HTML
const NON_MATH_PATTERN =
  /\\{1,2}(begin|end)\\{(enumerate|itemize|document|figure|table|center)\\}|\\{1,2}item|\\{1,2}section|\\{1,2}subsection/;

// ─── SMILES SVG component ──────────────────────────────────────────────

interface SmilesSvgProps {
  smiles: string;
  title?: string;
  width?: number;
  height?: number;
  theme?: 'light' | 'dark';
  onZoom?: (svgDataUrl: string) => void;
}

/**
 * Renders a single SMILES chemical structure onto an <svg> element using SmilesDrawer 2.0 SvgDrawer.
 *
 * Issue #126 & #144 — Crisp vector SVG rendering with theme and dimension options.
 */
const SmilesSvg: React.FC<SmilesSvgProps> = ({
  smiles,
  title,
  width = 260,
  height = 200,
  theme = 'light',
  onZoom,
}) => {
  const svgRef = useRef<SVGSVGElement>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!smiles || !svgRef.current) return;
    let cancelled = false;

    (async () => {
      try {
        const sd: any = await import('smiles-drawer');
        if (cancelled) return;

        const SmilesDrawer = sd.default ?? sd;
        const SvgDrawer = SmilesDrawer.SvgDrawer ?? sd.SvgDrawer;

        if (!SvgDrawer) {
          setError('SmilesDrawer renderer unavailable');
          return;
        }

        const svg = svgRef.current;
        if (!svg) return;

        // Clear existing SVG children
        while (svg.firstChild) {
          svg.removeChild(svg.firstChild);
        }

        const drawer = new SvgDrawer({ width, height, compactDrawing: false });

        if (typeof SmilesDrawer.parse === 'function') {
          SmilesDrawer.parse(
            smiles,
            (tree: any) => {
              if (cancelled) return;
              try {
                drawer.draw(tree, svg, theme, null, false);
                setError('');
              } catch (err: any) {
                setError(err?.message || 'Error drawing chemical structure');
              }
            },
            (parseErr: any) => {
              if (!cancelled) setError(parseErr?.message || 'Invalid SMILES notation');
            }
          );
        } else if (SmilesDrawer.Parser?.parse) {
          const tree = SmilesDrawer.Parser.parse(smiles);
          drawer.draw(tree, svg, theme, null, false);
          setError('');
        }
      } catch (e: any) {
        if (!cancelled) setError(e?.message || 'Invalid SMILES notation');
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [smiles, width, height, theme]);

  const handleClick = useCallback(() => {
    if (onZoom && svgRef.current) {
      const serializer = new XMLSerializer();
      const svgStr = serializer.serializeToString(svgRef.current);
      const dataUrl = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svgStr);
      onZoom(dataUrl);
    }
  }, [onZoom]);

  const isDark = theme === 'dark';

  return (
    <span
      className={`smiles-block smiles-block--${theme}`}
      style={{
        display: 'inline-flex',
        flexDirection: 'column',
        alignItems: 'center',
        margin: '8px 4px',
        verticalAlign: 'middle',
      }}
    >
      {error ? (
        <code style={{ fontSize: '12px', color: '#d32f2f', background: '#fff3e0', padding: '4px 8px', borderRadius: '4px' }}>
          {smiles}
        </code>
      ) : (
        <svg
          ref={svgRef}
          width={width}
          height={height}
          onClick={handleClick}
          style={{
            display: 'block',
            maxWidth: '100%',
            border: isDark ? '1px solid #374151' : '1px solid #e0e0e0',
            borderRadius: '6px',
            background: isDark ? '#1e1e24' : '#fff',
            cursor: onZoom ? 'zoom-in' : 'default',
          }}
          aria-label={title || `Chemical structure: ${smiles}`}
        />
      )}
      {title && (
        <span
          style={{
            fontSize: '11px',
            color: isDark ? '#9ca3af' : '#546e7a',
            fontStyle: 'italic',
            marginTop: '4px',
            textAlign: 'center',
          }}
        >
          {title}
        </span>
      )}
    </span>
  );
};

// ─── SMILES extraction ─────────────────────────────────────────────

interface SmilesToken {
  placeholder: string;
  smiles: string;
  title?: string;
  width: number;
  height: number;
  theme: 'light' | 'dark';
}

function extractSmilesBlocks(text: string): { processedText: string; smilesTokens: SmilesToken[] } {
  const smilesTokens: SmilesToken[] = [];
  let idx = 0;

  const processedText = text.replace(
    /<smiles(?:\s+([^>]*?))?>([\s\S]*?)<\/smiles>(?:\s*<!--\s*(.*?)\s*-->)?/gi,
    (_match, rawAttrs: string | undefined, smiles: string, commentTitle?: string) => {
      const placeholder = `%%%NAG_SMILES_${idx++}%%%`;
      const attrs = rawAttrs || '';
      const titleMatch = attrs.match(/title="([^"]*)"/i);
      const widthMatch = attrs.match(/width="(\d+)"/i);
      const heightMatch = attrs.match(/height="(\d+)"/i);
      const themeMatch = attrs.match(/theme="(light|dark)"/i);

      smilesTokens.push({
        placeholder,
        smiles: smiles.trim(),
        title: titleMatch ? titleMatch[1] : (commentTitle?.trim() || undefined),
        width: widthMatch ? parseInt(widthMatch[1], 10) : 260,
        height: heightMatch ? parseInt(heightMatch[1], 10) : 200,
        theme: (themeMatch ? themeMatch[1] : 'light') as 'light' | 'dark',
      });
      return placeholder;
    }
  );

  return { processedText, smilesTokens };
}

// ─── SVG extraction ───────────────────────────────────────────────

function extractSvgBlocks(text: string): { processedText: string; svgTokens: Map<string, string> } {
  const svgTokens = new Map<string, string>();
  let idx = 0;
  const processedText = text.replace(/<svg[\s\S]*?<\/svg>/gi, (match) => {
    const placeholder = `%%%NAG_SVG_${idx++}%%%`;
    svgTokens.set(placeholder, match);
    return placeholder;
  });
  return { processedText, svgTokens };
}

// ─── LaTeX & Markdown preprocessing ───────────────────────────────

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
  let prev: string;
  do {
    prev = s;
    s = s.replace(/\\{2,}([a-zA-Z]+|[{}_#$%&^~])/g, '\\$1');
  } while (s !== prev);
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

function extractAndRenderMath(text: string): { processedText: string; tokens: Map<string, string> } {
  const tokens = new Map<string, string>();
  let idx = 0;
  const ph = (rendered: string) => {
    const key = `%%%NAG_MATH_${idx++}%%%`;
    tokens.set(key, rendered);
    return key;
  };

  // 1. Display $$...$$
  text = text.replace(/\\\\?\${2}([\s\S]*?)\\\\?\${2}/g, (_, m) => ph(renderKatex(m, true)));
  // 2. Display \[...\]
  text = text.replace(/\\{1,4}\[([\s\S]*?)\\\\{1,4}\]/g, (_, m) => ph(renderKatex(m, true)));
  // 3. LaTeX environments
  const envRegex = /\\{1,4}begin\{(matrix|pmatrix|bmatrix|vmatrix|Vmatrix|cases|align|align\*|aligned|equation|equation\*|gather|gather\*)\}([\s\S]*?)\\{1,4}end\{\1\}/g;
  text = text.replace(envRegex, (match, _e, _i, offset, full) => {
    const clean = match.replace(/\\{2,}/g, '\\');
    return ph(renderKatex(clean, shouldDisplayBlock(clean, match, offset, full)));
  });
  // 4. Inline \(...\)
  text = text.replace(/\\{1,4}\(([\s\S]*?)\\{1,4}\)/g, (_, m) => ph(renderKatex(m, false)));
  // 5. Inline $...$
  text = text.replace(/(^|[^\\])\$([^$\n\r]+?)\$(?!\$)/g, (_, prefix, m) => (prefix || '') + ph(renderKatex(m, false)));

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
    .replace(/(^|[^a-zA-Z0-9_])_([^_\n\r]+?)_([^a-zA-Z0-9_]|$)/g, '$1<em>$2</em>$3');
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
        tableHeaderCols = line.split('|').length - 2;
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

// ─── Main MathRenderer Component ──────────────────────────────────

export const MathRenderer: React.FC<MathRendererProps> = React.memo(({
  content,
  className = '',
  inline = false,
}) => {
  const [zoomImg, setZoomImg] = useState<string | null>(null);

  const { htmlWithPlaceholders, smilesTokens } = useMemo(() => {
    if (!content || !content.trim()) {
      return { htmlWithPlaceholders: '', smilesTokens: [] };
    }

    try {
      let text = unescapeNewlines(content.trim());
      text = decodeHtmlEntities(text);

      const { processedText: withoutSvg, svgTokens } = extractSvgBlocks(text);
      text = withoutSvg;

      const { processedText: withoutSmiles, smilesTokens: sTokens } = extractSmilesBlocks(text);
      text = withoutSmiles;

      const { processedText: withoutMath, tokens: mathTokens } = extractAndRenderMath(text);
      text = withoutMath;

      if (!inline) {
        text = normalizeMarkdownTables(text);
      }

      text = cleanLatexDocCommands(text);
      text = parseInlineMarkdown(text);

      let parsed = (inline
        ? marked.parseInline(text, { gfm: true, breaks: true })
        : marked.parse(text, { gfm: true, breaks: true, async: false })) as string;

      mathTokens.forEach((renderedKatex, placeholder) => {
        parsed = parsed.split(placeholder).join(renderedKatex);
      });

      svgTokens.forEach((svgContent, placeholder) => {
        parsed = parsed.split(placeholder).join(svgContent);
      });

      return { htmlWithPlaceholders: parsed, smilesTokens: sTokens };
    } catch {
      return {
        htmlWithPlaceholders: `<span class="math-render-fallback">${content}</span>`,
        smilesTokens: [],
      };
    }
  }, [content, inline]);

  const handleZoom = useCallback((imgSrc: string) => {
    setZoomImg(imgSrc);
  }, []);

  const handleCloseZoom = useCallback(() => {
    setZoomImg(null);
  }, []);

  // Split HTML on SMILES placeholders and interleave React SMILES svg components
  const renderedSegments = useMemo(() => {
    if (smilesTokens.length === 0) {
      return (
        <span
          className="math-renderer-content"
          dangerouslySetInnerHTML={{ __html: htmlWithPlaceholders }}
        />
      );
    }

    // Build regex to split on all SMILES placeholders
    const pattern = new RegExp(`(${smilesTokens.map(t => t.placeholder).join('|')})`, 'g');
    const parts = htmlWithPlaceholders.split(pattern);
    const tokenMap = new Map(smilesTokens.map(t => [t.placeholder, t]));

    return parts.map((part, idx) => {
      const token = tokenMap.get(part);
      if (token) {
        return (
          <SmilesSvg
            key={`smiles-${idx}`}
            smiles={token.smiles}
            title={token.title}
            width={token.width}
            height={token.height}
            theme={token.theme}
            onZoom={handleZoom}
          />
        );
      }
      if (!part) return null;
      return (
        <span
          key={`html-${idx}`}
          dangerouslySetInnerHTML={{ __html: part }}
        />
      );
    });
  }, [htmlWithPlaceholders, smilesTokens, handleZoom]);

  const ElementType = inline ? 'span' : 'div';

  return (
    <>
      <ElementType
        className={`math-renderer ${inline ? 'math-renderer--inline' : 'math-renderer--block'} ${className}`}
      >
        {renderedSegments}
      </ElementType>

      {zoomImg && (
        <ImageZoomModal
          isOpen={true}
          src={zoomImg}
          alt="Chemical Structure (Zoomed)"
          onClose={handleCloseZoom}
        />
      )}
    </>
  );
});

MathRenderer.displayName = 'MathRenderer';
export default MathRenderer;
