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

  it('should fetch papers with query filters', (done) => {
    const mockPapers = [
      {
        id: 'paper-1',
        title: 'CGL Tier 1 Paper Set A',
        status: 'PUBLISHED',
        totalMarks: 200,
      },
    ];

    service.getPapers({ page: 0, size: 20, status: 'PUBLISHED' }).subscribe((res) => {
      expect(res.content.length).toBe(1);
      expect(res.content[0].paperId).toBe('paper-1');
      expect(service.papers().length).toBe(1);
      done();
    });

    const req = httpMock.expectOne('/api/v1/papers?page=0&size=20&status=PUBLISHED');
    expect(req.request.method).toBe('GET');
    req.flush({ data: { content: mockPapers, totalElements: 1 } });
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
