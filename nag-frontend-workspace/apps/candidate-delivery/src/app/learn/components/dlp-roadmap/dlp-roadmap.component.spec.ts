import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { DlpRoadmapComponent } from './dlp-roadmap.component';
import { LearningPathService } from '../../services/learning-path.service';
import { DynamicLearningPath } from '../../models/learning-path.model';

describe('DlpRoadmapComponent', () => {
  let component: DlpRoadmapComponent;
  let fixture: ComponentFixture<DlpRoadmapComponent>;
  let mockLearningPathService: jest.Mocked<Partial<LearningPathService>>;
  let mockRouter: jest.Mocked<Partial<Router>>;

  const samplePath: DynamicLearningPath = {
    id: 'dlp-1',
    candidateId: 'cand-1',
    status: 'ACTIVE',
    targetExam: 'GENERAL_COMPETITIVE',
    createdAt: new Date().toISOString(),
    gatekeeperPassed: false,
    milestoneGraph: {
      targetExam: 'GENERAL_COMPETITIVE',
      currentPhase: 1,
      gatekeeperPassed: false,
      phases: [
        {
          phaseNumber: 1,
          name: 'Phase 1: Remediation',
          focus: 'Remediate weak subtopics',
          status: 'ACTIVE',
          durationWeeks: 2,
          targetAccuracy: 70,
          passThreshold: 70,
          passed: false,
          items: [
            {
              milestoneId: 'ms-1',
              subtopicId: 501,
              subtopicName: 'Moment of Inertia',
              topicName: 'Rotational Mechanics',
              subjectName: 'Physics',
              status: 'NOT_STARTED',
              targetScore: 70,
            },
          ],
        },
        {
          phaseNumber: 2,
          name: 'Phase 2: Adaptive Quizzing',
          focus: 'Pacing and mixed drills',
          status: 'LOCKED',
          durationWeeks: 1,
          targetAccuracy: 70,
          passThreshold: 70,
          passed: false,
          items: [],
        },
        {
          phaseNumber: 3,
          name: 'Phase 3: Gatekeeper Test',
          focus: 'Pass criteria >= 75%',
          status: 'LOCKED',
          durationWeeks: 1,
          targetAccuracy: 75,
          passThreshold: 75,
          passed: false,
          items: [],
        },
      ],
    },
  };

  beforeEach(async () => {
    mockLearningPathService = {
      getLearningPath: jest.fn().mockReturnValue(of(samplePath)),
      generateLearningPath: jest.fn().mockReturnValue(of(samplePath)),
      startAdaptiveQuiz: jest.fn().mockReturnValue(
        of({
          quizSessionId: 'q-1',
          practiceSetId: 'set-quiz-123',
          targetAccuracy: 70,
          questionCount: 10,
        })
      ),
      submitAdaptiveQuiz: jest.fn().mockReturnValue(of(samplePath)),
      startGatekeeper: jest.fn().mockReturnValue(
        of({
          gatekeeperSessionId: 'gk-1',
          practiceSetId: 'set-gate-456',
          passThreshold: 75,
          questionCount: 15,
        })
      ),
      submitGatekeeper: jest.fn().mockReturnValue(of(samplePath)),
    };
    mockRouter = {
      navigate: jest.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [DlpRoadmapComponent],
      providers: [
        { provide: LearningPathService, useValue: mockLearningPathService },
        { provide: Router, useValue: mockRouter },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DlpRoadmapComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load learning path', () => {
    expect(component).toBeTruthy();
    expect(component.learningPath()).toEqual(samplePath);
    expect(component.phases().length).toBe(3);
    expect(component.currentPhaseNumber()).toBe(1);
    expect(component.gatekeeperPassed()).toBe(false);
  });

  it('should route to micro-lesson viewer on onStudyMicroLesson', () => {
    component.onStudyMicroLesson(samplePath.milestoneGraph.phases[0].items[0]);
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/learn/lesson', 501], {
      queryParams: { milestoneId: 'ms-1' },
    });
  });

  it('should start adaptive quiz and navigate to practice with practiceSetId', () => {
    component.onStartAdaptiveQuiz();
    expect(mockLearningPathService.startAdaptiveQuiz).toHaveBeenCalled();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/practice'], {
      queryParams: { setId: 'set-quiz-123' },
    });
  });

  it('should start gatekeeper test and navigate to practice with practiceSetId', () => {
    component.onStartGatekeeper();
    expect(mockLearningPathService.startGatekeeper).toHaveBeenCalled();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/practice'], {
      queryParams: { setId: 'set-gate-456' },
    });
  });

  it('should submit adaptive quiz score', () => {
    component.onSubmitAdaptiveQuizScore(80);
    expect(mockLearningPathService.submitAdaptiveQuiz).toHaveBeenCalled();
  });

  it('should submit gatekeeper score', () => {
    component.onSubmitGatekeeperScore(85);
    expect(mockLearningPathService.submitGatekeeper).toHaveBeenCalled();
  });
});
