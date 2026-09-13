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
  /\\(begin|end)\\{(enumerate|itemize|document|figure|table|center)\\}|\\item|\\textbf|\\textit|\\section|\\subsection/;

/**
 * Unescapes literal newline sequences (\n, \r\n, \t).
 */
function unescapeNewlines(text: string): string {
  if (!text) return '';
  return text
    .replace(/\\r\\n/g, '\n')
    .replace(/\\n/g, '\n')
    .replace(/\\t/g, '\t');
}

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
 * Transforms inline Markdown constructs (bold, italic, strike, code)
 * so they render properly even inside HTML block tags (<p>, <div>).
 */
function parseInlineMarkdown(text: string): string {
  if (!text) return '';
  return text
    // Bold + Italic: ***text*** or ___text___
    .replace(/\*\*\*([^\*\n\r]+?)\*\*\*/g, '<strong><em>$1</em></strong>')
    .replace(/___([^_\n\r]+?)___/g, '<strong><em>$1</em></strong>')
    // Bold: **text** or __text__
    .replace(/\*\*([^\*\n\r]+?)\*\*/g, '<strong>$1</strong>')
    .replace(/__([^_\n\r]+?)__/g, '<strong>$1</strong>')
    // Strikethrough: ~~text~~
    .replace(/~~([^~\n\r]+?)~~/g, '<del>$1</del>')
    // Inline code: `code`
    .replace(/`([^`\n\r]+?)`/g, '<code>$1</code>')
    // Italic: *text* (when not part of a math block or token)
    .replace(/(^|[^*])\*([^*\n\r]+?)\*([^*]|$)/g, '$1<em>$2</em>$3')
    // Italic: _text_ (when surrounded by non-alphanumeric boundaries)
    .replace(/(^|[^a-zA-Z0-9_])_([^_\n\r]+?)_([^a-zA-Z0-9_]|$)/g, '$1<em>$2</em>$3');
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
 * Sanitizes LaTeX expression before passing to KaTeX.
 */
function sanitizeLatex(latex: string): string {
  let s = latex.trim();
  s = s.replace(/\\*%/g, '\\%');
  return s;
}

/**
 * Renders a LaTeX string to HTML using KaTeX.
 */
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

/**
 * Parses mixed Markdown and LaTeX content into styled HTML.
 */
function parseContentToHtml(raw: string, inline = false): string {
  if (!raw || !raw.trim()) return '';

  const text = unescapeNewlines(raw.trim());
  const decoded = decodeHtmlEntities(text);
  const normalized = normalizeMathDelimiters(decoded);
  let cleaned = cleanLatexDocCommands(normalized);

  // Normalize pipe tables
  if (!inline) {
    cleaned = normalizeMarkdownTables(cleaned);
  }

  // Convert single-dollar $math$ (not preceded or followed by another $) to $$math$$
  cleaned = cleaned.replace(/(^|[^\$])\$([^$\n\r]+?)\$([^$]|$)/g, '$1$$$$$2$$$$$3');

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
  const mathPlaceholders: { placeholder: string; rendered: string }[] = [];
  const mathRegex = /\$\$([\s\S]*?)\$\$/g;
  let preprocessed = cleaned.replace(mathRegex, (_, latex) => {
    const key = `%%%NAG_MATH_BLOCK_${mathPlaceholders.length}%%%`;
    const isDisplayMode =
      !inline &&
      (latex.includes('\\displaystyle') ||
        latex.includes('\\begin{matrix}') ||
        latex.includes('\\begin{aligned}') ||
        latex.includes('\\xrightarrow') ||
        latex.includes('\n'));
    const rendered = renderKatexString(latex, isDisplayMode);
    mathPlaceholders.push({ placeholder: key, rendered });
    return key;
  });

  // Pre-process inline markdown formatting (bold, italic, strike, code) inside HTML elements
  preprocessed = parseInlineMarkdown(preprocessed);

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

  // Secondary pass for any inline markdown in HTML blocks passed through by marked
  htmlResult = parseInlineMarkdown(htmlResult);

  // Reinsert KaTeX rendered HTML
  for (const token of mathPlaceholders) {
    htmlResult = htmlResult.split(token.placeholder).join(token.rendered);
  }

  return htmlResult;
}

/**
 * MathRenderer component for rendering questions, options, and explanations
 * containing standard Markdown and LaTeX math expressions.
 */
export const MathRenderer: React.FC<MathRendererProps> = ({
  content,
  className = '',
  inline = false,
}) => {
  const htmlContent = useMemo(() => {
    if (!content) return '';
    return parseContentToHtml(content, inline);
  }, [content, inline]);

  if (!htmlContent) return null;

  if (inline) {
    return (
      <span
        className={`math-renderer inline-content ${className}`}
        dangerouslySetInnerHTML={{ __html: htmlContent }}
      />
    );
  }

  return (
    <div
      className={`math-renderer block-content leading-relaxed text-slate-800 ${className}`}
      dangerouslySetInnerHTML={{ __html: htmlContent }}
    />
  );
};

export default MathRenderer;
