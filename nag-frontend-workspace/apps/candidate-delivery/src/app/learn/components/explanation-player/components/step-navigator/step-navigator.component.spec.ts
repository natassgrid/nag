import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StepNavigatorComponent } from './step-navigator.component';
import { ExplanationStep } from '../../../../models/explanation-ir.model';

describe('StepNavigatorComponent', () => {
  let component: StepNavigatorComponent;
  let fixture: ComponentFixture<StepNavigatorComponent>;

  const mockSteps: ExplanationStep[] = [
    { id: 's1', sequence: 1, type: 'GIVEN', after: 'm = 10' },
    { id: 's2', sequence: 2, type: 'APPLY_RULE', after: 'F = m*a' },
    { id: 's3', sequence: 3, type: 'CONCLUDE', after: 'a = 5' },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StepNavigatorComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(StepNavigatorComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('steps', mockSteps);
    fixture.componentRef.setInput('activeStepIndex', 0);
    fixture.componentRef.setInput('isPlaying', false);
    fixture.componentRef.setInput('playbackSpeed', 1.0);
    fixture.detectChanges();
  });

  it('should render all step pills and step progress', () => {
    expect(component).toBeTruthy();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Step 1 of 3');
    expect(compiled.textContent).toContain('GIVEN');
    expect(compiled.textContent).toContain('CONCLUDE');
  });

  it('should emit stepChange on next and previous', () => {
    jest.spyOn(component.stepChange, 'emit');

    component.onNext();
    expect(component.stepChange.emit).toHaveBeenCalledWith(1);

    fixture.componentRef.setInput('activeStepIndex', 1);
    fixture.detectChanges();

    component.onPrevious();
    expect(component.stepChange.emit).toHaveBeenCalledWith(0);
  });

  it('should emit playToggle on toggle play', () => {
    jest.spyOn(component.playToggle, 'emit');

    component.onTogglePlay();
    expect(component.playToggle.emit).toHaveBeenCalledWith(true);
  });

  it('should emit speedChange on selecting speed', () => {
    jest.spyOn(component.speedChange, 'emit');

    component.onSelectSpeed(1.5);
    expect(component.speedChange.emit).toHaveBeenCalledWith(1.5);
  });
});
