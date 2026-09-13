import React, { useMemo } from 'react';
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

/**
 * Unescapes literal JSON/escaped newline sequences (\n, \r\n, \t) into real whitespace,
 * but ONLY when they are NOT part of a LaTeX command name.
 *
 * Problem: a naïve global `\n → newline` replacement corrupts LaTeX commands that begin
 * with the letters n, r, or t — e.g. `\neq`, `\neg`, `\rightarrow`, `\text`, `\tau`.
 * The string `\neq` in raw JSON is stored as `\` + `n` + `e` + `q`, and a blanket
 * replace turns it into newline + `eq`, breaking the rendered output.
 *
 * Safe rules:
 * - All standard LaTeX command names are lowercase (e.g. \neq, \neg, \rightarrow).
 *   Uppercase variants like \N, \I don't exist as common math commands.
 * - Therefore `\n` followed by a LOWERCASE letter is a LaTeX command; block it.
 * - `\n` followed by an uppercase letter (e.g. \nI., \nII.) is a list separator; allow it.
 * - `\n` followed by a digit, space, or punctuation is always a JSON newline; allow it.
 * - Same logic for `\t` (blocks \tau, \theta, \times, \to, \text).
 */
function unescapeNewlines(text: string): string {
  if (!text) return '';
  return text
    // \r\n sequence — always a JSON line ending
    .replace(/\\r\\n/g, '\n')
    // \n only when NOT immediately followed by a lowercase ASCII letter (LaTeX command)
    .replace(/\\n(?![a-z])/g, '\n')
    // \t only when NOT immediately followed by a lowercase ASCII letter (e.g. \tau, \theta)
    .replace(/\\t(?![a-z])/g, '\t');
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
 *
 * FIX #1 — Multi-escaped backslashes:
 * Backend/LLM serialization sometimes emits 3–4 consecutive backslashes before LaTeX command
 * words (e.g. \\\\det, \\\neg, \\\\frac).  A single replacement pass only collapses one level;
 * we iterate until stable so every over-escaped chain is fully reduced to a single backslash.
 */
function sanitizeLatex(latex: string): string {
  if (!latex || typeof latex !== 'string') return '';
  let s = latex.trim();

  // Iteratively collapse over-escaped backslash chains before LaTeX commands/special chars
  // e.g. \\\\det -> \\det -> \det ; \\\neg -> \\neg -> \neg
  let previous: string;
  do {
    previous = s;
    s = s.replace(/\\{2,}([a-zA-Z]+|[{}_#$%&^~])/g, '\\$1');
  } while (s !== previous);

  // Ensure bare % is escaped for KaTeX (% is a LaTeX comment character)
  s = s.replace(/(?<!\\)%/g, '\\%');

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
 *
 * FIX #3 — Numbered-list / inline context:
 * A formula on a line that starts with a list marker (e.g. "1.", "I.", "-", "*")
 * followed by other text should never become a display block — it must stay inline
 * to avoid breaking the statement list flow.
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
  const charAfter =
    offset + fullMatch.length < fullStr.length ? fullStr[offset + fullMatch.length] : '';
  const isEnclosedInParens =
    (charBefore === '(' || charBefore === '[') && (charAfter === ')' || charAfter === ']');
  if (isEnclosedInParens) return false;

  const textBefore = fullStr.slice(0, offset);
  const lastNewlineBefore = textBefore.lastIndexOf('\n');
  const linePrefix =
    lastNewlineBefore === -1 ? textBefore : textBefore.slice(lastNewlineBefore + 1);

  const textAfter = fullStr.slice(offset + fullMatch.length);
  const nextNewlineAfter = textAfter.indexOf('\n');
  const lineSuffix = nextNewlineAfter === -1 ? textAfter : textAfter.slice(0, nextNewlineAfter);

  // FIX #3: If the line starts with a numbered/lettered/bulleted list marker treat as inline
  // Handles: "1.", "2.", "I.", "II.", "A.", "-", "*", "•"
  const listItemPrefix = /^\s*(\d+\.|[IVXLC]+\.|[A-Za-z]\.|[-*•])\s/;
  if (listItemPrefix.test(linePrefix)) return false;

  // Check if it is a single variable token (e.g. "A", "L", "\neg L", "x") embedded in running text
  const trimmedMath = math.trim();
  const isSingleVariableToken = /^(\\[a-zA-Z]+\s+)?[a-zA-Z0-9_]{1,3}$/.test(trimmedMath);

  const hasSurroundingText = linePrefix.trim() !== '' || lineSuffix.trim() !== '';

  if (isSingleVariableToken && hasSurroundingText) return false;

  return true;
}

/**
 * Extracts inline SVG blocks as opaque placeholders so that subsequent markdown
 * and LaTeX processing steps cannot corrupt attribute values or `<path>` data.
 *
 * FIX #4 — SVG + Markdown + LaTeX integration
 */
function extractSvgBlocks(
  text: string
): { processedText: string; tokens: Map<string, string> } {
  const tokens = new Map<string, string>();
  let idx = 0;
  const processedText = text.replace(/<svg[\s\S]*?<\/svg>/gi, (match) => {
    const placeholder = `%%%NAG_SVG_BLOCK_${idx++}%%%`;
    tokens.set(placeholder, match);
    return placeholder;
  });
  return { processedText, tokens };
}

/**
 * Identifies all LaTeX math expressions (display, environments, bracketed, parenthesis, and inline dollars)
 * and renders them into KaTeX HTML, substituting placeholders to protect the formulas.
 *
 * FIX #2 — Delimiter variations:
 * Upstream content may use 1–4 backslashes before ( ) [ ] delimiters.  The regex now
 * accepts any run of 1–4 backslashes so \\(, \\\(, \\\\( all match correctly.
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

  // 2. Math in \[ ... \] — accept 1–4 leading/trailing backslashes (FIX #2)
  text = text.replace(/\\{1,4}\[([\s\S]*?)\\{1,4}\]/g, (fullMatch, math, offset, fullStr) => {
    const isDisplay = shouldDisplayBlock(math, fullMatch, offset, fullStr, inline);
    const rendered = renderKatexString(math, isDisplay);
    return createPlaceholder(rendered);
  });

  // 3. LaTeX environments: \begin{...}...\end{...}
  const envRegex =
    /\\{1,4}begin\{(matrix|pmatrix|bmatrix|vmatrix|Vmatrix|cases|align|align\*|aligned|equation|equation\*|gather|gather\*)\}([\s\S]*?)\\{1,4}end\{\1\}/g;
  text = text.replace(envRegex, (fullMatch, _env, _inner, offset, fullStr) => {
    // Normalize to single backslashes before handing to KaTeX
    const cleanMatch = fullMatch.replace(/\\{2,}/g, '\\');
    const isDisplay = shouldDisplayBlock(cleanMatch, fullMatch, offset, fullStr, inline);
    const rendered = renderKatexString(cleanMatch, isDisplay);
    return createPlaceholder(rendered);
  });

  // 4. Inline Math: \( ... \) — accept 1–4 leading/trailing backslashes (FIX #2)
  text = text.replace(/\\{1,4}\(([\s\S]*?)\\{1,4}\)/g, (_, math) => {
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

    // 3. Extract SVG blocks as opaque placeholders FIRST (FIX #4)
    const { processedText: textWithoutSvg, tokens: svgTokens } = extractSvgBlocks(text);
    text = textWithoutSvg;

    // 4. Extract and render all LaTeX math expressions to placeholders
    // This protects math formulas containing \\, _, *, &, etc. from being corrupted by Markdown or doc cleaners
    const { processedText: textWithoutMath, tokens: mathTokens } = extractAndRenderMath(
      text,
      inline
    );
    text = textWithoutMath;

    // 5. Normalize pipe tables
    if (!inline) {
      text = normalizeMarkdownTables(text);
    }

    // 6. Clean LaTeX document-level commands outside math blocks
    text = cleanLatexDocCommands(text);

    // 7. Pre-process inline markdown formatting
    text = parseInlineMarkdown(text);

    // 8. Parse Markdown using marked
    let htmlResult = '';
    if (inline) {
      const parsed = marked.parseInline(text, { gfm: true, breaks: true });
      htmlResult = typeof parsed === 'string' ? parsed : '';
    } else {
      const parsed = marked.parse(text, { gfm: true, breaks: true, async: false });
      htmlResult = typeof parsed === 'string' ? parsed : '';
    }

    // 9. Secondary pass for any inline markdown in HTML blocks passed through by marked
    htmlResult = parseInlineMarkdown(htmlResult);

    // 10. Reinsert KaTeX rendered HTML
    for (const [placeholder, rendered] of mathTokens.entries()) {
      htmlResult = htmlResult.split(placeholder).join(rendered);
    }

    // 11. Reinsert SVG blocks (FIX #4)
    for (const [placeholder, svgBlock] of svgTokens.entries()) {
      htmlResult = htmlResult.split(placeholder).join(svgBlock);
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
