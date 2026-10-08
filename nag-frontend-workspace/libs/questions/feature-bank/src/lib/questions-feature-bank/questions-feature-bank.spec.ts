import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';
import { QuestionsFeatureBank } from './questions-feature-bank';
import {
  QuestionBankService,
  SubjectTopicService,
} from '@nag-frontend-workspace/questions-data-access';
import { NotificationService } from '@nag-frontend-workspace/shared-ui-components';

describe('QuestionsFeatureBank', () => {
  let component: QuestionsFeatureBank;
  let fixture: ComponentFixture<QuestionsFeatureBank>;
  let questionServiceMock: {
    questions: any;
    total: any;
    totalPages: any;
    currentPage: any;
    pageSize: any;
    loading: any;
    filter: any;
    loadQuestions: jest.Mock;
    deleteQuestion: jest.Mock;
    bulkTransition: jest.Mock;
  };
  let subjectTopicServiceMock: {
    getSubjects: jest.Mock;
  };
  let notificationServiceMock: {
    success: jest.Mock;
    error: jest.Mock;
    confirm: jest.Mock;
  };

  beforeEach(async () => {
    questionServiceMock = {
      questions: jest.fn().mockReturnValue([]),
      total: jest.fn().mockReturnValue(0),
      totalPages: jest.fn().mockReturnValue(1),
      currentPage: jest.fn().mockReturnValue(0),
      pageSize: jest.fn().mockReturnValue(20),
      loading: jest.fn().mockReturnValue(false),
      filter: { update: jest.fn() },
      loadQuestions: jest.fn().mockReturnValue(of({ content: [], totalElements: 0, totalPages: 1 })),
      deleteQuestion: jest.fn().mockReturnValue(of({})),
      bulkTransition: jest.fn().mockReturnValue(of({ totalRequested: 2, successCount: 2, failureCount: 0, results: [] })),
    };

    subjectTopicServiceMock = {
      getSubjects: jest.fn().mockReturnValue(of([{ id: 'sub-1', name: 'Mathematics' }])),
    };

    notificationServiceMock = {
      success: jest.fn(),
      error: jest.fn(),
      confirm: jest.fn().mockResolvedValue(true),
    };

    await TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule,
        RouterTestingModule,
        QuestionsFeatureBank,
      ],
      providers: [
        { provide: QuestionBankService, useValue: questionServiceMock },
        { provide: SubjectTopicService, useValue: subjectTopicServiceMock },
        { provide: NotificationService, useValue: notificationServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(QuestionsFeatureBank);
    component = fixture.componentInstance;
  });

  it('should create and initialize component with subjects', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
    expect(component.selectedSubject()).toBe('ALL');
    expect(component.selectedDifficulty()).toBe('ALL');
    expect(questionServiceMock.loadQuestions).toHaveBeenCalled();
  });

  it('should update search text and trigger filter reload', () => {
    component.onSearchTextChange('probability');
    expect(component.searchQuery()).toBe('probability');
    expect(questionServiceMock.loadQuestions).toHaveBeenCalled();
  });

  it('should update subject and difficulty filters and reset', () => {
    component.onSubjectChange('Mathematics');
    expect(component.selectedSubject()).toBe('Mathematics');

    component.onDifficultyChange('HARD');
    expect(component.selectedDifficulty()).toBe('HARD');

    component.resetFilters();
    expect(component.selectedSubject()).toBe('ALL');
    expect(component.selectedDifficulty()).toBe('ALL');
    expect(component.searchQuery()).toBe('');
  });
  it("should toggle selection and select all", () => {
    questionServiceMock.questions.mockReturnValue([{ id: "q1" }, { id: "q2" }]);
    expect(component.selectedQuestionIds().size).toBe(0);

    component.toggleSelectQuestion("q1");
    expect(component.selectedQuestionIds().has("q1")).toBe(true);

    component.toggleSelectAll();
    expect(component.isAllSelected()).toBe(true);

    component.clearSelection();
    expect(component.selectedQuestionIds().size).toBe(0);
  });

  it("should trigger bulk transition when items are selected", async () => {
    component.toggleSelectQuestion("q1");
    await component.onBulkTransition("REVIEW");

    expect(notificationServiceMock.confirm).toHaveBeenCalled();
    expect(questionServiceMock.bulkTransition).toHaveBeenCalledWith({
      questionIds: ["q1"],
      targetState: "REVIEW",
    });
    expect(notificationServiceMock.success).toHaveBeenCalled();
    expect(component.selectedQuestionIds().size).toBe(0);
  });
});
