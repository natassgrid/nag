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
  Question,
} from '@nag-frontend-workspace/questions-data-access';

describe('AdminQuestionTranslationComponent', () => {
  let component: AdminQuestionTranslationComponent;
  let fixture: ComponentFixture<AdminQuestionTranslationComponent>;
  let mockTranslationService: {
    listBatchJobs: jest.Mock;
    autoTranslate: jest.Mock;
    saveTranslation: jest.Mock;
    approveTranslation: jest.Mock;
    getApprovedTranslation: jest.Mock;
    resumeBatchJob: jest.Mock;
    cancelBatchJob: jest.Mock;
  };
  let mockQuestionBankService: {
    loadQuestions: jest.Mock;
  };
  let mockSubjectTopicService: {
    getSubjects: jest.Mock;
  };

  const sampleQuestion: Question = {
    id: 'q-1',
    code: 'Q-001',
    content: 'Sample Question Content',
    type: 'SINGLE_MCQ',
    difficulty: 'EASY',
    status: 'APPROVED',
    subject: 'Maths',
    marks: 4,
    negativeMarks: 1,
    options: [{ id: 'A', text: 'Option 1', isCorrect: true }],
    translationStatus: 'DRAFT',
    translationStatusMap: { hi: 'DRAFT' },
  };

  beforeEach(async () => {
    mockTranslationService = {
      listBatchJobs: jest.fn().mockReturnValue(of([])),
      autoTranslate: jest.fn().mockReturnValue(of({})),
      saveTranslation: jest.fn().mockReturnValue(of({ id: 'trans-1', status: 'DRAFT' })),
      approveTranslation: jest.fn().mockReturnValue(of({ success: true })),
      getApprovedTranslation: jest.fn().mockReturnValue(of(null)),
      resumeBatchJob: jest.fn().mockReturnValue(of({ id: 'job-1', status: 'IN_PROGRESS' })),
      cancelBatchJob: jest.fn().mockReturnValue(of({ id: 'job-1', status: 'CANCELLED' })),
    };
    mockQuestionBankService = {
      loadQuestions: jest.fn().mockReturnValue(of({ content: [sampleQuestion] })),
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

  it('should load questions with initial targetLang = hi', () => {
    expect(mockQuestionBankService.loadQuestions).toHaveBeenCalledWith(
      expect.objectContaining({
        page: 0,
        size: 100,
        targetLang: 'hi',
      })
    );
  });

  it('should reload questions when setLanguage is called', () => {
    mockQuestionBankService.loadQuestions.mockClear();
    component.setLanguage('ta');

    expect(component.selectedLanguage()).toBe('ta');
    expect(mockQuestionBankService.loadQuestions).toHaveBeenCalledWith(
      expect.objectContaining({
        targetLang: 'ta',
      })
    );
  });

  it('should reactively update question translation status on saveTranslation and approveCurrentTranslation', () => {
    component.openTranslationEditor(sampleQuestion);
    component.translatedContent.set('Hindi content');

    // Save as draft
    component.saveTranslation('DRAFT');
    const updatedDraft = component.questions().find((q) => q.id === 'q-1');
    expect(updatedDraft?.translationStatusMap?.['hi']).toBe('DRAFT');

    // Save as approved
    mockTranslationService.saveTranslation.mockReturnValue(of({ id: 'trans-1', status: 'APPROVED' }));
    component.approveCurrentTranslation();
    const updatedApproved = component.questions().find((q) => q.id === 'q-1');
    expect(updatedApproved?.translationStatusMap?.['hi']).toBe('APPROVED');
    expect(updatedApproved?.translationStatus).toBe('APPROVED');
  });

  it('should invoke resumeBatchJob on translationService and update batchJobs state', () => {
    component.batchJobs.set([
      { id: 'job-1', status: 'FAILED' } as any,
    ]);

    component.resumeJob('job-1');

    expect(mockTranslationService.resumeBatchJob).toHaveBeenCalledWith('job-1');
    const updated = component.batchJobs().find((j) => j.id === 'job-1');
    expect(updated?.status).toBe('IN_PROGRESS');
  });
});
