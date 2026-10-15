import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { SubjectTopicService } from './subject-topic.service';

describe('SubjectTopicService', () => {
  let service: SubjectTopicService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [SubjectTopicService],
    });

    service = TestBed.inject(SubjectTopicService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch hierarchy from /api/v1/subjects/hierarchy', (done) => {
    const mockHierarchy = [
      {
        id: 1,
        name: 'Physics',
        code: 'PHYS',
        topics: [
          {
            id: 10,
            name: 'Mechanics',
            subtopics: [{ id: 100, name: 'Kinematics' }],
          },
        ],
      },
    ];

    service.getHierarchy().subscribe((res) => {
      expect(res.length).toBe(1);
      expect(res[0].name).toBe('Physics');
      expect(res[0].topics[0].name).toBe('Mechanics');
      done();
    });

    const req = httpMock.expectOne('/api/v1/subjects/hierarchy');
    expect(req.request.method).toBe('GET');
    req.flush({ data: mockHierarchy });
  });

  it('should fetch target examinations from /api/v1/examinations', (done) => {
    const mockExams = {
      data: {
        content: [
          {
            id: 'exam-uuid-1',
            name: 'Joint Entrance Examination Main',
            code: 'JEE_MAIN',
            category: 'ENTRANCE',
            examinationType: 'MAIN',
          },
          {
            id: 'exam-uuid-2',
            name: 'National Eligibility Entrance Test',
            code: 'NEET',
            category: 'ENTRANCE',
            examinationType: 'MAIN',
          },
        ],
        totalElements: 2,
      },
    };

    service.getExaminations().subscribe((exams) => {
      expect(exams.length).toBe(2);
      expect(exams[0].code).toBe('JEE_MAIN');
      expect(exams[0].name).toBe('Joint Entrance Examination Main');
      expect(exams[1].code).toBe('NEET');
      done();
    });

    const req = httpMock.expectOne('/api/v1/examinations?page=0&size=100');
    expect(req.request.method).toBe('GET');
    req.flush(mockExams);
  });

  it('should fallback to /api/v1/examinations/public on failure', (done) => {
    const mockPublicExams = {
      data: {
        content: [
          {
            id: 'exam-pub-1',
            name: 'GATE 2026',
            code: 'GATE',
            category: 'ENTRANCE',
          },
        ],
      },
    };

    service.getExaminations().subscribe((exams) => {
      expect(exams.length).toBe(1);
      expect(exams[0].code).toBe('GATE');
      done();
    });

    const req1 = httpMock.expectOne('/api/v1/examinations?page=0&size=100');
    req1.flush('Unauthorized', { status: 403, statusText: 'Forbidden' });

    const req2 = httpMock.expectOne('/api/v1/examinations/public?page=0&size=100');
    expect(req2.request.method).toBe('GET');
    req2.flush(mockPublicExams);
  });
});
