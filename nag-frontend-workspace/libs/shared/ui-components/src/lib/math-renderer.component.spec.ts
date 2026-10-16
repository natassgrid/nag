import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SimpleChange } from '@angular/core';
import { MathRendererComponent } from './math-renderer.component';

describe('MathRendererComponent', () => {
  let component: MathRendererComponent;
  let fixture: ComponentFixture<MathRendererComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MathRendererComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(MathRendererComponent);
    component = fixture.componentInstance;
  });

  const setContent = (content: string) => {
    fixture.componentRef.setInput('content', content);
    component.ngOnChanges({
      content: new SimpleChange(null, content, true),
    });
    fixture.detectChanges();
  };

  it('should render latex math formula content', () => {
    setContent('Solve $E = mc^2$');
    const html = (component.renderedHtml() as any)?.changingThisBreaksApplicationSecurity || '';
    expect(html).toContain('katex');
  });

  it('should render chemistry reactions using mhchem syntax in display mode', () => {
    setContent('$$\\ce{2H2 + O2 -> 2H2O}$$');
    const html = (component.renderedHtml() as any)?.changingThisBreaksApplicationSecurity || '';
    expect(html).toContain('katex');
    expect(html).not.toContain('katex-error');
  });

  it('should render chemistry reactions with escaped braces syntax \\ce\\{...\\}', () => {
    setContent('Reaction: $$\\ce\\{2H2 + O2 -> 2H2O\\}$$');
    const html = (component.renderedHtml() as any)?.changingThisBreaksApplicationSecurity || '';
    expect(html).toContain('katex');
    expect(html).not.toContain('katex-error');
  });

  it('should render standalone chemistry reactions without dollar delimiters', () => {
    setContent('When heated, \\ce{CaCO3 -> CaO + CO2 ^} decomposes.');
    const html = (component.renderedHtml() as any)?.changingThisBreaksApplicationSecurity || '';
    expect(html).toContain('katex');
    expect(html).not.toContain('katex-error');
  });

  it('should render nested chemistry notation like ions and states', () => {
    setContent('Oxidation: \\ce{Fe^{2+} + e- -> Fe}');
    const html = (component.renderedHtml() as any)?.changingThisBreaksApplicationSecurity || '';
    expect(html).toContain('katex');
    expect(html).not.toContain('katex-error');
  });

  it('should repair control character artifacts from JSON unescaping (formfeed and tab)', () => {
    setContent('( r = \x0crac{500}{500 \t imes 365} )');
    const html = (component.renderedHtml() as any)?.changingThisBreaksApplicationSecurity || '';
    expect(html).toContain('katex');
    expect(html).not.toContain('katex-error');
  });

  it('should render bare formulas without math delimiters in cheat sheets or answers', () => {
    setContent('[1 - x] = \\frac{1}{100}');
    const html = (component.renderedHtml() as any)?.changingThisBreaksApplicationSecurity || '';
    expect(html).toContain('katex');
    expect(html).not.toContain('katex-error');
  });
});
