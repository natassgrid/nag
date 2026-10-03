import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import {
  CandidateResultsService,
  DEFAULT_SCORECARDS,
  mapBackendResultToScorecard,
} from './candidate-results.service';

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

  it('should map backend raw result entity correctly', () => {
    const backendResult = {
      id: 'res-custom-01',
      candidateId: '00000000-0000-0000-0000-000000000001',
      examId: 'exam-custom-01',
      totalScore: 250,
      overallPercentile: 98.5,
      overallRank: 10,
      categoryRank: 4,
      scorecardPdfRef: 'scorecards/res-01.pdf',
      digiLockerPushed: true,
      sectionScoresJson: JSON.stringify([
        {
          sectionName: 'Mathematics',
          marksObtained: 80,
          maxMarks: 100,
          accuracyRate: 90,
          questionsAttempted: 28,
          questionsTotal: 30,
          cutoffMarks: 40,
        },
      ]),
    };

    const scorecard = mapBackendResultToScorecard(backendResult, 0);
    expect(scorecard.id).toBe('res-custom-01');
    expect(scorecard.totalScore).toBe(250);
    expect(scorecard.percentile).toBe(98.5);
    expect(scorecard.nationalRank).toBe(10);
    expect(scorecard.categoryRank).toBe(4);
    expect(scorecard.digiLockerPushed).toBe(true);
    expect(scorecard.subjectScores.length).toBe(1);
    expect(scorecard.subjectScores[0].subject).toBe('Mathematics');
  });

  it('should load scorecards from backend API', (done) => {
    const mockBackendResults = [
      {
        id: 'res-api-1',
        candidateId: '00000000-0000-0000-0000-000000000001',
        examId: 'exam-1',
        totalScore: 280,
        overallPercentile: 99.1,
        overallRank: 50,
        digiLockerPushed: false,
      },
    ];

    service.loadScorecards().subscribe((cards) => {
      expect(cards.length).toBe(1);
      expect(cards[0].id).toBe('res-api-1');
      expect(cards[0].totalScore).toBe(280);
      expect(service.selectedScorecard()?.id).toBe('res-api-1');
      done();
    });

    const req = httpMock.expectOne('/api/v1/results/my-results');
    expect(req.request.method).toBe('GET');
    req.flush(mockBackendResults);
  });

  it('should load scorecards for specific candidate ID', (done) => {
    const userId = 'user-123';
    service.loadScorecards(userId).subscribe((cards) => {
      expect(cards.length).toBe(DEFAULT_SCORECARDS.length);
      done();
    });

    const req = httpMock.expectOne(`/api/v1/results/candidate/${userId}`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
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

  it('should request scorecard PDF download', (done) => {
    const blob = new Blob(['%PDF-1.4 mock content'], { type: 'application/pdf' });
    service.downloadScorecardPdf('res-1').subscribe((resBlob) => {
      expect(resBlob).toBeTruthy();
      done();
    });

    const req = httpMock.expectOne('/api/v1/results/res-1/scorecard/download');
    expect(req.request.method).toBe('GET');
    req.flush(blob);
  });
});
