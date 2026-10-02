import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ExamDeliveryComponent } from './exam-delivery.component';

describe('ExamDeliveryComponent', () => {
  let component: ExamDeliveryComponent;
  let fixture: ComponentFixture<ExamDeliveryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExamDeliveryComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ExamDeliveryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    component.ngOnDestroy();
  });

  it('should initialize questions and format countdown timer', () => {
    expect(component.questions().length).toBeGreaterThan(0);
    expect(component.formattedTime()).toBeDefined();
  });

  it('should allow selecting and clearing options', () => {
    const q1 = component.questions()[0];
    component.selectOption(q1, 'opt-b');

    expect(component.countAnswered()).toBe(1);
    expect(component.questions()[0].selectedOptionId).toBe('opt-b');

    component.clearResponse(q1);
    expect(component.countAnswered()).toBe(0);
    expect(component.questions()[0].selectedOptionId).toBeUndefined();
  });

  it('should toggle flag for review', () => {
    const q1 = component.questions()[0];
    component.toggleFlag(q1);
    expect(component.questions()[0].isFlagged).toBe(true);
    expect(component.countFlagged()).toBe(1);

    component.toggleFlag(q1);
    expect(component.questions()[0].isFlagged).toBe(false);
  });

  it('should navigate between questions', () => {
    component.goToQuestion(1);
    expect(component.currentIndex()).toBe(1);
    expect(component.currentItem()?.questionCode).toBe('NES-MATH-202');

    component.prevQuestion();
    expect(component.currentIndex()).toBe(0);

    component.nextQuestion();
    expect(component.currentIndex()).toBe(1);
  });
});
