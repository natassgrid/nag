import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { FormBuilder } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { PracticeSetFormModalComponent } from './practice-set-form-modal.component';
import { PracticeSetService } from '../../services';
import { PaperService } from '@nag-frontend-workspace/examinations-data-access';
import {
  QuestionBankService,
  SubjectTopicService,
  Question,
} from '@nag-frontend-workspace/questions-data-access';

describe('PracticeSetFormModalComponent', () => {
  let component: PracticeSetFormModalComponent;
  let fixture: ComponentFixture<PracticeSetFormModalComponent>;
  let practiceSetServiceMock: any;
  let paperServiceMock: any;
  let questionBankServiceMock: any;
  let subjectTopicServiceMock: any;
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
      { questionId: 'q-uuid-1', text: 'Sample Question 1' },
      { questionId: 'q-uuid-2', text: 'Sample Question 2' },
    ],
    paperDefinitionJson: JSON.stringify({ questionIds: ['q-uuid-1', 'q-uuid-2'] }),
  };

  const mockBankQuestions: Question[] = [
    {
      id: 'bq-1',
      code: 'Q-MATH-01',
      content: 'What is the limit of sin(x)/x as x -> 0?',
      difficulty: 'EASY',
      status: 'APPROVED',
      subject: 'Mathematics',
      topic: 'Calculus',
      subtopic: 'Limits',
      type: 'SINGLE_MCQ',
      marks: 4,
      negativeMarks: 1,
      options: [],
      tags: ['calculus'],
    },
    {
      id: 'bq-2',
      code: 'Q-PHYS-01',
      content: 'State Newtons second law of motion in vector notation.',
      difficulty: 'MEDIUM',
      status: 'APPROVED',
      subject: 'Physics',
      topic: 'Mechanics',
      subtopic: 'Dynamics',
      type: 'SINGLE_MCQ',
      marks: 4,
      negativeMarks: 1,
      options: [],
      tags: ['mechanics'],
    },
    {
      id: 'bq-quant-1',
      code: 'Q-QA-01',
      content: 'A train 240 m long passes a pole in 24 seconds. Find speed of train in km/hr.',
      difficulty: 'MEDIUM',
      status: 'APPROVED',
      subject: 'Quantitative Aptitude / Mathematical Abilities',
      topic: 'Speed, Time and Distance',
      subtopic: 'Trains',
      type: 'SINGLE_MCQ',
      marks: 2,
      negativeMarks: 0.5,
      options: [],
      tags: ['speed', 'trains'],
    },
  ];

  beforeEach(async () => {
    practiceSetServiceMock = {
      create: jest.fn().mockReturnValue(of({ id: 'new-set-123', name: 'Test Set' })),
      update: jest.fn().mockReturnValue(of({ id: 'existing-set-123', name: 'Updated Set' })),
    };

    paperServiceMock = {
      getPapers: jest.fn().mockReturnValue(of({ content: mockPapers, totalElements: 2 })),
      getPaper: jest.fn().mockReturnValue(of(mockPaperDetail)),
    };

    questionBankServiceMock = {
      loadQuestions: jest.fn().mockReturnValue(
        of({ content: mockBankQuestions.slice(0, 2), totalElements: 2, totalPages: 1 })
      ),
      getQuestionsByIds: jest.fn().mockReturnValue(of([])),
    };

    subjectTopicServiceMock = {
      getSubjects: jest.fn().mockReturnValue(
        of([
          { id: 1, name: 'Mathematics' },
          { id: 2, name: 'Physics' },
          { id: 3, name: 'Quantitative Aptitude / Mathematical Abilities', code: 'QAMA' },
        ])
      ),
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
        { provide: QuestionBankService, useValue: questionBankServiceMock },
        { provide: SubjectTopicService, useValue: subjectTopicServiceMock },
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
    expect(questionBankServiceMock.loadQuestions).toHaveBeenCalled();
    expect(subjectTopicServiceMock.getSubjects).toHaveBeenCalled();
  });

  it('should load paper details and attach questions when paper is selected', () => {
    component.onPaperSelected('paper-1');
    expect(paperServiceMock.getPaper).toHaveBeenCalledWith('paper-1');
    expect(component.attachedQuestionIds()).toEqual(['q-uuid-1', 'q-uuid-2']);
    expect(component.totalQuestionsCount()).toBe(2);
    expect(component.form.get('name')?.value).toBe('Practice - NES 2026 Mathematics Practice');
    expect(component.canSubmit()).toBe(true);
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

  it('should switch to manual mode and support manual question selection', () => {
    component.onSourceTypeChange('MANUAL');
    expect(component.sourceType()).toBe('MANUAL');
    expect(component.attachedQuestionIds()).toEqual([]);

    // Select first question
    component.toggleQuestionSelection(mockBankQuestions[0]);
    expect(component.attachedQuestionIds()).toContain('bq-1');
    expect(component.isQuestionSelected('bq-1')).toBe(true);
    expect(component.totalQuestionsCount()).toBe(1);

    // Toggle off
    component.toggleQuestionSelection(mockBankQuestions[0]);
    expect(component.isQuestionSelected('bq-1')).toBe(false);
  });

  it('should support selectAllFiltered and clearSelectedQuestions in manual mode', () => {
    component.onSourceTypeChange('MANUAL');
    component.selectAllFiltered();
    expect(component.attachedQuestionIds()).toEqual(['bq-1', 'bq-2']);

    component.clearSelectedQuestions();
    expect(component.attachedQuestionIds()).toEqual([]);
  });

  it('should dynamically query database with selected subject filter and reset page', () => {
    questionBankServiceMock.loadQuestions.mockClear();

    component.onSubjectChange('Quantitative Aptitude / Mathematical Abilities');

    expect(component.selectedSubject()).toBe('Quantitative Aptitude / Mathematical Abilities');
    expect(component.bankPage()).toBe(0);
    expect(questionBankServiceMock.loadQuestions).toHaveBeenCalledWith({
      page: 0,
      size: 20,
      search: undefined,
      subject: 'Quantitative Aptitude / Mathematical Abilities',
      difficulty: undefined,
    });
  });

  it('should dynamically query database with selected difficulty filter and reset page', () => {
    questionBankServiceMock.loadQuestions.mockClear();

    component.onDifficultyChange('HARD');

    expect(component.selectedDifficulty()).toBe('HARD');
    expect(component.bankPage()).toBe(0);
    expect(questionBankServiceMock.loadQuestions).toHaveBeenCalledWith({
      page: 0,
      size: 20,
      search: undefined,
      subject: undefined,
      difficulty: 'HARD',
    });
  });

  it('should dynamically query database on debounced search query change', fakeAsync(() => {
    questionBankServiceMock.loadQuestions.mockClear();

    component.onSearchChange('trains');
    expect(component.questionSearchQuery()).toBe('trains');
    // Not called immediately before debounce
    expect(questionBankServiceMock.loadQuestions).not.toHaveBeenCalled();

    tick(300);

    expect(questionBankServiceMock.loadQuestions).toHaveBeenCalledWith({
      page: 0,
      size: 20,
      search: 'trains',
      subject: undefined,
      difficulty: undefined,
    });
  }));

  it('should paginate question bank queries from database', () => {
    component.bankTotalPages.set(5);
    questionBankServiceMock.loadQuestions.mockClear();

    component.onPageChange(2);

    expect(component.bankPage()).toBe(2);
    expect(questionBankServiceMock.loadQuestions).toHaveBeenCalledWith({
      page: 2,
      size: 20,
      search: undefined,
      subject: undefined,
      difficulty: undefined,
    });
  });

  it('should submit create request with curated questions and source MANUAL', () => {
    component.onSourceTypeChange('MANUAL');
    component.form.patchValue({
      name: 'Calculus Curation Set',
      durationMinutes: 45,
    });

    component.toggleQuestionSelection(mockBankQuestions[0]);
    component.onSubmit();

    expect(practiceSetServiceMock.create).toHaveBeenCalledWith({
      name: 'Calculus Curation Set',
      description: null,
      durationMinutes: 45,
      subjectSlug: 'mathematics',
      questionIds: ['bq-1'],
      source: 'MANUAL',
      totalQuestions: 1,
    });
    expect(dialogRefMock.close).toHaveBeenCalled();
  });

  it('should enable submit button when typing valid name manually', () => {
    expect(component.canSubmit()).toBe(false);
    component.form.patchValue({ name: 'Custom Practice Set' });
    expect(component.canSubmit()).toBe(true);
  });

  it('should close dialog on cancel', () => {
    component.onCancel();
    expect(dialogRefMock.close).toHaveBeenCalled();
  });
});

describe('PracticeSetFormModalComponent in Edit Mode with Quantitative Aptitude', () => {
  let component: PracticeSetFormModalComponent;
  let fixture: ComponentFixture<PracticeSetFormModalComponent>;
  let practiceSetServiceMock: any;
  let paperServiceMock: any;
  let questionBankServiceMock: any;
  let subjectTopicServiceMock: any;
  let dialogRefMock: any;

  const existingPracticeSet = {
    id: '22222222-2222-2222-2222-222222222222',
    name: 'Quantitative Aptitude Speed Practice',
    description: 'High-yield arithmetic, algebra, data interpretation, and speed math practice set.',
    durationMinutes: 60,
    subjectSlug: 'quantitative-aptitude',
    source: 'MANUAL',
    questionIds: ['bq-quant-1'],
    totalQuestions: 1,
    published: true,
  };

  const mockPreloadedQuestion: Question = {
    id: 'bq-quant-1',
    code: 'Q-QA-01',
    content: 'A train 240 m long passes a pole in 24 seconds.',
    difficulty: 'MEDIUM',
    status: 'APPROVED',
    subject: 'Quantitative Aptitude / Mathematical Abilities',
    topic: 'Speed, Time and Distance',
    subtopic: 'Trains',
    type: 'SINGLE_MCQ',
    marks: 2,
    negativeMarks: 0.5,
    options: [],
    tags: ['speed'],
  };

  beforeEach(async () => {
    practiceSetServiceMock = {
      update: jest.fn().mockReturnValue(of({ ...existingPracticeSet })),
    };

    paperServiceMock = {
      getPapers: jest.fn().mockReturnValue(of({ content: [], totalElements: 0 })),
      getPaper: jest.fn().mockReturnValue(of({})),
    };

    questionBankServiceMock = {
      loadQuestions: jest.fn().mockReturnValue(
        of({ content: [mockPreloadedQuestion], totalElements: 1, totalPages: 1 })
      ),
      getQuestionsByIds: jest.fn().mockReturnValue(of([mockPreloadedQuestion])),
    };

    subjectTopicServiceMock = {
      getSubjects: jest.fn().mockReturnValue(
        of([
          { id: 1, name: 'General Intelligence & Reasoning', code: 'GIR' },
          { id: 2, name: 'Quantitative Aptitude / Mathematical Abilities', code: 'QAMA' },
          { id: 3, name: 'General Awareness', code: 'GA' },
        ])
      ),
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
        { provide: QuestionBankService, useValue: questionBankServiceMock },
        { provide: SubjectTopicService, useValue: subjectTopicServiceMock },
        { provide: MatDialogRef, useValue: dialogRefMock },
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            set: existingPracticeSet,
            mode: 'edit',
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PracticeSetFormModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should auto-match subject and pre-filter questions from db for Quantitative Aptitude practice set', () => {
    expect(component.sourceType()).toBe('MANUAL');
    expect(component.selectedSubject()).toBe('Quantitative Aptitude / Mathematical Abilities');
    expect(questionBankServiceMock.loadQuestions).toHaveBeenCalledWith({
      page: 0,
      size: 20,
      search: undefined,
      subject: 'Quantitative Aptitude / Mathematical Abilities',
      difficulty: undefined,
    });
  });

  it('should preload existing attached questions by IDs and display them in selected list', () => {
    expect(questionBankServiceMock.getQuestionsByIds).toHaveBeenCalledWith(['bq-quant-1']);
    expect(component.attachedQuestionIds()).toEqual(['bq-quant-1']);

    const selected = component.selectedQuestionsList();
    expect(selected.length).toBe(1);
    expect(selected[0].code).toBe('Q-QA-01');
    expect(selected[0].content).toContain('A train 240 m long');
  });

  it('should submit update request on save changes', () => {
    component.onSubmit();

    expect(practiceSetServiceMock.update).toHaveBeenCalledWith(
      '22222222-2222-2222-2222-222222222222',
      {
        name: 'Quantitative Aptitude Speed Practice',
        description: 'High-yield arithmetic, algebra, data interpretation, and speed math practice set.',
        durationMinutes: 60,
        totalQuestions: 1,
        subjectSlug: 'quantitative-aptitude',
        questionIds: ['bq-quant-1'],
      }
    );
    expect(dialogRefMock.close).toHaveBeenCalled();
  });
});
