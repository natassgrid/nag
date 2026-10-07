import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  inject,
  output,
  signal,
  ViewChild,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import katex from 'katex';
import 'katex/dist/contrib/mhchem.mjs';

export interface SymbolSnippet {
  label: string;
  latex: string;
  preview?: string;
  description?: string;
}

export interface PaletteCategory {
  id: string;
  name: string;
  icon: string;
  snippets: SymbolSnippet[];
}

@Component({
  selector: 'nag-formula-symbol-palette',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule],
  templateUrl: './formula-symbol-palette.component.html',
  styleUrl: './formula-symbol-palette.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FormulaSymbolPaletteComponent {
  private sanitizer = inject(DomSanitizer);

  /** Emitted when a formula or snippet is chosen for insertion into parent editor */
  readonly symbolSelected = output<string>();

  @ViewChild('customInputRef') customInputRef?: ElementRef<HTMLTextAreaElement>;

  readonly selectedCategory = signal<string>('math');
  readonly customFormula = signal<string>('');
  readonly isDisplayMode = signal<boolean>(false);
  readonly previewHtml = signal<SafeHtml>('');
  readonly syntaxError = signal<string>('');

  readonly categories: PaletteCategory[] = [
    {
      id: 'math',
      name: 'Basic Math',
      icon: 'calculate',
      snippets: [
        { label: 'Fraction', latex: '\\frac{a}{b}' },
        { label: 'Square Root', latex: '\\sqrt{x}' },
        { label: 'Nth Root', latex: '\\sqrt[n]{x}' },
        { label: 'Power', latex: 'x^{2}' },
        { label: 'Subscript', latex: 'x_{i}' },
        { label: 'Sub & Sup', latex: 'x_{i}^{2}' },
        { label: 'Multiply', latex: '\\times' },
        { label: 'Divide', latex: '\\div' },
        { label: 'Plus-Minus', latex: '\\pm' },
        { label: 'Not Equal', latex: '\\neq' },
        { label: 'Approx', latex: '\\approx' },
        { label: 'Less Equal', latex: '\\leq' },
        { label: 'Greater Equal', latex: '\\geq' },
        { label: 'Degree', latex: '^\\circ' },
      ],
    },
    {
      id: 'chemistry',
      name: 'Chemistry (\\ce)',
      icon: 'science',
      snippets: [
        { label: 'Reaction Arrow', latex: '\\ce{A -> B}' },
        { label: 'Reversible Arrow', latex: '\\ce{A <=> B}' },
        { label: 'Catalyst Arrow', latex: '\\ce{A ->[\\text{cat}] B}' },
        { label: 'Water Synthesis', latex: '\\ce{2H2 + O2 -> 2H2O}' },
        { label: 'Combustion', latex: '\\ce{CH4 + 2O2 -> CO2 + 2H2O}' },
        { label: 'Precipitate', latex: '\\ce{Ag+ + Cl- -> AgCl v}' },
        { label: 'Gas Evolution', latex: '\\ce{Zn + 2HCl -> ZnCl2 + H2 ^}' },
        { label: 'Sulfate Ion', latex: '\\ce{SO4^2-}' },
        { label: 'Hydrate Salt', latex: '\\ce{CuSO4 . 5H2O}' },
        { label: 'Phase (aq/s)', latex: '\\ce{NaCl(aq) + AgNO3(aq) -> AgCl(s) + NaNO3(aq)}' },
        { label: 'Physics Unit', latex: '\\pu{9.8 m/s^2}' },
        { label: 'Gas Constant', latex: '\\pu{8.314 J/(mol K)}' },
      ],
    },
    {
      id: 'greek',
      name: 'Greek Letters',
      icon: 'translate',
      snippets: [
        { label: 'alpha (α)', latex: '\\alpha' },
        { label: 'beta (β)', latex: '\\beta' },
        { label: 'gamma (γ)', latex: '\\gamma' },
        { label: 'delta (δ)', latex: '\\delta' },
        { label: 'Delta (Δ)', latex: '\\Delta' },
        { label: 'theta (θ)', latex: '\\theta' },
        { label: 'lambda (λ)', latex: '\\lambda' },
        { label: 'Lambda (Λ)', latex: '\\Lambda' },
        { label: 'mu (μ)', latex: '\\mu' },
        { label: 'pi (π)', latex: '\\pi' },
        { label: 'Pi (Π)', latex: '\\Pi' },
        { label: 'sigma (σ)', latex: '\\sigma' },
        { label: 'Sigma (Σ)', latex: '\\Sigma' },
        { label: 'omega (ω)', latex: '\\omega' },
        { label: 'Omega (Ω)', latex: '\\Omega' },
        { label: 'phi (φ)', latex: '\\phi' },
        { label: 'rho (ρ)', latex: '\\rho' },
      ],
    },
    {
      id: 'calculus',
      name: 'Calculus & Analysis',
      icon: 'trending_up',
      snippets: [
        { label: 'Definite Integral', latex: '\\int_{a}^{b} f(x) \\, dx' },
        { label: 'Double Integral', latex: '\\iint_{D} f(x,y) \\, dxdy' },
        { label: 'Contour Integral', latex: '\\oint_{C} f(z) \\, dz' },
        { label: 'Derivative', latex: '\\frac{df}{dx}' },
        { label: 'Partial Deriv', latex: '\\frac{\\partial f}{\\partial x}' },
        { label: 'Limit', latex: '\\lim_{x \\to 0} f(x)' },
        { label: 'Summation', latex: '\\sum_{i=1}^{n} x_i' },
        { label: 'Product', latex: '\\prod_{i=1}^{n} x_i' },
        { label: 'Infinity', latex: '\\infty' },
      ],
    },
    {
      id: 'logic',
      name: 'Sets & Logic',
      icon: 'hub',
      snippets: [
        { label: 'Element Of', latex: '\\in' },
        { label: 'Not In', latex: '\\notin' },
        { label: 'Subset', latex: '\\subset' },
        { label: 'Subset Eq', latex: '\\subseteq' },
        { label: 'Union', latex: '\\cup' },
        { label: 'Intersection', latex: '\\cap' },
        { label: 'Empty Set', latex: '\\emptyset' },
        { label: 'For All', latex: '\\forall' },
        { label: 'Exists', latex: '\\exists' },
        { label: 'Therefore', latex: '\\therefore' },
        { label: 'Because', latex: '\\because' },
        { label: 'Implies', latex: '\\implies' },
        { label: 'Equivalent', latex: '\\iff' },
      ],
    },
    {
      id: 'matrices',
      name: 'Algebra & Matrices',
      icon: 'grid_on',
      snippets: [
        { label: '2x2 Matrix', latex: '\\begin{pmatrix} a & b \\\\ c & d \\end{pmatrix}' },
        { label: '3x3 Matrix', latex: '\\begin{pmatrix} a & b & c \\\\ d & e & f \\\\ g & h & i \\end{pmatrix}' },
        { label: 'Determinant', latex: '\\begin{vmatrix} a & b \\\\ c & d \\end{vmatrix}' },
        { label: 'Cases / Piecewise', latex: 'f(x) = \\begin{cases} x^2 & x \\geq 0 \\\\ -x & x < 0 \\end{cases}' },
        { label: 'Binomial', latex: '\\binom{n}{k}' },
      ],
    },
  ];

  selectCategory(id: string): void {
    this.selectedCategory.set(id);
  }

  onCustomFormulaChange(val: string): void {
    this.customFormula.set(val);
    this.updatePreview();
  }

  toggleDisplayMode(): void {
    this.isDisplayMode.update((v) => !v);
    this.updatePreview();
  }

  insertSnippet(snippet: SymbolSnippet): void {
    const raw = snippet.latex;
    // Format based on current display mode preference or inline default
    const formatted = this.isDisplayMode() ? `$$${raw}$$` : `$${raw}$`;
    this.symbolSelected.emit(formatted);
  }

  insertCustom(): void {
    const raw = this.customFormula().trim();
    if (!raw || this.syntaxError()) return;
    const formatted = this.isDisplayMode() ? `$$${raw}$$` : `$${raw}$`;
    this.symbolSelected.emit(formatted);
    this.customFormula.set('');
    this.previewHtml.set('');
    this.syntaxError.set('');
  }

  appendToCustom(latexSnippet: string): void {
    const cur = this.customFormula();
    const next = cur ? `${cur} ${latexSnippet}` : latexSnippet;
    this.customFormula.set(next);
    this.updatePreview();
  }

  private updatePreview(): void {
    const raw = this.customFormula().trim();
    if (!raw) {
      this.previewHtml.set('');
      this.syntaxError.set('');
      return;
    }

    try {
      const html = katex.renderToString(raw, {
        throwOnError: true,
        displayMode: this.isDisplayMode(),
        output: 'htmlAndMathml',
      });
      this.previewHtml.set(this.sanitizer.bypassSecurityTrustHtml(html));
      this.syntaxError.set('');
    } catch (err: any) {
      const msg = err?.message?.replace(/^KaTeX parse error:\s*/i, '') || 'Invalid LaTeX syntax';
      this.syntaxError.set(msg);
      this.previewHtml.set('');
    }
  }
}
