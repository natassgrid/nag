import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReviewSummaryStatsComponent } from './review-summary-stats.component';
import { ReviewStats } from '../../models';

describe('ReviewSummaryStatsComponent', () => {
  let component: ReviewSummaryStatsComponent;
  let fixture: ComponentFixture<ReviewSummaryStatsComponent>;

  const mockStats: ReviewStats = {
    correct: 30,
    incorrect: 10,
    unanswered: 10,
    disputed: 2,
    score: 85,
    maxScore: 100,
    accuracyPercent: 75,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReviewSummaryStatsComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ReviewSummaryStatsComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('stats', mockStats);
    fixture.componentRef.setInput('totalQuestions', 50);
    fixture.detectChanges();
  });

  it('should render review statistics summary', () => {
    expect(component).toBeTruthy();
    expect(component.stats().correct).toBe(30);
    expect(component.stats().score).toBe(85);
  });
});
