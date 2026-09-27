import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AdminAiQuestionGenerationComponent } from './admin-ai-question-generation.component';
import {
  QuestionAiService,
  QuestionBankService,
} from '@nag-frontend-workspace/questions-data-access';

describe('AdminAiQuestionGenerationComponent', () => {
  let component: AdminAiQuestionGenerationComponent;
  let fixture: ComponentFixture<AdminAiQuestionGenerationComponent>;
  let mockAiService: {
    generateQuestions: jest.Mock;
    listBatchJobs: jest.Mock;
  };
  let mockQuestionBankService: {
    createQuestion: jest.Mock;
  };

  beforeEach(async () => {
    mockAiService = {
      generateQuestions: jest.fn().mockReturnValue(of({ questions: [] })),
      listBatchJobs: jest.fn().mockReturnValue(of({ content: [] })),
    };
    mockQuestionBankService = {
      createQuestion: jest.fn().mockReturnValue(of({})),
    };

    await TestBed.configureTestingModule({
      imports: [AdminAiQuestionGenerationComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: QuestionAiService, useValue: mockAiService },
        { provide: QuestionBankService, useValue: mockQuestionBankService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminAiQuestionGenerationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should render AI generation view and default to realtime tab', () => {
    expect(component).toBeTruthy();
    expect(component.activeTab()).toBe('realtime');
  });

  it('should switch tabs between realtime and batch', () => {
    component.activeTab.set('batch');
    expect(component.activeTab()).toBe('batch');
  });
});
