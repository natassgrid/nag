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

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { SimpleChange } from '@angular/core';
import { MathRendererComponent } from './math-renderer.component';

describe('MathRendererComponent', () => {
  let component: MathRendererComponent;
  let fixture: ComponentFixture<MathRendererComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MathRendererComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(MathRendererComponent);
    component = fixture.componentInstance;
  });

  function setContent(content: string, inline: boolean = false): void {
    component.content = content;
    component.inline = inline;
    component.ngOnChanges({
      content: new SimpleChange(null, content, true),
      inline: new SimpleChange(null, inline, true)
    });
    fixture.detectChanges();
  }

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('LaTeX Math rendering (FR-1, FR-2, FR-3, Issue #143)', () => {
    it('should render inline LaTeX wrapped in $...$', () => {
      setContent('Find the value of $x^2 + y^2 = z^2$ when $x=3$ and $y=4$.');
      const rendererDiv = fixture.debugElement.query(By.css('.math-renderer'));
      const innerHTML = rendererDiv.nativeElement.innerHTML;
      expect(innerHTML).toContain('katex');
      expect(innerHTML).toContain('Find the value of');
    });

    it('should render display LaTeX wrapped in $$...$$', () => {
      setContent('Solve the integral: $$\\int_{0}^{1} x^2 \\, dx$$');
      const rendererDiv = fixture.debugElement.query(By.css('.math-renderer'));
      const innerHTML = rendererDiv.nativeElement.innerHTML;
      expect(innerHTML).toContain('katex');
      expect(innerHTML).toContain('katex-display');
    });

    it('should render LaTeX display math wrapped in \\[ ... \\]', () => {
      setContent('\\[ E = mc^2 \\]');
      const rendererDiv = fixture.debugElement.query(By.css('.math-renderer'));
      const innerHTML = rendererDiv.nativeElement.innerHTML;
      expect(innerHTML).toContain('katex');
      expect(innerHTML).toContain('katex-display');
    });

    it('should render LaTeX matrix and pmatrix environments', () => {
      setContent('Find the determinant of:\n\\begin{pmatrix} 1 & 2 \\\\ 3 & 4 \\end{pmatrix}');
      const rendererDiv = fixture.debugElement.query(By.css('.math-renderer'));
      const innerHTML = rendererDiv.nativeElement.innerHTML;
      expect(innerHTML).toContain('katex');
    });

    it('should render chemical formulas via mhchem \\ce{}', () => {
      setContent('Reaction: $$\\ce{2H2 + O2 -> 2H2O}$$');
      const rendererDiv = fixture.debugElement.query(By.css('.math-renderer'));
      const innerHTML = rendererDiv.nativeElement.innerHTML;
      expect(innerHTML).toContain('katex');
      expect(innerHTML).not.toContain('math-render-error');
    });

    it('should handle over-escaped backslashes in LaTeX commands from JSON serialization', () => {
      const rawText = '**Statements:**\\n1. A matrix $$A$$ is invertible if and only if its determinant is non-zero ($$\\\\det(A) \\\\neq 0$$).\\n2. A square matrix $$A$$ has a non-zero determinant if and only if its row vectors are linearly independent ($$L$$).\\n3. Matrix $$M$$ has row vectors that are linearly dependent ($$\\\\neg L$$).\\n\\n**Conclusions:**\\nI. Matrix $$M$$ is not invertible.\\nII. The determinant of Matrix $$M$$ is zero ($$\\\\det(M) = 0$$).';
      setContent(rawText);

      const rendererDiv = fixture.debugElement.query(By.css('.math-renderer'));
      const innerHTML = rendererDiv.nativeElement.innerHTML;
      expect(innerHTML).toContain('katex');
      expect(innerHTML).not.toContain('math-render-error');
      expect(innerHTML).toContain('Statements');
    });

    it('should not treat normal parentheses and numbers as LaTeX formulas', () => {
      const text = 'Statements: (1) All poets are daydreamers. (2) All painters are daydreamers.\nConclusions: (I) Some painters are poets. (II) Some daydreamers are painters.';
      setContent(text);

      const rendererDiv = fixture.debugElement.query(By.css('.math-renderer'));
      const innerHTML = rendererDiv.nativeElement.innerHTML;
      expect(innerHTML).toContain('(1) All poets are daydreamers.');
      expect(innerHTML).toContain('(I) Some painters are poets.');
      expect(innerHTML).not.toContain('katex');
    });
  });

  describe('SMILES Chemical Structure Rendering (Issue #144)', () => {
    it('should render <smiles> tag to canvas placeholder', () => {
      setContent('<smiles>c1ccccc1</smiles>');
      const rendererDiv = fixture.debugElement.query(By.css('.math-renderer'));
      const innerHTML = rendererDiv.nativeElement.innerHTML;
      expect(innerHTML).toContain('<canvas');
      expect(innerHTML).toContain('data-smiles="c1ccccc1"');
      expect(innerHTML).toContain('class="smiles-canvas"');
    });

    it('should parse width, height, theme and title attributes from <smiles> tag', () => {
      setContent('<smiles width="300" height="220" theme="dark" title="Benzene Ring">c1ccccc1</smiles>');
      const rendererDiv = fixture.debugElement.query(By.css('.math-renderer'));
      const innerHTML = rendererDiv.nativeElement.innerHTML;
      expect(innerHTML).toContain('width="300"');
      expect(innerHTML).toContain('height="220"');
      expect(innerHTML).toContain('data-theme="dark"');
      expect(innerHTML).toContain('Benzene Ring');
    });
  });

  describe('SVG rendering (FR-4)', () => {
    it('should pass SVG content through parseContent as HTML segments', () => {
      const svgContent = '<svg width="100" height="100"><circle cx="50" cy="50" r="40" fill="none" stroke="black"/></svg>';
      setContent(svgContent);

      const rendererDiv = fixture.debugElement.query(By.css('.math-renderer'));
      const innerHTML = rendererDiv.nativeElement.innerHTML;
      expect(innerHTML).toContain('<svg');
      expect(innerHTML).toContain('circle');
    });

    it('should render SVG alongside plain text without stripping tags', () => {
      setContent('Text before\n<svg width="100" height="100"><circle cx="50" cy="50" r="40" fill="none" stroke="black"/></svg>\nText after');

      const rendererDiv = fixture.debugElement.query(By.css('.math-renderer'));
      const innerHTML = rendererDiv.nativeElement.innerHTML;

      expect(innerHTML).toContain('Text before');
      expect(innerHTML).toContain('Text after');
      expect(innerHTML).toContain('<svg');
    });

    it('should render SVG alongside LaTeX math blocks', () => {
      setContent('$$x^2$$\n<svg width="50" height="50"><rect x="0" y="0" width="50" height="50"/></svg>');

      const rendererDiv = fixture.debugElement.query(By.css('.math-renderer'));
      const innerHTML = rendererDiv.nativeElement.innerHTML;

      expect(innerHTML).toContain('katex');
      expect(innerHTML).toContain('<svg');
    });
  });

  describe('Table rendering (FR-5)', () => {
    it('should render GFM pipe tables with headers', () => {
      const tableContent = '| Col 1 | Col 2 |\n| --- | --- |\n| Val 1 | Val 2 |';
      setContent(tableContent);

      const rendererDiv = fixture.debugElement.query(By.css('.math-renderer'));
      const innerHTML = rendererDiv.nativeElement.innerHTML;

      expect(innerHTML).toContain('<table');
      expect(innerHTML).toContain('<th');
      expect(innerHTML).toContain('Col 1');
      expect(innerHTML).toContain('Val 1');
    });

    it('should auto-fix pipe tables that are missing delimiter rows', () => {
      const rawTable = '| Header A | Header B |\n| Cell 1 | Cell 2 |';
      setContent(rawTable);

      const rendererDiv = fixture.debugElement.query(By.css('.math-renderer'));
      const innerHTML = rendererDiv.nativeElement.innerHTML;

      expect(innerHTML).toContain('<table');
      expect(innerHTML).toContain('Header A');
    });
  });

  describe('Mixed content (FR-6)', () => {
    it('should render mixed Markdown, LaTeX, and SVG together', () => {
      const mixedContent = [
        '# Question 1',
        '',
        'Consider the function $f(x) = x^2 + 2x + 1$ shown below:',
        '',
        '<svg width="100" height="50"><line x1="0" y1="25" x2="100" y2="25" stroke="red"/></svg>',
        '',
        'Calculate the integral:',
        '$$\\int_{0}^{2} f(x) \\, dx$$',
        '',
        '| Option | Value |',
        '| --- | --- |',
        '| A | 8.67 |',
        '| B | 9.33 |'
      ].join('\n');

      setContent(mixedContent);

      const rendererDiv = fixture.debugElement.query(By.css('.math-renderer'));
      const innerHTML = rendererDiv.nativeElement.innerHTML;

      expect(innerHTML).toContain('<h1');
      expect(innerHTML).toContain('Question 1');
      expect(innerHTML).toContain('katex');
      expect(innerHTML).toContain('<svg');
      expect(innerHTML).toContain('<table');
    });
  });
});
