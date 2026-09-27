import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { CandidateBrowseService, DEFAULT_MOCK_EXAMS, DEFAULT_MOCK_CENTRES } from './candidate-browse.service';
import { ApplyExamPayload } from '../models';

describe('CandidateBrowseService', () => {
  let service: CandidateBrowseService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [CandidateBrowseService],
    });

    service = TestBed.inject(CandidateBrowseService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created with default mock catalogue and centres', () => {
    expect(service).toBeTruthy();
    expect(service.catalog().length).toBeGreaterThan(0);
    expect(service.centres().length).toBeGreaterThan(0);
  });

  it('should fallback to default catalog with applied status when public API returns empty', (done) => {
    service.loadPublicCatalog().subscribe((exams) => {
      expect(exams.length).toBe(DEFAULT_MOCK_EXAMS.length);
      expect(service.loading()).toBeFalse();
      done();
    });

    const publicReq = httpMock.expectOne('/api/v1/examinations/public?page=0&size=50');
    expect(publicReq.request.method).toBe('GET');
    publicReq.flush({ data: { content: [] } });

    const myExamsReq = httpMock.expectOne('/api/v1/examinations/my-exams');
    expect(myExamsReq.request.method).toBe('GET');
    myExamsReq.flush({ data: [{ examId: 'exam-1', status: 'APPLIED' }] });
  });

  it('should map backend examinations payload when returned from public API', (done) => {
    const backendMock = [
      {
        id: 'be-exam-1',
        name: 'National Civil Engineering Assessment',
        code: 'NCEA-2026',
        conductingAuthority: 'UPSC',
        category: 'ENGINEERING',
        durationMinutes: 120,
        totalMarks: 200,
        feeAmount: 500,
        status: 'OPEN',
      },
    ];

    service.loadPublicCatalog().subscribe((exams) => {
      expect(exams.length).toBe(1);
      expect(exams[0].code).toBe('NCEA-2026');
      expect(exams[0].applied).toBeTrue();
      done();
    });

    const publicReq = httpMock.expectOne('/api/v1/examinations/public?page=0&size=50');
    publicReq.flush({ data: { content: backendMock } });

    const myExamsReq = httpMock.expectOne('/api/v1/examinations/my-exams');
    myExamsReq.flush({ data: [{ examId: 'be-exam-1' }] });
  });

  it('should load public centres and update centres signal', (done) => {
    const centresMock = [
      {
        id: 'c-test-1',
        centreName: 'Test Hub Pune',
        region: 'West',
        state: 'Maharashtra',
        district: 'Pune',
        city: 'Pune',
        totalCapacity: 500,
      },
    ];

    service.loadPublicCentres().subscribe((centres) => {
      expect(centres.length).toBe(1);
      expect(service.centres()[0].city).toBe('Pune');
      done();
    });

    const req = httpMock.expectOne('/api/v1/examinations/centres/public');
    req.flush({ data: centresMock });
  });

  it('should submit application and update exam applied status in catalog', (done) => {
    const payload: ApplyExamPayload = {
      firstChoiceCentreId: 'c-delhi-01',
      pwdRequired: false,
    };

    service.applyForExam('exam-2', payload).subscribe((receipt) => {
      expect(receipt).toBeTruthy();
      expect(receipt.status).toBe('CONFIRMED');
      const updated = service.catalog().find((e) => e.id === 'exam-2');
      expect(updated?.applied).toBeTrue();
      done();
    });

    const req = httpMock.expectOne('/api/v1/examinations/exam-2/apply');
    expect(req.request.method).toBe('POST');
    req.flush({
      data: {
        applicationId: 'APP-999',
        hallTicketNumber: 'NAG-GATE-DPI-999999',
        candidateName: 'Aryan Sharma',
        status: 'CONFIRMED',
      },
    });
  });
});
