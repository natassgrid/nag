import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ExaminationService } from './examination.service';

describe('ExaminationService', () => {
  let service: ExaminationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ExaminationService],
    });

    service = TestBed.inject(ExaminationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should initialize signals with defaults', () => {
    expect(service.exams()).toEqual([]);
    expect(service.loading()).toBe(false);
  });

  it('should fetch examinations list with pagination parameters', (done) => {
    const mockData = [
      {
        id: 'exam-1',
        title: 'Combined Graduate Level Examination',
        code: 'CGL-2026',
        status: 'PUBLISHED',
      },
    ];

    service.getExams(0, 10, 'CGL').subscribe((exams) => {
      expect(exams.length).toBe(1);
      expect(exams[0].code).toBe('CGL-2026');
      expect(service.exams().length).toBe(1);
      done();
    });

    const req = httpMock.expectOne('/api/v1/examinations?page=0&size=10&search=CGL');
    expect(req.request.method).toBe('GET');
    req.flush({ data: { content: mockData } });
  });

  it('should create new examination', (done) => {
    const payload = {
      title: 'SSC MTS 2026',
      code: 'MTS-2026',
      totalMarks: 100,
      passingMarks: 35,
      durationMinutes: 90,
      instructions: 'Standard instructions',
    };

    service.createExam(payload as any).subscribe((res) => {
      expect(res.id).toBe('exam-mts');
      done();
    });

    const req = httpMock.expectOne('/api/v1/examinations');
    expect(req.request.method).toBe('POST');
    req.flush({ data: { id: 'exam-mts', ...payload } });
  });
});
