import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReviewQuestionCardComponent } from './review-question-card.component';
import { ReviewQuestionItem } from '../../models';

describe('ReviewQuestionCardComponent', () => {
  let component: ReviewQuestionCardComponent;
  let fixture: ComponentFixture<ReviewQuestionCardComponent>;

  const mockQuestion: ReviewQuestionItem = {
    questionId: 'q-1',
    questionNumber: 1,
    stem: 'What is the capital of India?',
    options: [
      { id: 'opt-1', text: 'New Delhi', isCorrect: true },
      { id: 'opt-2', text: 'Mumbai', isCorrect: false },
    ],
    selectedOptionId: 'opt-1',
    correctOptionId: 'opt-1',
    isCorrect: true,
    marksAwarded: 2,
    maxMarks: 2,
    timeSpentSeconds: 45,
    explanation: 'New Delhi is the official capital of India.',
    disputeStatus: 'NONE',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReviewQuestionCardComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ReviewQuestionCardComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('question', mockQuestion);
    fixture.componentRef.setInput('currentIndex', 0);
    fixture.componentRef.setInput('totalFiltered', 50);
    fixture.detectChanges();
  });

  it('should render review question card and get correct option letter', () => {
    expect(component).toBeTruthy();
    expect(component.getOptionLetter(0)).toBe('A');
    expect(component.getOptionLetter(1)).toBe('B');
  });
});
