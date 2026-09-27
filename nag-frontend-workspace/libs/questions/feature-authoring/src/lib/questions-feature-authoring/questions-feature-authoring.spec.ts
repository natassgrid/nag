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
      createQuestion: jest.fn().mockReturnValue(of({ id: 'q-new' })),
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
});
