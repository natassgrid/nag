import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReviewQuestionPaletteComponent } from './review-question-palette.component';
import { ReviewQuestionItem } from '../../models';

describe('ReviewQuestionPaletteComponent', () => {
  let component: ReviewQuestionPaletteComponent;
  let fixture: ComponentFixture<ReviewQuestionPaletteComponent>;

  const mockQuestions: ReviewQuestionItem[] = [
    {
      questionId: 'q-1',
      questionNumber: 1,
      stem: 'What is the capital of India?',
      options: [],
      selectedOptionId: 'opt-1',
      correctOptionId: 'opt-1',
      isCorrect: true,
      marksAwarded: 2,
      maxMarks: 2,
      timeSpentSeconds: 45,
      explanation: 'New Delhi is the official capital of India.',
      disputeStatus: 'NONE',
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReviewQuestionPaletteComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ReviewQuestionPaletteComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('questions', mockQuestions);
    fixture.componentRef.setInput('currentIndex', 0);
    fixture.detectChanges();
  });

  it('should render review question palette', () => {
    expect(component).toBeTruthy();
    expect(component.questions().length).toBe(1);
  });
});
