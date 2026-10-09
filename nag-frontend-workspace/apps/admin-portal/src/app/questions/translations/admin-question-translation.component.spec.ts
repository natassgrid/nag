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
    createdAt: '2026-10-09T08:00:00Z',
    updatedAt: '2026-10-09T09:00:00Z',
  };

  const sampleQuestion2: Question = {
    id: 'q-2',
    code: 'Q-002',
    content: 'Sample Question 2',
    type: 'SINGLE_MCQ',
    difficulty: 'HARD',
    status: 'APPROVED',
    subject: 'Physics',
    marks: 4,
    negativeMarks: 1,
    options: [{ id: 'A', text: 'Option 1', isCorrect: true }],
    translationStatus: 'DRAFT',
    translationStatusMap: { hi: 'DRAFT' },
    createdAt: '2026-10-09T07:00:00Z',
    updatedAt: '2026-10-09T10:00:00Z',
  };

  beforeEach(async () => {
    mockTranslationService = {
      listBatchJobs: jest.fn().mockReturnValue(of({ content: [], totalElements: 0 })),
      autoTranslate: jest.fn().mockReturnValue(of({})),
      saveTranslation: jest.fn().mockReturnValue(of({ id: 'trans-1', status: 'DRAFT' })),
      approveTranslation: jest.fn().mockReturnValue(of({ success: true })),
      getApprovedTranslation: jest.fn().mockReturnValue(of(null)),
      resumeBatchJob: jest.fn().mockReturnValue(of({ id: 'job-1', status: 'IN_PROGRESS' })),
      cancelBatchJob: jest.fn().mockReturnValue(of({ id: 'job-1', status: 'CANCELLED' })),
    };
    mockQuestionBankService = {
      loadQuestions: jest.fn().mockReturnValue(of({ content: [sampleQuestion], totalElements: 1 })),
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

  it('should load questions with initial targetLang = hi and recent-first sorting', () => {
    expect(mockQuestionBankService.loadQuestions).toHaveBeenCalledWith(
      expect.objectContaining({
        page: 0,
        size: 20,
        targetLang: 'hi',
        sort: 'updatedAt',
        order: 'desc',
      })
    );
  });

  it('should reload questions and reset to page 0 when setLanguage is called', () => {
    component.questionsPage.set(2);
    mockQuestionBankService.loadQuestions.mockClear();
    component.setLanguage('ta');

    expect(component.selectedLanguage()).toBe('ta');
    expect(component.questionsPage()).toBe(0);
    expect(mockQuestionBankService.loadQuestions).toHaveBeenCalledWith(
      expect.objectContaining({
        page: 0,
        targetLang: 'ta',
      })
    );
  });

  it('should reset page to 0 when search query or subject filter changes', () => {
    component.questionsPage.set(3);
    mockQuestionBankService.loadQuestions.mockClear();

    component.onSearchChange('Newton');
    expect(component.questionsPage()).toBe(0);
    expect(component.searchQuery()).toBe('Newton');
    expect(mockQuestionBankService.loadQuestions).toHaveBeenCalledWith(
      expect.objectContaining({
        page: 0,
        search: 'Newton',
      })
    );

    component.questionsPage.set(2);
    mockQuestionBankService.loadQuestions.mockClear();

    component.onSubjectChange('Physics');
    expect(component.questionsPage()).toBe(0);
    expect(component.selectedSubject()).toBe('Physics');
    expect(mockQuestionBankService.loadQuestions).toHaveBeenCalledWith(
      expect.objectContaining({
        page: 0,
        subject: 'Physics',
      })
    );
  });

  it('should handle questions pagination changes', () => {
    mockQuestionBankService.loadQuestions.mockClear();

    component.onQuestionsPageChange({ pageIndex: 2, pageSize: 50 });
    expect(component.questionsPage()).toBe(2);
    expect(component.questionsPageSize()).toBe(50);
    expect(mockQuestionBankService.loadQuestions).toHaveBeenCalledWith(
      expect.objectContaining({
        page: 2,
        size: 50,
      })
    );
  });

  it('should sort questions recent-first (updatedAt DESC falling back to createdAt DESC)', () => {
    component.questions.set([sampleQuestion, sampleQuestion2]);
    const displayed = component.displayedQuestions();
    // sampleQuestion2 updatedAt is 10:00, sampleQuestion is 09:00
    expect(displayed[0].id).toBe('q-2');
    expect(displayed[1].id).toBe('q-1');
  });

  it('should handle batch jobs pagination and reload jobs with page parameters', () => {
    mockTranslationService.listBatchJobs.mockClear();

    component.onBatchJobsPageChange({ pageIndex: 1, pageSize: 50 });
    expect(component.batchJobsPage()).toBe(1);
    expect(component.batchJobsPageSize()).toBe(50);
    expect(mockTranslationService.listBatchJobs).toHaveBeenCalledWith(1, 50);
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
