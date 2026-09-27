import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { CandidateReviewComponent } from './candidate-review.component';

describe('CandidateReviewComponent', () => {
  let component: CandidateReviewComponent;
  let fixture: ComponentFixture<CandidateReviewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CandidateReviewComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CandidateReviewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should initialize review questions and compute review statistics', () => {
    expect(component.questions().length).toBeGreaterThan(0);
    const stats = component.stats();
    expect(stats.correct + stats.incorrect + stats.unattempted).toBe(component.questions().length);
  });

  it('should filter questions by correctness status', () => {
    component.onFilterChange('CORRECT');
    const filtered = component.filteredQuestions();
    filtered.forEach((q) => {
      expect(q.isCorrect).toBe(true);
    });
  });

  it('should allow raising dispute notification', () => {
    const q1 = component.questions()[0];
    expect(() => component.raiseDispute(q1)).not.toThrow();
  });
});
