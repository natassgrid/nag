import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';
import { QuestionsFeatureAuthoring } from './questions-feature-authoring';
import {
  QuestionBankService,
  PassageService,
  SubjectTopicService,
} from '@nag-frontend-workspace/questions-data-access';

describe('QuestionsFeatureAuthoring', () => {
  let component: QuestionsFeatureAuthoring;
  let fixture: ComponentFixture<QuestionsFeatureAuthoring>;
  let questionBankMock: {
    createQuestion: jest.Mock;
  };
  let passageServiceMock: {
    createPassage: jest.Mock;
  };
  let subjectTopicServiceMock: {
    getSubjects: jest.Mock;
    getTopics: jest.Mock;
    getSubtopics: jest.Mock;
  };

  beforeEach(async () => {
    questionBankMock = {
      createQuestion: jest.fn().mockReturnValue(of({ id: 'q-new', code: 'Q-12345678' })),
    };

    passageServiceMock = {
      createPassage: jest.fn().mockReturnValue(of({ id: 'p-new' })),
    };

    subjectTopicServiceMock = {
      getSubjects: jest.fn().mockReturnValue(of([{ id: 1, name: 'Physics', code: 'PHY' }])),
      getTopics: jest.fn().mockReturnValue(of([{ id: 10, name: 'Mechanics', code: 'MECH' }])),
      getSubtopics: jest.fn().mockReturnValue(of([{ id: 100, name: 'Kinematics', code: 'KIN' }])),
    };

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
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(QuestionsFeatureAuthoring);
    component = fixture.componentInstance;
  });

  it('should create and load taxonomy on init', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
    expect(subjectTopicServiceMock.getSubjects).toHaveBeenCalled();
    expect(component.subjects().length).toBe(1);
  });

  it('should switch authoring mode between standalone and passage', () => {
    expect(component.mode()).toBe('STANDALONE');
    component.setMode('PASSAGE');
    expect(component.mode()).toBe('PASSAGE');
    component.setMode('STANDALONE');
    expect(component.mode()).toBe('STANDALONE');
  });

  it('should handle subject change and load topics', () => {
    component.onSubjectChange(1);
    expect(component.selectedSubjectId).toBe(1);
    expect(subjectTopicServiceMock.getTopics).toHaveBeenCalledWith(1);
  });

  it('should show validation error when saving standalone question without subject', () => {
    component.selectedSubjectId = null;
    component.content = 'What is acceleration due to gravity?';
    component.saveItem();
    expect(component.feedback()).toEqual({
      type: 'error',
      message: 'Please select a Subject from the taxonomy hierarchy before saving.',
    });
    expect(questionBankMock.createQuestion).not.toHaveBeenCalled();
  });

  it('should show validation error when saving standalone question with empty content', () => {
    component.selectedSubjectId = 1;
    component.content = '   ';
    component.saveItem();
    expect(component.feedback()).toEqual({
      type: 'error',
      message: 'Question problem statement cannot be empty.',
    });
    expect(questionBankMock.createQuestion).not.toHaveBeenCalled();
  });

  it('should create standalone question with questionType, type, and state=DRAFT', () => {
    component.selectedSubjectId = 1;
    component.selectedTopicId = 10;
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

  it('should create passage set with questionType and state=DRAFT for each sub-question', () => {
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
});
