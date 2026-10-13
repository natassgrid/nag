import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';
import { MicroLessonViewerComponent } from './micro-lesson-viewer.component';
import { MicroLessonService } from '../../services/micro-lesson.service';
import { LearningPathService } from '../../services/learning-path.service';
import { SubtopicMicroLesson } from '../../models/micro-lesson.model';

describe('MicroLessonViewerComponent', () => {
  let component: MicroLessonViewerComponent;
  let fixture: ComponentFixture<MicroLessonViewerComponent>;
  let mockMicroLessonService: jest.Mocked<Partial<MicroLessonService>>;
  let mockLearningPathService: jest.Mocked<Partial<LearningPathService>>;
  let mockRouter: jest.Mocked<Partial<Router>>;

  const sampleLesson: SubtopicMicroLesson = {
    id: 'lesson-1',
    subtopicId: 501,
    subtopicName: 'Moment of Inertia',
    topicName: 'Rotational Mechanics',
    subjectName: 'Physics',
    targetExam: 'JEE_MAIN',
    status: 'PUBLISHED',
    contentVersion: 1,
    content: {
      coreConcept: 'Moment of inertia measures rotational inertia: $$I = \\sum m_i r_i^2$$.',
      trapAlert: 'Candidates often confuse the parallel axis theorem with perpendicular axis theorem.',
      formulaCheatSheet: '$$I = I_{cm} + Md^2$$',
      stepByStepBreakdown: {
        problemStatement: 'Find moment of inertia of a solid cylinder.',
        steps: [
          {
            stepNumber: 1,
            title: 'Identify axis',
            explanation: 'Symmetry axis passing through centroid.',
            formula: '$$I = \\frac{1}{2} M R^2$$',
          },
        ],
      },
    },
  };

  beforeEach(async () => {
    mockMicroLessonService = {
      getPublishedMicroLesson: jest.fn().mockReturnValue(of(sampleLesson)),
    };
    mockLearningPathService = {
      completeMilestone: jest.fn().mockReturnValue(of({} as any)),
    };
    mockRouter = {
      navigate: jest.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [MicroLessonViewerComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: (key: string) => (key === 'subtopicId' ? '501' : null),
              },
              queryParamMap: {
                get: (key: string) => (key === 'milestoneId' ? 'ms-1' : null),
              },
            },
          },
        },
        { provide: Router, useValue: mockRouter },
        { provide: MicroLessonService, useValue: mockMicroLessonService },
        { provide: LearningPathService, useValue: mockLearningPathService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MicroLessonViewerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load lesson', () => {
    expect(component).toBeTruthy();
    expect(component.lesson()).toEqual(sampleLesson);
    expect(component.loading()).toBe(false);
  });

  it('should mark milestone as completed', () => {
    component.onMarkCompleted();
    expect(mockLearningPathService.completeMilestone).toHaveBeenCalledWith('ms-1');
    expect(component.isCompleted()).toBe(true);
  });

  it('should navigate back to roadmap', () => {
    component.onBackToRoadmap();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/learn/roadmap']);
  });
});
