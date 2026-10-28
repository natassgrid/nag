import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { of } from 'rxjs';
import { SubjectTopicService } from '@nag-frontend-workspace/questions-data-access';
import { QuestionAuthoringWizardDialogComponent } from './question-authoring-wizard-dialog.component';
import { QuestionWizardDialogData } from '../../models/authoring.model';

describe('QuestionAuthoringWizardDialogComponent', () => {
  let component: QuestionAuthoringWizardDialogComponent;
  let fixture: ComponentFixture<QuestionAuthoringWizardDialogComponent>;
  let dialogRefMock: { close: jest.Mock };
  let subjectTopicServiceMock: {
    getTopics: jest.Mock;
    getSubtopics: jest.Mock;
    createSubject: jest.Mock;
    createTopic: jest.Mock;
  };

  const defaultData: QuestionWizardDialogData = {
    mode: 'STANDALONE',
    editingQuestionId: null,
    subjects: [{ id: 1, name: 'Physics', code: 'PHY' }],
    topics: [{ id: 10, name: 'Mechanics', code: 'MECH' }],
    subtopics: [{ id: 100, name: 'Kinematics', code: 'KIN' }],
    selectedSubjectId: 1,
    selectedTopicId: 10,
    selectedSubtopicId: 100,
    cognitiveLevel: 'UNDERSTAND',
    type: 'MULTIPLE_CHOICE',
    difficulty: 'MEDIUM',
    marks: 4,
    negativeMarks: 1,
    content: 'Consider $$F = ma$$',
    explanation: 'Newton second law',
    options: [
      { id: 'A', text: 'Force', isCorrect: true },
      { id: 'B', text: 'Energy', isCorrect: false },
    ],
    passageTitle: 'Passage 1',
    passageContent: 'Passage content text',
    subQuestions: [
      {
        passageOrderIndex: 1,
        content: 'Sub Q 1',
        questionType: 'MULTIPLE_CHOICE',
        difficulty: 'EASY',
        cognitiveLevel: 'REMEMBER',
        marks: 4,
        negativeMarks: 1,
        explanation: '',
        options: [
          { id: 'A', text: 'A1', isCorrect: true },
          { id: 'B', text: 'B1', isCorrect: false },
        ],
      },
    ],
    activeSubQuestionIndex: 0,
    initialStep: 0,
  };

  beforeEach(async () => {
    dialogRefMock = { close: jest.fn() };
    subjectTopicServiceMock = {
      getTopics: jest.fn().mockReturnValue(of([{ id: 10, name: 'Mechanics', code: 'MECH' }])),
      getSubtopics: jest.fn().mockReturnValue(of([{ id: 100, name: 'Kinematics', code: 'KIN' }])),
      createSubject: jest.fn().mockReturnValue(of({ id: 2, name: 'Math', code: 'MTH' })),
      createTopic: jest.fn().mockReturnValue(of({ id: 20, name: 'Algebra', code: 'ALG' })),
    };

    await TestBed.configureTestingModule({
      imports: [QuestionAuthoringWizardDialogComponent],
      providers: [
        { provide: MatDialogRef, useValue: dialogRefMock },
        { provide: MAT_DIALOG_DATA, useValue: defaultData },
        { provide: SubjectTopicService, useValue: subjectTopicServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(QuestionAuthoringWizardDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should initialize with 4 steps in STANDALONE mode and navigate between steps', () => {
    expect(component).toBeTruthy();
    expect(component.steps().length).toBe(4);
    expect(component.currentStep()).toBe(0);

    component.nextStep();
    expect(component.currentStep()).toBe(1);

    component.prevStep();
    expect(component.currentStep()).toBe(0);

    component.goToStep(3);
    expect(component.isLastStep()).toBe(true);
  });

  it('should switch to PASSAGE mode with 3 steps and reset currentStep to 0', () => {
    component.goToStep(2);
    component.setMode('PASSAGE');
    expect(component.mode()).toBe('PASSAGE');
    expect(component.steps().length).toBe(3);
    expect(component.currentStep()).toBe(0);
  });

  it('should emit switch-to-inpage result with updated state when switchToInPage is called', () => {
    component.content.set('Updated in wizard');
    component.switchToInPage();

    expect(dialogRefMock.close).toHaveBeenCalledWith(
      expect.objectContaining({
        action: 'switch-to-inpage',
        state: expect.objectContaining({
          content: 'Updated in wizard',
        }),
      })
    );
  });

  it('should validate required fields before saving from wizard', () => {
    component.content.set('   ');
    component.saveFromWizard();

    expect(component.validationError()).toContain('cannot be empty');
    expect(component.currentStep()).toBe(1);
    expect(dialogRefMock.close).not.toHaveBeenCalled();

    component.content.set('Valid question stem');
    component.saveFromWizard();
    expect(dialogRefMock.close).toHaveBeenCalledWith(
      expect.objectContaining({
        action: 'save',
      })
    );
  });
});
