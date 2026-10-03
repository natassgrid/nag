import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PaperService } from './paper.service';

describe('PaperService', () => {
  let service: PaperService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [PaperService],
    });

    service = TestBed.inject(PaperService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should fetch papers with query filters and normalize id and paperId', (done) => {
    const mockPapers = [
      {
        paperId: 'p-1001',
        name: 'CGL Tier 1 Paper Set A',
        status: 'PUBLISHED',
        totalMarks: 200,
      },
      {
        id: 'p-1002',
        name: 'CGL Tier 1 Paper Set B',
        status: 'DRAFT',
        totalMarks: 200,
      },
    ];

    service.getPapers({ page: 0, size: 20, status: 'PUBLISHED' }).subscribe((res) => {
      expect(res.content.length).toBe(2);
      expect(res.content[0].id).toBe('p-1001');
      expect(res.content[0].paperId).toBe('p-1001');
      expect(res.content[1].id).toBe('p-1002');
      expect(res.content[1].paperId).toBe('p-1002');
      expect(service.papers().length).toBe(2);
      done();
    });

    const req = httpMock.expectOne('/api/v1/papers?page=0&size=20&status=PUBLISHED');
    expect(req.request.method).toBe('GET');
    req.flush({ data: { content: mockPapers, totalElements: 2 } });
  });

  it('should fetch single paper detail by paperId', (done) => {
    const mockPaper = {
      id: 'paper-123',
      name: 'Test Paper',
      status: 'DRAFT',
    };

    service.getPaper('paper-123').subscribe((res) => {
      expect(res.id).toBe('paper-123');
      done();
    });

    const req = httpMock.expectOne('/api/v1/papers/paper-123');
    expect(req.request.method).toBe('GET');
    req.flush({ data: mockPaper });
  });

  it('should throw error when getPaper is called with invalid paperId', (done) => {
    service.getPaper('undefined').subscribe({
      next: () => done.fail('Should have failed'),
      error: (err) => {
        expect(err.message).toContain('Invalid paperId');
        done();
      },
    });

    httpMock.expectNone('/api/v1/papers/undefined');
  });

  it('should generate paper using blueprint parameters', (done) => {
    const reqPayload = {
      examId: 'exam-101',
      shiftId: 'shift-1',
      title: 'Automated Paper Set 1',
      sections: [],
    };

    service.generatePaper(reqPayload as any).subscribe((res) => {
      expect(res.paperId).toBe('generated-paper-1');
      done();
    });

    const req = httpMock.expectOne('/api/v1/papers/generate');
    expect(req.request.method).toBe('POST');
    req.flush({ data: { paperId: 'generated-paper-1', status: 'GENERATED' } });
  });
});
