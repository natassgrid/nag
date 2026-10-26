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

  it('should switch language and fetch localized micro-lesson', () => {
    const hindiLesson: SubtopicMicroLesson = {
      ...sampleLesson,
      languageCode: 'hi',
      fallbackToEnglish: false,
      content: {
        ...sampleLesson.content,
        coreConcept: 'जड़त्वाघूर्ण घूर्णी जड़त्व का माप है: $$I = \\sum m_i r_i^2$$.',
      },
    };
    mockMicroLessonService.getMicroLessonByLanguage = jest.fn().mockReturnValue(of(hindiLesson));

    component.changeLanguage('hi');
    expect(component.selectedLanguage()).toBe('hi');
    expect(mockMicroLessonService.getMicroLessonByLanguage).toHaveBeenCalledWith('lesson-1', 'hi');
    expect(component.lesson()?.languageCode).toBe('hi');
    expect(component.lesson()?.content.coreConcept).toContain('जड़त्वाघूर्ण');
  });

  it('should display fallback notice when fallbackToEnglish is true', () => {
    const fallbackLesson: SubtopicMicroLesson = {
      ...sampleLesson,
      languageCode: 'hi',
      fallbackToEnglish: true,
    };
    mockMicroLessonService.getMicroLessonByLanguage = jest.fn().mockReturnValue(of(fallbackLesson));

    component.changeLanguage('hi');
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Translation for the selected regional language is being prepared');
  });

  it('should generate 5 interactive slides (INTRO, CONCEPT, FORMULA, STEPS, QUIZ) and navigate manually', () => {
    expect(component.totalSlides()).toBe(5);
    expect(component.slides().map((s) => s.type)).toEqual([
      'INTRO',
      'CONCEPT',
      'FORMULA',
      'STEPS',
      'QUIZ',
    ]);
    expect(component.currentSlideIndex()).toBe(0);

    component.onNextSlide();
    expect(component.currentSlideIndex()).toBe(1);

    component.onSelectSlide(3);
    expect(component.currentSlideIndex()).toBe(3);

    component.onPrevSlide();
    expect(component.currentSlideIndex()).toBe(2);
  });

  it('should reveal stepNumber sequence on click/tap on STEPS slide and reset', () => {
    component.onSelectSlide(3);
    expect(component.revealedStepCount()).toBe(0);

    component.onRevealNextStep();
    expect(component.revealedStepCount()).toBe(1);

    component.onResetSteps();
    expect(component.revealedStepCount()).toBe(0);

    component.onRevealAllSteps();
    expect(component.revealedStepCount()).toBe(1);
  });

  it('should record quiz option selection and compute success status', () => {
    component.onSelectSlide(4);
    component.onSelectQuizOption({
      questionIndex: 0,
      option: 'Verified by governing principle',
    });

    expect(component.quizSelections()[0]).toEqual({
      selectedOption: 'Verified by governing principle',
      isCorrect: true,
    });

    component.onResetQuiz();
    expect(component.quizSelections()).toEqual({});
  });

  it('should toggle Auto-Play mode, playback speed, audio narration, and AI tutor stage', () => {
    expect(component.isPlaying()).toBe(false);
    expect(component.playbackMode()).toBe('MANUAL');

    component.onTogglePlay();
    expect(component.isPlaying()).toBe(true);
    expect(component.playbackMode()).toBe('AUTO');

    component.onSpeedChange(1.5);
    expect(component.playbackSpeed()).toBe(1.5);

    component.onToggleAudio();
    expect(component.audioEnabled()).toBe(true);

    component.onToggleTutorStage();
    expect(component.tutorStageExpanded()).toBe(true);

    component.onTogglePlay();
    expect(component.isPlaying()).toBe(false);
    expect(component.playbackMode()).toBe('MANUAL');
  });

  it('should handle array-shaped and object-shaped stepByStepBreakdown and increment revealedStepCount via onRevealNextStep and Auto-Play', () => {
    // 1. Array-shaped stepByStepBreakdown
    const arrayLesson: SubtopicMicroLesson = {
      ...sampleLesson,
      id: 'lesson-array-breakdown',
      content: {
        ...sampleLesson.content,
        stepByStepBreakdown: [
          { stepNumber: 1, title: 'Step 1: Formula', explanation: 'State governing formula' },
          { stepNumber: 2, title: 'Step 2: Substitution', explanation: 'Substitute values' },
          { stepNumber: 3, title: 'Step 3: Solution', explanation: 'Calculate final result', tip: 'Final Answer: 523.6' },
        ] as any,
      },
    };

    component.lesson.set(arrayLesson);
    component.onSelectSlide(3); // STEPS slide
    component.onResetSteps();
    expect(component.revealedStepCount()).toBe(0);

    // Manual step reveal on array-shaped breakdown
    component.onRevealNextStep();
    expect(component.revealedStepCount()).toBe(1);
    component.onRevealNextStep();
    expect(component.revealedStepCount()).toBe(2);
    component.onRevealNextStep();
    expect(component.revealedStepCount()).toBe(3);

    // Auto-Play step playback on STEPS slide initializes and increments steps
    component.onResetSteps();
    expect(component.revealedStepCount()).toBe(0);
    component.onTogglePlay();
    fixture.detectChanges();
    expect(component.isPlaying()).toBe(true);
    expect(component.revealedStepCount()).toBe(1); // Auto-play effect reveals step 1 when at 0
    component.onRevealNextStep();
    expect(component.revealedStepCount()).toBe(2);
    component.onTogglePlay(); // Stop play
    expect(component.isPlaying()).toBe(false);

    // 2. Object-shaped stepByStepBreakdown
    const objectLesson: SubtopicMicroLesson = {
      ...sampleLesson,
      id: 'lesson-object-breakdown',
      content: {
        ...sampleLesson.content,
        stepByStepBreakdown: {
          problemStatement: 'Calculate volume and area of sphere.',
          steps: [
            { stepNumber: 1, title: 'Step 1: Identify', explanation: 'Radius R = 5 cm' },
            { stepNumber: 2, title: 'Step 2: Substitute', explanation: 'Substitute into formula' },
            { stepNumber: 3, title: 'Step 3: Compute', explanation: 'Compute result' },
          ],
        },
      },
    };

    component.lesson.set(objectLesson);
    component.onResetSteps();
    expect(component.revealedStepCount()).toBe(0);

    component.onRevealNextStep();
    expect(component.revealedStepCount()).toBe(1);
    component.onRevealNextStep();
    expect(component.revealedStepCount()).toBe(2);
    component.onRevealNextStep();
    expect(component.revealedStepCount()).toBe(3);
    component.onRevealNextStep(); // Already reached max
    expect(component.revealedStepCount()).toBe(3);
  });
});

