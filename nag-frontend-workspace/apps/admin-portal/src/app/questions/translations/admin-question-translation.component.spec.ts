import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AdminQuestionTranslationComponent } from './admin-question-translation.component';
import {
  TranslationService,
  QuestionBankService,
  SubjectTopicService,
} from '@nag-frontend-workspace/questions-data-access';

describe('AdminQuestionTranslationComponent', () => {
  let component: AdminQuestionTranslationComponent;
  let fixture: ComponentFixture<AdminQuestionTranslationComponent>;
  let mockTranslationService: {
    listBatchJobs: jest.Mock;
    autoTranslate: jest.Mock;
  };
  let mockQuestionBankService: {
    loadQuestions: jest.Mock;
  };
  let mockSubjectTopicService: {
    getSubjects: jest.Mock;
  };

  beforeEach(async () => {
    mockTranslationService = {
      listBatchJobs: jest.fn().mockReturnValue(of([])),
      autoTranslate: jest.fn().mockReturnValue(of({})),
    };
    mockQuestionBankService = {
      loadQuestions: jest.fn().mockReturnValue(of({ content: [] })),
    };
    mockSubjectTopicService = {
      getSubjects: jest.fn().mockReturnValue(of([])),
    };

    await TestBed.configureTestingModule({
      imports: [AdminQuestionTranslationComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: TranslationService, useValue: mockTranslationService },
        { provide: QuestionBankService, useValue: mockQuestionBankService },
        { provide: SubjectTopicService, useValue: mockSubjectTopicService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminQuestionTranslationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should render question translation management', () => {
    expect(component).toBeTruthy();
  });
});
