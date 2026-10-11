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
      questionType: 'SINGLE_MCQ',
      content: 'What is O(1) in $$\\mathcal{O}$$ notation?',
      optionsJson: JSON.stringify([
        { id: 'opt-A', text: 'Constant time' },
        { id: 'opt-B', text: 'Linear time' },
      ]),
      explanation: 'Constant time operations execute in fixed steps: $$\\mathcal{O}(1)$$.',
      topic: 'Complexity',
      subject: 'CS',
      primaryLanguage: 'hi',
      fallbackToEnglish: false,
      primaryTranslation: {
        language: 'hi',
        content: '$$\\mathcal{O}$$ संकेतन में O(1) क्या दर्शाता है?',
        explanation: 'नियत समय संचालन निश्चित चरणों में निष्पादित होते हैं: $$\\mathcal{O}(1)$$.',
        options: [
          { id: 'opt-A', text: 'नियत समय' },
          { id: 'opt-B', text: 'रैखिक समय' },
        ],
      },
    },
    {
      questionId: 'q-2',
      candidateAnswer: 'opt-B',
      correctAnswer: 'opt-C',
      correct: false,
      marksAwarded: -1,
      timeSpentMs: 18000,
      markedForReview: true,
      questionType: 'SINGLE_MCQ',
      content: 'What is Dijkstra algorithm used for?',
      optionsJson: JSON.stringify([
        { id: 'opt-A', text: 'Sorting' },
        { id: 'opt-B', text: 'Pattern matching' },
        { id: 'opt-C', text: 'Shortest path' },
      ]),
      explanation: 'Dijkstra finds the shortest paths between nodes in a graph.',
      topic: 'Graphs',
      subject: 'CS',
      primaryLanguage: 'hi',
      fallbackToEnglish: true,
    },
    {
      questionId: 'q-3',
      candidateAnswer: null,
      correctAnswer: 'opt-D',
      correct: false,
      marksAwarded: 0,
      timeSpentMs: 0,
      markedForReview: false,
      questionType: 'SINGLE_MCQ',
      content: 'What is a B-Tree?',
      optionsJson: JSON.stringify([
        { id: 'opt-D', text: 'Self-balancing tree' },
      ]),
      explanation: 'A B-tree is a self-balancing search tree.',
      topic: 'Data Structures',
      subject: 'CS',
    },
    {
      questionId: 'q-4',
      candidateAnswer: '["opt-A", "opt-B"]',
      correctAnswer: '["opt-A", "opt-B"]',
      correct: true,
      marksAwarded: 4,
      timeSpentMs: 25000,
      markedForReview: true,
      questionType: 'MULTI_MCQ',
      content: 'Select all NP-complete problems:',
      optionsJson: JSON.stringify([
        { id: 'opt-A', text: '3-SAT' },
        { id: 'opt-B', text: 'Vertex Cover' },
        { id: 'opt-C', text: 'Shortest Path' },
      ]),
      explanation: 'Both 3-SAT and Vertex Cover are NP-complete.',
      topic: 'Complexity',
      subject: 'CS',
    },
    {
      questionId: 'q-5',
      candidateAnswer: '3.14',
      correctAnswer: '3.14',
      correct: true,
      marksAwarded: 3,
      timeSpentMs: 15000,
      markedForReview: false,
      questionType: 'NUMERICAL',
      content: 'Calculate $$\\pi$$ rounded to two decimal places:',
      optionsJson: null,
      explanation: '$$\\pi \\approx 3.14159...$$ which rounds to 3.14.',
      topic: 'Mathematics',
      subject: 'Math',
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PracticeQuestionReviewComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(PracticeQuestionReviewComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('questionResults', mockQuestions);
    fixture.componentRef.setInput('selectedLanguage', 'hi');
    fixture.detectChanges();
  });

  it('should initialize counts and parse options correctly across all question types', () => {
    expect(component).toBeTruthy();
    expect(component.totalCount()).toBe(5);
    expect(component.correctCount()).toBe(3);
    expect(component.incorrectCount()).toBe(1);
    expect(component.skippedCount()).toBe(1);
    expect(component.flaggedCount()).toBe(2);
  });

  it('should filter questions correctly by ALL, CORRECT, INCORRECT, SKIPPED, and FLAGGED', () => {
    component.setFilter('CORRECT');
    expect(component.filteredResults().length).toBe(3);
    expect(component.filteredResults().map((q) => q.questionId)).toEqual(['q-1', 'q-4', 'q-5']);

    component.setFilter('INCORRECT');
    expect(component.filteredResults().length).toBe(1);
    expect(component.filteredResults()[0].questionId).toBe('q-2');

    component.setFilter('SKIPPED');
    expect(component.filteredResults().length).toBe(1);
    expect(component.filteredResults()[0].questionId).toBe('q-3');

    component.setFilter('FLAGGED');
    expect(component.filteredResults().length).toBe(2);
    expect(component.filteredResults().map((q) => q.questionId)).toEqual(['q-2', 'q-4']);

    component.setFilter('ALL');
    expect(component.filteredResults().length).toBe(5);
  });

  it('should verify candidate and correct answer helpers without substring collisions', () => {
    const q1 = mockQuestions[0];
    expect(component.isOptionSelected(q1, 'opt-A')).toBe(true);
    expect(component.isOptionSelected(q1, 'opt-B')).toBe(false);
    expect(component.isOptionCorrect(q1, 'opt-A')).toBe(true);

    const q4Multi = mockQuestions[3];
    expect(component.isOptionSelected(q4Multi, 'opt-A')).toBe(true);
    expect(component.isOptionSelected(q4Multi, 'opt-B')).toBe(true);
    expect(component.isOptionCorrect(q4Multi, 'opt-A')).toBe(true);
    expect(component.isOptionCorrect(q4Multi, 'opt-B')).toBe(true);
    expect(component.isOptionCorrect(q4Multi, 'opt-C')).toBe(false);

    // Verify avoidance of substring bug (e.g. opt-1 should not match opt-10)
    const set = component.parseOptionIds('["opt-10", "opt-20"]');
    expect(set.has('opt-1')).toBe(false);
    expect(set.has('opt-10')).toBe(true);
  });

  it('should identify question type labels and numerical/direct value questions', () => {
    expect(component.getQuestionTypeLabel(mockQuestions[0])).toBe('Multiple Choice (Single-Select)');
    expect(component.getQuestionTypeLabel(mockQuestions[3])).toBe('Multiple Choice (Multi-Select)');
    expect(component.getQuestionTypeLabel(mockQuestions[4])).toBe('Numerical Value');

    expect(component.isNumericalOrDirect(mockQuestions[0])).toBe(false);
    expect(component.isNumericalOrDirect(mockQuestions[4])).toBe(true);
  });

  it('should render bilingual question details and synchronize option text', () => {
    const q1 = mockQuestions[0];
    expect(component.isBilingual(q1)).toBe(true);
    expect(component.isFallback(q1)).toBe(false);
    expect(component.getDisplayContent(q1)).toBe('$$\\mathcal{O}$$ संकेतन में O(1) क्या दर्शाता है?');
    expect(component.getBaselineEnglishContent(q1)).toBe('What is O(1) in $$\\mathcal{O}$$ notation?');
    expect(component.getDisplayExplanation(q1)).toBe('नियत समय संचालन निश्चित चरणों में निष्पादित होते हैं: $$\\mathcal{O}(1)$$.');
    expect(component.getBaselineEnglishExplanation(q1)).toBe('Constant time operations execute in fixed steps: $$\\mathcal{O}(1)$$.');

    const options = component.getResolvedOptions(q1);
    expect(options.length).toBe(2);
    expect(options[0].id).toBe('opt-A');
    expect(options[0].text).toBe('नियत समय');
    expect(options[0].englishText).toBe('Constant time');

    const element: HTMLElement = fixture.nativeElement;
    const englishRef = element.querySelector('.english-reference');
    expect(englishRef).toBeTruthy();
    expect(englishRef?.textContent).toContain('English Reference');
    expect(englishRef?.textContent).toContain('What is O(1)');

    const baselineOption = element.querySelector('.option-text-baseline');
    expect(baselineOption).toBeTruthy();
    expect(baselineOption?.textContent).toContain('En: Constant time');
  });

  it('should display fallback indicator when translation is unavailable', () => {
    const q2 = mockQuestions[1];
    expect(component.isFallback(q2)).toBe(true);
    expect(component.isBilingual(q2)).toBe(false);

    const element: HTMLElement = fixture.nativeElement;
    const fallbackBadges = element.querySelectorAll('.fallback-indicator');
    expect(fallbackBadges.length).toBeGreaterThan(0);
    expect(fallbackBadges[0].textContent).toContain('Original English (Translation Unavailable)');
  });
});
