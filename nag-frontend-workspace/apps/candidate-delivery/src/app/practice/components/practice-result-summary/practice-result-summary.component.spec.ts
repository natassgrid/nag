import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PracticeResultSummaryComponent } from './practice-result-summary.component';
import { PracticeResult } from '../../models';

describe('PracticeResultSummaryComponent', () => {
  let component: PracticeResultSummaryComponent;
  let fixture: ComponentFixture<PracticeResultSummaryComponent>;

  const mockResult: PracticeResult = {
    sessionId: 'session-123',
    correctCount: 15,
    incorrectCount: 3,
    skippedCount: 2,
    obtainedMarks: 27,
    totalMarks: 40,
    accuracyPercent: 75.0,
    topicWiseBreakdown: {
      Calculus: { correct: 8, total: 10 },
      Algebra: { correct: 7, total: 10 },
    },
    difficultyBreakdown: null,
    timingBreakdown: null,
    flaggedCount: 4,
    questionResults: [],
    practiceSetName: 'Math Practice',
    mode: 'TIMED',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PracticeResultSummaryComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(PracticeResultSummaryComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('result', mockResult);
    fixture.detectChanges();
  });

  it('should render score metrics correctly', () => {
    expect(component).toBeTruthy();
    expect(component.result().obtainedMarks).toBe(27);
    expect(component.result().accuracyPercent).toBe(75.0);
    expect(component.topicStats().length).toBe(2);
    expect(component.topicStats()[0].percentage).toBe(80);
  });

  it('should compute status breakdown percentages and flagged questions', () => {
    expect(component.totalQuestionsCount()).toBe(20); // 15 + 3 + 2
    expect(component.correctPct()).toBe(75); // 15 / 20 = 75%
    expect(component.incorrectPct()).toBe(15); // 3 / 20 = 15%
    expect(component.skippedPct()).toBe(10); // 2 / 20 = 10%
    expect(component.flaggedCount()).toBe(4);
  });
});
