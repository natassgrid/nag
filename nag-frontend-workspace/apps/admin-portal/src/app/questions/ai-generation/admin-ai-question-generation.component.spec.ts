import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AdminAiQuestionGenerationComponent } from './admin-ai-question-generation.component';
import {
  QuestionAiService,
  QuestionBankService,
  SubjectTopicService,
} from '@nag-frontend-workspace/questions-data-access';

describe('AdminAiQuestionGenerationComponent', () => {
  let component: AdminAiQuestionGenerationComponent;
  let fixture: ComponentFixture<AdminAiQuestionGenerationComponent>;

  let mockAiService: jest.Mocked<Pick<QuestionAiService, 'generateQuestions' | 'listBatchJobs' | 'submitBatchJob' | 'cancelBatchJob'>>;
  let mockQuestionBankService: jest.Mocked<Pick<QuestionBankService, 'createQuestion'>>;
  let mockSubjectTopicService: jest.Mocked<Pick<SubjectTopicService, 'getSubjects' | 'getTopics' | 'getSubtopics'>>;

  const mockSubjects = [
    { id: 1, name: 'Mathematics' },
    { id: 2, name: 'Physics' },
  ];
  const mockTopics = [
    { id: 10, subjectId: 1, name: 'Algebra' },
    { id: 11, subjectId: 1, name: 'Calculus' },
  ];
  const mockSubtopics = [
    { id: 100, topicId: 10, name: 'Quadratic Equations' },
  ];

  beforeEach(async () => {
    mockAiService = {
      generateQuestions: jest.fn().mockReturnValue(of({
        questions: [], modelUsed: 'test', totalGenerated: 0, totalValid: 0, totalDuplicates: 0,
      })),
      listBatchJobs: jest.fn().mockReturnValue(of({ content: [], totalElements: 0 })),
      submitBatchJob: jest.fn().mockReturnValue(of({ id: 'job-1', status: 'PENDING', totalRequested: 0, totalGenerated: 0, totalFailed: 0, totalDuplicates: 0 })),
      cancelBatchJob: jest.fn().mockReturnValue(of({ id: 'job-1', status: 'CANCELLED', totalRequested: 0, totalGenerated: 0, totalFailed: 0, totalDuplicates: 0 })),
    };
    mockQuestionBankService = {
      createQuestion: jest.fn().mockReturnValue(of({ id: 'q-1' })),
    };
    mockSubjectTopicService = {
      getSubjects: jest.fn().mockReturnValue(of(mockSubjects)),
      getTopics: jest.fn().mockReturnValue(of(mockTopics)),
      getSubtopics: jest.fn().mockReturnValue(of(mockSubtopics)),
    };

    await TestBed.configureTestingModule({
      imports: [AdminAiQuestionGenerationComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: QuestionAiService, useValue: mockAiService },
        { provide: QuestionBankService, useValue: mockQuestionBankService },
        { provide: SubjectTopicService, useValue: mockSubjectTopicService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminAiQuestionGenerationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // ---------------------------------------------------------------------------
  // Rendering & initial state
  // ---------------------------------------------------------------------------

  it('should create and default to realtime tab', () => {
    expect(component).toBeTruthy();
    expect(component.activeTab()).toBe('realtime');
  });

  it('should switch to batch tab', () => {
    component.activeTab.set('batch');
    expect(component.activeTab()).toBe('batch');
  });

  // ---------------------------------------------------------------------------
  // Taxonomy loading on init
  // ---------------------------------------------------------------------------

  it('should load subjects from SubjectTopicService on init', () => {
    expect(mockSubjectTopicService.getSubjects).toHaveBeenCalled();
    expect(component.subjects()).toEqual(mockSubjects);
  });

  it('should show taxonomyError when getSubjects fails', () => {
    // Reset and re-call with error
    mockSubjectTopicService.getSubjects.mockReturnValue(throwError(() => ({ message: 'Failed to load subjects: Network error' })));
    component.loadTaxonomy();
    expect(component.taxonomyError()).toContain('Failed to load subjects');
    expect(component.taxonomyLoading()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // Cascading selects
  // ---------------------------------------------------------------------------

  it('should load topics when onSubjectChange is called', () => {
    component.onSubjectChange(mockSubjects[0]);
    expect(mockSubjectTopicService.getTopics).toHaveBeenCalledWith(1);
    expect(component.topics()).toEqual(mockTopics);
    expect(component.selectedSubject()?.id).toBe(1);
  });

  it('should reset topic and subtopic when subject changes', () => {
    // First select subject 1 with topic
    component.onSubjectChange(mockSubjects[0]);
    component.onTopicChange(mockTopics[0]);
    expect(component.selectedTopic()?.id).toBe(10);

    // Now change subject — topic/subtopic should reset
    component.onSubjectChange(mockSubjects[1]);
    expect(component.selectedTopic()).toBeNull();
    expect(component.topics()).toEqual(mockTopics); // new load for subject 2 would fire; mock returns same
  });

  it('should load subtopics when onTopicChange is called', () => {
    component.onSubjectChange(mockSubjects[0]);
    component.onTopicChange(mockTopics[0]);
    expect(mockSubjectTopicService.getSubtopics).toHaveBeenCalledWith(1, 10);
    expect(component.subtopics()).toEqual(mockSubtopics);
  });

  it('should set topicId in form when topic is selected', () => {
    component.onSubjectChange(mockSubjects[0]);
    component.onTopicChange(mockTopics[0]);
    expect(component.form.value.topicId).toBe(10);
  });

  // ---------------------------------------------------------------------------
  // generateQuestions - form validation & payload
  // ---------------------------------------------------------------------------

  it('should not call generateQuestions when form is invalid (missing topicId)', () => {
    component.form.patchValue({ subjectId: null, topicId: null });
    component.generateQuestions();
    expect(mockAiService.generateQuestions).not.toHaveBeenCalled();
  });

  it('should call generateQuestions with subjectId/topicId in payload', () => {
    component.onSubjectChange(mockSubjects[0]);
    component.onTopicChange(mockTopics[0]);
    component.form.patchValue({
      difficulty: 'HARD',
      cognitiveLevel: 'ANALYZE',
      questionType: 'SINGLE_MCQ',
      count: 2,
    });

    component.generateQuestions();

    expect(mockAiService.generateQuestions).toHaveBeenCalledWith(
      expect.objectContaining({
        subjectId: 1,
        topicId: 10,
        subject: 'Mathematics',
        topic: 'Algebra',
        difficulty: 'HARD',
      })
    );
  });

  it('should forward rawTextInput and description in generate request', () => {
    component.onSubjectChange(mockSubjects[0]);
    component.onTopicChange(mockTopics[0]);
    component.form.patchValue({
      rawTextInput: 'Find eigenvalues of 2x2 matrix',
      description: 'Focus on diagonalization',
    });

    component.generateQuestions();

    expect(mockAiService.generateQuestions).toHaveBeenCalledWith(
      expect.objectContaining({
        rawTextInput: 'Find eigenvalues of 2x2 matrix',
        description: 'Focus on diagonalization',
      })
    );
  });

  // ---------------------------------------------------------------------------
  // Auto-saved card marking
  // ---------------------------------------------------------------------------

  it('should mark questions with savedQuestionId as already saved after generation', () => {
    const responseWithAutoSaved = {
      questions: [
        { content: 'Q1', difficulty: 'HARD', cognitiveLevel: 'APPLY', questionType: 'SINGLE_MCQ', savedQuestionId: 'uuid-001' },
        { content: 'Q2', difficulty: 'EASY', cognitiveLevel: 'REMEMBER', questionType: 'SINGLE_MCQ' },
      ],
      modelUsed: 'test', totalGenerated: 2, totalValid: 2, totalDuplicates: 0,
    };
    mockAiService.generateQuestions.mockReturnValue(of(responseWithAutoSaved));

    component.onSubjectChange(mockSubjects[0]);
    component.onTopicChange(mockTopics[0]);
    component.generateQuestions();

    // Index 0 was auto-saved, index 1 was not
    expect(component.savedQuestionIds().has(0)).toBe(true);
    expect(component.savedQuestionIds().has(1)).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // saveQuestionToBank - payload correctness
  // ---------------------------------------------------------------------------

  it('should call createQuestion with subjectId, topicId, subtopicId and questionType', () => {
    component.onSubjectChange(mockSubjects[0]);
    component.onTopicChange(mockTopics[0]);
    component.form.patchValue({ subtopicId: 100, questionType: 'MULTI_MCQ' });

    const generatedQuestion = {
      content: 'Test question content',
      difficulty: 'MEDIUM',
      cognitiveLevel: 'APPLY',
      questionType: 'MULTI_MCQ',
      options: [],
    };

    component.saveQuestionToBank(generatedQuestion as any, 0);

    expect(mockQuestionBankService.createQuestion).toHaveBeenCalledWith(
      expect.objectContaining({
        subjectId: 1,
        topicId: 10,
        subtopicId: 100,
        questionType: 'MULTI_MCQ',
        content: 'Test question content',
      })
    );
  });

  it('should not call createQuestion again for an already-saved index', () => {
    component.savedQuestionIds.set(new Set([0]));
    const question = { content: 'Q', difficulty: 'EASY', cognitiveLevel: 'REMEMBER', questionType: 'SINGLE_MCQ' };
    component.saveQuestionToBank(question as any, 0);
    expect(mockQuestionBankService.createQuestion).not.toHaveBeenCalled();
  });

  it('should mark index as saved after successful createQuestion', () => {
    component.onSubjectChange(mockSubjects[0]);
    component.onTopicChange(mockTopics[0]);
    const question = { content: 'Q', difficulty: 'EASY', cognitiveLevel: 'REMEMBER', questionType: 'SINGLE_MCQ' };
    component.saveQuestionToBank(question as any, 3);
    expect(component.savedQuestionIds().has(3)).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // Batch
  // ---------------------------------------------------------------------------

  it('should add item to batch with subjectId/topicId when addCurrentToBatch is called', () => {
    component.onSubjectChange(mockSubjects[0]);
    component.onTopicChange(mockTopics[0]);
    component.form.patchValue({ difficulty: 'EASY', cognitiveLevel: 'REMEMBER', questionType: 'NUMERICAL', count: 1 });

    component.addCurrentToBatch();

    const items = component.batchItems();
    expect(items.length).toBe(1);
    expect(items[0].subjectId).toBe(1);
    expect(items[0].topicId).toBe(10);
    expect(items[0].subject).toBe('Mathematics');
    expect(items[0].topic).toBe('Algebra');
  });

  it('should remove batch item at correct index', () => {
    component.batchItems.set([
      { subject: 'A', topic: 'T', difficulty: 'EASY', cognitiveLevel: 'REMEMBER', questionType: 'SINGLE_MCQ', count: 1 },
      { subject: 'B', topic: 'T', difficulty: 'MEDIUM', cognitiveLevel: 'APPLY', questionType: 'MULTI_MCQ', count: 2 },
    ]);
    component.removeBatchItem(0);
    expect(component.batchItems().length).toBe(1);
    expect(component.batchItems()[0].subject).toBe('B');
  });

  // ---------------------------------------------------------------------------
  // openInAuthoring
  // ---------------------------------------------------------------------------

  it('should navigate to /questions/authoring with prefill state', () => {
    component.onSubjectChange(mockSubjects[0]);
    component.onTopicChange(mockTopics[0]);

    const mockRouter = { navigate: jest.fn() };
    (component as any).router = mockRouter;

    const question = { content: 'Q', difficulty: 'HARD', cognitiveLevel: 'EVALUATE', questionType: 'DESCRIPTIVE', answerKey: 'A' };
    component.openInAuthoring(question as any);

    expect(mockRouter.navigate).toHaveBeenCalledWith(
      ['/questions/authoring'],
      expect.objectContaining({
        state: expect.objectContaining({
          prefill: expect.objectContaining({
            content: 'Q',
            subjectId: 1,
            topicId: 10,
            subject: 'Mathematics',
            topic: 'Algebra',
          }),
        }),
      })
    );
  });
});
