import {
  ExamDocument,
  ExamElement,
  ExamText,
  BlockType,
  VOID_TYPES,
  LIST_TYPES,
  SCHEMA_LIMITS,
  HIGHLIGHT_COLORS,
  TEXT_COLORS,
  MarkType
} from '../models';

/**
 * Validation result for a document.
 */
export interface ValidationResult {
  valid: boolean;
  errors: ValidationError[];
}

export interface ValidationError {
  path: number[];
  message: string;
  severity: 'error' | 'warning';
}

/**
 * Allowed block types in the schema.
 */
const ALLOWED_BLOCK_TYPES: Set<string> = new Set<BlockType>([
  'paragraph', 'heading-one', 'heading-two', 'heading-three',
  'numbered-list', 'bulleted-list', 'list-item',
  'image', 'audio', 'video'
]);

/**
 * Allowed mark keys.
 */
const ALLOWED_MARKS: Set<string> = new Set<MarkType>([
  'bold', 'italic', 'underline', 'superscript', 'subscript', 'highlight', 'color'
]);

/**
 * Allowed highlight color keys.
 */
const ALLOWED_HIGHLIGHT_KEYS = new Set(HIGHLIGHT_COLORS.map(c => c.key));

/**
 * Allowed text color keys.
 */
const ALLOWED_COLOR_KEYS = new Set(TEXT_COLORS.map(c => c.key));

/**
 * Validate an ExamDocument against the schema.
 *
 * Checks:
 * - Allowed node types
 * - Allowed marks and their values
 * - Maximum nesting depth
 * - Maximum block count
 * - Maximum document size (JSON bytes)
 * - Maximum embedded media count
 * - No unknown attributes
 */
export function validateDocument(document: ExamDocument): ValidationResult {
  const errors: ValidationError[] = [];
  let blockCount = 0;
  let mediaCount = 0;

  // Check document size
  const jsonSize = new TextEncoder().encode(JSON.stringify(document)).length;
  if (jsonSize > SCHEMA_LIMITS.maxDocumentSizeBytes) {
    errors.push({
      path: [],
      message: `Document size (${(jsonSize / 1024).toFixed(1)} KB) exceeds maximum (${(SCHEMA_LIMITS.maxDocumentSizeBytes / 1024).toFixed(0)} KB)`,
      severity: 'error'
    });
  }

  // Validate each top-level element
  for (let i = 0; i < document.length; i++) {
    validateElement(document[i], [i], 0, errors);
    blockCount += countBlocks(document[i]);
    mediaCount += countMedia(document[i]);
  }

  // Check block count
  if (blockCount > SCHEMA_LIMITS.maxBlocks) {
    errors.push({
      path: [],
      message: `Block count (${blockCount}) exceeds maximum (${SCHEMA_LIMITS.maxBlocks})`,
      severity: 'error'
    });
  }

  // Check media count
  if (mediaCount > SCHEMA_LIMITS.maxEmbeddedMedia) {
    errors.push({
      path: [],
      message: `Embedded media count (${mediaCount}) exceeds maximum (${SCHEMA_LIMITS.maxEmbeddedMedia})`,
      severity: 'error'
    });
  }

  return {
    valid: errors.filter(e => e.severity === 'error').length === 0,
    errors
  };
}

function validateElement(element: ExamElement, path: number[], depth: number, errors: ValidationError[]): void {
  // Check nesting depth
  if (depth > SCHEMA_LIMITS.maxNesting) {
    errors.push({
      path,
      message: `Nesting depth (${depth}) exceeds maximum (${SCHEMA_LIMITS.maxNesting})`,
      severity: 'error'
    });
    return;
  }

  // Check node type
  if (!element.type || !ALLOWED_BLOCK_TYPES.has(element.type)) {
    errors.push({
      path,
      message: `Unknown node type: '${element.type}'`,
      severity: 'error'
    });
    return;
  }

  // Check allowed attributes per type
  validateElementAttributes(element, path, errors);

  // Validate children
  if (!element.children || !Array.isArray(element.children)) {
    errors.push({
      path,
      message: `Element '${element.type}' missing children array`,
      severity: 'error'
    });
    return;
  }

  // Void elements should only have [{ text: '' }]
  if (VOID_TYPES.includes(element.type)) {
    if (element.children.length !== 1 || (element.children[0] as ExamText).text !== '') {
      errors.push({
        path,
        message: `Void element '${element.type}' should have children: [{ text: '' }]`,
        severity: 'warning'
      });
    }
    return;
  }

  // List elements should contain list-items
  if (LIST_TYPES.includes(element.type)) {
    for (let i = 0; i < element.children.length; i++) {
      const child = element.children[i] as ExamElement;
      if (child.type !== 'list-item') {
        errors.push({
          path: [...path, i],
          message: `List element should only contain list-item children, found '${child.type || 'text'}'`,
          severity: 'error'
        });
      } else {
        validateElement(child, [...path, i], depth + 1, errors);
      }
    }
    return;
  }

  // Validate text children
  for (let i = 0; i < element.children.length; i++) {
    const child = element.children[i];
    if (isTextNode(child)) {
      validateTextNode(child as ExamText, [...path, i], errors);
    } else {
      // Nested elements (shouldn't happen outside lists, but validate anyway)
      validateElement(child as ExamElement, [...path, i], depth + 1, errors);
    }
  }
}

function validateElementAttributes(element: ExamElement, path: number[], errors: ValidationError[]): void {
  const allowed = getAllowedAttributes(element.type);
  const actual = Object.keys(element).filter(k => k !== 'type' && k !== 'children');

  for (const attr of actual) {
    if (!allowed.has(attr)) {
      errors.push({
        path,
        message: `Unknown attribute '${attr}' on '${element.type}'`,
        severity: 'error'
      });
    }
  }

  // Validate indent range
  if ('indent' in element) {
    const indent = (element as any).indent;
    if (typeof indent === 'number' && (indent < 0 || indent > SCHEMA_LIMITS.maxIndent)) {
      errors.push({
        path,
        message: `Indent (${indent}) out of range [0, ${SCHEMA_LIMITS.maxIndent}]`,
        severity: 'error'
      });
    }
  }

  // Validate media assetId
  if (VOID_TYPES.includes(element.type)) {
    if (!('assetId' in element) || !(element as any).assetId) {
      errors.push({
        path,
        message: `Media element '${element.type}' requires assetId`,
        severity: 'error'
      });
    }
  }
}

function validateTextNode(text: ExamText, path: number[], errors: ValidationError[]): void {
  if (typeof text.text !== 'string') {
    errors.push({
      path,
      message: `Text node must have a 'text' string property`,
      severity: 'error'
    });
    return;
  }

  // Check for unknown marks
  const knownTextProps = new Set(['text', ...ALLOWED_MARKS]);
  for (const key of Object.keys(text)) {
    if (!knownTextProps.has(key)) {
      errors.push({
        path,
        message: `Unknown mark '${key}' on text node`,
        severity: 'error'
      });
    }
  }

  // Validate highlight value
  if (text.highlight && !ALLOWED_HIGHLIGHT_KEYS.has(text.highlight)) {
    errors.push({
      path,
      message: `Invalid highlight color '${text.highlight}'`,
      severity: 'error'
    });
  }

  // Validate text color value
  if (text.color && !ALLOWED_COLOR_KEYS.has(text.color)) {
    errors.push({
      path,
      message: `Invalid text color '${text.color}'`,
      severity: 'error'
    });
  }

  // Mutually exclusive: superscript/subscript
  if (text.superscript && text.subscript) {
    errors.push({
      path,
      message: `Text cannot be both superscript and subscript`,
      severity: 'warning'
    });
  }
}

function getAllowedAttributes(type: BlockType): Set<string> {
  switch (type) {
    case 'paragraph':
      return new Set(['align', 'indent']);
    case 'heading-one':
    case 'heading-two':
    case 'heading-three':
      return new Set(['align']);
    case 'list-item':
      return new Set(['indent']);
    case 'numbered-list':
    case 'bulleted-list':
      return new Set([]);
    case 'image':
      return new Set(['assetId', 'alt']);
    case 'audio':
    case 'video':
      return new Set(['assetId']);
    default:
      return new Set([]);
  }
}

function isTextNode(node: any): boolean {
  return typeof node === 'object' && 'text' in node && !('type' in node);
}

function countBlocks(element: ExamElement): number {
  let count = 1;
  if (element.children) {
    for (const child of element.children) {
      if ('type' in child) {
        count += countBlocks(child as ExamElement);
      }
    }
  }
  return count;
}

function countMedia(element: ExamElement): number {
  let count = VOID_TYPES.includes(element.type) ? 1 : 0;
  if (element.children) {
    for (const child of element.children) {
      if ('type' in child) {
        count += countMedia(child as ExamElement);
      }
    }
  }
  return count;
}
