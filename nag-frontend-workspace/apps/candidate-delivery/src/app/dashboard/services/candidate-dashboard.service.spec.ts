import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { CandidateDashboardService, DEFAULT_ENROLLED_EXAMS } from './candidate-dashboard.service';

describe('CandidateDashboardService', () => {
  let service: CandidateDashboardService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [CandidateDashboardService],
    });

    service = TestBed.inject(CandidateDashboardService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should initialize with default enrolled exams and KPI metrics', () => {
    expect(service).toBeTruthy();
    expect(service.enrolledExams().length).toBeGreaterThan(0);
    expect(service.metrics().totalRegistered).toBe(DEFAULT_ENROLLED_EXAMS.length);
  });

  it('should load enrolled exams from backend and update signals', (done) => {
    const mockData = [
      {
        examId: 'ex-101',
        examCode: 'NAG-CS-2026',
        name: 'National CS Assessment',
        scheduledDate: '2026-11-20',
        scheduledTime: '10:00 AM - 01:00 PM',
        durationMinutes: 180,
        centerName: 'IIT Delhi Hub',
        centerAddress: 'Hauz Khas, New Delhi',
        hallTicketNumber: 'HT-99901',
        status: 'UPCOMING',
      },
    ];

    service.loadEnrolledExams().subscribe((exams) => {
      expect(exams.length).toBe(1);
      expect(exams[0].id).toBe('ex-101');
      expect(exams[0].code).toBe('NAG-CS-2026');
      expect(service.enrolledExams().length).toBe(1);
      done();
    });

    const req = httpMock.expectOne('/api/v1/examinations/my-exams');
    expect(req.request.method).toBe('GET');
    req.flush({ data: mockData });
  });

  it('should generate digital admit card with QR security token', (done) => {
    const exam = DEFAULT_ENROLLED_EXAMS[0];

    service.getAdmitCard(exam, 'Aryan Sharma').subscribe((card) => {
      expect(card).toBeTruthy();
      expect(card.candidateName).toBe('Aryan Sharma');
      expect(card.rollNumber).toBe(exam.rollNumber);
      expect(card.qrVerificationToken).toContain(exam.rollNumber);
      expect(card.instructions.length).toBeGreaterThan(0);
      done();
    });

    const req = httpMock.expectOne(`/api/v1/examinations/${exam.id}/admit-card`);
    req.flush({
      data: {
        applicationId: 'APP-9988',
        candidateName: 'Aryan Sharma',
        category: 'General',
      },
    });
  });
});
