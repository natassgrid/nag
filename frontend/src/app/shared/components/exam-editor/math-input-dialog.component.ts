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
  ViewEncapsulation,
  ViewChild,
  ElementRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Inject } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import katex from 'katex';
import 'katex/dist/contrib/mhchem.js';

export interface MathInputDialogData {
  /** Pre-fill with existing LaTeX (for edit mode) */
  latex?: string;
  display?: boolean;
}

export interface SymbolSnippet {
  label: string;
  latex: string;
  preview?: SafeHtml;
  tooltip?: string;
}

export interface SymbolCategory {
  name: string;
  icon: string;
  snippets: SymbolSnippet[];
}

/**
 * Modernized dialog for authoring LaTeX math formulas with live KaTeX preview
 * and categorized symbol helper palettes.
 *
 * Addresses Issue #143:
 *  - Categorized symbol helper palettes (Greek letters, operators, fractions, matrices, calculus, chemistry)
 *  - Cursor-aware snippet insertion in LaTeX input textarea
 *  - Live real-time KaTeX preview supporting both inline & display block modes
 *  - Robust syntax error reporting
 *  - Keyboard accessibility (Ctrl+Enter / Enter to insert, Esc to cancel)
 *  - Pre-fill & edit mode support
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
    MatIconModule,
    MatTabsModule,
    MatTooltipModule
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
  template: `
    <div class="math-dialog-container" (keydown)="onDialogKeydown($event)">
      <div mat-dialog-title class="math-dialog-header">
        <div class="header-title">
          <mat-icon class="header-icon">functions</mat-icon>
          <span>{{ isEdit ? 'Edit Math Formula' : 'Insert Math Formula' }}</span>
        </div>
        <button mat-icon-button type="button" (click)="cancel()" class="close-btn" matTooltip="Cancel (Esc)">
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <mat-dialog-content class="math-dialog-content">
        <!-- LaTeX input area -->
        <mat-form-field appearance="outline" class="latex-form-field">
          <mat-label>LaTeX expression</mat-label>
          <textarea
            #latexInput
            matInput
            rows="3"
            [(ngModel)]="latex"
            (ngModelChange)="onLatexChange()"
            (keydown)="onTextareaKeydown($event)"
            placeholder="e.g. \\frac{a}{b}, \\sum_{i=1}^{n} x_i, \\ce{H2SO4}, \\alpha + \\beta"
            class="latex-textarea"
          ></textarea>
          <button
            *ngIf="latex"
            mat-icon-button
            matSuffix
            type="button"
            (click)="clearLatex()"
            matTooltip="Clear input"
            class="clear-btn"
          >
            <mat-icon>backspace</mat-icon>
          </button>
          <mat-hint>Type LaTeX directly or click palette symbols below. Single backslash (\\) inside.</mat-hint>
        </mat-form-field>

        <!-- Display mode option -->
        <div class="display-mode-row">
          <mat-checkbox [(ngModel)]="display" (ngModelChange)="onLatexChange()" color="primary">
            Display block (centered on its own line)
          </mat-checkbox>
        </div>

        <!-- Live KaTeX preview -->
        <div class="math-preview-section">
          <div class="preview-header">
            <span class="preview-title">Live Preview:</span>
            <span class="preview-mode-tag" [class.tag-block]="display">
              {{ display ? 'Display Block Mode' : 'Inline Mode' }}
            </span>
          </div>

          <div
            class="math-preview-box"
            [class.math-preview-box--block]="display"
            [class.math-preview-box--error]="!!renderError"
          >
            <div
              *ngIf="previewHtml && !renderError"
              [innerHTML]="previewHtml"
              class="math-preview-rendered"
            ></div>

            <div *ngIf="!previewHtml && !renderError" class="math-preview-placeholder">
              <mat-icon class="placeholder-icon">visibility</mat-icon>
              <span>Type a LaTeX expression or click symbols from the palette to see preview</span>
            </div>

            <div *ngIf="renderError" class="math-preview-error">
              <mat-icon class="error-icon">error_outline</mat-icon>
              <div class="error-text">
                <strong>KaTeX Syntax Error:</strong>
                <span>{{ renderError }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Categorized Symbol Palette -->
        <div class="palette-container">
          <div class="palette-title">Symbol & Template Palette</div>

          <mat-tab-group animationDuration="0ms" class="palette-tabs" [(selectedIndex)]="selectedTabIndex">
            <mat-tab *ngFor="let cat of categories">
              <ng-template mat-tab-label>
                <mat-icon class="tab-icon">{{ cat.icon }}</mat-icon>
                <span>{{ cat.name }}</span>
              </ng-template>

              <div class="palette-grid">
                <button
                  *ngFor="let item of cat.snippets"
                  type="button"
                  class="palette-btn"
                  [matTooltip]="item.tooltip || item.latex"
                  matTooltipPosition="above"
                  (click)="insertSnippet(item.latex)"
                >
                  <span *ngIf="item.preview" [innerHTML]="item.preview" class="btn-preview"></span>
                  <span *ngIf="!item.preview" class="btn-label">{{ item.label }}</span>
                </button>
              </div>
            </mat-tab>
          </mat-tab-group>
        </div>
      </mat-dialog-content>

      <mat-dialog-actions align="end" class="math-dialog-actions">
        <button mat-button (click)="cancel()" type="button">Cancel</button>
        <button
          mat-raised-button
          color="primary"
          [disabled]="!latex.trim() || !!renderError"
          (click)="confirm()"
          type="button"
          class="confirm-btn"
        >
          <mat-icon>{{ isEdit ? 'save' : 'add' }}</mat-icon>
          <span>{{ isEdit ? 'Update Formula' : 'Insert Formula' }}</span>
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [`
    .math-dialog-container {
      display: flex;
      flex-direction: column;
      max-height: 90vh;
      box-sizing: border-box;
    }
    .math-dialog-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 12px 24px 8px;
      margin: 0;
      border-bottom: 1px solid #e0e0e0;
    }
    .header-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 18px;
      font-weight: 600;
      color: #1a237e;
    }
    .header-icon {
      color: #3f51b5;
      font-size: 24px;
      width: 24px;
      height: 24px;
    }
    .close-btn {
      color: #757575;
    }
    .math-dialog-content {
      padding: 16px 24px !important;
      overflow-y: auto;
      max-height: 65vh;
    }
    .latex-form-field {
      width: 100%;
      margin-bottom: 4px;
    }
    .latex-textarea {
      font-family: 'JetBrains Mono', 'Fira Code', 'Courier New', monospace !important;
      font-size: 13.5px !important;
      line-height: 1.5;
    }
    .clear-btn {
      color: #9e9e9e;
    }
    .display-mode-row {
      margin: 2px 0 12px;
      display: flex;
      align-items: center;
    }
    .math-preview-section {
      margin-bottom: 16px;
    }
    .preview-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 6px;
    }
    .preview-title {
      font-size: 12px;
      font-weight: 600;
      color: #424242;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .preview-mode-tag {
      font-size: 11px;
      padding: 2px 8px;
      border-radius: 12px;
      background: #e8f5e9;
      color: #2e7d32;
      font-weight: 500;
    }
    .preview-mode-tag.tag-block {
      background: #e3f2fd;
      color: #1565c0;
    }
    .math-preview-box {
      min-height: 64px;
      padding: 14px 18px;
      border: 1px solid #cfd8dc;
      border-radius: 8px;
      background: #fdfdfd;
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      box-shadow: inset 0 1px 3px rgba(0,0,0,0.03);
      transition: all 0.2s ease;
    }
    .math-preview-box--block {
      justify-content: center;
      background: #f8faff;
      border-color: #bbdefb;
    }
    .math-preview-box--error {
      border-color: #ef9a9a;
      background: #fff8f8;
    }
    .math-preview-rendered {
      font-size: 16px;
      color: #212121;
      max-width: 100%;
      overflow-x: auto;
    }
    .math-preview-placeholder {
      display: flex;
      align-items: center;
      gap: 8px;
      color: #9e9e9e;
      font-size: 13px;
      font-style: italic;
    }
    .placeholder-icon {
      font-size: 18px;
      width: 18px;
      height: 18px;
      color: #bdbdbd;
    }
    .math-preview-error {
      display: flex;
      align-items: flex-start;
      gap: 8px;
      color: #c62828;
      font-size: 12.5px;
    }
    .error-icon {
      font-size: 18px;
      width: 18px;
      height: 18px;
      color: #d32f2f;
      flex-shrink: 0;
      margin-top: 1px;
    }
    .error-text {
      display: flex;
      flex-direction: column;
      gap: 2px;
    }
    .palette-container {
      margin-top: 8px;
      border: 1px solid #e0e0e0;
      border-radius: 8px;
      overflow: hidden;
      background: #fafafa;
    }
    .palette-title {
      font-size: 12px;
      font-weight: 600;
      color: #616161;
      padding: 8px 14px;
      background: #f5f5f5;
      border-bottom: 1px solid #e0e0e0;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .palette-tabs .mat-mdc-tab-header {
      background: #fff;
    }
    .tab-icon {
      font-size: 16px;
      width: 16px;
      height: 16px;
      margin-right: 4px;
      vertical-align: middle;
    }
    .palette-grid {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      padding: 10px;
      max-height: 160px;
      overflow-y: auto;
      background: #fff;
    }
    .palette-btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-width: 36px;
      height: 34px;
      padding: 4px 8px;
      background: #f8f9fa;
      border: 1px solid #e2e8f0;
      border-radius: 6px;
      cursor: pointer;
      font-size: 13px;
      color: #334155;
      transition: all 0.15s ease;
      user-select: none;
    }
    .palette-btn:hover {
      background: #e0f2fe;
      border-color: #7dd3fc;
      color: #0369a1;
      transform: translateY(-1px);
      box-shadow: 0 2px 4px rgba(0,0,0,0.05);
    }
    .palette-btn:active {
      transform: translateY(0);
    }
    .btn-preview {
      display: inline-flex;
      align-items: center;
    }
    .math-dialog-actions {
      padding: 12px 24px;
      border-top: 1px solid #e0e0e0;
      margin: 0;
      gap: 8px;
    }
    .confirm-btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
    }
  `]
})
export class MathInputDialogComponent implements OnInit {
  @ViewChild('latexInput') latexInputRef?: ElementRef<HTMLTextAreaElement>;

  latex = '';
  display = false;
  isEdit = false;
  previewHtml: SafeHtml | null = null;
  renderError = '';
  selectedTabIndex = 0;

  categories: SymbolCategory[] = [];

  constructor(
    private dialogRef: MatDialogRef<MathInputDialogComponent>,
    @Inject(MAT_DIALOG_DATA) data: MathInputDialogData,
    private sanitizer: DomSanitizer,
    private cdr: ChangeDetectorRef
  ) {
    this.latex = data?.latex || '';
    this.display = data?.display || false;
    this.isEdit = !!data?.latex;
  }

  ngOnInit(): void {
    this.initializePaletteCategories();
    if (this.latex) {
      this.onLatexChange();
    }
  }

  private initializePaletteCategories(): void {
    this.categories = [
      {
        name: 'Basic & Algebra',
        icon: 'calculate',
        snippets: [
          { label: 'Fraction', latex: '\\frac{a}{b}', tooltip: 'Fraction: \\frac{a}{b}' },
          { label: 'Sqrt', latex: '\\sqrt{x}', tooltip: 'Square Root: \\sqrt{x}' },
          { label: 'Nth Root', latex: '\\sqrt[n]{x}', tooltip: 'N-th Root: \\sqrt[n]{x}' },
          { label: 'Power', latex: 'x^{n}', tooltip: 'Superscript: x^{n}' },
          { label: 'Subscript', latex: 'x_{n}', tooltip: 'Subscript: x_{n}' },
          { label: 'Sub+Power', latex: 'x_{i}^{n}', tooltip: 'Sub & Super: x_{i}^{n}' },
          { label: '±', latex: '\\pm', tooltip: 'Plus-Minus: \\pm' },
          { label: '×', latex: '\\times', tooltip: 'Times: \\times' },
          { label: '÷', latex: '\\div', tooltip: 'Divide: \\div' },
          { label: '·', latex: '\\cdot', tooltip: 'Dot: \\cdot' },
          { label: '≠', latex: '\\neq', tooltip: 'Not Equal: \\neq' },
          { label: '≈', latex: '\\approx', tooltip: 'Approximately: \\approx' },
          { label: '≤', latex: '\\leq', tooltip: 'Less Equal: \\leq' },
          { label: '≥', latex: '\\geq', tooltip: 'Greater Equal: \\geq' },
          { label: '∞', latex: '\\infty', tooltip: 'Infinity: \\infty' },
          { label: '%', latex: '\\%', tooltip: 'Percent: \\%' },
          { label: '°', latex: '^{\\circ}', tooltip: 'Degree: ^{\\circ}' },
        ]
      },
      {
        name: 'Greek Letters',
        icon: 'translate',
        snippets: [
          { label: 'α', latex: '\\alpha', tooltip: 'Alpha: \\alpha' },
          { label: 'β', latex: '\\beta', tooltip: 'Beta: \\beta' },
          { label: 'γ', latex: '\\gamma', tooltip: 'Gamma: \\gamma' },
          { label: 'δ', latex: '\\delta', tooltip: 'Delta: \\delta' },
          { label: 'ε', latex: '\\epsilon', tooltip: 'Epsilon: \\epsilon' },
          { label: 'θ', latex: '\\theta', tooltip: 'Theta: \\theta' },
          { label: 'λ', latex: '\\lambda', tooltip: 'Lambda: \\lambda' },
          { label: 'μ', latex: '\\mu', tooltip: 'Mu: \\mu' },
          { label: 'π', latex: '\\pi', tooltip: 'Pi: \\pi' },
          { label: 'σ', latex: '\\sigma', tooltip: 'Sigma: \\sigma' },
          { label: 'τ', latex: '\\tau', tooltip: 'Tau: \\tau' },
          { label: 'φ', latex: '\\phi', tooltip: 'Phi: \\phi' },
          { label: 'ω', latex: '\\omega', tooltip: 'Omega: \\omega' },
          { label: 'Δ', latex: '\\Delta', tooltip: 'Capital Delta: \\Delta' },
          { label: 'Γ', latex: '\\Gamma', tooltip: 'Capital Gamma: \\Gamma' },
          { label: 'Θ', latex: '\\Theta', tooltip: 'Capital Theta: \\Theta' },
          { label: 'Λ', latex: '\\Lambda', tooltip: 'Capital Lambda: \\Lambda' },
          { label: 'Σ', latex: '\\Sigma', tooltip: 'Capital Sigma: \\Sigma' },
          { label: 'Ω', latex: '\\Omega', tooltip: 'Capital Omega: \\Omega' }
        ]
      },
      {
        name: 'Operators & Logic',
        icon: 'all_inclusive',
        snippets: [
          { label: '∈', latex: '\\in', tooltip: 'In: \\in' },
          { label: '∉', latex: '\\notin', tooltip: 'Not in: \\notin' },
          { label: '⊂', latex: '\\subset', tooltip: 'Subset: \\subset' },
          { label: '⊆', latex: '\\subseteq', tooltip: 'Subset Equal: \\subseteq' },
          { label: '∪', latex: '\\cup', tooltip: 'Union: \\cup' },
          { label: '∩', latex: '\\cap', tooltip: 'Intersection: \\cap' },
          { label: '→', latex: '\\to', tooltip: 'Right Arrow: \\to' },
          { label: '←', latex: '\\leftarrow', tooltip: 'Left Arrow: \\leftarrow' },
          { label: '⇒', latex: '\\Rightarrow', tooltip: 'Implies: \\Rightarrow' },
          { label: '⇔', latex: '\\Leftrightarrow', tooltip: 'If and only if: \\Leftrightarrow' },
          { label: '∀', latex: '\\forall', tooltip: 'For all: \\forall' },
          { label: '∃', latex: '\\exists', tooltip: 'Exists: \\exists' },
          { label: '¬', latex: '\\neg', tooltip: 'Negation: \\neg' },
          { label: '∧', latex: '\\land', tooltip: 'Logical AND: \\land' },
          { label: '∨', latex: '\\lor', tooltip: 'Logical OR: \\lor' },
          { label: '∇', latex: '\\nabla', tooltip: 'Nabla: \\nabla' },
          { label: '∂', latex: '\\partial', tooltip: 'Partial: \\partial' }
        ]
      },
      {
        name: 'Calculus & Sums',
        icon: 'show_chart',
        snippets: [
          { label: 'Integral', latex: '\\int_{a}^{b} f(x)\\,dx', tooltip: 'Definite Integral: \\int_{a}^{b} f(x)\\,dx' },
          { label: 'Indef Integral', latex: '\\int f(x)\\,dx', tooltip: 'Indefinite Integral: \\int f(x)\\,dx' },
          { label: 'Double Integral', latex: '\\iint f(x,y)\\,dx\\,dy', tooltip: 'Double Integral: \\iint f(x,y)\\,dx\\,dy' },
          { label: 'Contour Integral', latex: '\\oint \\vec{F}\\cdot d\\vec{r}', tooltip: 'Contour: \\oint \\vec{F}\\cdot d\\vec{r}' },
          { label: 'Sum', latex: '\\sum_{i=1}^{n} x_i', tooltip: 'Summation: \\sum_{i=1}^{n} x_i' },
          { label: 'Product', latex: '\\prod_{i=1}^{n} x_i', tooltip: 'Product: \\prod_{i=1}^{n} x_i' },
          { label: 'Limit', latex: '\\lim_{x \\to 0}', tooltip: 'Limit: \\lim_{x \\to 0}' },
          { label: 'Limit Inf', latex: '\\lim_{n \\to \\infty}', tooltip: 'Limit to Inf: \\lim_{n \\to \\infty}' },
          { label: 'df/dx', latex: '\\frac{df}{dx}', tooltip: 'Derivative: \\frac{df}{dx}' },
          { label: 'd²f/dx²', latex: '\\frac{d^2 f}{dx^2}', tooltip: 'Second Derivative: \\frac{d^2 f}{dx^2}' },
          { label: '∂f/∂x', latex: '\\frac{\\partial f}{\\partial x}', tooltip: 'Partial Derivative: \\frac{\\partial f}{\\partial x}' }
        ]
      },
      {
        name: 'Matrices & Brackets',
        icon: 'grid_view',
        snippets: [
          { label: 'pmatrix (2x2)', latex: '\\begin{pmatrix} a & b \\\\ c & d \\end{pmatrix}', tooltip: 'Matrix (): \\begin{pmatrix} a & b \\\\ c & d \\end{pmatrix}' },
          { label: 'bmatrix (2x2)', latex: '\\begin{bmatrix} a & b \\\\ c & d \\end{bmatrix}', tooltip: 'Matrix []: \\begin{bmatrix} a & b \\\\ c & d \\end{bmatrix}' },
          { label: 'vmatrix (Det)', latex: '\\begin{vmatrix} a & b \\\\ c & d \\end{vmatrix}', tooltip: 'Determinant: \\begin{vmatrix} a & b \\\\ c & d \\end{vmatrix}' },
          { label: 'Cases / Piecewise', latex: '\\begin{cases} x & x \\ge 0 \\\\ -x & x < 0 \\end{cases}', tooltip: 'Cases: \\begin{cases} ... \\end{cases}' },
          { label: 'Adaptive ()', latex: '\\left( \\frac{a}{b} \\right)', tooltip: 'Adaptive (): \\left( \\frac{a}{b} \\right)' },
          { label: 'Adaptive []', latex: '\\left[ \\frac{a}{b} \\right]', tooltip: 'Adaptive []: \\left[ \\frac{a}{b} \\right]' },
          { label: 'Adaptive {}', latex: '\\left\\{ x \\mid x > 0 \\right\\}', tooltip: 'Adaptive {}: \\left\\{ ... \\right\\}' },
          { label: 'Binomial', latex: '\\binom{n}{k}', tooltip: 'Binomial: \\binom{n}{k}' }
        ]
      },
      {
        name: 'Chemistry & Units',
        icon: 'science',
        snippets: [
          { label: 'H2O', latex: '\\ce{H2O}', tooltip: 'Water: \\ce{H2O}' },
          { label: 'H2SO4', latex: '\\ce{H2SO4}', tooltip: 'Sulfuric acid: \\ce{H2SO4}' },
          { label: 'Reaction', latex: '\\ce{2H2 + O2 -> 2H2O}', tooltip: 'Reaction: \\ce{2H2 + O2 -> 2H2O}' },
          { label: 'Combustion', latex: '\\ce{CH4 + 2O2 -> CO2 + 2H2O}', tooltip: 'Combustion: \\ce{CH4 + 2O2 -> CO2 + 2H2O}' },
          { label: 'Precipitate', latex: '\\ce{Ag+ + Cl- -> AgCl v}', tooltip: 'Precipitation: \\ce{Ag+ + Cl- -> AgCl v}' },
          { label: 'Isotope', latex: '\\ce{^{235}_{92}U}', tooltip: 'Nuclear Isotope: \\ce{^{235}_{92}U}' },
          { label: '9.8 m/s²', latex: '\\pu{9.8 m/s^2}', tooltip: 'Physical Unit: \\pu{9.8 m/s^2}' },
          { label: 'Avogadro', latex: '\\pu{6.022e23 mol^{-1}}', tooltip: 'Avogadro Constant: \\pu{6.022e23 mol^{-1}}' },
          { label: '100 kJ/mol', latex: '\\pu{100 kJ/mol}', tooltip: 'Energy Unit: \\pu{100 kJ/mol}' },
          { label: '25 °C', latex: '\\pu{25 ^{\\circ}C}', tooltip: 'Temperature: \\pu{25 ^{\\circ}C}' }
        ]
      }
    ];

    // Pre-render KaTeX previews for all snippets
    for (const cat of this.categories) {
      for (const item of cat.snippets) {
        item.preview = this.renderSnippet(item.latex);
      }
    }
  }

  onLatexChange(): void {
    this.renderError = '';
    const trimmed = this.latex.trim();
    if (!trimmed) {
      this.previewHtml = null;
      this.cdr.markForCheck();
      return;
    }

    try {
      const html = katex.renderToString(trimmed, {
        throwOnError: true,
        displayMode: this.display,
        output: 'htmlAndMathml',
        trust: false,
        strict: 'ignore'
      });
      this.previewHtml = this.sanitizer.bypassSecurityTrustHtml(html);
    } catch (e: any) {
      this.previewHtml = null;
      this.renderError = e?.message?.replace(/^KaTeX parse error:\s*/i, '') || 'Invalid LaTeX syntax';
    }
    this.cdr.markForCheck();
  }

  insertSnippet(snippetLatex: string): void {
    const textarea = this.latexInputRef?.nativeElement;
    if (!textarea) {
      this.latex = (this.latex ? this.latex + ' ' : '') + snippetLatex;
      this.onLatexChange();
      return;
    }

    const start = textarea.selectionStart ?? this.latex.length;
    const end = textarea.selectionEnd ?? this.latex.length;
    const before = this.latex.substring(0, start);
    const after = this.latex.substring(end);
    
    // Add spaces where appropriate
    const needsLeadingSpace = start > 0 && before[start - 1] !== ' ' && before[start - 1] !== '{' && before[start - 1] !== '(';
    const insertStr = (needsLeadingSpace ? ' ' : '') + snippetLatex;
    
    this.latex = before + insertStr + after;
    this.onLatexChange();

    setTimeout(() => {
      textarea.focus();
      const newPos = start + insertStr.length;
      textarea.setSelectionRange(newPos, newPos);
    }, 0);
  }

  clearLatex(): void {
    this.latex = '';
    this.onLatexChange();
    this.latexInputRef?.nativeElement.focus();
  }

  onTextareaKeydown(event: KeyboardEvent): void {
    if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
      event.preventDefault();
      this.confirm();
    }
  }

  onDialogKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      event.preventDefault();
      this.cancel();
    }
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  confirm(): void {
    const trimmed = this.latex.trim();
    if (!trimmed || this.renderError) return;
    this.dialogRef.close({
      latex: trimmed,
      display: this.display
    });
  }

  private renderSnippet(latex: string): SafeHtml {
    try {
      const html = katex.renderToString(latex, {
        throwOnError: false,
        output: 'html',
        trust: false,
        strict: 'ignore'
      });
      return this.sanitizer.bypassSecurityTrustHtml(html);
    } catch {
      return this.sanitizer.bypassSecurityTrustHtml(latex);
    }
  }
}
