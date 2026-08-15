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

/**
 * Structured document model for the Exam Editor.
 *
 * This defines all allowed node types, marks, and the document schema.
 * The editor persists JSON — never raw HTML.
 *
 * Based on the Slate.js document model but with a restricted, secure schema.
 */

// ─── Text Marks ──────────────────────────────────────────────────────────────

export interface ExamTextMarks {
  bold?: boolean;
  italic?: boolean;
  underline?: boolean;
  superscript?: boolean;
  subscript?: boolean;
  highlight?: string;    // allowed color key, e.g. 'yellow', 'green'
  color?: string;        // allowed color key from limited palette
}

export interface ExamText {
  text: string;
  bold?: boolean;
  italic?: boolean;
  underline?: boolean;
  superscript?: boolean;
  subscript?: boolean;
  highlight?: string;
  color?: string;
}

// ─── Block Node Types ────────────────────────────────────────────────────────

export type BlockType =
  | 'paragraph'
  | 'heading-one'
  | 'heading-two'
  | 'heading-three'
  | 'numbered-list'
  | 'bulleted-list'
  | 'list-item'
  | 'image'
  | 'audio'
  | 'video';

export type TextAlignment = 'left' | 'center' | 'right' | 'justify';

// ─── Element Nodes ───────────────────────────────────────────────────────────

export interface ParagraphElement {
  type: 'paragraph';
  align?: TextAlignment;
  indent?: number; // 0-3
  children: ExamText[];
}

export interface HeadingOneElement {
  type: 'heading-one';
  align?: TextAlignment;
  children: ExamText[];
}

export interface HeadingTwoElement {
  type: 'heading-two';
  align?: TextAlignment;
  children: ExamText[];
}

export interface HeadingThreeElement {
  type: 'heading-three';
  align?: TextAlignment;
  children: ExamText[];
}

export interface NumberedListElement {
  type: 'numbered-list';
  children: ListItemElement[];
}

export interface BulletedListElement {
  type: 'bulleted-list';
  children: ListItemElement[];
}

export interface ListItemElement {
  type: 'list-item';
  indent?: number; // 0-3
  children: ExamText[];
}

export interface ImageElement {
  type: 'image';
  assetId: string;
  alt?: string;
  children: ExamText[]; // Slate requires children; use [{text:''}] for voids
}

export interface AudioElement {
  type: 'audio';
  assetId: string;
  children: ExamText[];
}

export interface VideoElement {
  type: 'video';
  assetId: string;
  children: ExamText[];
}

// ─── Union Types ─────────────────────────────────────────────────────────────

export type ExamElement =
  | ParagraphElement
  | HeadingOneElement
  | HeadingTwoElement
  | HeadingThreeElement
  | NumberedListElement
  | BulletedListElement
  | ListItemElement
  | ImageElement
  | AudioElement
  | VideoElement;

export type ExamNode = ExamElement | ExamText;

// ─── Document Root ───────────────────────────────────────────────────────────

export type ExamDocument = ExamElement[];

// ─── Mark Type Enum ──────────────────────────────────────────────────────────

export type MarkType = 'bold' | 'italic' | 'underline' | 'superscript' | 'subscript' | 'highlight' | 'color';

// ─── Void Elements (media) ───────────────────────────────────────────────────

export const VOID_TYPES: BlockType[] = ['image', 'audio', 'video'];

export const LIST_TYPES: BlockType[] = ['numbered-list', 'bulleted-list'];

export const TEXT_ALIGN_TYPES: TextAlignment[] = ['left', 'center', 'right', 'justify'];

// ─── Allowed Color Palettes ──────────────────────────────────────────────────

export const HIGHLIGHT_COLORS: { key: string; label: string; hex: string }[] = [
  { key: 'yellow', label: 'Yellow', hex: '#fff176' },
  { key: 'green', label: 'Green', hex: '#a5d6a7' },
  { key: 'blue', label: 'Blue', hex: '#90caf9' },
  { key: 'pink', label: 'Pink', hex: '#f48fb1' },
  { key: 'orange', label: 'Orange', hex: '#ffcc80' },
];

export const TEXT_COLORS: { key: string; label: string; hex: string }[] = [
  { key: 'default', label: 'Default', hex: '#212121' },
  { key: 'red', label: 'Red', hex: '#d32f2f' },
  { key: 'blue', label: 'Blue', hex: '#1565c0' },
  { key: 'green', label: 'Green', hex: '#2e7d32' },
  { key: 'purple', label: 'Purple', hex: '#6a1b9a' },
  { key: 'brown', label: 'Brown', hex: '#4e342e' },
];

// ─── Default Document ────────────────────────────────────────────────────────

export const EMPTY_DOCUMENT: ExamDocument = [
  { type: 'paragraph', children: [{ text: '' }] }
];

// ─── Schema Constants ────────────────────────────────────────────────────────

export const SCHEMA_LIMITS = {
  maxNesting: 3,
  maxBlocks: 200,
  maxDocumentSizeBytes: 512_000, // 500 KB
  maxEmbeddedMedia: 20,
  maxIndent: 3,
};
