import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PracticeResultPanelComponent } from './practice-result-panel.component';
import { PracticeService } from '../../services/practice.service';
import { of, throwError } from 'rxjs';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('PracticeResultPanelComponent', () => {
  let component: PracticeResultPanelComponent;
  let fixture: ComponentFixture<PracticeResultPanelComponent>;
  let practiceServiceMock: any;

  const mockResult = {
    sessionId: 'session-123',
    correctCount: 8,
    incorrectCount: 2,
    skippedCount: 0,
    obtainedMarks: 14,
    totalMarks: 20,
    accuracyPercent: 80.0,
    topicWiseBreakdown: {
      Algorithms: { correct: 4, total: 5 },
      Databases: { correct: 4, total: 5 },
    },
    difficultyBreakdown: null,
    timingBreakdown: null,
    flaggedCount: 1,
    questionResults: [
      {
        questionId: 'q-1',
        candidateAnswer: 'opt-A',
        correctAnswer: 'opt-A',
        correct: true,
        marksAwarded: 2,
        timeSpentMs: 15000,
        markedForReview: false,
        content: 'What is the time complexity of QuickSort average case?',
        optionsJson: JSON.stringify([
          { id: 'opt-A', text: 'O(N log N)' },
          { id: 'opt-B', text: 'O(N^2)' },
        ]),
        explanation: 'QuickSort has average case complexity of O(N log N).',
        topic: 'Algorithms',
        subject: 'Computer Science',
      },
    ],
    practiceSetName: 'Algorithms Mastery Test',
    mode: 'TIMED',
  };

  beforeEach(async () => {
    practiceServiceMock = {
      getResult: jest.fn().mockReturnValue(of(mockResult)),
    };

    await TestBed.configureTestingModule({
      imports: [PracticeResultPanelComponent, NoopAnimationsModule],
      providers: [
        { provide: PracticeService, useValue: practiceServiceMock },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ sessionId: 'session-123' }),
            },
            paramMap: of(convertToParamMap({ sessionId: 'session-123' })),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PracticeResultPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load practice result and display summary', () => {
    expect(component).toBeTruthy();
    expect(practiceServiceMock.getResult).toHaveBeenCalledWith('session-123', 'en');
    expect(component.result()).toEqual(mockResult);
    expect(component.isLoading()).toBe(false);
  });

  it('should handle error when result fetching fails and allow retry', () => {
    practiceServiceMock.getResult.mockReturnValue(throwError(() => new Error('Server error')));
    component.loadResult('session-123');

    expect(component.error()).toBeTruthy();
    expect(component.isLoading()).toBe(false);

    // Test retry
    practiceServiceMock.getResult.mockReturnValue(of(mockResult));
    component.retry();
    expect(component.result()).toEqual(mockResult);
    expect(component.error()).toBeNull();
  });
});
