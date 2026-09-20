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
import 'katex/dist/contrib/mhchem.mjs';

export interface MathInputDialogData {
  /** Pre-fill with existing LaTeX (for edit mode) */
  latex?: string;
  display?: boolean;
}

export interface SymbolSnippet {
  label: string;
  latex: string;
  preview?: string;
  description?: string;
}

export interface SymbolCategory {
  name: string;
  icon: string;
  snippets: SymbolSnippet[];
}

/**
 * Modernized dialog for authoring LaTeX formulas & equations (Math, Physics, Chemistry)
 * with live KaTeX + mhchem preview.
 *
 * Addresses Issue #143:
 *  - Categorized symbol palette (basic, calculus, algebra, chemistry, greek, etc.)
 *  - Live rendered KaTeX preview with error feedback
 *  - LaTeX syntax validation
 *  - Keyboard shortcuts (Ctrl+Enter to insert, Esc to cancel)
 *  - Display mode toggle ($$...$$ vs $...$)
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
  templateUrl: './math-input-dialog.component.html',
  styleUrls: ['./math-input-dialog.component.scss']
})
export class MathInputDialogComponent implements OnInit {
  @ViewChild('latexInput') latexInputRef?: ElementRef<HTMLTextAreaElement>;

  latex = '';
  display = false;
  isEdit = false;
  previewHtml: SafeHtml = '';
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
    this.display = !!data?.display;
    this.isEdit = !!data?.latex;
  }

  ngOnInit(): void {
    this.initializeCategories();
    this.updatePreview();
  }

  private initializeCategories(): void {
    this.categories = [
      {
        name: 'Basic & Arithmetic',
        icon: 'calculate',
        snippets: [
          { label: 'Fraction', latex: '\\frac{a}{b}', preview: '\\frac{a}{b}' },
          { label: 'Power / Exp', latex: 'x^{2}', preview: 'x^2' },
          { label: 'Subscript', latex: 'x_{i}', preview: 'x_i' },
          { label: 'Square Root', latex: '\\sqrt{x}', preview: '\\sqrt{x}' },
          { label: 'N-th Root', latex: '\\sqrt[n]{x}', preview: '\\sqrt[n]{x}' },
          { label: 'Times', latex: '\\times', preview: '\\times' },
          { label: 'Divide', latex: '\\div', preview: '\\div' },
          { label: 'Plus-Minus', latex: '\\pm', preview: '\\pm' },
          { label: 'Not Equal', latex: '\\neq', preview: '\\neq' },
          { label: 'Approx', latex: '\\approx', preview: '\\approx' },
          { label: 'Less Equal', latex: '\\leq', preview: '\\leq' },
          { label: 'Greater Equal', latex: '\\geq', preview: '\\geq' }
        ]
      },
      {
        name: 'Calculus & Analysis',
        icon: 'trending_up',
        snippets: [
          { label: 'Integral', latex: '\\int_{a}^{b} f(x) \\, dx', preview: '\\int f(x) dx' },
          { label: 'Double Integral', latex: '\\iint_{D} f(x,y) \\, dxdy', preview: '\\iint' },
          { label: 'Contour Integral', latex: '\\oint_{C} f(z) \\, dz', preview: '\\oint' },
          { label: 'Derivative', latex: '\\frac{df}{dx}', preview: '\\frac{df}{dx}' },
          { label: 'Partial Deriv', latex: '\\frac{\\partial f}{\\partial x}', preview: '\\frac{\\partial f}{\\partial x}' },
          { label: 'Limit', latex: '\\lim_{x \\to 0} f(x)', preview: '\\lim_{x \\to 0}' },
          { label: 'Summation', latex: '\\sum_{i=1}^{n} x_i', preview: '\\sum_{i=1}^n' },
          { label: 'Product', latex: '\\prod_{i=1}^{n} x_i', preview: '\\prod' },
          { label: 'Infinity', latex: '\\infty', preview: '\\infty' },
          { label: 'Gradient', latex: '\\nabla f', preview: '\\nabla' }
        ]
      },
      {
        name: 'Linear Algebra & Sets',
        icon: 'grid_view',
        snippets: [
          { label: 'Matrix (2x2)', latex: '\\begin{pmatrix} a & b \\\\ c & d \\end{pmatrix}', preview: '\\begin{pmatrix} a & b \\\\ c & d \\end{pmatrix}' },
          { label: 'Determinant', latex: '\\begin{vmatrix} a & b \\\\ c & d \\end{vmatrix}', preview: '\\begin{vmatrix} a & b \\\\ c & d \\end{vmatrix}' },
          { label: 'Vector', latex: '\\vec{v}', preview: '\\vec{v}' },
          { label: 'Dot Product', latex: '\\vec{a} \\cdot \\vec{b}', preview: '\\vec{a} \\cdot \\vec{b}' },
          { label: 'Cross Product', latex: '\\vec{a} \\times \\vec{b}', preview: '\\vec{a} \\times \\vec{b}' },
          { label: 'Element of', latex: '\\in', preview: '\\in' },
          { label: 'Subset', latex: '\\subset', preview: '\\subset' },
          { label: 'Union', latex: '\\cup', preview: '\\cup' },
          { label: 'Intersection', latex: '\\cap', preview: '\\cap' },
          { label: 'For All', latex: '\\forall', preview: '\\forall' },
          { label: 'Exists', latex: '\\exists', preview: '\\exists' }
        ]
      },
      {
        name: 'Chemistry & Physics',
        icon: 'science',
        snippets: [
          { label: 'Reaction Arrow', latex: '\\ce{A -> B}', preview: '\\ce{A -> B}' },
          { label: 'Equilibrium', latex: '\\ce{A <=> B}', preview: '\\ce{A <=> B}' },
          { label: 'Water Synthesis', latex: '\\ce{2H2 + O2 -> 2H2O}', preview: '\\ce{2H2 + O2 -> 2H2O}' },
          { label: 'Combustion', latex: '\\ce{CH4 + 2O2 -> CO2 + 2H2O}', preview: '\\ce{CH4 + 2O2 -> CO2 + 2H2O}' },
          { label: 'Hydronium', latex: '\\ce{H3O+}', preview: '\\ce{H3O+}' },
          { label: 'Sulfate Ion', latex: '\\ce{SO4^{2-}}', preview: '\\ce{SO4^{2-}}' },
          { label: 'State (aq)', latex: '\\ce{NaCl(aq)}', preview: '\\ce{NaCl(aq)}' },
          { label: 'Delta / Heat', latex: '\\ce{->[\\Delta]}', preview: '\\ce{->[\\Delta]}' },
          { label: 'Physical Unit', latex: '\\pu{9.8 m/s^2}', preview: '\\pu{9.8 m/s^2}' },
          { label: 'Joules / Energy', latex: '\\pu{4.184 J/(g K)}', preview: '\\pu{4.184 J/(g K)}' }
        ]
      },
      {
        name: 'Greek Letters',
        icon: 'language',
        snippets: [
          { label: 'alpha', latex: '\\alpha', preview: '\\alpha' },
          { label: 'beta', latex: '\\beta', preview: '\\beta' },
          { label: 'gamma', latex: '\\gamma', preview: '\\gamma' },
          { label: 'delta', latex: '\\delta', preview: '\\delta' },
          { label: 'Delta', latex: '\\Delta', preview: '\\Delta' },
          { label: 'theta', latex: '\\theta', preview: '\\theta' },
          { label: 'lambda', latex: '\\lambda', preview: '\\lambda' },
          { label: 'Lambda', latex: '\\Lambda', preview: '\\Lambda' },
          { label: 'mu', latex: '\\mu', preview: '\\mu' },
          { label: 'pi', latex: '\\pi', preview: '\\pi' },
          { label: 'sigma', latex: '\\sigma', preview: '\\sigma' },
          { label: 'Sigma', latex: '\\Sigma', preview: '\\Sigma' },
          { label: 'omega', latex: '\\omega', preview: '\\omega' },
          { label: 'Omega', latex: '\\Omega', preview: '\\Omega' }
        ]
      }
    ];
  }

  insertSnippet(snippet: SymbolSnippet): void {
    const input = this.latexInputRef?.nativeElement;
    if (!input) {
      this.latex += (this.latex ? ' ' : '') + snippet.latex;
      this.updatePreview();
      return;
    }

    const start = input.selectionStart ?? this.latex.length;
    const end = input.selectionEnd ?? this.latex.length;
    const before = this.latex.substring(0, start);
    const after = this.latex.substring(end);

    const insertion = snippet.latex;
    this.latex = before + insertion + after;

    const newCursorPos = start + insertion.length;
    setTimeout(() => {
      input.focus();
      input.setSelectionRange(newCursorPos, newCursorPos);
    });

    this.updatePreview();
  }

  onLatexChange(): void {
    this.updatePreview();
  }

  onDisplayToggle(): void {
    this.updatePreview();
  }

  clearLatex(): void {
    this.latex = '';
    this.updatePreview();
    this.latexInputRef?.nativeElement.focus();
  }

  onDialogKeydown(event: KeyboardEvent): void {
    if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
      event.preventDefault();
      this.confirm();
    } else if (event.key === 'Escape') {
      event.preventDefault();
      this.cancel();
    }
  }

  private updatePreview(): void {
    const trimmed = this.latex.trim();
    if (!trimmed) {
      this.previewHtml = '';
      this.renderError = '';
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
      this.renderError = '';
    } catch (e: any) {
      this.renderError = e?.message?.replace(/^KaTeX parse error:\s*/i, '') || 'Invalid LaTeX syntax';
      this.previewHtml = '';
    }
    this.cdr.markForCheck();
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
}
