import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StatusBadgeComponent, StatusVariant } from './status-badge.component';

describe('StatusBadgeComponent', () => {
  let component: StatusBadgeComponent;
  let fixture: ComponentFixture<StatusBadgeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StatusBadgeComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(StatusBadgeComponent);
    component = fixture.componentInstance;
  });

  it('should create and render label with proper badge variant classes', () => {
    fixture.componentRef.setInput('label', 'Active');
    fixture.componentRef.setInput('variant', 'success' as StatusVariant);
    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(fixture.nativeElement.textContent).toContain('Active');
    expect(component.badgeClass()).toContain('bg-emerald-50');

    fixture.componentRef.setInput('variant', 'warn' as StatusVariant);
    expect(component.badgeClass()).toContain('bg-amber-50');

    fixture.componentRef.setInput('variant', 'error' as StatusVariant);
    expect(component.badgeClass()).toContain('bg-rose-50');

    fixture.componentRef.setInput('variant', 'info' as StatusVariant);
    expect(component.badgeClass()).toContain('bg-sky-50');

    fixture.componentRef.setInput('variant', 'primary' as StatusVariant);
    expect(component.badgeClass()).toContain('bg-indigo-50');

    fixture.componentRef.setInput('variant', 'neutral' as StatusVariant);
    expect(component.badgeClass()).toContain('bg-slate-100');
  });
});
