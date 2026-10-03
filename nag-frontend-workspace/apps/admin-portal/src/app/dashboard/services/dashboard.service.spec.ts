import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { DashboardService } from './dashboard.service';
import { DEFAULT_DASHBOARD_SUMMARY, DashboardSummary } from '../models/dashboard.model';

describe('DashboardService', () => {
  let service: DashboardService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [DashboardService],
    });

    service = TestBed.inject(DashboardService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should retrieve dashboard summary from backend API', (done) => {
    const mockResponse: Partial<DashboardSummary> = {
      tenantId: 'nta-tenant',
      kpis: {
        totalQuestions: 50000,
        pendingReviewQuestions: 200,
        activeExaminations: 20,
        registeredCandidates: 2000000,
        activeSessions: 10000,
        pendingGradingTasks: 500,
        activeBatchJobs: 5,
        questionTrend: '+250 this week',
        examTrend: '5 running live',
        candidateTrend: '100% capacity',
        gradingTrend: 'avg 15m turn-around',
      },
    };

    service.getDashboardSummary('nta-tenant').subscribe((summary) => {
      expect(summary).toBeDefined();
      expect(summary.tenantId).toBe('nta-tenant');
      expect(summary.kpis.totalQuestions).toBe(50000);
      expect(summary.kpis.pendingReviewQuestions).toBe(200);
      expect(summary.systemServices.length).toBeGreaterThan(0);
      done();
    });

    const req = httpMock.expectOne((r) => r.url === '/api/v1/admin/dashboard/summary' && r.params.get('tenantId') === 'nta-tenant');
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);
  });

  it('should gracefully fall back to DEFAULT_DASHBOARD_SUMMARY on HTTP error', (done) => {
    service.getDashboardSummary().subscribe((summary) => {
      expect(summary).toBeDefined();
      expect(summary.kpis.totalQuestions).toBe(DEFAULT_DASHBOARD_SUMMARY.kpis.totalQuestions);
      expect(summary.examBreakdown.draft).toBe(DEFAULT_DASHBOARD_SUMMARY.examBreakdown.draft);
      done();
    });

    const req = httpMock.expectOne('/api/v1/admin/dashboard/summary');
    expect(req.request.method).toBe('GET');
    req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });
  });
});
