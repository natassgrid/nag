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

import {
  ExamDocument,
  ExamElement,
  ExamText,
  ParagraphElement,
  HeadingOneElement,
  HeadingTwoElement,
  HeadingThreeElement,
  NumberedListElement,
  BulletedListElement,
  ListItemElement,
  ImageElement,
  MathInlineElement,
  ChemicalStructureElement,
  EMPTY_DOCUMENT
} from '../models';

/**
 * Serializer — converts between the structured ExamDocument JSON model and
 * human-readable Markdown strings used by the backend API.
 *
 * Round-trip guarantees:
 *  • math-inline  ↔  $$...$$ (AGENTS.md LaTeX convention)
 *  • chemical-structure ↔  <smiles>...</smiles>
 *  • headings H1–H3  ↔  # / ## / ### markdown
 *  • bold/italic/underline/sub/super  ↔  **…** / *…* / <u>…</u> / <sub>…</sub> / <sup>…</sup>
 *  • numbered/bulleted lists  ↔  1. … / - …
 *  • images  ↔  ![alt](assetUrl)
 *  • alignment / indent: stored as HTML attributes on the serialised paragraph
 *
 * Content detection (load path):
 *  • Detects EXAM_JSON  by detecting a JSON array whose first element has a `type` key
 *  • Detects HTML      by detecting leading < tag
 *  • Falls back to MARKDOWN
 */

// ─── Serialise (ExamDocument → Markdown string) ───

/**
 * Serialise an ExamDocument to a Markdown string for API persistence.
 */
export function serialiseDocument(doc: ExamDocument): string {
  if (!doc || doc.length === 0) return '';
  return doc.map(el => serialiseElement(el, 0)).join('\n');
}

function serialiseElement(el: ExamElement, _depth: number): string {
  switch (el.type) {
    case 'paragraph': {
      const p = el as ParagraphElement;
      const text = serialiseInlineChildren(p.children);
      return text || '';
    }
    case 'heading-one':
      return `# ${serialiseInlineChildren((el as HeadingOneElement).children)}`;
    case 'heading-two':
      return `## ${serialiseInlineChildren((el as HeadingTwoElement).children)}`;
    case 'heading-three':
      return `### ${serialiseInlineChildren((el as HeadingThreeElement).children)}`;
    case 'numbered-list': {
      const list = el as NumberedListElement;
      return list.children
        .map((item, idx) => `${idx + 1}. ${serialiseInlineChildren((item as ListItemElement).children)}`)
        .join('\n');
    }
    case 'bulleted-list': {
      const list = el as BulletedListElement;
      return list.children
        .map(item => `- ${serialiseInlineChildren((item as ListItemElement).children)}`)
        .join('\n');
    }
    case 'list-item':
      return serialiseInlineChildren((el as ListItemElement).children);
    case 'image': {
      const img = el as ImageElement;
      const alt = img.alt || 'Image';
      const url = `/api/v1/assets/${img.assetId}/download`;
      return `![${alt}](${url})`;
    }
    case 'audio':
    case 'video': {
      const media = el as any;
      const url = `/api/v1/assets/${media.assetId}/download`;
      return `[${el.type}](${url})`;
    }
    case 'math-inline': {
      const math = el as MathInlineElement;
      const cleanLatex = sanitizeLatex(math.latex);
      return math.display ? `\n$$${cleanLatex}$$\n` : `$$${cleanLatex}$$`;
    }
    case 'chemical-structure': {
      const chem = el as ChemicalStructureElement;
      const titleAttr = chem.title ? ` title="${chem.title.replace(/"/g, '&quot;')}"` : '';
      const widthAttr = chem.width && chem.width !== 260 ? ` width="${chem.width}"` : '';
      const heightAttr = chem.height && chem.height !== 200 ? ` height="${chem.height}"` : '';
      const themeAttr = chem.theme && chem.theme !== 'light' ? ` theme="${chem.theme}"` : '';
      const titleComment = chem.title ? ` <!-- ${chem.title} -->` : '';
      return `<smiles${titleAttr}${widthAttr}${heightAttr}${themeAttr}>${chem.smiles}</smiles>${titleComment}`;
    }
    default:
      return '';
  }
}

function serialiseInlineChildren(children: ExamText[]): string {
  return children.map(child => serialiseTextNode(child)).join('');
}

function serialiseTextNode(text: ExamText): string {
  let s = text.text || '';
  if (!s) return '';

  const sentinel = decodeSentinel(s);
  if (sentinel) {
    if (sentinel.kind === 'math') {
      return `$$${sanitizeLatex(sentinel.payload)}$$`;
    }
    if (sentinel.kind === 'smiles') {
      return `<smiles>${sentinel.payload}</smiles>`;
    }
  }

  // Strip any accidental null characters (\u0000) to ensure PostgreSQL JSON compatibility
  s = s.replace(/\x00/g, '');

  // Apply marks from innermost to outermost
  if (text.subscript) s = `<sub>${s}</sub>`;
  if (text.superscript) s = `<sup>${s}</sup>`;
  if (text.underline) s = `<u>${s}</u>`;
  if (text.bold && text.italic) { s = `***${s}***`; }
  else if (text.bold) { s = `**${s}**`; }
  else if (text.italic) { s = `*${s}*`; }

  return s;
}

// ─── Deserialise (string → ExamDocument) ───

/**
 * Possible content formats stored in the backend.
 */
export type ContentFormat = 'EXAM_JSON' | 'MARKDOWN' | 'HTML';

/**
 * Auto-detect the content format of a string and deserialise into ExamDocument.
 */
export function deserialiseContent(raw: string | null | undefined): ExamDocument {
  if (!raw || typeof raw !== 'string') return [...EMPTY_DOCUMENT];
  const trimmed = raw.trim();
  if (!trimmed) return [...EMPTY_DOCUMENT];

  const format = detectFormat(trimmed);
  switch (format) {
    case 'EXAM_JSON':
      return parseJsonToDocument(trimmed);
    case 'HTML':
      return parseHtmlToDocument(trimmed);
    case 'MARKDOWN':
    default:
      return parseMarkdownToDocument(trimmed);
  }
}

function detectFormat(trimmed: string): ContentFormat {
  if (trimmed.startsWith('[') && trimmed.endsWith(']')) {
    try {
      const parsed = JSON.parse(trimmed);
      if (Array.isArray(parsed) && (parsed.length === 0 || (parsed[0] && typeof parsed[0].type === 'string'))) {
        return 'EXAM_JSON';
      }
    } catch {
      // not JSON, fall through
    }
  }
  if (trimmed.startsWith('<') && trimmed.includes('>')) {
    return 'HTML';
  }
  return 'MARKDOWN';
}

function parseJsonToDocument(json: string): ExamDocument {
  try {
    const parsed = JSON.parse(json);
    return Array.isArray(parsed) && parsed.length > 0 ? (parsed as ExamDocument) : [...EMPTY_DOCUMENT];
  } catch {
    return [...EMPTY_DOCUMENT];
  }
}

function parseHtmlToDocument(html: string): ExamDocument {
  const parser = new DOMParser();
  const doc = parser.parseFromString(html, 'text/html');
  const body = doc.body;

  const result: ExamElement[] = [];
  Array.from(body.children).forEach(el => {
    const parsed = domElementToExamElement(el as HTMLElement);
    if (parsed) result.push(parsed);
  });

  return result.length > 0 ? result : [...EMPTY_DOCUMENT];
}

function domElementToExamElement(el: HTMLElement): ExamElement | null {
  const tag = el.tagName.toLowerCase();
  switch (tag) {
    case 'h1':
      return { type: 'heading-one', children: extractTextNodes(el) };
    case 'h2':
      return { type: 'heading-two', children: extractTextNodes(el) };
    case 'h3':
      return { type: 'heading-three', children: extractTextNodes(el) };
    case 'ol':
      return {
        type: 'numbered-list',
        children: Array.from(el.children).map(li => ({
          type: 'list-item' as const,
          children: extractTextNodes(li as HTMLElement)
        }))
      };
    case 'ul':
      return {
        type: 'bulleted-list',
        children: Array.from(el.children).map(li => ({
          type: 'list-item' as const,
          children: extractTextNodes(li as HTMLElement)
        }))
      };
    case 'p':
    default:
      return { type: 'paragraph', children: extractTextNodes(el) };
  }
}

function extractTextNodes(parent: HTMLElement): ExamText[] {
  const texts: ExamText[] = [];
  Array.from(parent.childNodes).forEach(node => {
    if (node.nodeType === Node.TEXT_NODE) {
      if (node.textContent) texts.push({ text: node.textContent });
    } else if (node.nodeType === Node.ELEMENT_NODE) {
      const el = node as HTMLElement;
      const inner = el.textContent || '';
      const tag = el.tagName.toLowerCase();
      const mark: Partial<ExamText> = {};
      if (tag === 'strong' || tag === 'b') mark.bold = true;
      if (tag === 'em' || tag === 'i') mark.italic = true;
      if (tag === 'u') mark.underline = true;
      if (tag === 'sup') mark.superscript = true;
      if (tag === 'sub') mark.subscript = true;
      texts.push({ text: inner, ...mark });
    }
  });
  return texts.length > 0 ? texts : [{ text: '' }];
}

// ─── Markdown Parser ───

/**
 * Parses Markdown into an ExamDocument AST.
 * Handles headings (#, ##, ###), lists (1. , - ), images (![alt](url)),
 * LaTeX block/inline ($$...$$), and chemical structures (<smiles ...>...</smiles>).
 */
export function parseMarkdownToDocument(md: string): ExamDocument {
  if (!md || !md.trim()) return [...EMPTY_DOCUMENT];

  // Unescape literal JSON newline escapes if any
  const normalized = unescapeNewlines(md).replace(/\r\n/g, '\n');
  const lines = normalized.split('\n');
  const doc: ExamElement[] = [];

  let i = 0;
  while (i < lines.length) {
    const line = lines[i];

    // Blank line
    if (line.trim() === '') {
      i++;
      continue;
    }

    // --- Standalone Block Math ($$...$$ on own line or multi-line)
    if (line.trim().startsWith('$$')) {
      let latex = '';
      const trimmed = line.trim();
      if (trimmed.startsWith('$$') && trimmed.endsWith('$$') && trimmed.length > 4) {
        // Single-line block math: "$$\frac{a}{b}$$"
        latex = trimmed.slice(2, -2).trim();
        doc.push({
          type: 'math-inline',
          latex: sanitizeLatex(latex),
          display: true,
          children: [{ text: '' }]
        } as MathInlineElement);
        i++;
        continue;
      } else {
        // Multi-line block math
        latex = trimmed.slice(2);
        i++;
        while (i < lines.length && !lines[i].trim().endsWith('$$')) {
          latex += (latex ? '\n' : '') + lines[i];
          i++;
        }
        if (i < lines.length) {
          const endLine = lines[i].trim();
          latex += (latex ? '\n' : '') + endLine.slice(0, -2).trim();
          i++;
        }
        doc.push({
          type: 'math-inline',
          latex: sanitizeLatex(latex),
          display: true,
          children: [{ text: '' }]
        } as MathInlineElement);
        continue;
      }
    }

    // --- Standalone SMILES chemical structure tag: <smiles ...>...</smiles>
    const smilesBlockMatch = line.trim().match(/^<smiles(?:\s+([^>]*?))?>([\s\S]*?)<\/smiles>(?:\s*<!--\s*(.*?)\s*-->)?$/i);
    if (smilesBlockMatch) {
      const attrs = smilesBlockMatch[1] || '';
      const smiles = smilesBlockMatch[2].trim();
      const commentTitle = smilesBlockMatch[3];

      const titleAttr = attrs.match(/title=["']([^"']+)["']/i);
      const widthAttr = attrs.match(/width=["']?(\d+)["']?/i);
      const heightAttr = attrs.match(/height=["']?(\d+)["']?/i);
      const themeAttr = attrs.match(/theme=["'](light|dark)["']/i);

      doc.push({
        type: 'chemical-structure',
        smiles,
        title: titleAttr ? titleAttr[1] : (commentTitle || undefined),
        width: widthAttr ? parseInt(widthAttr[1], 10) : undefined,
        height: heightAttr ? parseInt(heightAttr[1], 10) : undefined,
        theme: (themeAttr ? themeAttr[1] : undefined) as any,
        children: [{ text: '' }]
      } as ChemicalStructureElement);
      i++;
      continue;
    }

    // --- Headings
    if (line.startsWith('### ')) {
      doc.push({ type: 'heading-three', children: parseInlineLine(line.slice(4)) });
      i++;
      continue;
    }
    if (line.startsWith('## ')) {
      doc.push({ type: 'heading-two', children: parseInlineLine(line.slice(3)) });
      i++;
      continue;
    }
    if (line.startsWith('# ')) {
      doc.push({ type: 'heading-one', children: parseInlineLine(line.slice(2)) });
      i++;
      continue;
    }

    // --- Numbered List
    if (/^\d+\.\s/.test(line)) {
      const items: ListItemElement[] = [];
      while (i < lines.length && /^\d+\.\s/.test(lines[i])) {
        const itemText = lines[i].replace(/^\d+\.\s/, '');
        items.push({ type: 'list-item', children: parseInlineLine(itemText) });
        i++;
      }
      doc.push({ type: 'numbered-list', children: items });
      continue;
    }

    // --- Bulleted List
    if (/^[-*]\s/.test(line)) {
      const items: ListItemElement[] = [];
      while (i < lines.length && /^[-*]\s/.test(lines[i])) {
        const itemText = lines[i].replace(/^[-*]\s/, '');
        items.push({ type: 'list-item', children: parseInlineLine(itemText) });
        i++;
      }
      doc.push({ type: 'bulleted-list', children: items });
      continue;
    }

    // --- Image: ![alt](url)
    const imgMatch = line.match(/^!\[(.*?)\]\((.*?)\)$/);
    if (imgMatch) {
      const assetMatch = imgMatch[2].match(/\/api\/v1\/assets\/([^/]+)\/download/);
      const assetId = assetMatch ? assetMatch[1] : imgMatch[2];
      doc.push({
        type: 'image',
        assetId,
        alt: imgMatch[1],
        children: [{ text: '' }]
      });
      i++;
      continue;
    }

    // --- Paragraph (may contain inline $$...$$ and <smiles>)
    doc.push({ type: 'paragraph', children: parseInlineLine(line) });
    i++;
  }

  return doc.length > 0 ? doc : [...EMPTY_DOCUMENT];
}

/**
 * Parse an inline text line that may contain $$...$$ math tokens,
 * <smiles>...</smiles>, and standard Markdown marks (bold, italic, etc.).
 */
function parseInlineLine(line: string): ExamText[] {
  if (!line) return [{ text: '' }];

  const results: ExamText[] = [];

  // Tokenise by $$...$$ and <smiles ...>...</smiles>
  const tokenRe = /\$\$([\s\S]*?)\$\$|<smiles(?:\s+([^>]*?))?>([\s\S]*?)<\/smiles>/gi;
  let lastIndex = 0;
  let match: RegExpExecArray | null;

  while ((match = tokenRe.exec(line)) !== null) {
    if (match.index > lastIndex) {
      const before = line.slice(lastIndex, match.index);
      results.push(...parseMarkupText(before));
    }

    if (match[1] !== undefined) {
      // $$...$$ math
      results.push({ text: encodeMathSentinel(sanitizeLatex(match[1])) });
    } else if (match[3] !== undefined) {
      // <smiles ...>...</smiles>
      results.push({ text: encodeSmilesSentinel(match[3]) });
    }

    lastIndex = tokenRe.lastIndex;
  }

  if (lastIndex < line.length) {
    results.push(...parseMarkupText(line.slice(lastIndex)));
  }

  return results.length > 0 ? results : [{ text: '' }];
}

/**
 * Parse a plain text segment for bold/italic/underline/super/subscript marks.
 */
function parseMarkupText(text: string): ExamText[] {
  if (!text) return [];

  const result: ExamText[] = [];
  const markRe = /(\*\*\*([^*]+?)\*\*\*|\*\*([^*]+?)\*\*|\*([^*]+?)\*|<u>([^<]+?)<\/u>|<sup>([^<]+?)<\/sup>|<sub>([^<]+?)<\/sub>)/g;
  let last = 0;
  let m: RegExpExecArray | null;

  while ((m = markRe.exec(text)) !== null) {
    if (m.index > last) result.push({ text: text.slice(last, m.index) });

    if (m[2] !== undefined) result.push({ text: m[2], bold: true, italic: true });
    else if (m[3] !== undefined) result.push({ text: m[3], bold: true });
    else if (m[4] !== undefined) result.push({ text: m[4], italic: true });
    else if (m[5] !== undefined) result.push({ text: m[5], underline: true });
    else if (m[6] !== undefined) result.push({ text: m[6], superscript: true });
    else if (m[7] !== undefined) result.push({ text: m[7], subscript: true });

    last = markRe.lastIndex;
  }

  if (last < text.length) result.push({ text: text.slice(last) });

  return result.length > 0 ? result : [{ text }];
}

/**
 * Unescape JSON-encoded newline / tab sequences in a string,
 * but NOT when immediately followed by a lowercase LaTeX command letter.
 */
function unescapeNewlines(text: string): string {
  if (!text) return '';
  return text
    .replace(/\\r\\n/g, '\n')
    .replace(/\\n(?![a-z])/g, '\n')
    .replace(/\\t(?![a-z])/g, '\t');
}

// ─── Inline Sentinel Helpers ───

/** Sentinel prefix for inline math in text children (Unicode Private Use Area) */
export const MATH_SENTINEL = '\uE000math\uE000';
/** Sentinel prefix for inline SMILES in text children (Unicode Private Use Area) */
export const SMILES_SENTINEL = '\uE000smiles\uE000';

export function encodeMathSentinel(latex: string): string {
  return `${MATH_SENTINEL}${latex}\uE000`;
}

export function encodeSmilesSentinel(smiles: string): string {
  return `${SMILES_SENTINEL}${smiles}\uE000`;
}

/** Decode a text sentinel back to its payload. Returns null if not a sentinel. */
export function decodeSentinel(text: string): { kind: 'math' | 'smiles'; payload: string } | null {
  if (!text) return null;
  // Handle safe sentinel (\uE000) as well as legacy sentinel (\x00)
  if (text.startsWith(MATH_SENTINEL) && text.endsWith('\uE000')) {
    return { kind: 'math', payload: text.slice(MATH_SENTINEL.length, -1) };
  }
  if (text.startsWith('\x00math\x00') && text.endsWith('\x00')) {
    return { kind: 'math', payload: text.slice(6, -1) };
  }
  if (text.startsWith(SMILES_SENTINEL) && text.endsWith('\uE000')) {
    return { kind: 'smiles', payload: text.slice(SMILES_SENTINEL.length, -1) };
  }
  if (text.startsWith('\x00smiles\x00') && text.endsWith('\x00')) {
    return { kind: 'smiles', payload: text.slice(8, -1) };
  }
  return null;
}

/**
 * Normalizes and sanitizes LaTeX strings, repairing mangled AI outputs and escape artifacts:
 *  - Converts '\ ext' or '\ text' -> '\text'
 *  - Replaces '\t' tab character that unescaped '\text', '\tan', '\times', etc.
 *  - Strips stray whitespace after backslash before command words ('\ frac' -> '\frac')
 *  - Collapses duplicated backslashes before commands
 *  - Escapes bare '%' characters for KaTeX
 */
export function sanitizeLatex(latex: string): string {
  if (!latex || typeof latex !== 'string') return '';
  let s = latex.trim();

  // 1. Repair tab characters that were unescaped from \t-commands
  s = s.replace(/\t(ext|an|imes|heta|au|o|herefore|ilde|extbf|extit)\b/g, '\\$1');

  // 2. Fix '\ ext' or '\  ext' or '\ text' -> '\text'
  s = s.replace(/\\+(\s*)ext(?=\{|\s|$|[0-9])/g, '\\text');

  // 3. Fix whitespace between backslash and command letters ('\ frac' -> '\frac')
  s = s.replace(/\\+\s+([a-zA-Z]+)/g, '\\$1');

  // 4. Deduplicate multiple backslashes before commands
  let previous: string;
  do {
    previous = s;
    s = s.replace(/\\{2,}([a-zA-Z]+|[{}_#$%&^~])/g, '\\$1');
  } while (s !== previous);

  // 5. Ensure bare % is escaped for KaTeX
  s = s.replace(/(?<!\\)%/g, '\\%');

  return s;
}
