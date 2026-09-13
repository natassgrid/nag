import React, { useMemo } from 'react';
import katex from 'katex';
import { marked } from 'marked';

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
 * Converts LaTeX document-structure commands to readable HTML / Markdown.
 */
function cleanLatexDocCommands(text: string): string {
  if (!text) return '';
  return text
    .replace(/\\begin\{enumerate\}/g, '')
    .replace(/\\end\{enumerate\}/g, '')
    .replace(/\\begin\{itemize\}/g, '')
    .replace(/\\end\{itemize\}/g, '')
    .replace(/\\item\s*/g, '\n- ')
    .replace(/\\textbf\{([^}]*)\}/g, '**$1**')
    .replace(/\\textit\{([^}]*)\}/g, '*$1*')
    .replace(/\\\\(\s|$)/g, '\n$1');
}

/**
 * Normalizes ASCII / pipe matrices and tables missing standard Markdown separator rows.
 */
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
        // If the next line isn't already a markdown delimiter row (|---|---|)
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
 * Parses mixed Markdown and LaTeX content into styled HTML.
 */
function parseContentToHtml(raw: string, inline = false): string {
  if (!raw || !raw.trim()) return '';

  let text = raw.trim();

  // If text contains literal escaped newlines like '\n', unescape them
  if (text.includes('\\n')) {
    text = text.replace(/\\n/g, '\n');
  }

  const decoded = decodeHtmlEntities(text);
  const normalized = normalizeMathDelimiters(decoded);
  let cleaned = cleanLatexDocCommands(normalized);

  // Normalize pipe tables
  if (!inline) {
    cleaned = normalizeMarkdownTables(cleaned);
  }

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

  // Extract LaTeX segments and replace with placeholder tokens
  const mathPlaceholders: { key: string; rendered: string }[] = [];
  const mathRegex = /\$\$([\s\S]*?)\$\$/g;
  const preprocessed = cleaned.replace(mathRegex, (_, latex) => {
    const key = `%%%NAG_MATH_BLOCK_${mathPlaceholders.length}%%%`;
    const isDisplayMode =
      !inline &&
      (latex.includes('\\displaystyle') ||
        latex.includes('\\begin{matrix}') ||
        latex.includes('\\begin{aligned}') ||
        latex.includes('\\xrightarrow') ||
        latex.includes('\n'));
    const rendered = renderKatexString(latex, isDisplayMode);
    mathPlaceholders.push({ key, rendered });
    return key;
  });

  // Parse Markdown using marked
  let htmlResult = '';
  try {
    if (inline) {
      const parsed = marked.parseInline(preprocessed, { gfm: true, breaks: true });
      htmlResult = typeof parsed === 'string' ? parsed : '';
    } else {
      const parsed = marked.parse(preprocessed, { gfm: true, breaks: true, async: false });
      htmlResult = typeof parsed === 'string' ? parsed : '';
    }
  } catch {
    htmlResult = preprocessed;
  }

  // Restore KaTeX rendered math tokens
  for (const ph of mathPlaceholders) {
    htmlResult = htmlResult.split(ph.key).join(ph.rendered);
  }

  return htmlResult;
}

export const MathRenderer: React.FC<MathRendererProps> = React.memo(
  ({ content, className = '', inline = false }) => {
    const renderedHtml = useMemo(() => {
      return parseContentToHtml(content || '', inline);
    }, [content, inline]);

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
  }
);
