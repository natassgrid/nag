import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { LearningPathService } from './learning-path.service';

describe('LearningPathService', () => {
  let service: LearningPathService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [LearningPathService],
    });
    service = TestBed.inject(LearningPathService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should retrieve current learning path via GET /api/learning-path/current', () => {
    service.getLearningPath().subscribe();
    const req = httpMock.expectOne('/api/learning-path/current');
    expect(req.request.method).toBe('GET');
    req.flush({});
  });

  it('should retrieve summary via GET /api/learning-path/summary', () => {
    service.getSummary().subscribe();
    const req = httpMock.expectOne('/api/learning-path/summary');
    expect(req.request.method).toBe('GET');
    req.flush({ totalSubtopics: 10, conceptualGapsCount: 2, applicationGapsCount: 3, proficientCount: 5, masteryPercentage: 50.0 });
  });

  it('should recalculate learning path via POST /api/learning-path/recalculate', () => {
    service.generateLearningPath('JEE_MAIN').subscribe();
    const req = httpMock.expectOne('/api/learning-path/recalculate');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ examTarget: 'JEE_MAIN' });
    req.flush({});
  });

  it('should complete milestone via POST /api/learning-path/milestones/:id/complete', () => {
    service.completeMilestone('M1').subscribe();
    const req = httpMock.expectOne('/api/learning-path/milestones/M1/complete');
    expect(req.request.method).toBe('POST');
    req.flush({});
  });

  it('should start adaptive quiz via POST /api/learning-path/adaptive-quiz/start', () => {
    service.startAdaptiveQuiz().subscribe();
    const req = httpMock.expectOne('/api/learning-path/adaptive-quiz/start');
    expect(req.request.method).toBe('POST');
    req.flush({});
  });

  it('should submit adaptive quiz score via POST /api/learning-path/adaptive-quiz/submit', () => {
    service.submitAdaptiveQuiz('quiz-1', 80).subscribe();
    const req = httpMock.expectOne('/api/learning-path/adaptive-quiz/submit');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ sessionId: 'quiz-1', scorePercent: 80 });
    req.flush({});
  });

  it('should start gatekeeper via POST /api/learning-path/gatekeeper/start', () => {
    service.startGatekeeper().subscribe();
    const req = httpMock.expectOne('/api/learning-path/gatekeeper/start');
    expect(req.request.method).toBe('POST');
    req.flush({});
  });

  it('should submit gatekeeper score via POST /api/learning-path/gatekeeper/submit', () => {
    service.submitGatekeeper('gk-1', 85).subscribe();
    const req = httpMock.expectOne('/api/learning-path/gatekeeper/submit');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ sessionId: 'gk-1', scorePercent: 85 });
    req.flush({});
  });

  it('should get gatekeeper status via GET /api/learning-path/gatekeeper/status', () => {
    service.getGatekeeperStatus().subscribe();
    const req = httpMock.expectOne('/api/learning-path/gatekeeper/status');
    expect(req.request.method).toBe('GET');
    req.flush({ gatekeeperRequired: true, clearedForFullMock: false });
  });
});
