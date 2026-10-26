import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { QuestionsFeatureAuthoring } from './questions-feature-authoring';
import {
  QuestionBankService,
  PassageService,
  SubjectTopicService,
} from '@nag-frontend-workspace/questions-data-access';

/** Factory for a minimal DRAFT Question fixture. */
function makeDraftQuestion(overrides: Partial<Record<string, unknown>> = {}): Record<string, unknown> {
  return {
    id: 'q-edit-id',
    code: 'Q-EDITCODE',
    content: 'What is the speed of light?',
    type: 'SINGLE_MCQ',
    difficulty: 'MEDIUM',
    status: 'DRAFT',
    cognitiveLevel: 'REMEMBER',
    marks: 4,
    negativeMarks: 1,
    subjectId: '1',
    topicId: '10',
    subtopicId: '100',
    explanation: 'The speed of light is approximately 3e8 m/s.',
    options: [
      { id: 'A', text: '3e8 m/s', isCorrect: true },
      { id: 'B', text: '3e6 m/s', isCorrect: false },
      { id: 'C', text: '3e4 m/s', isCorrect: false },
      { id: 'D', text: '3e2 m/s', isCorrect: false },
    ],
    answerKey: 'A',
    ...overrides,
  };
}

/** Helper: create an ActivatedRoute stub that returns the given query params. */
function makeActivatedRouteStub(queryParams: Record<string, string> = {}) {
  return {
    snapshot: {
      queryParamMap: {
        get: (key: string) => queryParams[key] ?? null,
      },
    },
  };
}

describe('QuestionsFeatureAuthoring', () => {
  let component: QuestionsFeatureAuthoring;
  let fixture: ComponentFixture<QuestionsFeatureAuthoring>;
  let questionBankMock: {
    createQuestion: jest.Mock;
    updateQuestion: jest.Mock;
    getQuestionById: jest.Mock;
  };
  let passageServiceMock: {
    createPassage: jest.Mock;
  };
  let subjectTopicServiceMock: {
    getSubjects: jest.Mock;
    getTopics: jest.Mock;
    getSubtopics: jest.Mock;
  };

  /** Re-compile TestBed with the given ActivatedRoute stub. */
  async function compileWithRoute(queryParams: Record<string, string> = {}) {
    await TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule,
        RouterTestingModule,
        QuestionsFeatureAuthoring,
      ],
      providers: [
        { provide: QuestionBankService, useValue: questionBankMock },
        { provide: PassageService, useValue: passageServiceMock },
        { provide: SubjectTopicService, useValue: subjectTopicServiceMock },
        { provide: ActivatedRoute, useValue: makeActivatedRouteStub(queryParams) },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(QuestionsFeatureAuthoring);
    component = fixture.componentInstance;
  }

  beforeEach(() => {
    questionBankMock = {
      createQuestion: jest.fn().mockReturnValue(of({ id: 'q-new', code: 'Q-12345678' })),
      updateQuestion: jest.fn().mockReturnValue(of({ id: 'q-edit-id', code: 'Q-EDITCODE' })),
      getQuestionById: jest.fn().mockReturnValue(of(makeDraftQuestion())),
    };

    passageServiceMock = {
      createPassage: jest.fn().mockReturnValue(of({ id: 'p-new' })),
    };

    subjectTopicServiceMock = {
      getSubjects: jest.fn().mockReturnValue(of([{ id: 1, name: 'Physics', code: 'PHY' }])),
      getTopics: jest.fn().mockReturnValue(of([{ id: 10, name: 'Mechanics', code: 'MECH' }])),
      getSubtopics: jest.fn().mockReturnValue(of([{ id: 100, name: 'Kinematics', code: 'KIN' }])),
    };
  });

  afterEach(() => TestBed.resetTestingModule());

  // ── CREATE MODE ───────────────────────────────────────────────────────────

  it('should create and load taxonomy on init (create mode)', async () => {
    await compileWithRoute();
    fixture.detectChanges();
    expect(component).toBeTruthy();
    expect(subjectTopicServiceMock.getSubjects).toHaveBeenCalled();
    expect(component.subjects().length).toBe(1);
    expect(component.editingQuestionId()).toBeNull();
    expect(questionBankMock.getQuestionById).not.toHaveBeenCalled();
  });

  it('should switch authoring mode between standalone and passage', async () => {
    await compileWithRoute();
    expect(component.mode()).toBe('STANDALONE');
    component.setMode('PASSAGE');
    expect(component.mode()).toBe('PASSAGE');
    component.setMode('STANDALONE');
    expect(component.mode()).toBe('STANDALONE');
  });

  it('should handle subject change and load topics', async () => {
    await compileWithRoute();
    component.onSubjectChange(1);
    expect(component.selectedSubjectId).toBe(1);
    expect(subjectTopicServiceMock.getTopics).toHaveBeenCalledWith(1);
  });

  it('should show validation error when saving standalone question without subject', async () => {
    await compileWithRoute();
    component.selectedSubjectId = null;
    component.content = 'What is acceleration due to gravity?';
    component.saveItem();
    expect(component.feedback()).toEqual({
      type: 'error',
      message: 'Please select a Subject from the taxonomy hierarchy before saving.',
    });
    expect(questionBankMock.createQuestion).not.toHaveBeenCalled();
  });

  it('should show validation error when saving standalone question with empty content', async () => {
    await compileWithRoute();
    component.selectedSubjectId = 1;
    component.selectedTopicId = 10;
    component.selectedSubtopicId = 100;
    component.content = '   ';
    component.saveItem();
    expect(component.feedback()).toEqual({
      type: 'error',
      message: 'Question problem statement cannot be empty.',
    });
    expect(questionBankMock.createQuestion).not.toHaveBeenCalled();
  });

  it('should create standalone question with questionType, type, and state=DRAFT', async () => {
    await compileWithRoute();
    component.selectedSubjectId = 1;
    component.selectedTopicId = 10;
    component.selectedSubtopicId = 100;
    component.content = 'What is the speed of light? $c = 3 \\times 10^8 \\text{ m/s}$';
    component.type = 'MULTIPLE_CHOICE';
    component.options = [
      { id: 'A', text: '3 x 10^8 m/s', isCorrect: true },
      { id: 'B', text: '3 x 10^6 m/s', isCorrect: false },
    ];
    component.saveItem();

    expect(questionBankMock.createQuestion).toHaveBeenCalledTimes(1);
    const sentPayload = questionBankMock.createQuestion.mock.calls[0][0];
    expect(sentPayload.questionType).toBe('SINGLE_MCQ');
    expect(sentPayload.type).toBe('SINGLE_MCQ');
    expect(sentPayload.state).toBe('DRAFT');
    expect(sentPayload.status).toBe('DRAFT');
    expect(component.feedback()?.type).toBe('success');
  });

  it('should create passage set with questionType and state=DRAFT for each sub-question', async () => {
    await compileWithRoute();
    component.setMode('PASSAGE');
    component.selectedSubjectId = 1;
    component.passageContent = 'A particle moves along a straight line with uniform acceleration...';
    component.subQuestions = [
      {
        passageOrderIndex: 1,
        content: 'Find initial velocity',
        questionType: 'MULTIPLE_CHOICE',
        difficulty: 'MEDIUM',
        cognitiveLevel: 'APPLY',
        marks: 4,
        negativeMarks: 1,
        explanation: 'Using v = u + at',
        options: [
          { id: 'A', text: '10 m/s', isCorrect: true },
          { id: 'B', text: '20 m/s', isCorrect: false },
        ],
      },
      {
        passageOrderIndex: 2,
        content: 'Find distance traversed',
        questionType: 'MULTIPLE_CHOICE',
        difficulty: 'MEDIUM',
        cognitiveLevel: 'APPLY',
        marks: 4,
        negativeMarks: 1,
        explanation: 'Using s = ut + 0.5at^2',
        options: [
          { id: 'A', text: '50 m', isCorrect: true },
          { id: 'B', text: '100 m', isCorrect: false },
        ],
      },
    ];

    component.saveItem();

    expect(passageServiceMock.createPassage).toHaveBeenCalledTimes(1);
    const sentPassage = passageServiceMock.createPassage.mock.calls[0][0];
    expect(sentPassage.state).toBe('DRAFT');
    expect(sentPassage.status).toBe('DRAFT');
    expect(sentPassage.subQuestions.length).toBe(2);
    expect(sentPassage.subQuestions[0].questionType).toBe('SINGLE_MCQ');
    expect(sentPassage.subQuestions[0].state).toBe('DRAFT');
    expect(sentPassage.subQuestions[1].questionType).toBe('SINGLE_MCQ');
    expect(sentPassage.subQuestions[1].state).toBe('DRAFT');
  });

  // ── EDIT MODE ─────────────────────────────────────────────────────────────

  it('should load existing question by ID on init when edit query param is present', async () => {
    await compileWithRoute({ edit: 'q-edit-id' });
    fixture.detectChanges();

    expect(component.editingQuestionId()).toBe('q-edit-id');
    expect(questionBankMock.getQuestionById).toHaveBeenCalledWith('q-edit-id');
  });

  it('should populate form fields with fetched question data', async () => {
    await compileWithRoute({ edit: 'q-edit-id' });
    fixture.detectChanges();

    expect(component.content).toBe('What is the speed of light?');
    expect(component.explanation).toBe('The speed of light is approximately 3e8 m/s.');
    // SINGLE_MCQ maps to the UI token MULTIPLE_CHOICE
    expect(component.type).toBe('MULTIPLE_CHOICE');
    expect(component.difficulty).toBe('MEDIUM');
    expect(component.cognitiveLevel).toBe('REMEMBER');
    expect(component.marks).toBe(4);
    expect(component.negativeMarks).toBe(1);
    expect(component.selectedSubjectId).toBe(1);
    expect(component.selectedTopicId).toBe(10);
    expect(component.selectedSubtopicId).toBe(100);
    expect(component.options.length).toBe(4);
    expect(component.options[0].isCorrect).toBe(true);
  });

  it('should call updateQuestion (PUT) when saving in edit mode', async () => {
    await compileWithRoute({ edit: 'q-edit-id' });
    fixture.detectChanges();

    // Ensure form is valid
    component.selectedSubjectId = 1;
    component.content = 'Updated question content';
    component.saveItem();

    expect(questionBankMock.updateQuestion).toHaveBeenCalledTimes(1);
    expect(questionBankMock.updateQuestion).toHaveBeenCalledWith(
      'q-edit-id',
      expect.objectContaining({ content: 'Updated question content' })
    );
    expect(questionBankMock.createQuestion).not.toHaveBeenCalled();
    expect(component.feedback()?.type).toBe('success');
    expect(component.feedback()?.message).toContain('updated');
  });

  it('should reject editing PUBLISHED questions with an error message', async () => {
    questionBankMock.getQuestionById.mockReturnValue(
      of(makeDraftQuestion({ status: 'PUBLISHED' }))
    );
    await compileWithRoute({ edit: 'q-edit-id' });
    fixture.detectChanges();

    expect(component.editingQuestionId()).toBeNull();
    expect(component.feedback()?.type).toBe('error');
    expect(component.feedback()?.message).toContain('PUBLISHED');
  });

  it('should apply imported question to UI without saving to bank', async () => {
    await compileWithRoute();
    fixture.detectChanges();

    const imported = {
      content: 'Imported question problem statement with $$E = mc^2$$',
      explanation: 'Imported explanation',
      type: 'MULTIPLE_CHOICE' as any,
      difficulty: 'HARD' as any,
      cognitiveLevel: 'APPLY',
      marks: 4,
      negativeMarks: 1,
      options: [
        { id: 'A', text: 'Option 1', isCorrect: true },
        { id: 'B', text: 'Option 2', isCorrect: false },
        { id: 'C', text: 'Option 3', isCorrect: false },
        { id: 'D', text: 'Option 4', isCorrect: false },
      ],
      answerKey: 'A',
    };

    component.applyImportedQuestion(imported);

    expect(component.content).toBe(imported.content);
    expect(component.explanation).toBe(imported.explanation);
    expect(component.difficulty).toBe('HARD');
    expect(component.cognitiveLevel).toBe('APPLY');
    expect(component.options.length).toBe(4);
    expect(questionBankMock.createQuestion).not.toHaveBeenCalled();
    expect(component.feedback()?.type).toBe('success');
    expect(component.feedback()?.message).toContain('loaded into authoring form');
  });

  it('should pass selected taxonomy when opening prompt import dialog and sync back on close', async () => {
    subjectTopicServiceMock.getSubjects.mockReturnValue(
      of([
        { id: 1, name: 'Physics', code: 'PHY' },
        { id: 2, name: 'Chemistry', code: 'CHEM' },
      ])
    );
    subjectTopicServiceMock.getTopics.mockImplementation((subjId: number) => {
      if (subjId === 2) {
        return of([{ id: 20, name: 'Organic Chemistry', code: 'ORG' }]);
      }
      return of([{ id: 10, name: 'Mechanics', code: 'MECH' }]);
    });
    subjectTopicServiceMock.getSubtopics.mockImplementation((_subjId: number, topId: number) => {
      if (topId === 20) {
        return of([{ id: 200, name: 'Alkanes', code: 'ALK' }]);
      }
      return of([{ id: 100, name: 'Kinematics', code: 'KIN' }]);
    });

    await compileWithRoute();
    fixture.detectChanges();

    component.selectedSubjectId = 1;
    component.selectedTopicId = 10;
    component.selectedSubtopicId = 100;

    const afterClosedSubject = of({
      action: 'close',
      taxonomy: {
        subjectId: 2,
        topicId: 20,
        subtopicId: 200,
        subjectName: 'Chemistry',
        topicName: 'Organic Chemistry',
        subtopicName: 'Alkanes',
        isCustomSubject: false,
        isCustomTopic: false,
        isCustomSubtopic: false,
      },
    });

    const openSpy = jest.spyOn((component as any).dialog, 'open').mockReturnValue({
      afterClosed: () => afterClosedSubject,
    } as any);

    component.openAiPromptImportDialog('prompt');

    expect(openSpy).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        data: expect.objectContaining({
          subjectId: 1,
          topicId: 10,
          subtopicId: 100,
        }),
      })
    );

    // Verify parent taxonomy updated
    expect(component.selectedSubjectId).toBe(2);
    expect(component.selectedTopicId).toBe(20);
    expect(component.selectedSubtopicId).toBe(200);
  });
});
