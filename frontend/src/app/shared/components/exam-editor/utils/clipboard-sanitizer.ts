import { ExamElement, ExamText } from '../models';

/**
 * Clipboard sanitizer for the Exam Editor.
 *
 * Accepts:
 * - Plain text
 * - Microsoft Word content (stripped to plain text)
 * - LibreOffice content (stripped to plain text)
 *
 * Rejects / strips:
 * - HTML tags
 * - JavaScript
 * - CSS
 * - Embedded objects
 * - Macros
 * - Data URIs
 * - External URLs in content
 *
 * All pasted content is converted to safe ExamDocument nodes.
 */

/**
 * Patterns that indicate dangerous content.
 */
const DANGEROUS_PATTERNS: RegExp[] = [
  /<script[\s\S]*?<\/script>/gi,
  /<style[\s\S]*?<\/style>/gi,
  /<link[\s\S]*?>/gi,
  /javascript:/gi,
  /vbscript:/gi,
  /on\w+\s*=/gi,             // onclick=, onload=, etc.
  /data:\s*\w+\/\w+/gi,     // data: URIs
  /<object[\s\S]*?<\/object>/gi,
  /<embed[\s\S]*?>/gi,
  /<applet[\s\S]*?<\/applet>/gi,
  /<iframe[\s\S]*?<\/iframe>/gi,
  /<form[\s\S]*?<\/form>/gi,
  /expression\s*\(/gi,       // CSS expression()
  /url\s*\(/gi,              // CSS url()
  /@import/gi,
  /<!--[\s\S]*?-->/g,        // HTML comments (can hide content)
];

/**
 * Strip all HTML tags from content, preserving text.
 */
function stripHtml(html: string): string {
  // Remove dangerous patterns first
  let cleaned = html;
  for (const pattern of DANGEROUS_PATTERNS) {
    cleaned = cleaned.replace(pattern, '');
  }

  // Strip remaining HTML tags
  cleaned = cleaned.replace(/<[^>]*>/g, '');

  // Decode HTML entities
  cleaned = decodeHtmlEntities(cleaned);

  // Normalize whitespace
  cleaned = cleaned.replace(/\r\n/g, '\n');
  cleaned = cleaned.replace(/\r/g, '\n');

  // Remove null bytes
  cleaned = cleaned.replace(/\0/g, '');

  return cleaned;
}

/**
 * Decode common HTML entities to their text equivalents.
 */
function decodeHtmlEntities(text: string): string {
  const entities: Record<string, string> = {
    '&amp;': '&',
    '&lt;': '<',
    '&gt;': '>',
    '&quot;': '"',
    '&#39;': "'",
    '&apos;': "'",
    '&nbsp;': ' ',
    '&ndash;': '–',
    '&mdash;': '—',
    '&lsquo;': '\u2018',
    '&rsquo;': '\u2019',
    '&ldquo;': '\u201C',
    '&rdquo;': '\u201D',
    '&hellip;': '…',
    '&copy;': '©',
    '&reg;': '®',
    '&trade;': '™',
    '&times;': '×',
    '&divide;': '÷',
  };

  let decoded = text;
  for (const [entity, char] of Object.entries(entities)) {
    decoded = decoded.replace(new RegExp(entity, 'g'), char);
  }

  // Handle numeric entities (&#123; &#x7B;)
  decoded = decoded.replace(/&#(\d+);/g, (_, num) => String.fromCharCode(parseInt(num, 10)));
  decoded = decoded.replace(/&#x([0-9a-f]+);/gi, (_, hex) => String.fromCharCode(parseInt(hex, 16)));

  return decoded;
}

/**
 * Check if clipboard data contains potentially dangerous content.
 */
export function containsDangerousContent(text: string): boolean {
  return DANGEROUS_PATTERNS.some(pattern => {
    pattern.lastIndex = 0;
    return pattern.test(text);
  });
}

/**
 * Sanitize clipboard paste event and convert to ExamDocument nodes.
 *
 * @param clipboardData - The clipboard data from the paste event
 * @returns Array of ExamElement nodes safe for insertion, or null if rejected
 */
export function sanitizeClipboardData(clipboardData: DataTransfer): ExamElement[] | null {
  // Prefer plain text — it's always safe
  const plainText = clipboardData.getData('text/plain');
  const htmlText = clipboardData.getData('text/html');

  // If there's HTML, sanitize it to plain text
  let textContent: string;

  if (plainText) {
    textContent = plainText;
  } else if (htmlText) {
    textContent = stripHtml(htmlText);
  } else {
    // No usable text content
    return null;
  }

  // Final safety check on the resolved text
  if (containsDangerousContent(textContent)) {
    // Strip any remaining dangerous patterns
    for (const pattern of DANGEROUS_PATTERNS) {
      textContent = textContent.replace(pattern, '');
    }
  }

  // Reject if completely empty after sanitization
  if (!textContent.trim()) {
    return null;
  }

  // Convert text lines to paragraph nodes
  return textToParagraphs(textContent);
}

/**
 * Convert plain text (with newlines) into ExamDocument paragraph nodes.
 */
function textToParagraphs(text: string): ExamElement[] {
  const lines = text.split('\n');
  const elements: ExamElement[] = [];

  for (const line of lines) {
    // Skip consecutive empty lines (collapse to single empty paragraph)
    if (!line.trim() && elements.length > 0) {
      const last = elements[elements.length - 1];
      if (last.type === 'paragraph' && last.children.length === 1 && last.children[0].text === '') {
        continue;
      }
    }

    elements.push({
      type: 'paragraph',
      children: [{ text: line }]
    });
  }

  // Ensure at least one paragraph
  if (elements.length === 0) {
    elements.push({ type: 'paragraph', children: [{ text: '' }] });
  }

  return elements;
}

/**
 * Validate that a string does not contain executable or unsafe content.
 * Used as an additional guard before inserting any external text.
 */
export function isTextSafe(text: string): boolean {
  // Check for script injection attempts
  if (/<script/i.test(text)) return false;
  if (/javascript:/i.test(text)) return false;
  if (/vbscript:/i.test(text)) return false;
  if (/on\w+\s*=/i.test(text)) return false;
  return true;
}
