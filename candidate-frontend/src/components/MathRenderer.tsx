/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

import React, { useMemo, useState, useCallback, useEffect, useRef } from 'react';
import { ImageZoomModal } from './ImageZoomModal';
import katex from 'katex';
import { marked } from 'marked';
// mhchem adds \ce{} (chemical equations) and \pu{} (physical units) to KaTeX.
// It ships inside the katex package — no extra npm dependency needed.
import 'katex/dist/contrib/mhchem.js';

interface MathRendererProps {
  /** Raw content string containing mixed Markdown text, HTML, $$LaTeX$$, $LaTeX$, \(...\), or \[...\] */
  content?: string | null;
  /** Optional custom CSS classes for the container */
  className?: string;
  /** Force inline span vs block wrapper (default: false for block-capable container) */
  inline?: boolean;
}

/** Non-math LaTeX commands that should be rendered as plain text/HTML */
const NON_MATH_PATTERN =
  /\\{1,2}(begin|end)\{(enumerate|itemize|document|figure|table|center)\}|\\{1,2}item|\\{1,2}section|\\{1,2}subsection/;

// ─── SMILES canvas component ──────────────────────────────────────────────────

interface SmilesCanvasProps {
  smiles: string;
  title?: string;
  width?: number;
  height?: number;
  onZoom?: (svg: string) => void;
}

/**
 * Renders a single SMILES structure onto a <canvas> using SmilesDrawer 2.0.
 * Falls back to displaying the raw SMILES string if the library is unavailable.
 *
 * Issue #126 — SmilesDrawer 2.0 integration.
 */
const SmilesCanvas: React.FC<SmilesCanvasProps> = ({
  smiles,
  title,
  width = 260,
  height = 210,
  onZoom,
}) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!smiles || !canvasRef.current) return;
    let cancelled = false;

    (async () => {
      try {
        // Dynamic import — gracefully degrades if smiles-drawer not installed
        const sd: any = await import('smiles-drawer');
        if (cancelled) return;

        // smiles-drawer v2 exports differ — try SvgDrawer → Drawer → default
        const DrawerClass =
          sd.SvgDrawer ??
          sd.Drawer ??
          sd.default?.SvgDrawer ??
          sd.default?.Drawer ??
          sd.default;

        if (!DrawerClass) {
          setError('smiles-drawer not available');
          return;
        }

        const drawer = new DrawerClass({ width, height, compactDrawing: false });
        const canvas = canvasRef.current;
        if (!canvas) return;

        if (typeof drawer.draw === 'function') {
          drawer.draw(smiles, canvas, 'light', false);
        } else if (typeof drawer.drawToCanvas === 'function') {
          drawer.drawToCanvas(smiles, canvas, 'light');
        } else if (typeof drawer.parse === 'function') {
          const tree = drawer.parse(smiles);
          if (tree) drawer.draw(tree, canvas, 'light', false);
        }
      } catch (e: any) {
        if (!cancelled) setError(e?.message || 'Invalid SMILES');
      }
    })();

    return () => { cancelled = true; };
  }, [smiles, width, height]);

  const handleClick = useCallback(() => {
    if (onZoom && canvasRef.current) {
      // Convert canvas to data URL for zoom modal
      onZoom(canvasRef.current.toDataURL('image/png'));
    }
  }, [onZoom]);

  return (
    <span className="smiles-block" style={{ display: 'inline-flex', flexDirection: 'column', alignItems: 'center', margin: '8px 4px', verticalAlign: 'middle' }}>
      {error ? (
        <code style={{ fontSize: '12px', color: '#d32f2f', background: '#fff3e0', padding: '4px 8px', borderRadius: '4px' }}>
          {smiles}
        </code>
      ) : (
        <canvas
          ref={canvasRef}
          width={width}
          height={height}
          onClick={handleClick}
          style={{
            display: 'block',
            maxWidth: '100%',
            border: '1px solid #e0e0e0',
            borderRadius: '4px',
            background: '#fff',
            cursor: onZoom ? 'zoom-in' : 'default',
          }}
          aria-label={title || `Chemical structure: ${smiles}`}
        />
      )}
      {title && (
        <span style={{ fontSize: '11px', color: '#546e7a', fontStyle: 'italic', marginTop: '4px', textAlign: 'center' }}>
          {title}
        </span>
      )}
    </span>
  );
};

// ─── SMILES extraction ────────────────────────────────────────────────────────

interface SmilesToken {
  placeholder: string;
  smiles: string;
  title?: string;
}

/**
 * Extracts <smiles>...</smiles> blocks from text, replacing them with unique
 * placeholder strings. Returns the modified text and a list of SMILES tokens.
 *
 * Issue #126 — SMILES extraction step runs before LaTeX/Markdown processing
 * so chemical notation is never corrupted by those pipelines.
 */
function extractSmilesBlocks(text: string): { processedText: string; smilesTokens: SmilesToken[] } {
  const smilesTokens: SmilesToken[] = [];
  let idx = 0;

  const processedText = text.replace(
    /<smiles>([\s\S]*?)<\/smiles>(?:\s*<!--\s*(.*?)\s*-->)?/gi,
    (_match, smiles: string, title?: string) => {
      const placeholder = `%%%NAG_SMILES_${idx++}%%%`;
      smilesTokens.push({
        placeholder,
        smiles: smiles.trim(),
        title: title?.trim() || undefined,
      });
      return placeholder;
    }
  );

  return { processedText, smilesTokens };
}

// ─── Utility functions (unchanged from original) ─────────────────────────────

function unescapeNewlines(text: string): string {
  if (!text) return '';
  return text
    .replace(/\\r\\n/g, '\n')
    .replace(/\\n(?![a-z])/g, '\n')
    .replace(/\\t(?![a-z])/g, '\t');
}

function decodeHtmlEntities(text: string): string {
  if (!text) return '';
  return text
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

function cleanLatexDocCommands(text: string): string {
  if (!text) return '';
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

function parseInlineMarkdown(text: string): string {
  if (!text) return '';
  return text
    .replace(/\*\*\*([^\*\n\r]+?)\*\*\*/g, '<strong><em>$1</em></strong>')
    .replace(/___([^_\n\r]+?)___/g, '<strong><em>$1</em></strong>')
    .replace(/\*\*([^\*\n\r]+?)\*\*/g, '<strong>$1</strong>')
    .replace(/__([^_\n\r]+?)__/g, '<strong>$1</strong>')
    .replace(/~~([^~\n\r]+?)~~/g, '<del>$1</del>')
    .replace(/`([^`\n\r]+?)`/g, '<code>$1</code>')
    .replace(/(^|[^*])\*([^*\\n\r]+?)\*([^*]|$)/g, '$1<em>$2</em>$3')
    .replace(/(^|[^a-zA-Z0-9_])_([^_\n\r]+?)_([^a-zA-Z0-9_]|$)/g, '$1<em>$2</em>$3');
}

function escapeHtml(str: string): string {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

function sanitizeLatex(latex: string): string {
  if (!latex || typeof latex !== 'string') return '';
  let s = latex.trim();
  let previous: string;
  do {
    previous = s;
    s = s.replace(/\\{2,}([a-zA-Z]+|[{}_#$%&^~])/g, '\\$1');
  } while (s !== previous);
  s = s.replace(/(?<!\\)%/g, '\\%');
  return s;
}

function renderKatexString(latex: string, displayMode = false): string {
  const trimmed = sanitizeLatex(latex);
  if (!trimmed) return '';
  if (NON_MATH_PATTERN.test(trimmed)) {
    return `<span>${escapeHtml(trimmed)}</span>`;
  }
  try {
    return katex.renderToString(trimmed, {
      throwOnError: false,
      displayMode,
      output: 'htmlAndMathml',
      trust: false,
      strict: 'ignore',
    });
  } catch {
    return `<span class="math-render-error text-amber-600 font-mono text-xs">${escapeHtml(trimmed)}</span>`;
  }
}

function shouldDisplayBlock(
  math: string,
  fullMatch: string,
  offset: number,
  fullStr: string,
  forceInline: boolean
): boolean {
  if (forceInline) return false;
  const charBefore = offset > 0 ? fullStr[offset - 1] : '';
  const charAfter = offset + fullMatch.length < fullStr.length ? fullStr[offset + fullMatch.length] : '';
  if ((charBefore === '(' || charBefore === '[') && (charAfter === ')' || charAfter === ']')) return false;
  const textBefore = fullStr.slice(0, offset);
  const lastNewlineBefore = textBefore.lastIndexOf('\n');
  const linePrefix = lastNewlineBefore === -1 ? textBefore : textBefore.slice(lastNewlineBefore + 1);
  const textAfter = fullStr.slice(offset + fullMatch.length);
  const nextNewlineAfter = textAfter.indexOf('\n');
  const lineSuffix = nextNewlineAfter === -1 ? textAfter : textAfter.slice(0, nextNewlineAfter);
  if (/^\s*(\d+\.|[IVXLC]+\.|[A-Za-z]\.|[-*•])\s/.test(linePrefix)) return false;
  const trimmedMath = math.trim();
  const isSingleVariableToken = /^(\\[a-zA-Z]+\s+)?[a-zA-Z0-9_]{1,3}$/.test(trimmedMath);
  const hasSurroundingText = linePrefix.trim() !== '' || lineSuffix.trim() !== '';
  if (isSingleVariableToken && hasSurroundingText) return false;
  return true;
}

function extractSvgBlocks(text: string): { processedText: string; tokens: Map<string, string> } {
  const tokens = new Map<string, string>();
  let idx = 0;
  const processedText = text.replace(/<svg[\s\S]*?<\/svg>/gi, (match) => {
    const placeholder = `%%%NAG_SVG_BLOCK_${idx++}%%%`;
    tokens.set(placeholder, match);
    return placeholder;
  });
  return { processedText, tokens };
}

function extractAndRenderMath(text: string, inline: boolean): { processedText: string; tokens: Map<string, string> } {
  const tokens = new Map<string, string>();
  let tokenIndex = 0;
  const createPlaceholder = (rendered: string): string => {
    const placeholder = `%%%NAG_MATH_BLOCK_${tokenIndex++}%%%`;
    tokens.set(placeholder, rendered);
    return placeholder;
  };
  text = text.replace(/\$\$([\s\S]*?)\$\$/g, (fullMatch, math, offset, fullStr) => {
    const isDisplay = shouldDisplayBlock(math, fullMatch, offset, fullStr, inline);
    return createPlaceholder(renderKatexString(math, isDisplay));
  });
  text = text.replace(/\\{1,4}\[([\s\S]*?)\\{1,4}\]/g, (fullMatch, math, offset, fullStr) => {
    const isDisplay = shouldDisplayBlock(math, fullMatch, offset, fullStr, inline);
    return createPlaceholder(renderKatexString(math, isDisplay));
  });
  const envRegex = /\\{1,4}begin\{(matrix|pmatrix|bmatrix|vmatrix|Vmatrix|cases|align|align\*|aligned|equation|equation\*|gather|gather\*)\}([\s\S]*?)\\{1,4}end\{\1\}/g;
  text = text.replace(envRegex, (fullMatch, _env, _inner, offset, fullStr) => {
    const cleanMatch = fullMatch.replace(/\\{2,}/g, '\\');
    const isDisplay = shouldDisplayBlock(cleanMatch, fullMatch, offset, fullStr, inline);
    return createPlaceholder(renderKatexString(cleanMatch, isDisplay));
  });
  text = text.replace(/\\{1,4}\(([\s\S]*?)\\{1,4}\)/g, (_, math) => createPlaceholder(renderKatexString(math, false)));
  text = text.replace(/(^|[^\\])\$([^\$\n\r]+?)\$(?!\$)/g, (_, prefix, math) => (prefix || '') + createPlaceholder(renderKatexString(math, false)));
  return { processedText: text, tokens };
}

// ─── HTML parsing (returns HTML string for dangerouslySetInnerHTML) ───────────

/**
 * Parses mixed Markdown, LaTeX, and SMILES content into HTML.
 * SMILES blocks are replaced with placeholder strings — the React render
 * phase replaces them with <SmilesCanvas> components via splitOnPlaceholders.
 */
function parseContentToHtml(raw: string, inline: boolean): {
  htmlParts: string[];
  smilesTokens: SmilesToken[];
} {
  if (!raw || !raw.trim()) return { htmlParts: [''], smilesTokens: [] };

  try {
    let text = unescapeNewlines(raw.trim());
    text = decodeHtmlEntities(text);

    // 3a. Extract SVG blocks
    const { processedText: textWithoutSvg, tokens: svgTokens } = extractSvgBlocks(text);
    text = textWithoutSvg;

    // 3b. Extract SMILES blocks BEFORE LaTeX/Markdown (Issue #126)
    const { processedText: textWithoutSmiles, smilesTokens } = extractSmilesBlocks(text);
    text = textWithoutSmiles;

    // 4. Extract and render LaTeX
    const { processedText: textWithoutMath, tokens: mathTokens } = extractAndRenderMath(text, inline);
    text = textWithoutMath;

    if (!inline) text = normalizeMarkdownTables(text);
    text = cleanLatexDocCommands(text);
    text = parseInlineMarkdown(text);

    let htmlResult = '';
    if (inline) {
      const parsed = marked.parseInline(text, { gfm: true, breaks: true });
      htmlResult = typeof parsed === 'string' ? parsed : '';
    } else {
      const parsed = marked.parse(text, { gfm: true, breaks: true, async: false });
      htmlResult = typeof parsed === 'string' ? parsed : '';
    }

    htmlResult = parseInlineMarkdown(htmlResult);

    for (const [placeholder, rendered] of mathTokens.entries()) {
      htmlResult = htmlResult.split(placeholder).join(rendered);
    }
    for (const [placeholder, svgBlock] of svgTokens.entries()) {
      htmlResult = htmlResult.split(placeholder).join(svgBlock);
    }

    // Split the final HTML on SMILES placeholders so we can interleave
    // <SmilesCanvas> React components with dangerouslySetInnerHTML spans.
    if (smilesTokens.length === 0) {
      return { htmlParts: [htmlResult], smilesTokens: [] };
    }

    // Build split array: [html, smiles, html, smiles, html, ...]
    const htmlParts: string[] = [];
    let remaining = htmlResult;
    for (const token of smilesTokens) {
      const idx = remaining.indexOf(token.placeholder);
      if (idx === -1) {
        htmlParts.push(remaining);
        remaining = '';
      } else {
        htmlParts.push(remaining.slice(0, idx));
        remaining = remaining.slice(idx + token.placeholder.length);
      }
    }
    htmlParts.push(remaining);

    return { htmlParts, smilesTokens };
  } catch (err) {
    console.warn('Failed to parse Markdown / Math content:', err);
    return { htmlParts: [escapeHtml(raw)], smilesTokens: [] };
  }
}

/** Global CSS injected once for image zoom + SMILES styles */
const CONTENT_STYLES = `
.math-rendered-content img {
  max-width: 100%;
  height: auto;
  cursor: zoom-in;
  border-radius: 4px;
  transition: opacity 0.15s;
}
.math-rendered-content img:hover { opacity: 0.85; }
`;

// ─── Main MathRenderer component ─────────────────────────────────────────────

/**
 * MathRenderer — renders examination content containing Markdown, LaTeX math,
 * and SMILES chemical structure notation.
 *
 * Additions (Issue #126):
 *  • Parses <smiles>...</smiles> tags.
 *  • Renders each SMILES string as a 2D skeletal structure canvas via SmilesDrawer 2.0.
 *  • Chemical structures support click-to-zoom via ImageZoomModal (PNG snapshot).
 */
export const MathRenderer: React.FC<MathRendererProps> = ({
  content,
  className = '',
  inline = false,
}) => {
  const [zoomSrc, setZoomSrc] = useState<{ src: string; alt: string } | null>(null);

  const handleImageClick = useCallback((e: React.MouseEvent<HTMLDivElement>) => {
    const target = e.target as HTMLElement;
    if (target.tagName === 'IMG') {
      const img = target as HTMLImageElement;
      setZoomSrc({ src: img.src, alt: img.alt || 'Question figure' });
    }
  }, []);

  const handleSmilesZoom = useCallback((dataUrl: string, smiles: string) => {
    setZoomSrc({ src: dataUrl, alt: `Chemical structure: ${smiles}` });
  }, []);

  const { htmlParts, smilesTokens } = useMemo(
    () => (content ? parseContentToHtml(content, inline) : { htmlParts: [''], smilesTokens: [] }),
    [content, inline]
  );

  if (!content) return null;

  // Inline mode: single span, no SMILES canvas interleaving
  if (inline) {
    const html = htmlParts.join('');
    return html ? (
      <span
        className={`math-rendered-content inline ${className}`}
        dangerouslySetInnerHTML={{ __html: html }}
      />
    ) : null;
  }

  // Block mode: interleave HTML parts with SmilesCanvas components
  const nodes: React.ReactNode[] = [];
  htmlParts.forEach((part, i) => {
    if (part) {
      nodes.push(
        <span
          key={`html-${i}`}
          dangerouslySetInnerHTML={{ __html: part }}
        />
      );
    }
    if (i < smilesTokens.length) {
      const token = smilesTokens[i];
      nodes.push(
        <SmilesCanvas
          key={`smiles-${i}`}
          smiles={token.smiles}
          title={token.title}
          onZoom={(dataUrl) => handleSmilesZoom(dataUrl, token.smiles)}
        />
      );
    }
  });

  return (
    <>
      <style>{CONTENT_STYLES}</style>
      <div
        className={`math-rendered-content ${className}`}
        onClick={handleImageClick}
        style={{ cursor: 'default' }}
      >
        {nodes}
      </div>
      <ImageZoomModal
        src={zoomSrc?.src || ''}
        alt={zoomSrc?.alt || ''}
        isOpen={zoomSrc !== null}
        onClose={() => setZoomSrc(null)}
      />
    </>
  );
};

export default MathRenderer;
