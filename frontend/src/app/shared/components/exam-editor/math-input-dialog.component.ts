/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

import {
  Component,
  OnInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  ViewEncapsulation
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { Inject } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import katex from 'katex';
import 'katex/dist/contrib/mhchem.js';

export interface MathInputDialogData {
  /** Pre-fill with existing LaTeX (for edit mode) */
  latex?: string;
  display?: boolean;
}

/**
 * Dialog for authoring a LaTeX math formula with live KaTeX preview.
 * Opened by the MathInlinePlugin toolbar button.
 */
@Component({
  selector: 'exam-math-input-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,
    MatIconModule
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
  template: `
    <h2 mat-dialog-title>
      <mat-icon style="vertical-align:middle;margin-right:6px">functions</mat-icon>
      Insert Math Formula
    </h2>

    <mat-dialog-content>
      <mat-form-field appearance="outline" style="width:100%">
        <mat-label>LaTeX expression</mat-label>
        <textarea
          matInput
          rows="3"
          [(ngModel)]="latex"
          (ngModelChange)="onLatexChange()"
          placeholder="e.g. \\frac{a}{b}, \\sum_{i=1}^{n} x_i, \\ce{H2SO4}"
          style="font-family: monospace; font-size:13px"
        ></textarea>
        <mat-hint>Use $$ ... $$ convention — single \\backslash inside the box</mat-hint>
      </mat-form-field>

      <mat-checkbox [(ngModel)]="display" (ngModelChange)="onLatexChange()" style="margin-top:8px">
        Display block (centred on its own line)
      </mat-checkbox>

      <!-- Live KaTeX preview -->
      <div class="math-preview" [class.math-preview--block]="display">
        <span class="math-preview-label">Preview:</span>
        <span
          *ngIf="previewHtml"
          [innerHTML]="previewHtml"
          class="math-preview-content"
        ></span>
        <span *ngIf="!previewHtml" class="math-preview-empty">
          (type LaTeX above to see preview)
        </span>
        <span *ngIf="renderError" class="math-preview-error">
          <mat-icon style="font-size:14px;vertical-align:middle">warning</mat-icon>
          {{ renderError }}
        </span>
      </div>

      <!-- Quick-insert snippets -->
      <div class="math-snippets">
        <span class="math-snippets-label">Quick insert:</span>
        <button mat-stroked-button *ngFor="let s of SNIPPETS" (click)="insertSnippet(s.latex)" type="button" class="snippet-btn">
          <span [innerHTML]="s.preview"></span>
        </button>
      </div>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button (click)="cancel()" type="button">Cancel</button>
      <button mat-raised-button color="primary" [disabled]="!latex.trim()" (click)="confirm()" type="button">
        Insert
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .math-preview {
      margin: 16px 0 8px;
      padding: 12px 16px;
      border: 1px solid #e0e0e0;
      border-radius: 6px;
      background: #fafafa;
      min-height: 48px;
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 8px;
    }
    .math-preview--block { justify-content: center; }
    .math-preview-label { font-size: 11px; color: #757575; margin-right: 8px; }
    .math-preview-empty { color: #9e9e9e; font-size: 13px; }
    .math-preview-error { color: #d32f2f; font-size: 12px; }
    .math-snippets { margin-top: 8px; display: flex; flex-wrap: wrap; gap: 6px; align-items: center; }
    .math-snippets-label { font-size: 11px; color: #757575; }
    .snippet-btn { font-size: 12px; min-width: 0; padding: 0 8px; height: 28px; line-height: 28px; }
  `]
})
export class MathInputDialogComponent implements OnInit {
  latex = '';
  display = false;
  previewHtml: SafeHtml | null = null;
  renderError = '';

  readonly SNIPPETS = [
    { label: 'Fraction',    latex: '\\frac{a}{b}',           preview: this.renderSnippet('\\frac{a}{b}') },
    { label: 'Sqrt',        latex: '\\sqrt{x}',              preview: this.renderSnippet('\\sqrt{x}') },
    { label: 'Sum',         latex: '\\sum_{i=1}^{n} x_i',    preview: this.renderSnippet('\\sum_{i=1}^{n} x_i') },
    { label: 'Integral',    latex: '\\int_{a}^{b} f(x)\\,dx', preview: this.renderSnippet('\\int_{a}^{b} f(x)\\,dx') },
    { label: 'Matrix',      latex: '\\begin{pmatrix}a & b\\\\c & d\\end{pmatrix}', preview: this.renderSnippet('\\begin{pmatrix}a & b\\\\c & d\\end{pmatrix}') },
    { label: 'Alpha',       latex: '\\alpha',                preview: this.renderSnippet('\\alpha') },
    { label: 'Beta',        latex: '\\beta',                 preview: this.renderSnippet('\\beta') },
    { label: '≠',           latex: '\\neq',                  preview: this.renderSnippet('\\neq') },
    { label: '≤',           latex: '\\leq',                  preview: this.renderSnippet('\\leq') },
    { label: '≥',           latex: '\\geq',                  preview: this.renderSnippet('\\geq') },
    { label: '∞',           latex: '\\infty',                preview: this.renderSnippet('\\infty') },
    { label: 'ce{}',        latex: '\\ce{H2SO4}',            preview: this.renderSnippet('\\ce{H2SO4}') },
  ];

  constructor(
    private dialogRef: MatDialogRef<MathInputDialogComponent>,
    @Inject(MAT_DIALOG_DATA) data: MathInputDialogData,
    private sanitizer: DomSanitizer,
    private cdr: ChangeDetectorRef
  ) {
    this.latex = data?.latex || '';
    this.display = data?.display || false;
  }

  ngOnInit(): void {
    if (this.latex) this.onLatexChange();
  }

  onLatexChange(): void {
    this.renderError = '';
    if (!this.latex.trim()) { this.previewHtml = null; return; }
    try {
      const html = katex.renderToString(this.latex.trim(), {
        throwOnError: true,
        displayMode: this.display,
        output: 'htmlAndMathml',
        trust: false,
        strict: 'ignore'
      });
      this.previewHtml = this.sanitizer.bypassSecurityTrustHtml(html);
    } catch (e: any) {
      this.previewHtml = null;
      this.renderError = e?.message?.replace(/^KaTeX parse error:\s*/i, '') || 'Invalid LaTeX';
    }
    this.cdr.markForCheck();
  }

  insertSnippet(latex: string): void {
    this.latex = (this.latex ? this.latex + ' ' : '') + latex;
    this.onLatexChange();
  }

  cancel(): void { this.dialogRef.close(null); }

  confirm(): void {
    if (!this.latex.trim()) return;
    this.dialogRef.close({ latex: this.latex.trim(), display: this.display });
  }

  private renderSnippet(latex: string): string {
    try {
      return katex.renderToString(latex, { throwOnError: false, output: 'html', trust: false, strict: 'ignore' });
    } catch { return latex; }
  }
}
