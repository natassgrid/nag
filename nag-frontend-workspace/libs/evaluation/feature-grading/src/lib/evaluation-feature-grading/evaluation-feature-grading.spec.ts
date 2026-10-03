import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { signal } from '@angular/core';
import { EvaluationFeatureGrading } from './evaluation-feature-grading';
import {
  EvaluationService,
  GradingTask,
} from '@nag-frontend-workspace/evaluation-data-access';

describe('EvaluationFeatureGrading', () => {
  let component: EvaluationFeatureGrading;
  let fixture: ComponentFixture<EvaluationFeatureGrading>;

  const mockTask: GradingTask = {
    id: 't-10',
    examId: 'exam-1',
    candidateRoll: 'ROLL-01',
    questionCode: 'Q-01',
    questionContent: 'Describe photosynthesis',
    candidateResponse: 'Light reactions and dark reactions...',
    maxMarks: 5,
    status: 'PENDING',
  };

  let evalServiceMock: {
    tasks: any;
    disputes: any;
    analytics: any;
    loading: any;
    loadPendingTasks: jest.Mock;
    loadDisputes: jest.Mock;
    submitGrade: jest.Mock;
  };

  beforeEach(async () => {
    evalServiceMock = {
      tasks: signal([mockTask]),
      disputes: signal([]),
      analytics: signal(null),
      loading: signal(false),
      loadPendingTasks: jest.fn().mockReturnValue(of([mockTask])),
      loadDisputes: jest.fn().mockReturnValue(of([])),
      submitGrade: jest.fn().mockReturnValue(of({ ...mockTask, status: 'GRADED' })),
    };

    await TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule,
        EvaluationFeatureGrading,
      ],
      providers: [
        { provide: EvaluationService, useValue: evalServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(EvaluationFeatureGrading);
    component = fixture.componentInstance;
  });

  it('should create and load pending tasks on init', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
    expect(evalServiceMock.loadPendingTasks).toHaveBeenCalled();
    expect(component.selectedTask()?.id).toBe('t-10');
  });

  it('should submit grade and clear selection', () => {
    component.selectedTask.set(mockTask);
    component.handleSubmitGrade({
      taskId: 't-10',
      awardedMarks: 4,
      evaluatorComments: 'Good overview',
    });

    expect(evalServiceMock.submitGrade).toHaveBeenCalledWith('t-10', 4, 'Good overview');
    expect(component.selectedTask()).toBeNull();
  });
});
