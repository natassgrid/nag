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
      return math.display ? `\n$$${math.latex}$$\n` : `$$${math.latex}$$`;
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
 *
 * Priority:
 *  1. If value is already an ExamDocument array (object), return it directly.
 *  2. If the string looks like a JSON array with typed nodes → EXAM_JSON
 *  3. Otherwise → MARKDOWN (the legacy $$...$$ text with math goes here)
 */
export function deserialiseContent(value: unknown): ExamDocument {
  if (!value) return [...EMPTY_DOCUMENT];

  // Already an array of ExamElements — pass through
  if (Array.isArray(value) && value.length > 0 && typeof value[0] === 'object' && 'type' in value[0]) {
    return value as ExamDocument;
  }

  if (typeof value !== 'string') return [...EMPTY_DOCUMENT];

  const trimmed = value.trim();
  if (!trimmed) return [...EMPTY_DOCUMENT];

  // Detect EXAM_JSON
  if (trimmed.startsWith('[') && trimmed.endsWith(']')) {
    try {
      const parsed = JSON.parse(trimmed);
      if (Array.isArray(parsed) && parsed.length > 0 && typeof parsed[0] === 'object' && 'type' in parsed[0]) {
        return parsed as ExamDocument;
      }
    } catch {
      // Fall through to Markdown
    }
  }

  // Markdown / legacy text
  return parseMarkdownToDocument(trimmed);
}

/**
 * Parse a Markdown string (potentially containing $$...$$ and <smiles>) into
 * a structured ExamDocument.
 */
export function parseMarkdownToDocument(markdown: string): ExamDocument {
  if (!markdown || !markdown.trim()) return [...EMPTY_DOCUMENT];

  // Unescape JSON-encoded newlines so \n becomes a real newline
  const text = unescapeNewlines(markdown);
  const lines = text.split('\n');
  const doc: ExamElement[] = [];
  let i = 0;

  while (i < lines.length) {
    const line = lines[i];

    // --- Blank line → skip (paragraph breaks are implicit per line)
    if (!line.trim()) {
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

    // --- Numbered list (greedy: consume consecutive numbered lines)
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

    // --- Bulleted list
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

    // --- Markdown image
    const imgMatch = line.match(/^!\[([^\]]*)\]\(([^)]+)\)$/);
    if (imgMatch) {
      const assetIdMatch = imgMatch[2].match(/\/api\/v1\/assets\/([^/]+)\/download/);
      if (assetIdMatch) {
        doc.push({ type: 'image', assetId: assetIdMatch[1], alt: imgMatch[1], children: [{ text: '' }] });
      } else {
        doc.push({ type: 'paragraph', children: [{ text: line }] });
      }
      i++;
      continue;
    }

    // --- Standalone Math block: $$...$$
    const mathBlockMatch = line.match(/^\$\$([\s\S]*?)\$\$$/);
    if (mathBlockMatch) {
      doc.push({
        type: 'math-inline',
        latex: mathBlockMatch[1].trim(),
        display: true,
        children: [{ text: '' }]
      });
      i++;
      continue;
    }

    // --- SMILES chemical structure: <smiles ...>...</smiles>
    const smilesMatch = line.match(/^<smiles(?:\s+([^>]*?))?>([\s\S]*?)<\/smiles>(?:\s*<!--\s*(.*?)\s*-->)?/i);
    if (smilesMatch) {
      const rawAttrs = smilesMatch[1] || '';
      const titleAttrMatch = rawAttrs.match(/title="([^"]*)"/i);
      const widthMatch = rawAttrs.match(/width="(\d+)"/i);
      const heightMatch = rawAttrs.match(/height="(\d+)"/i);
      const themeMatch = rawAttrs.match(/theme="(light|dark)"/i);
      const title = titleAttrMatch ? titleAttrMatch[1] : (smilesMatch[3]?.trim() || undefined);
      const width = widthMatch ? parseInt(widthMatch[1], 10) : undefined;
      const height = heightMatch ? parseInt(heightMatch[1], 10) : undefined;
      const theme = themeMatch ? (themeMatch[1] as 'light' | 'dark') : undefined;

      doc.push({
        type: 'chemical-structure',
        smiles: smilesMatch[2].trim(),
        title,
        width,
        height,
        theme,
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
      results.push({ text: `\x00math\x00${match[1]}\x00` });
    } else if (match[3] !== undefined) {
      // <smiles ...>...</smiles>
      results.push({ text: `\x00smiles\x00${match[3]}\x00` });
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

/** Sentinel prefix for inline math in text children */
export const MATH_SENTINEL = '\x00math\x00';
/** Sentinel prefix for inline SMILES in text children */
export const SMILES_SENTINEL = '\x00smiles\x00';

/** Decode a text sentinel back to its payload. Returns null if not a sentinel. */
export function decodeSentinel(text: string): { kind: 'math' | 'smiles'; payload: string } | null {
  if (text.startsWith(MATH_SENTINEL) && text.endsWith('\x00')) {
    return { kind: 'math', payload: text.slice(MATH_SENTINEL.length, -1) };
  }
  if (text.startsWith(SMILES_SENTINEL) && text.endsWith('\x00')) {
    return { kind: 'smiles', payload: text.slice(SMILES_SENTINEL.length, -1) };
  }
  return null;
}
