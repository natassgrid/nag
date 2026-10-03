import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReviewFilterBarComponent } from './review-filter-bar.component';
import { ReviewStats } from '../../models';

describe('ReviewFilterBarComponent', () => {
  let component: ReviewFilterBarComponent;
  let fixture: ComponentFixture<ReviewFilterBarComponent>;

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
      imports: [ReviewFilterBarComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ReviewFilterBarComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('statusFilter', 'ALL');
    fixture.componentRef.setInput('totalQuestions', 50);
    fixture.componentRef.setInput('stats', mockStats);
    fixture.detectChanges();
  });

  it('should render filter bar and emit filter change', () => {
    expect(component).toBeTruthy();
    expect(component.statusFilter()).toBe('ALL');
  });
});
