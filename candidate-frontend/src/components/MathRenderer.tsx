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
  /\\{1,2}(begin|end)\\{(enumerate|itemize|document|figure|table|center)\\}|\\{1,2}item|\\{1,2}section|\\{1,2}subsection/;

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
 * Converts LaTeX document-structure commands to readable HTML / Markdown.
 */
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
    .replace(/(^|[^*])\*([^*\\n\r]+?)\*([^*]|$)/g, '$1<em>$2</em>$3')
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
  if (!latex || typeof latex !== 'string') return '';
  let s = latex.trim();
  // Collapse over-escaped backslashes before LaTeX command words or special characters
  // e.g. \\\\det -> \det, \\\\neq -> \neq, \\\\neg -> \neg, \\\\frac -> \frac, \\\\% -> \%
  s = s.replace(/\\{2,}([a-zA-Z]+|[{}_#$%&])/g, '\\$1');
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
 * Determines whether a math formula should be rendered in display mode (centered block)
 * vs inline mode (seamlessly inside text).
 */
function shouldDisplayBlock(
  math: string,
  fullMatch: string,
  offset: number,
  fullStr: string,
  forceInline: boolean
): boolean {
  if (forceInline) return false;

  // Check if directly wrapped in parentheses or brackets e.g. ($$math$$)
  const charBefore = offset > 0 ? fullStr[offset - 1] : '';
  const charAfter = offset + fullMatch.length < fullStr.length ? fullStr[offset + fullMatch.length] : '';
  const isEnclosedInParens = (charBefore === '(' || charBefore === '[') && (charAfter === ')' || charAfter === ']');
  if (isEnclosedInParens) {
    return false;
  }

  // Check if it is a single variable token (e.g. "A", "L", "\neg L", "x") embedded in running text
  const trimmedMath = math.trim();
  const isSingleVariableToken = /^(\\neg\s+)?[a-zA-Z0-9_]{1,3}$/.test(trimmedMath);

  const textBefore = fullStr.slice(0, offset);
  const lastNewlineBefore = textBefore.lastIndexOf('\n');
  const linePrefix = lastNewlineBefore === -1 ? textBefore : textBefore.slice(lastNewlineBefore + 1);

  const textAfter = fullStr.slice(offset + fullMatch.length);
  const nextNewlineAfter = textAfter.indexOf('\n');
  const lineSuffix = nextNewlineAfter === -1 ? textAfter : textAfter.slice(0, nextNewlineAfter);

  const hasSurroundingText = linePrefix.trim() !== '' || lineSuffix.trim() !== '';

  if (isSingleVariableToken && hasSurroundingText) {
    return false;
  }

  return true;
}

/**
 * Identifies all LaTeX math expressions (display, environments, bracketed, parenthesis, and inline dollars)
 * and renders them into KaTeX HTML, substituting placeholders to protect the formulas.
 */
function extractAndRenderMath(
  text: string,
  inline: boolean
): { processedText: string; tokens: Map<string, string> } {
  const tokens = new Map<string, string>();
  let tokenIndex = 0;

  const createPlaceholder = (rendered: string): string => {
    const placeholder = `%%%NAG_MATH_BLOCK_${tokenIndex++}%%%`;
    tokens.set(placeholder, rendered);
    return placeholder;
  };

  // 1. Math in $$ ... $$ (display mode when standalone, inline mode when in running text)
  text = text.replace(/\$\$([\s\S]*?)\$\$/g, (fullMatch, math, offset, fullStr) => {
    const isDisplay = shouldDisplayBlock(math, fullMatch, offset, fullStr, inline);
    const rendered = renderKatexString(math, isDisplay);
    return createPlaceholder(rendered);
  });

  // 2. Math in \[ ... \] or \\[ ... \\] (MUST have backslash prefix)
  text = text.replace(/\\{1,2}\[([\s\S]*?)\\{1,2}\]/g, (fullMatch, math, offset, fullStr) => {
    const isDisplay = shouldDisplayBlock(math, fullMatch, offset, fullStr, inline);
    const rendered = renderKatexString(math, isDisplay);
    return createPlaceholder(rendered);
  });

  // 3. LaTeX environments: \begin{matrix|pmatrix|bmatrix|vmatrix|Vmatrix|cases|align|align*|aligned|equation|equation*|gather|gather*}...\end{...}
  const envRegex =
    /\\{1,2}begin\{(matrix|pmatrix|bmatrix|vmatrix|Vmatrix|cases|align|align\*|aligned|equation|equation\*|gather|gather\*)\}([\s\S]*?)\\{1,2}end\{\1\}/g;
  text = text.replace(envRegex, (fullMatch, _env, _inner, offset, fullStr) => {
    const cleanMatch = fullMatch.replace(/\\\\/g, '\\');
    const isDisplay = shouldDisplayBlock(cleanMatch, fullMatch, offset, fullStr, inline);
    const rendered = renderKatexString(cleanMatch, isDisplay);
    return createPlaceholder(rendered);
  });

  // 4. Inline Math: \( ... \) or \\( ... \\) (MUST have backslash prefix, NOT plain parentheses)
  text = text.replace(/\\{1,2}\(([\s\S]*?)\\{1,2}\)/g, (_, math) => {
    const rendered = renderKatexString(math, false);
    return createPlaceholder(rendered);
  });

  // 5. Inline Math: $ ... $ (avoid escaped \$ and ensure non-empty)
  text = text.replace(/(^|[^\\])\$([^\$\n\r]+?)\$(?!\$)/g, (_, prefix, math) => {
    const rendered = renderKatexString(math, false);
    return (prefix || '') + createPlaceholder(rendered);
  });

  return { processedText: text, tokens };
}

/**
 * Parses mixed Markdown and LaTeX content into styled HTML.
 */
function parseContentToHtml(raw: string, inline = false): string {
  if (!raw || !raw.trim()) return '';

  try {
    // 1. Unescape literal \n, \r\n, \t sequences if received as raw text
    let text = unescapeNewlines(raw.trim());

    // 2. Decode common HTML entities that might surround math
    text = decodeHtmlEntities(text);

    // 3. Extract and render all LaTeX math expressions to placeholders FIRST
    // This protects math formulas containing \\, _, *, &, etc. from being corrupted by Markdown or doc cleaners
    const { processedText: textWithoutMath, tokens: mathTokens } = extractAndRenderMath(text, inline);
    text = textWithoutMath;

    // 4. Normalize pipe tables
    if (!inline) {
      text = normalizeMarkdownTables(text);
    }

    // 5. Clean LaTeX document-level commands outside math blocks
    text = cleanLatexDocCommands(text);

    // 6. Pre-process inline markdown formatting
    text = parseInlineMarkdown(text);

    // 7. Parse Markdown using marked
    let htmlResult = '';
    if (inline) {
      const parsed = marked.parseInline(text, { gfm: true, breaks: true });
      htmlResult = typeof parsed === 'string' ? parsed : '';
    } else {
      const parsed = marked.parse(text, { gfm: true, breaks: true, async: false });
      htmlResult = typeof parsed === 'string' ? parsed : '';
    }

    // 8. Secondary pass for any inline markdown in HTML blocks passed through by marked
    htmlResult = parseInlineMarkdown(htmlResult);

    // 9. Reinsert KaTeX rendered HTML
    for (const [placeholder, rendered] of mathTokens.entries()) {
      htmlResult = htmlResult.split(placeholder).join(rendered);
    }

    return htmlResult;
  } catch (err) {
    console.warn('Failed to parse Markdown / Math content:', err);
    return escapeHtml(raw);
  }
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

  if (!htmlContent) {
    return null;
  }

  if (inline) {
    return (
      <span
        className={`math-rendered-content inline ${className}`}
        dangerouslySetInnerHTML={{ __html: htmlContent }}
      />
    );
  }

  return (
    <div
      className={`math-rendered-content ${className}`}
      dangerouslySetInnerHTML={{ __html: htmlContent }}
    />
  );
};

export default MathRenderer;
