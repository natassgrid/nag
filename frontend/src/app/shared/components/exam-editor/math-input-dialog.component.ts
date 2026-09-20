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
  OnInit,
  ChangeDetectionStrategy,
  ViewEncapsulation,
  ChangeDetectorRef,
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
import { sanitizeLatex } from './utils/serializer';

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
 * Math & Chemical Formula Insert / Edit Dialog
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
    this.latex = sanitizeLatex(data?.latex || '');
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
          { label: 'Summation', latex: '\\sum_{i=1}^{n} x_i', preview: '\\sum x_i' },
          { label: 'Product', latex: '\\prod_{i=1}^{n} x_i', preview: '\\prod x_i' },
          { label: 'Infinity', latex: '\\infty', preview: '\\infty' }
        ]
      },
      {
        name: 'Chemistry (\u005cce)',
        icon: 'science',
        snippets: [
          { label: 'Reaction Arrow', latex: '\\ce{A -> B}', preview: '\\ce{A -> B}' },
          { label: 'Reversible Arrow', latex: '\\ce{A <=> B}', preview: '\\ce{A <=> B}' },
          { label: 'Water Formation', latex: '\\ce{2H2 + O2 -> 2H2O}', preview: '\\ce{2H2 + O2 -> 2H2O}' },
          { label: 'Combustion', latex: '\\ce{CH4 + 2O2 -> CO2 + 2H2O}', preview: '\\ce{CH4 + 2O2 -> CO2 + 2H2O}' },
          { label: 'Precipitate (v)', latex: '\\ce{Ag+ + Cl- -> AgCl v}', preview: '\\ce{AgCl v}' },
          { label: 'Gas Evolution (^)', latex: '\\ce{Zn + 2HCl -> ZnCl2 + H2 ^}', preview: '\\ce{H2 ^}' },
          { label: 'Charges/Ions', latex: '\\ce{SO4^2- + Ba^2+ -> BaSO4}', preview: '\\ce{SO4^2-}' },
          { label: 'Hydrate', latex: '\\ce{CuSO4 . 5H2O}', preview: '\\ce{CuSO4 . 5H2O}' },
          { label: 'State Symbols', latex: '\\ce{NaCl(aq) + AgNO3(aq) -> AgCl(s) + NaNO3(aq)}', preview: '\\ce{NaCl(aq)}' },
          { label: 'Physical Unit', latex: '\\pu{9.8 m/s^2}', preview: '\\pu{9.8 m/s^2}' }
        ]
      },
      {
        name: 'Algebra & Matrices',
        icon: 'grid_on',
        snippets: [
          {
            label: '2x2 Matrix',
            latex: '\\begin{pmatrix} a & b \\\\ c & d \\end{pmatrix}',
            preview: '\\begin{pmatrix} a & b \\\\ c & d \\end{pmatrix}'
          },
          {
            label: '3x3 Matrix',
            latex: '\\begin{pmatrix} a & b & c \\\\ d & e & f \\\\ g & h & i \\end{pmatrix}',
            preview: '\\begin{pmatrix} 1 & 0 \\\\ 0 & 1 \\end{pmatrix}'
          },
          {
            label: 'Determinant',
            latex: '\\begin{vmatrix} a & b \\\\ c & d \\end{vmatrix}',
            preview: '\\begin{vmatrix} a & b \\\\ c & d \\end{vmatrix}'
          },
          {
            label: 'Cases / Piecewise',
            latex: 'f(x) = \\begin{cases} x^2 & x \\geq 0 \\\\ -x & x < 0 \\end{cases}',
            preview: '\\begin{cases} a \\\\ b \\end{cases}'
          },
          { label: 'Binomial Coeff', latex: '\\binom{n}{k}', preview: '\\binom{n}{k}' }
        ]
      },
      {
        name: 'Greek Letters',
        icon: 'translate',
        snippets: [
          { label: 'alpha (\u03b1)', latex: '\\alpha', preview: '\\alpha' },
          { label: 'beta (\u03b2)', latex: '\\beta', preview: '\\beta' },
          { label: 'gamma (\u03b3)', latex: '\\gamma', preview: '\\gamma' },
          { label: 'delta (\u03b4)', latex: '\\delta', preview: '\\delta' },
          { label: 'Delta (\u0394)', latex: '\\Delta', preview: '\\Delta' },
          { label: 'theta (\u03b8)', latex: '\\theta', preview: '\\theta' },
          { label: 'lambda (\u03bb)', latex: '\\lambda', preview: '\\lambda' },
          { label: 'mu (\u03bc)', latex: '\\mu', preview: '\\mu' },
          { label: 'pi (\u03c0)', latex: '\\pi', preview: '\\pi' },
          { label: 'sigma (\u03c3)', latex: '\\sigma', preview: '\\sigma' },
          { label: 'Sigma (\u03a3)', latex: '\\Sigma', preview: '\\Sigma' },
          { label: 'omega (\u03c9)', latex: '\\omega', preview: '\\omega' },
          { label: 'Omega (\u03a9)', latex: '\\Omega', preview: '\\Omega' },
          { label: 'phi (\u03c6)', latex: '\\phi', preview: '\\phi' },
          { label: 'rho (\u03c1)', latex: '\\rho', preview: '\\rho' }
        ]
      },
      {
        name: 'Sets & Logic',
        icon: 'hub',
        snippets: [
          { label: 'Element Of', latex: '\\in', preview: '\\in' },
          { label: 'Not In', latex: '\\notin', preview: '\\notin' },
          { label: 'Subset', latex: '\\subset', preview: '\\subset' },
          { label: 'Subset Eq', latex: '\\subseteq', preview: '\\subseteq' },
          { label: 'Union', latex: '\\cup', preview: '\\cup' },
          { label: 'Intersection', latex: '\\cap', preview: '\\cap' },
          { label: 'Empty Set', latex: '\\emptyset', preview: '\\emptyset' },
          { label: 'For All', latex: '\\forall', preview: '\\forall' },
          { label: 'Exists', latex: '\\exists', preview: '\\exists' },
          { label: 'Therefore', latex: '\\therefore', preview: '\\therefore' },
          { label: 'Because', latex: '\\because', preview: '\\because' },
          { label: 'Implies', latex: '\\implies', preview: '\\implies' },
          { label: 'Equivalent', latex: '\\iff', preview: '\\iff' }
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
    const raw = this.latex.trim();
    if (!raw) {
      this.previewHtml = '';
      this.renderError = '';
      this.cdr.markForCheck();
      return;
    }

    const trimmed = sanitizeLatex(raw);

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
    const raw = this.latex.trim();
    const trimmed = sanitizeLatex(raw);
    if (!trimmed || this.renderError) return;
    this.dialogRef.close({
      latex: trimmed,
      display: this.display
    });
  }
}
