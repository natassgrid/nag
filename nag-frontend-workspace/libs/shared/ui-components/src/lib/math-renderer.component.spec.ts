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

  it('should render latex math formula content', () => {
    fixture.componentRef.setInput('content', 'Solve $E = mc^2$');
    component.ngOnChanges({
      content: new SimpleChange(null, 'Solve $E = mc^2$', true),
    });
    fixture.detectChanges();

    expect(component.renderedHtml()).toBeDefined();
  });
});
