import { ComponentFixture, TestBed } from '@angular/core/testing';
import { QuestionsUiQuestionCard, QuestionCardData } from './questions-ui-question-card';

describe('QuestionsUiQuestionCard', () => {
  let component: QuestionsUiQuestionCard;
  let fixture: ComponentFixture<QuestionsUiQuestionCard>;

  const mockData: QuestionCardData = {
    id: 'q-101',
    code: 'Q-PHY-001',
    content: 'What is the speed of light in vacuum?',
    type: 'SINGLE_CHOICE',
    difficulty: 'EASY',
    status: 'APPROVED',
    marks: 4,
    negativeMarks: 1,
    options: [
      { id: 'opt-1', text: '3 x 10^8 m/s', isCorrect: true },
      { id: 'opt-2', text: '3 x 10^6 m/s', isCorrect: false },
    ],
    tags: ['physics', 'optics'],
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [QuestionsUiQuestionCard],
    }).compileComponents();

    fixture = TestBed.createComponent(QuestionsUiQuestionCard);
    component = fixture.componentInstance;
  });

  it('should render question card details and options', () => {
    fixture.componentRef.setInput('data', mockData);
    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(fixture.nativeElement.textContent).toContain('What is the speed of light');
    expect(fixture.nativeElement.textContent).toContain('3 x 10^8 m/s');
    expect(component.difficultyVariant()).toBe('success');
  });

  it('should map difficulty variants properly', () => {
    fixture.componentRef.setInput('data', { ...mockData, difficulty: 'MEDIUM' });
    expect(component.difficultyVariant()).toBe('warn');

    fixture.componentRef.setInput('data', { ...mockData, difficulty: 'HARD' });
    expect(component.difficultyVariant()).toBe('error');

    fixture.componentRef.setInput('data', { ...mockData, difficulty: 'OTHER' });
    expect(component.difficultyVariant()).toBe('neutral');
  });

  it('should format question type and option labels', () => {
    expect(component.formatType('MULTIPLE_CHOICE')).toBe('multiple choice');
    expect(component.formatType('')).toBe('');
    expect(component.getOptionLabel(0)).toBe('A');
    expect(component.getOptionLabel(1)).toBe('B');
  });

  it('should emit cardEdit and cardDelete events', () => {
    fixture.componentRef.setInput('data', mockData);
    fixture.detectChanges();

    const editSpy = jest.spyOn(component.cardEdit, 'emit');
    const deleteSpy = jest.spyOn(component.cardDelete, 'emit');

    const fakeEvent = { stopPropagation: jest.fn() } as unknown as Event;

    component.onEditClicked(fakeEvent);
    expect(fakeEvent.stopPropagation).toHaveBeenCalled();
    expect(editSpy).toHaveBeenCalledWith(mockData);

    component.onDeleteClicked(fakeEvent);
    expect(deleteSpy).toHaveBeenCalledWith('q-101');
  });

  it('should emit cardSelect when checkbox is toggled', () => {
    fixture.componentRef.setInput('data', mockData);
    fixture.componentRef.setInput('selectable', true);
    fixture.detectChanges();

    const selectSpy = jest.spyOn(component.cardSelect, 'emit');
    const fakeEvent = { stopPropagation: jest.fn() } as unknown as Event;

    component.onSelectClicked(fakeEvent);
    expect(fakeEvent.stopPropagation).toHaveBeenCalled();
    expect(selectSpy).toHaveBeenCalledWith(mockData);
  });
});
