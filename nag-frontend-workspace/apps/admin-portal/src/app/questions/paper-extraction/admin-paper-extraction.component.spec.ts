import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AdminPaperExtractionComponent } from './admin-paper-extraction.component';
import { PaperExtractionService } from './services';
import { NotificationService } from '@nag-frontend-workspace/shared-ui-components';
import { SubjectTopicService } from '@nag-frontend-workspace/questions-data-access';
import { QuestionPaperMetadata, ExtractedQuestion } from './models';

describe('AdminPaperExtractionComponent', () => {
  let component: AdminPaperExtractionComponent;
  let fixture: ComponentFixture<AdminPaperExtractionComponent>;

  let mockPaperService: {
    uploadPaper: jest.Mock;
    listPapers: jest.Mock;
    getPaper: jest.Mock;
    cancelJob: jest.Mock;
    retryJob: jest.Mock;
    applyAnswerKey: jest.Mock;
    listExtractedQuestions: jest.Mock;
    updateExtractedQuestion: jest.Mock;
    promoteQuestions: jest.Mock;
  };

  let mockNotificationService: {
    success: jest.Mock;
    error: jest.Mock;
    info: jest.Mock;
    warning: jest.Mock;
  };

  let mockSubjectTopicService: {
    getSubjects: jest.Mock;
  };

  const samplePaper: QuestionPaperMetadata = {
    id: 'paper-1',
    examinationName: 'UPSC Prelims GS-I',
    examYear: 2026,
    department: 'UPSC',
    paperCode: 'GS-101',
    totalPages: 24,
    pagesProcessed: 24,
    questionsExtracted: 100,
    questionsApproved: 95,
    questionsPromoted: 0,
    extractionStatus: 'COMPLETED',
    progressPercentage: 100,
  };

  const sampleExtractedQuestion: ExtractedQuestion = {
    id: 'eq-1',
    paperMetadataId: 'paper-1',
    questionNumber: 1,
    sourcePageNumber: 2,
    questionType: 'SINGLE_MCQ',
    content: 'Which of the following is correct regarding quantum computing?',
    options: [
      { id: 'A', text: 'Option A', correct: true },
      { id: 'B', text: 'Option B', correct: false },
      { id: 'C', text: 'Option C', correct: false },
      { id: 'D', text: 'Option D', correct: false },
    ],
    answerKey: 'A',
    status: 'APPROVED',
  };

  beforeEach(async () => {
    mockPaperService = {
      uploadPaper: jest.fn().mockReturnValue(of(samplePaper)),
      initiateExtraction: jest.fn().mockReturnValue(of(samplePaper)),
      listPapers: jest.fn().mockReturnValue(of({ content: [samplePaper], totalElements: 1 })),
      getPaper: jest.fn().mockReturnValue(of(samplePaper)),
      cancelJob: jest.fn().mockReturnValue(of(samplePaper)),
      retryJob: jest.fn().mockReturnValue(of(samplePaper)),
      applyAnswerKey: jest.fn().mockReturnValue(of(samplePaper)),
      listExtractedQuestions: jest.fn().mockReturnValue(of({ content: [sampleExtractedQuestion], totalElements: 1 })),
      updateExtractedQuestion: jest.fn().mockReturnValue(of(sampleExtractedQuestion)),
      promoteQuestions: jest.fn().mockReturnValue(of({
        paperMetadataId: 'paper-1',
        totalRequested: 1,
        totalPromoted: 1,
        totalSkipped: 0,
        totalFailed: 0,
        promotedQuestionIds: ['eq-1'],
      })),
    };

    mockNotificationService = {
      success: jest.fn(),
      error: jest.fn(),
      info: jest.fn(),
      warning: jest.fn(),
    };

    mockSubjectTopicService = {
      getSubjects: jest.fn().mockReturnValue(of([{ id: 1, name: 'General Studies' }])),
    };

    const mockAssetService = {
      upload: jest.fn().mockReturnValue(of({ id: 'asset-uuid-1', originalFilename: 'paper.pdf' })),
    };

    await TestBed.configureTestingModule({
      imports: [AdminPaperExtractionComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: PaperExtractionService, useValue: mockPaperService },
        { provide: NotificationService, useValue: mockNotificationService },
        { provide: SubjectTopicService, useValue: mockSubjectTopicService },
        { provide: require('../../assets/asset.service').AssetService, useValue: mockAssetService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminPaperExtractionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load initial papers and subjects', () => {
    expect(component).toBeTruthy();
    expect(mockPaperService.listPapers).toHaveBeenCalled();
    expect(mockSubjectTopicService.getSubjects).toHaveBeenCalled();
    expect(component.papers().length).toBe(1);
    expect(component.availableSubjects()).toEqual(['General Studies']);
  });

  it('should compute KPI stats accurately', () => {
    const kpis = component.kpiStats();
    expect(kpis.totalPapers).toBe(1);
    expect(kpis.pagesProcessed).toBe(24);
    expect(kpis.questionsExtracted).toBe(100);
    expect(kpis.questionsApproved).toBe(95);
    expect(kpis.questionsPromoted).toBe(0);
  });

  it('should transition to detail view when a paper is selected', () => {
    component.handleSelectPaper(samplePaper);
    expect(component.viewMode()).toBe('detail');
    expect(component.selectedPaper()).toBe(samplePaper);
    expect(mockPaperService.listExtractedQuestions).toHaveBeenCalledWith('paper-1', expect.anything());
    expect(component.extractedQuestions().length).toBe(1);
  });

  it('should transition back to list view from detail view', () => {
    component.handleSelectPaper(samplePaper);
    expect(component.viewMode()).toBe('detail');

    component.handleBackToList();
    expect(component.viewMode()).toBe('list');
    expect(component.selectedPaper()).toBeNull();
    expect(mockPaperService.listPapers).toHaveBeenCalledTimes(2);
  });

  it('should handle paper upload and notify success', () => {
    const dummyFile = new File(['dummy content'], 'paper.pdf', { type: 'application/pdf' });
    component.handleUpload({ file: dummyFile, metadata: { examinationName: 'UPSC Prelims GS-I', examYear: 2026 } });

    expect(mockPaperService.initiateExtraction).toHaveBeenCalledWith(expect.objectContaining({
      examinationName: 'UPSC Prelims GS-I',
      examYear: 2026,
      fileAssetId: 'asset-uuid-1',
    }));
    expect(mockNotificationService.success).toHaveBeenCalledWith(expect.stringContaining('initiated successfully'));
  });

  it('should handle paper cancellation', () => {
    component.handleCancelJob(samplePaper);
    expect(mockPaperService.cancelJob).toHaveBeenCalledWith('paper-1');
    expect(mockNotificationService.info).toHaveBeenCalledWith(expect.stringContaining('cancelled'));
  });

  it('should handle paper retry', () => {
    component.handleRetryJob(samplePaper);
    expect(mockPaperService.retryJob).toHaveBeenCalledWith('paper-1');
    expect(mockNotificationService.success).toHaveBeenCalledWith(expect.stringContaining('restarted'));
  });

  it('should promote questions and report outcome', () => {
    component.handleSelectPaper(samplePaper);
    component.handlePromoteApproved({ paperId: 'paper-1' });

    expect(mockPaperService.promoteQuestions).toHaveBeenCalledWith('paper-1', expect.anything());
    expect(mockNotificationService.success).toHaveBeenCalledWith(expect.stringContaining('Promotion Complete: 1 questions promoted'));
  });
});
