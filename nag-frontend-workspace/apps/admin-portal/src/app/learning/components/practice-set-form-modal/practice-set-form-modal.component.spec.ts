import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormBuilder } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { PracticeSetFormModalComponent } from './practice-set-form-modal.component';
import { PracticeSetService } from '../../services';
import { PaperService } from '@nag-frontend-workspace/examinations-data-access';

describe('PracticeSetFormModalComponent', () => {
  let component: PracticeSetFormModalComponent;
  let fixture: ComponentFixture<PracticeSetFormModalComponent>;
  let practiceSetServiceMock: any;
  let paperServiceMock: any;
  let dialogRefMock: any;

  const mockPapers = [
    {
      id: 'paper-1',
      paperId: 'paper-1',
      name: 'NES 2026 Mathematics Practice',
      examName: 'NES 2026',
      shiftName: 'Morning Shift',
      isPractice: true,
      status: 'APPROVED',
    },
    {
      id: 'paper-2',
      paperId: 'paper-2',
      name: 'NES 2026 Science Practice',
      examName: 'NES 2026',
      shiftName: 'Afternoon Shift',
      isPractice: true,
      status: 'DRAFT',
    },
  ];

  const mockPaperDetail = {
    id: 'paper-1',
    name: 'NES 2026 Mathematics Practice',
    examName: 'NES 2026',
    shiftName: 'math-shift',
    totalQuestions: 2,
    questions: [
      { id: 'q-uuid-1', text: 'Sample Question 1' },
      { id: 'q-uuid-2', text: 'Sample Question 2' },
    ],
    paperDefinitionJson: JSON.stringify({ questionIds: ['q-uuid-1', 'q-uuid-2'] }),
  };

  beforeEach(async () => {
    practiceSetServiceMock = {
      create: jest.fn().mockReturnValue(of({ id: 'new-set-123', name: 'Test Set' })),
      update: jest.fn().mockReturnValue(of({ id: 'existing-set-123', name: 'Updated Set' })),
    };

    paperServiceMock = {
      getPapers: jest.fn().mockReturnValue(of({ content: mockPapers, totalElements: 2 })),
      getPaper: jest.fn().mockReturnValue(of(mockPaperDetail)),
    };

    dialogRefMock = {
      close: jest.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [PracticeSetFormModalComponent, NoopAnimationsModule],
      providers: [
        FormBuilder,
        { provide: PracticeSetService, useValue: practiceSetServiceMock },
        { provide: PaperService, useValue: paperServiceMock },
        { provide: MatDialogRef, useValue: dialogRefMock },
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            set: null,
            mode: 'create',
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PracticeSetFormModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load practice papers in create mode', () => {
    expect(component).toBeTruthy();
    expect(paperServiceMock.getPapers).toHaveBeenCalledWith({ page: 0, size: 50, isPractice: true });
    expect(component.practicePapers().length).toBe(2);
  });

  it('should load paper details and attach questions when paper is selected', () => {
    component.onPaperSelected('paper-1');
    expect(paperServiceMock.getPaper).toHaveBeenCalledWith('paper-1');
    expect(component.attachedQuestionIds()).toEqual(['q-uuid-1', 'q-uuid-2']);
    expect(component.totalQuestionsCount()).toBe(2);
    expect(component.form.get('name')?.value).toBe('Practice - NES 2026 Mathematics Practice');
  });

  it('should submit create request with attached questions and source EXAM_CLONE', () => {
    component.onPaperSelected('paper-1');
    component.onSubmit();

    expect(practiceSetServiceMock.create).toHaveBeenCalledWith({
      name: 'Practice - NES 2026 Mathematics Practice',
      description: 'Generated from official practice paper: NES 2026 Mathematics Practice',
      durationMinutes: 30,
      subjectSlug: 'math-shift',
      questionIds: ['q-uuid-1', 'q-uuid-2'],
      source: 'EXAM_CLONE',
      totalQuestions: 2,
    });
    expect(dialogRefMock.close).toHaveBeenCalled();
  });

  it('should switch to manual mode and clear attached questions', () => {
    component.onPaperSelected('paper-1');
    expect(component.attachedQuestionIds().length).toBe(2);

    component.onSourceTypeChange('MANUAL');
    expect(component.sourceType()).toBe('MANUAL');
    expect(component.attachedQuestionIds()).toEqual([]);
  });

  it('should close dialog on cancel', () => {
    component.onCancel();
    expect(dialogRefMock.close).toHaveBeenCalled();
  });
});
