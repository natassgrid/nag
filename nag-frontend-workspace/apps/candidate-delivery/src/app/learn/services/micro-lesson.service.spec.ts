import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MicroLessonService } from './micro-lesson.service';

describe('Candidate MicroLessonService', () => {
  let service: MicroLessonService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        MicroLessonService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(MicroLessonService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should pass lang query parameter in getPublishedMicroLesson', () => {
    service
      .getPublishedMicroLesson(501, {
        targetExam: 'JEE_MAIN',
        lang: 'hi',
      })
      .subscribe((lesson) => {
        expect(lesson.id).toBe('lesson-501');
        expect(lesson.languageCode).toBe('hi');
      });

    const req = httpMock.expectOne(
      (r) =>
        r.url === '/api/micro-lessons/subtopic/501' &&
        r.params.get('targetExam') === 'JEE_MAIN' &&
        r.params.get('lang') === 'hi'
    );
    expect(req.request.method).toBe('GET');
    req.flush({
      id: 'lesson-501',
      subtopicId: 501,
      subtopicName: 'Moment of Inertia',
      languageCode: 'hi',
      fallbackToEnglish: false,
      content: {
        coreConcept: 'घूर्णी जड़त्व',
      },
    });
  });

  it('should fetch localized micro-lesson via getMicroLessonByLanguage', () => {
    service.getMicroLessonByLanguage('lesson-501', 'ta').subscribe((lesson) => {
      expect(lesson.id).toBe('lesson-501');
      expect(lesson.languageCode).toBe('ta');
      expect(lesson.fallbackToEnglish).toBe(true);
    });

    const req = httpMock.expectOne('/api/v1/microlessons/lesson-501/language/ta');
    expect(req.request.method).toBe('GET');
    req.flush({
      id: 'lesson-501',
      subtopicId: 501,
      languageCode: 'ta',
      fallbackToEnglish: true,
      content: {
        coreConcept: 'English fallback concept',
      },
    });
  });

  it('should normalize micro-lesson with languageCode and fallbackToEnglish', () => {
    const raw = {
      id: 'lesson-2',
      subtopicId: '200',
      languageCode: 'mr',
      fallbackToEnglish: false,
      coreConcept: 'मराठी संकल्पना',
      trap: 'सावधगिरी',
      cheatSheet: ['$$E = mc^2$$'],
    };

    const normalized = service.normalizeMicroLesson(raw);
    expect(normalized.id).toBe('lesson-2');
    expect(normalized.subtopicId).toBe(200);
    expect(normalized.languageCode).toBe('mr');
    expect(normalized.fallbackToEnglish).toBe(false);
    expect(normalized.content.coreConcept).toBe('मराठी संकल्पना');
  });

  it('should normalize micro-lesson with lessonType and difficultyLevel (e.g. MEDIUM to CORE)', () => {
    const raw = {
      id: 'lesson-3',
      subtopicId: '37',
      lessonType: 'CONCEPT',
      difficultyLevel: 'MEDIUM',
      languageCode: 'en',
      coreConcept: 'Sphere concept',
    };

    const normalized = service.normalizeMicroLesson(raw);
    expect(normalized.lessonType).toBe('CONCEPT');
    expect(normalized.difficultyLevel).toBe('CORE');
  });

  it('should pass both lang and language query parameters in getMicroLessonsBySubtopic', () => {
    service
      .getMicroLessonsBySubtopic(37, {
        lessonType: 'CONCEPT',
        difficulty: 'CORE',
        lang: 'en',
      })
      .subscribe((lessons) => {
        expect(lessons.length).toBe(1);
        expect(lessons[0].difficultyLevel).toBe('CORE');
      });

    const req = httpMock.expectOne(
      (r) =>
        r.url === '/api/micro-lessons/subtopic/37' &&
        r.params.get('lessonType') === 'CONCEPT' &&
        r.params.get('difficulty') === 'CORE' &&
        r.params.get('lang') === 'en' &&
        r.params.get('language') === 'en'
    );
    expect(req.request.method).toBe('GET');
    req.flush([
      {
        id: 'lesson-37',
        subtopicId: 37,
        lessonType: 'CONCEPT',
        difficultyLevel: 'CORE',
        languageCode: 'en',
        content: { coreConcept: 'Sphere concept' },
      },
    ]);
  });

  it('should build 5 slides and multiple micro-tutorial approaches for a subtopic', () => {
    const normalized = service.normalizeMicroLesson({
      id: 'lesson-37',
      subtopicId: 37,
      subtopic: 'Equation of a Sphere',
      topic: '3D Geometry',
      subject: 'Mathematics',
      lessonType: 'CONCEPT',
      difficultyLevel: 'CORE',
      coreConcept: 'A sphere is the locus of a point in 3D space equidistant from a fixed center.',
      trap: 'Forgetting to divide coefficients of x, y, z by -2 when finding center.',
      cheatSheet: ['$$(x - a)^2 + (y - b)^2 + (z - c)^2 = R^2$$'],
      stepByStepBreakdown: {
        questionStem: 'Find the radius of $$x^2 + y^2 + z^2 = 25$$.',
        steps: ['Compare with $$x^2 + y^2 + z^2 = R^2$$ to get $$R = 5$$.'],
        finalAnswer: '$$R = 5$$',
      },
    });

    const slides = service.buildSlidesForLesson(normalized);
    expect(slides.length).toBe(5);
    expect(slides.map((s) => s.type)).toEqual(['INTRO', 'CONCEPT', 'FORMULA', 'STEPS', 'QUIZ']);
    expect(normalized.content.quickCheckItems?.length).toBeGreaterThan(0);
    expect(normalized.content.quickCheckItems?.[0].options?.length).toBe(4);

    const singleApproach = service.buildApproachesForSubtopic(normalized, [normalized]);
    expect(singleApproach.length).toBe(1);
    expect(singleApproach[0].badge).toBe('Approach 1');

    const variant2 = {
      ...normalized,
      id: 'lesson-37-variant2',
      sequenceNo: 2,
      approachName: 'Geometric Decomposition',
    };
    const multiApproaches = service.buildApproachesForSubtopic(normalized, [normalized, variant2]);
    expect(multiApproaches.length).toBe(2);
    expect(multiApproaches[0].badge).toBe('Approach 1');
    expect(multiApproaches[1].badge).toBe('Approach 2');
    expect(multiApproaches[1].approachName).toBe('Geometric Decomposition');
  });

  it('should normalize micro-lesson when content.stepByStepBreakdown is a direct JSON array', () => {
    const raw = {
      id: 'lesson-sphere',
      subtopicId: 37,
      subtopicName: 'Sphere Geometry',
      lessonType: 'CONCEPT',
      difficultyLevel: 'CORE',
      languageCode: 'en',
      content: {
        coreConcept: 'Sphere volume and surface area equations',
        stepByStepBreakdown: [
          {
            stepNumber: 1,
            title: 'Step 1',
            explanation: 'Step 1: Identify the given parameters and state the governing formula in KaTeX notation.',
          },
          {
            stepNumber: 2,
            title: 'Step 2',
            explanation: 'Step 2: Substitute the numerical value and simplify step-by-step in KaTeX notation.',
          },
          {
            stepNumber: 3,
            title: 'Step 3',
            explanation: 'Step 3: Compute the final result with proper units or percentage in KaTeX notation.',
            tip: 'Final Answer: $$V = \\frac{4}{3}\\pi (5)^3 = 523.6 \\text{ cm}^3$$, $$A = 4\\pi (5)^2 = 314.2 \\text{ cm}^2$$',
          },
        ],
      },
    };

    const result = service.normalizeMicroLesson(raw);
    const breakdown = result.content.stepByStepBreakdown as any;
    expect(breakdown).toBeDefined();
    expect(breakdown.steps.length).toBe(3);
    expect(breakdown.steps[0].title).toBe('Step 1');
    expect(breakdown.steps[1].title).toBe('Step 2');
    expect(breakdown.steps[2].title).toBe('Step 3');
    expect(breakdown.finalAnswer).toBe(
      '$$V = \\frac{4}{3}\\pi (5)^3 = 523.6 \\text{ cm}^3$$, $$A = 4\\pi (5)^2 = 314.2 \\text{ cm}^2$$'
    );
  });
});
