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
  Component,
  Input,
  Output,
  EventEmitter,
  forwardRef,
  ChangeDetectionStrategy,
  ChangeDetectorRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, FormsModule } from '@angular/forms';
import { QuillModule } from 'ngx-quill';

/**
 * Rich text editor for examination content using Quill.js (via ngx-quill).
 *
 * Usage with reactive forms:
 * ```html
 * <exam-editor formControlName="content" placeholder="Enter question content..."></exam-editor>
 * ```
 *
 * Usage with two-way binding:
 * ```html
 * <exam-editor [value]="htmlContent" (valueChange)="onChanged($event)"></exam-editor>
 * ```
 */
@Component({
  selector: 'exam-editor',
  standalone: true,
  imports: [CommonModule, FormsModule, QuillModule],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ExamEditorComponent),
      multi: true
    }
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './exam-editor.component.html',
  styleUrls: ['./exam-editor.component.scss']
})
export class ExamEditorComponent implements ControlValueAccessor {

  @Input() placeholder = 'Start typing...';
  @Input() disabled = false;

  @Input()
  set value(val: any) {
    if (val !== this.content) {
      this.content = this.normalizeValue(val);
      this.cdr.markForCheck();
    }
  }

  @Output() valueChange = new EventEmitter<string>();

  content: string = '';

  quillModules = {
    toolbar: [
      ['bold', 'italic', 'underline'],
      [{ script: 'sub' }, { script: 'super' }],
      [{ header: [1, 2, 3, false] }],
      [{ list: 'ordered' }, { list: 'bullet' }],
      [{ align: [] }],
      [{ indent: '-1' }, { indent: '+1' }],
      ['link', 'image'],
      ['clean']
    ]
  };

  // ControlValueAccessor callbacks
  private onChange: (value: string) => void = () => {};
  private onTouched: () => void = () => {};

  constructor(private cdr: ChangeDetectorRef) {}

  // ─── ControlValueAccessor ────────────────────────────────────────────────

  writeValue(value: any): void {
    this.content = this.normalizeValue(value);
    this.cdr.markForCheck();
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    this.cdr.markForCheck();
  }

  // ─── Event Handlers ──────────────────────────────────────────────────────

  onContentChanged(event: any): void {
    const html = event.html || '';
    this.content = html;
    this.onChange(html);
    this.valueChange.emit(html);
  }

  onEditorBlur(): void {
    this.onTouched();
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────

  private normalizeValue(val: any): string {
    if (!val) return '';
    if (typeof val === 'string') return val;
    // If old ExamDocument array format is passed, convert to empty string
    if (Array.isArray(val)) return '';
    return '';
  }
}
