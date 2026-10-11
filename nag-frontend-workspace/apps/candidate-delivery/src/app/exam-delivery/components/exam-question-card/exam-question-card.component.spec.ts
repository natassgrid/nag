import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ExamQuestionCardComponent } from './exam-question-card.component';
import { ExamItem } from '../../models';

describe('ExamQuestionCardComponent', () => {
  let component: ExamQuestionCardComponent;
  let fixture: ComponentFixture<ExamQuestionCardComponent>;

  const mockItem: ExamItem = {
    id: 'q-1',
    order: 1,
    questionCode: 'Q1',
    content: 'What is the time complexity of binary search?',
    options: [
      { id: 'opt-1', text: 'O(log n)' },
      { id: 'opt-2', text: 'O(n)' },
    ],
    marks: 2,
    negativeMarks: 0.5,
    selectedOptionId: undefined,
    isFlagged: false,
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

  it('should recognize bilingual translation when primaryTranslation is present', () => {
    const bilingualItem: ExamItem = {
      ...mockItem,
      primaryLanguage: 'hi',
      primaryTranslation: {
        languageCode: 'hi',
        content: 'बाइनरी सर्च की समय जटिलता क्या है?',
        options: [
          { id: 'opt-1', text: 'O(log n)' },
          { id: 'opt-2', text: 'O(n)' },
        ],
      },
    };
    fixture.componentRef.setInput('item', bilingualItem);
    fixture.detectChanges();

    expect(component.hasTranslation()).toBe(true);
    expect(component.isFallback()).toBe(false);
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('HI Primary');
    expect(compiled.textContent).toContain('English Reference:');
  });

  it('should display fallback indicator badge when translation is absent', () => {
    const fallbackItem: ExamItem = {
      ...mockItem,
      primaryLanguage: 'te',
      fallbackToEnglish: true,
    };
    fixture.componentRef.setInput('item', fallbackItem);
    fixture.detectChanges();

    expect(component.hasTranslation()).toBe(false);
    expect(component.isFallback()).toBe(true);
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Original English (Translation Unavailable)');
  });
});
