import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { EvaluationService, GradingTask, DisputeRecord } from './evaluation-data-access';

describe('EvaluationService', () => {
  let service: EvaluationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [EvaluationService],
    });

    service = TestBed.inject(EvaluationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should initialize signals with empty defaults', () => {
    expect(service.tasks()).toEqual([]);
    expect(service.disputes()).toEqual([]);
    expect(service.analytics()).toBeNull();
    expect(service.loading()).toBe(false);
  });

  it('should load pending grading tasks', (done) => {
    const mockTasks: GradingTask[] = [
      {
        id: 't-1',
        examId: 'ex-1',
        candidateRoll: 'ROLL-100',
        questionCode: 'Q-01',
        questionContent: 'Explain Doppler Effect',
        candidateResponse: 'Frequency shift due to relative motion...',
        maxMarks: 10,
        status: 'PENDING',
      },
    ];

    service.loadPendingTasks().subscribe((tasks) => {
      expect(tasks.length).toBe(1);
      expect(service.tasks().length).toBe(1);
      expect(service.loading()).toBe(false);
      done();
    });

    const req = httpMock.expectOne('/api/v1/evaluation/tasks/pending');
    expect(req.request.method).toBe('GET');
    req.flush(mockTasks);
  });

  it('should submit grade and update task in list', (done) => {
    service.tasks.set([
      {
        id: 't-1',
        examId: 'ex-1',
        candidateRoll: 'ROLL-100',
        questionCode: 'Q-01',
        questionContent: 'Explain Doppler Effect',
        candidateResponse: 'Frequency shift...',
        maxMarks: 10,
        status: 'PENDING',
      },
    ]);

    const updatedTask: GradingTask = {
      id: 't-1',
      examId: 'ex-1',
      candidateRoll: 'ROLL-100',
      questionCode: 'Q-01',
      questionContent: 'Explain Doppler Effect',
      candidateResponse: 'Frequency shift...',
      maxMarks: 10,
      awardedMarks: 9,
      comments: 'Excellent explanation',
      status: 'GRADED',
    };

    service.submitGrade('t-1', 9, 'Excellent explanation').subscribe((res) => {
      expect(res.awardedMarks).toBe(9);
      expect(service.tasks()[0].status).toBe('GRADED');
      done();
    });

    const req = httpMock.expectOne('/api/v1/evaluation/tasks/t-1/grade');
    expect(req.request.method).toBe('POST');
    req.flush(updatedTask);
  });

  it('should load disputes and resolve dispute', (done) => {
    service.loadDisputes().subscribe((disputes) => {
      expect(disputes.length).toBe(1);
    });
    const req1 = httpMock.expectOne('/api/v1/evaluation/disputes');
    req1.flush([
      {
        id: 'disp-1',
        candidateRoll: 'R-1',
        examCode: 'EX-1',
        questionCode: 'Q-1',
        reason: 'Ambiguous key',
        candidateArgument: 'Option B is also correct',
        status: 'OPEN',
        createdAt: '2026-03-01',
      },
    ]);

    service.resolveDispute('disp-1', 'RESOLVED', 'Marks awarded').subscribe((res) => {
      expect(res.status).toBe('RESOLVED');
      expect(service.disputes()[0].status).toBe('RESOLVED');
      done();
    });
    const req2 = httpMock.expectOne('/api/v1/evaluation/disputes/disp-1/resolve');
    req2.flush({
      id: 'disp-1',
      candidateRoll: 'R-1',
      examCode: 'EX-1',
      questionCode: 'Q-1',
      reason: 'Ambiguous key',
      candidateArgument: 'Option B is also correct',
      reviewerRemarks: 'Marks awarded',
      status: 'RESOLVED',
      createdAt: '2026-03-01',
    });
  });
});
