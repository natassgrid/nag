import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ExplanationPlayerComponent } from './explanation-player.component';
import { ExplanationIr } from '../../models/explanation-ir.model';

describe('ExplanationPlayerComponent', () => {
  let component: ExplanationPlayerComponent;
  let fixture: ComponentFixture<ExplanationPlayerComponent>;

  const mockExplanation: ExplanationIr = {
    explanationId: 'exp-full-test',
    questionId: 'q-999',
    status: 'APPROVED',
    problem: {
      statement: 'Solve for x: 2x + 4 = 10',
      authoritativeAnswer: '3',
    },
    concepts: ['Algebra', 'Linear Equations'],
    steps: [
      {
        id: 's1',
        sequence: 1,
        type: 'GIVEN',
        after: '2x + 4 = 10',
        justification: { why: 'Given equation from statement' },
      },
      {
        id: 's2',
        sequence: 2,
        type: 'TRANSFORM',
        before: '2x + 4 = 10',
        after: '2x = 6',
        operation: { name: 'SUBTRACT', target: 'both-sides', value: 4 },
        justification: { why: 'Subtract 4 from both sides to isolate 2x' },
      },
      {
        id: 's3',
        sequence: 3,
        type: 'CONCLUDE',
        before: '2x = 6',
        after: 'x = 3',
        operation: { name: 'DIVIDE', target: 'both-sides', value: 2 },
        justification: { why: 'Divide both sides by 2' },
      },
    ],
    hints: [
      { id: 'h1', level: 1, text: 'Subtract 4 from both sides first.' },
      { id: 'h2', level: 2, text: 'Divide the remaining value by 2.' },
    ],
    verification: {
      passed: true,
      confidence: 1.0,
      checks: ['2(3) + 4 = 10'],
    },
    misconceptions: [
      {
        id: 'm1',
        incorrectReasoning: '2x = 10 + 4 = 14',
        whyWrong: 'Added 4 instead of subtracting',
        correctedReasoning: 'Invert addition by subtracting 4: 10 - 4 = 6',
      },
    ],
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExplanationPlayerComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ExplanationPlayerComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('explanation', mockExplanation);
    fixture.detectChanges();
  });

  it('should create and display problem statement and concepts', () => {
    expect(component).toBeTruthy();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Question #q-999');
    expect(compiled.textContent).toContain('Solve for x: 2x + 4 = 10');
    expect(compiled.textContent).toContain('Algebra');
    expect(compiled.textContent).toContain('APPROVED');
  });

  it('should compute initial active step as step 1', () => {
    expect(component.activeStepIndex()).toBe(0);
    expect(component.currentStep()?.id).toBe('s1');
    expect(component.progressPercent()).toBe(33);
  });

  it('should update active step when selecting another step', () => {
    component.onSelectStep(1);
    fixture.detectChanges();

    expect(component.activeStepIndex()).toBe(1);
    expect(component.currentStep()?.id).toBe('s2');
    expect(component.currentStep()?.operation?.name).toBe('SUBTRACT');
    expect(component.progressPercent()).toBe(67);
  });

  it('should support playback toggle and speed selection', () => {
    component.onSelectSpeed(2.0);
    expect(component.playbackSpeed()).toBe(2.0);

    component.onTogglePlay(true);
    expect(component.isPlaying()).toBe(true);

    component.onTogglePlay(false);
    expect(component.isPlaying()).toBe(false);
  });
});
