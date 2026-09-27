import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EmptyStateComponent } from './empty-state.component';

describe('EmptyStateComponent', () => {
  let component: EmptyStateComponent;
  let fixture: ComponentFixture<EmptyStateComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EmptyStateComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(EmptyStateComponent);
    component = fixture.componentInstance;
  });

  it('should render title, description, and icon', () => {
    fixture.componentRef.setInput('title', 'No Records Found');
    fixture.componentRef.setInput('description', 'Try adjusting your filters');
    fixture.componentRef.setInput('icon', 'search_off');
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('No Records Found');
    expect(compiled.textContent).toContain('Try adjusting your filters');
    expect(component.icon()).toBe('search_off');
  });
});
