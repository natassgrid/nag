import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ExamQuestionPaletteComponent } from './exam-question-palette.component';
import { ExamItem } from '../../models';

describe('ExamQuestionPaletteComponent', () => {
  let component: ExamQuestionPaletteComponent;
  let fixture: ComponentFixture<ExamQuestionPaletteComponent>;

  const mockQuestions: ExamItem[] = [
    {
      id: 'q-1',
      questionNumber: 1,
      stem: 'Question 1',
      options: [],
      selectedOptionId: 'opt-1',
      isFlagged: false,
      status: 'ANSWERED',
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExamQuestionPaletteComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ExamQuestionPaletteComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('questions', mockQuestions);
    fixture.componentRef.setInput('currentIndex', 0);
    fixture.componentRef.setInput('countAnswered', 1);
    fixture.componentRef.setInput('countFlagged', 0);
    fixture.componentRef.setInput('countUnvisited', 0);
    fixture.detectChanges();
  });

  it('should render palette and emit selected question index', () => {
    expect(component).toBeTruthy();
    let selectedIdx = -1;
    component.selectQuestion.subscribe((idx) => (selectedIdx = idx));
    component.onSelectQuestion(3);
    expect(selectedIdx).toBe(3);
  });
});
