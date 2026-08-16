/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SimpleChanges } from '@angular/core';
import { MathRendererComponent } from './math-renderer.component';
import { By } from '@angular/platform-browser';

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

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('SVG rendering (FR-4)', () => {

    function setContent(value: string): void {
      component.content = value;
      component.ngOnChanges({
        content: {
          currentValue: value,
          previousValue: '',
          firstChange: true,
          isFirstChange: () => true
        }
      });
      fixture.detectChanges();
    }

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
      // KaTeX output present
      expect(innerHTML).toContain('katex');
      // SVG markup present
      expect(innerHTML).toContain('<svg');
      expect(innerHTML).toContain('<rect');
    });

    it('should handle empty content gracefully', () => {
      setContent('');
      expect(component.renderedHtml).toBe('');
    });

    it('should preserve complex SVG with nested elements and attributes', () => {
      setContent('<svg viewBox="0 0 200 200"><g><line x1="0" y1="0" x2="200" y2="200" stroke="red"></line><text x="10" y="20">Label</text></g></svg>');

      const rendererDiv = fixture.debugElement.query(By.css('.math-renderer'));
      const innerHTML = rendererDiv.nativeElement.innerHTML;
      expect(innerHTML).toContain('<svg');
      expect(innerHTML).toContain('viewBox');
      expect(innerHTML).toContain('line');
      expect(innerHTML).toContain('text');
      expect(innerHTML).toContain('Label');
    });

    it('should not treat SVG as math content (no $$...$$)', () => {
      setContent('<svg width="20" height="20"><circle r="5"/></svg>');

      const rendererDiv = fixture.debugElement.query(By.css('.math-renderer'));
      const innerHTML = rendererDiv.nativeElement.innerHTML;
      // Should NOT contain KaTeX error spans for SVG content
      expect(innerHTML).not.toContain('math-render-error');
      expect(innerHTML).toContain('<svg');
    });
  });
});
