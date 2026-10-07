import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormulaSymbolPaletteComponent } from './formula-symbol-palette.component';

describe('FormulaSymbolPaletteComponent', () => {
  let component: FormulaSymbolPaletteComponent;
  let fixture: ComponentFixture<FormulaSymbolPaletteComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FormulaSymbolPaletteComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(FormulaSymbolPaletteComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should initialize with categories including Basic Math, Chemistry, Greek Letters', () => {
    expect(component).toBeTruthy();
    expect(component.categories.length).toBeGreaterThanOrEqual(4);
    const catIds = component.categories.map((c) => c.id);
    expect(catIds).toContain('math');
    expect(catIds).toContain('chemistry');
    expect(catIds).toContain('greek');
    expect(catIds).toContain('calculus');
  });

  it('should switch categories', () => {
    component.selectCategory('chemistry');
    expect(component.selectedCategory()).toBe('chemistry');
    component.selectCategory('greek');
    expect(component.selectedCategory()).toBe('greek');
  });

  it('should emit formatted inline symbol when clicking a snippet in inline mode', () => {
    let emitted: string | undefined;
    component.symbolSelected.subscribe((s) => (emitted = s));

    const fractionSnippet = { label: 'Fraction', latex: '\\frac{a}{b}' };
    component.insertSnippet(fractionSnippet);

    expect(emitted).toBe('$\\frac{a}{b}$');
  });

  it('should emit display block symbol when isDisplayMode is active', () => {
    let emitted: string | undefined;
    component.symbolSelected.subscribe((s) => (emitted = s));

    component.toggleDisplayMode();
    expect(component.isDisplayMode()).toBe(true);

    const chemSnippet = { label: 'Water Synthesis', latex: '\\ce{2H2 + O2 -> 2H2O}' };
    component.insertSnippet(chemSnippet);

    expect(emitted).toBe('$$\\ce{2H2 + O2 -> 2H2O}$$');
  });

  it('should render live KaTeX preview on valid custom formula', () => {
    component.onCustomFormulaChange('x^2 + y^2 = r^2');
    expect(component.syntaxError()).toBe('');
    expect(component.previewHtml()).toBeTruthy();
  });

  it('should report syntax error on malformed LaTeX in custom input', () => {
    component.onCustomFormulaChange('\\frac{unclosed');
    expect(component.syntaxError()).toBeTruthy();
  });
});
