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
});
