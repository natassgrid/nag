import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StatCardComponent } from './stat-card.component';

describe('StatCardComponent', () => {
  let component: StatCardComponent;
  let fixture: ComponentFixture<StatCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StatCardComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(StatCardComponent);
    component = fixture.componentInstance;
  });

  it('should render label and value with appropriate variant class', () => {
    fixture.componentRef.setInput('label', 'Active Candidates');
    fixture.componentRef.setInput('value', 1250);
    fixture.componentRef.setInput('variant', 'success');
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Active Candidates');
    expect(compiled.textContent).toContain('1250');
    expect(component.iconBgClass()).toContain('emerald');
  });

  it('should return correct background class for all variants', () => {
    fixture.componentRef.setInput('label', 'Test');
    fixture.componentRef.setInput('value', 10);

    fixture.componentRef.setInput('variant', 'warn');
    expect(component.iconBgClass()).toContain('rose');

    fixture.componentRef.setInput('variant', 'accent');
    expect(component.iconBgClass()).toContain('amber');

    fixture.componentRef.setInput('variant', 'info');
    expect(component.iconBgClass()).toContain('sky');

    fixture.componentRef.setInput('variant', 'primary');
    expect(component.iconBgClass()).toContain('indigo');
  });
});
