import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
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
});
