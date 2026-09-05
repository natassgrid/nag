import React, { useMemo } from 'react';
import katex from 'katex';

interface MathRendererProps {
  /** Raw content string containing mixed text, HTML, $$LaTeX$$, $LaTeX$, \(...\), or \[...\] */
  content?: string | null;
  /** Optional custom CSS classes for the container */
  className?: string;
  /** Force inline span vs block wrapper (default: false for block-capable container) */
  inline?: boolean;
}

interface ContentSegment {
  type: 'html' | 'math';
  content: string;
  rendered?: string;
  isDisplayMode?: boolean;
}

/** Non-math LaTeX commands that should be rendered as plain text/HTML */
const NON_MATH_PATTERN =
  /\\(begin|end)\{(enumerate|itemize|document|figure|table|center)\}|\\item|\\textbf|\\textit|\\section|\\subsection/;

/**
 * Decodes HTML entities that rich text editors or JSON encodings introduce.
 */
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

/**
 * Normalizes alternate LaTeX math delimiters to canonical $$...$$ form.
 */
function normalizeMathDelimiters(text: string): string {
  if (!text) return '';
  return text
    .replace(/\\\[([\s\S]*?)\\\]/g, '$$$$$1$$$$')
    .replace(/\\\(([\s\S]*?)\\\)/g, '$$$$$1$$$$');
}

/**
 * Converts LaTeX document-structure commands to readable HTML.
 */
function cleanLatexDocCommands(text: string): string {
  if (!text) return '';
  return text
    .replace(/\\begin\{enumerate\}/g, '')
    .replace(/\\end\{enumerate\}/g, '')
    .replace(/\\begin\{itemize\}/g, '')
    .replace(/\\end\{itemize\}/g, '')
    .replace(/\\item\s*/g, '<br>&bull; ')
    .replace(/\\textbf\{([^}]*)\}/g, '<strong>$1</strong>')
    .replace(/\\textit\{([^}]*)\}/g, '<em>$1</em>')
    .replace(/\\\\(\s|$)/g, '<br>$1');
}

/**
 * Renders a LaTeX string to HTML using KaTeX.
 */
function renderKatexString(latex: string, displayMode = false): string {
  const trimmed = latex.trim();
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
      strict: false,
    });
  } catch {
    return `<span class="math-render-error text-amber-600 font-mono text-xs">${escapeHtml(trimmed)}</span>`;
  }
}

/**
 * Escapes HTML characters for safe plain text fallback.
 */
function escapeHtml(str: string): string {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

/**
 * Parses mixed content into math and HTML segments.
 */
function parseContentToHtml(raw: string): string {
  if (!raw || !raw.trim()) return '';

  const decoded = decodeHtmlEntities(raw);
  const normalized = normalizeMathDelimiters(decoded);
  let cleaned = cleanLatexDocCommands(normalized);

  // Convert single-dollar $math$ (not preceded or followed by another $) to $$math$$
  cleaned = cleaned.replace(/(^|[^\$])\$([^\$\n\r]+?)\$([^\$]|$)/g, '$1$$$$$2$$$$$3');

  // Handle unmatched odd count of $$
  const matches = cleaned.match(/\$\$/g);
  if (matches && matches.length % 2 !== 0) {
    if (cleaned.startsWith('$$')) {
      cleaned = cleaned.substring(2);
    } else if (cleaned.endsWith('$$')) {
      cleaned = cleaned.substring(0, cleaned.length - 2);
    }
  }

  const segments: ContentSegment[] = [];
  const mathRegex = /\$\$([\s\S]*?)\$\$/g;
  let lastIndex = 0;
  let match: RegExpExecArray | null;

  while ((match = mathRegex.exec(cleaned)) !== null) {
    if (match.index > lastIndex) {
      const htmlSlice = cleaned.substring(lastIndex, match.index);
      segments.push({ type: 'html', content: htmlSlice, rendered: htmlSlice });
    }

    const latex = match[1];
    // If the latex starts and ends on its own line or is lengthy/has \frac/\int/\sum, consider displayMode
    const isDisplayMode = latex.includes('\\displaystyle') || latex.includes('\\begin{matrix}') || latex.includes('\\begin{aligned}');
    segments.push({
      type: 'math',
      content: latex,
      rendered: renderKatexString(latex, isDisplayMode),
    });

    lastIndex = match.index + match[0].length;
  }

  if (lastIndex < cleaned.length) {
    const trailingHtml = cleaned.substring(lastIndex);
    segments.push({ type: 'html', content: trailingHtml, rendered: trailingHtml });
  }

  return segments.map((seg) => seg.rendered ?? seg.content).join('');
}

export const MathRenderer: React.FC<MathRendererProps> = React.memo(({ content, className = '', inline = false }) => {
  const renderedHtml = useMemo(() => {
    return parseContentToHtml(content || '');
  }, [content]);

  if (!content) return null;

  if (inline) {
    return (
      <span
        className={`math-rendered-inline inline-flex items-center flex-wrap gap-1 ${className}`}
        dangerouslySetInnerHTML={{ __html: renderedHtml }}
      />
    );
  }

  return (
    <div
      className={`math-rendered-content ${className}`}
      dangerouslySetInnerHTML={{ __html: renderedHtml }}
    />
  );
});

MathRenderer.displayName = 'MathRenderer';
