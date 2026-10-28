import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { SubjectTopicService } from '@nag-frontend-workspace/questions-data-access';
import { MicroLessonWizardDialogComponent } from './micro-lesson-wizard-dialog.component';
import { MicroLessonWizardDialogData } from '../../models';

describe('MicroLessonWizardDialogComponent', () => {
  let component: MicroLessonWizardDialogComponent;
  let fixture: ComponentFixture<MicroLessonWizardDialogComponent>;
  let dialogRefMock: { close: jest.Mock };
  let subjectTopicServiceMock: any;

  const defaultData: MicroLessonWizardDialogData = {
    editingLessonId: null,
    subjects: [{ id: 10, name: 'Physics' }],
    topics: [{ id: 100, name: 'Mechanics' }],
    subtopics: [{ id: 1000, name: 'Work, Energy & Power' }],
    selectedSubjectId: 10,
    selectedTopicId: 100,
    selectedSubtopicId: 1000,
    customSubtopicName: 'Work, Energy & Power',
    examOptions: [{ label: 'JEE Main', value: 'JEE_MAIN' }],
    targetExam: 'JEE_MAIN',
    lessonType: 'CONCEPT',
    difficultyLevel: 'CORE',
    sequenceNo: 1,
    estimatedMinutes: 5,
    status: 'APPROVED',
    coreConcept: 'Work-Energy Theorem $$W = \\Delta K$$',
    trapAlert: {
      title: 'Sign Trap',
      description: 'Watch signs',
      commonMistake: 'Sign error',
      correctApproach: 'Include all forces',
    },
    formulaEntries: [
      { name: 'Work-Energy', latex: '$$W = \\Delta K$$', description: '', variables: [] },
    ],
    problemStatement: 'Block of mass 2 kg...',
    steps: [{ stepNumber: 1, title: 'Step 1', explanation: 'Apply theorem', formula: '', tip: '' }],
    finalAnswer: '$$v = 10\\text{ m/s}$$',
    keyTakeaways: ['Include friction'],
    quickCheckItems: [],
    reviewerNotes: '',
    initialStep: 0,
  };

  beforeEach(async () => {
    dialogRefMock = { close: jest.fn() };
    subjectTopicServiceMock = {
      getTopics: jest.fn().mockReturnValue(of([{ id: 100, name: 'Mechanics' }])),
      getSubtopics: jest.fn().mockReturnValue(of([{ id: 1000, name: 'Work, Energy & Power' }])),
      createSubject: jest.fn().mockReturnValue(of({ id: 20, name: 'Chemistry' })),
      createTopic: jest.fn().mockReturnValue(of({ id: 200, name: 'Thermodynamics' })),
    };

    await TestBed.configureTestingModule({
      imports: [MicroLessonWizardDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRefMock },
        { provide: MAT_DIALOG_DATA, useValue: defaultData },
        { provide: SubjectTopicService, useValue: subjectTopicServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MicroLessonWizardDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should initialize with 6 wizard steps and navigate forward and backward', () => {
    expect(component).toBeTruthy();
    expect(component.stepsList.length).toBe(6);
    expect(component.currentStep()).toBe(0);

    component.nextStep();
    expect(component.currentStep()).toBe(1);

    component.prevStep();
    expect(component.currentStep()).toBe(0);

    component.goToStep(5);
    expect(component.isLastStep()).toBe(true);
  });

  it('should close with action="switch-to-inpage" and preserve modified state', () => {
    component.coreConcept.set('Modified concept in wizard');
    component.switchToInPage();

    expect(dialogRefMock.close).toHaveBeenCalledWith(
      expect.objectContaining({
        action: 'switch-to-inpage',
        state: expect.objectContaining({
          coreConcept: 'Modified concept in wizard',
        }),
      })
    );
  });

  it('should validate coreConcept and required formulas before closing with action="save"', () => {
    component.coreConcept.set('   ');
    component.saveFromWizard();

    expect(component.validationError()).toContain('Core Concept');
    expect(component.currentStep()).toBe(1);
    expect(dialogRefMock.close).not.toHaveBeenCalled();

    component.coreConcept.set('Valid Core Concept $$E = mc^2$$');
    component.saveFromWizard();

    expect(dialogRefMock.close).toHaveBeenCalledWith(
      expect.objectContaining({
        action: 'save',
      })
    );
  });
});
