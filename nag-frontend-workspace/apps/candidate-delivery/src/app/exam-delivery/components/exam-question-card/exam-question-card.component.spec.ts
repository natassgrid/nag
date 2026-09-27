import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ExamQuestionCardComponent } from './exam-question-card.component';
import { ExamItem } from '../../models';

describe('ExamQuestionCardComponent', () => {
  let component: ExamQuestionCardComponent;
  let fixture: ComponentFixture<ExamQuestionCardComponent>;

  const mockItem: ExamItem = {
    id: 'q-1',
    questionNumber: 1,
    stem: 'What is the time complexity of binary search?',
    options: [
      { id: 'opt-1', content: 'O(log n)' },
      { id: 'opt-2', content: 'O(n)' },
    ],
    selectedOptionId: null,
    isFlagged: false,
    status: 'NOT_VISITED',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExamQuestionCardComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ExamQuestionCardComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('item', mockItem);
    fixture.componentRef.setInput('currentIndex', 0);
    fixture.componentRef.setInput('totalQuestions', 10);
    fixture.detectChanges();
  });

  it('should render question card and format letter options', () => {
    expect(component).toBeTruthy();
    expect(component.getLetter(0)).toBe('A');
    expect(component.getLetter(1)).toBe('B');
  });

  it('should emit selectOption when option is chosen', () => {
    let selected = '';
    component.selectOption.subscribe((id) => (selected = id));
    component.onSelectOption('opt-1');
    expect(selected).toBe('opt-1');
  });
});
