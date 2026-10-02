import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PracticeQuestionReviewComponent } from './practice-question-review.component';
import { QuestionResult } from '../../models';

describe('PracticeQuestionReviewComponent', () => {
  let component: PracticeQuestionReviewComponent;
  let fixture: ComponentFixture<PracticeQuestionReviewComponent>;

  const mockQuestions: QuestionResult[] = [
    {
      questionId: 'q-1',
      candidateAnswer: 'opt-A',
      correctAnswer: 'opt-A',
      correct: true,
      marksAwarded: 2,
      timeSpentMs: 12000,
      markedForReview: false,
      content: 'What is O(1)?',
      optionsJson: JSON.stringify([
        { id: 'opt-A', text: 'Constant time' },
        { id: 'opt-B', text: 'Linear time' },
      ]),
      explanation: 'Constant time operations execute in fixed steps.',
      topic: 'Complexity',
      subject: 'CS',
    },
    {
      questionId: 'q-2',
      candidateAnswer: 'opt-B',
      correctAnswer: 'opt-C',
      correct: false,
      marksAwarded: -1,
      timeSpentMs: 18000,
      markedForReview: false,
      content: 'What is Dijkstra algorithm used for?',
      optionsJson: JSON.stringify([
        { id: 'opt-A', text: 'Sorting' },
        { id: 'opt-B', text: 'Pattern matching' },
        { id: 'opt-C', text: 'Shortest path' },
      ]),
      explanation: 'Dijkstra finds the shortest paths between nodes in a graph.',
      topic: 'Graphs',
      subject: 'CS',
    },
    {
      questionId: 'q-3',
      candidateAnswer: null,
      correctAnswer: 'opt-D',
      correct: false,
      marksAwarded: 0,
      timeSpentMs: 0,
      markedForReview: false,
      content: 'What is a B-Tree?',
      optionsJson: JSON.stringify([
        { id: 'opt-D', text: 'Self-balancing tree' },
      ]),
      explanation: 'A B-tree is a self-balancing search tree.',
      topic: 'Data Structures',
      subject: 'CS',
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PracticeQuestionReviewComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(PracticeQuestionReviewComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('questionResults', mockQuestions);
    fixture.detectChanges();
  });

  it('should initialize counts and parse options', () => {
    expect(component).toBeTruthy();
    expect(component.totalCount()).toBe(3);
    expect(component.correctCount()).toBe(1);
    expect(component.incorrectCount()).toBe(1);
    expect(component.skippedCount()).toBe(1);
  });

  it('should filter questions correctly', () => {
    component.setFilter('CORRECT');
    expect(component.filteredResults().length).toBe(1);
    expect(component.filteredResults()[0].questionId).toBe('q-1');

    component.setFilter('INCORRECT');
    expect(component.filteredResults().length).toBe(1);
    expect(component.filteredResults()[0].questionId).toBe('q-2');

    component.setFilter('SKIPPED');
    expect(component.filteredResults().length).toBe(1);
    expect(component.filteredResults()[0].questionId).toBe('q-3');
  });

  it('should verify candidate and correct answer helpers', () => {
    const q1 = mockQuestions[0];
    expect(component.isOptionSelected(q1, 'opt-A')).toBe(true);
    expect(component.isOptionSelected(q1, 'opt-B')).toBe(false);
    expect(component.isOptionCorrect(q1, 'opt-A')).toBe(true);
  });
});
