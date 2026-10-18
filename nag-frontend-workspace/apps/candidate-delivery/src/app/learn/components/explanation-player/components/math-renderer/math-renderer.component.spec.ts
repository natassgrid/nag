import { ComponentFixture, TestBed } from '@angular/core/testing';
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

  it('should create and render empty content initially', () => {
    expect(component).toBeTruthy();
    expect(component.renderedHtml()).toBe('');
  });

  it('should render inline and block KaTeX expressions without errors', () => {
    fixture.componentRef.setInput('content', 'Solve $x^2 + 5x + 6 = 0$ for $x$');
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.innerHTML).toContain('katex');
  });

  it('should render display math blocks $$...$$', () => {
    fixture.componentRef.setInput('content', '$$\\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}$$');
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.innerHTML).toContain('katex');
  });
});
