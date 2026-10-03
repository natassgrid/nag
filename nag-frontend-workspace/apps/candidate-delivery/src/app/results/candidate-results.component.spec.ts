import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { CandidateResultsComponent } from './candidate-results.component';
import { CandidateResultsService, DEFAULT_SCORECARDS } from './services';

describe('CandidateResultsComponent', () => {
  let component: CandidateResultsComponent;
  let fixture: ComponentFixture<CandidateResultsComponent>;
  let resultsService: CandidateResultsService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CandidateResultsComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [provideRouter([]), CandidateResultsService],
    }).compileComponents();

    fixture = TestBed.createComponent(CandidateResultsComponent);
    component = fixture.componentInstance;
    resultsService = TestBed.inject(CandidateResultsService);
    resultsService.scorecards.set(DEFAULT_SCORECARDS);
    resultsService.selectedScorecard.set(DEFAULT_SCORECARDS[0]);
    fixture.detectChanges();
  });

  it('should create candidate results component', () => {
    expect(component).toBeTruthy();
  });

  it('should allow changing selected exam scorecard', () => {
    const second = DEFAULT_SCORECARDS[1];
    component.onSelectScorecard(second);
    expect(resultsService.selectedScorecard()).toEqual(second);
  });

  it('should handle push to DigiLocker', () => {
    const pushSpy = jest.spyOn(resultsService, 'pushToDigiLocker').mockReturnValue(
      of({
        docId: 'DL-12345',
        status: 'ISSUED',
        transactionId: 'TXN-999',
        pushedAt: '2026-09-25T10:00:00Z',
      })
    );

    component.onPushDigiLocker('res-nes-2026-01');
    expect(pushSpy).toHaveBeenCalledWith('res-nes-2026-01');
  });

  it('should trigger scorecard PDF download', () => {
    const blob = new Blob(['pdf'], { type: 'application/pdf' });
    const downloadSpy = jest.spyOn(resultsService, 'downloadScorecardPdf').mockReturnValue(of(blob));

    component.downloadScorecard();
    expect(downloadSpy).toHaveBeenCalledWith(DEFAULT_SCORECARDS[0].id);
  });

  it('should fallback to print if download fails', () => {
    jest.spyOn(resultsService, 'downloadScorecardPdf').mockReturnValue(throwError(() => new Error('Failed')));
    const printSpy = jest.spyOn(window, 'print').mockImplementation(() => {});

    component.downloadScorecard();
    expect(printSpy).toHaveBeenCalled();
  });
});
