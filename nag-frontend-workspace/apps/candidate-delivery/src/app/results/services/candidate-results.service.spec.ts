import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { CandidateResultsService, DEFAULT_SCORECARDS } from './candidate-results.service';

describe('CandidateResultsService', () => {
  let service: CandidateResultsService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [CandidateResultsService],
    });

    service = TestBed.inject(CandidateResultsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should initialize with default scorecards', () => {
    expect(service).toBeTruthy();
    expect(service.scorecards().length).toBe(DEFAULT_SCORECARDS.length);
    expect(service.selectedScorecard()).toEqual(DEFAULT_SCORECARDS[0]);
  });

  it('should switch selected scorecard', () => {
    const second = DEFAULT_SCORECARDS[1];
    service.selectScorecard(second);
    expect(service.selectedScorecard()).toEqual(second);
  });

  it('should push scorecard to DigiLocker and update state', (done) => {
    const target = DEFAULT_SCORECARDS[1];
    service.selectScorecard(target);

    service.pushToDigiLocker(target.id).subscribe((res) => {
      expect(res.status).toBe('ISSUED');
      expect(service.selectedScorecard()?.digiLockerPushed).toBe(true);
      done();
    });

    const req = httpMock.expectOne(`/api/v1/results/${target.id}/digilocker/push`);
    expect(req.request.method).toBe('POST');
    req.flush({ docId: 'DL-DOC-12345', transactionId: 'TXN-998877' });
  });
});
