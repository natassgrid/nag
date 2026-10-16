import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MicroLessonTableComponent } from './micro-lesson-table.component';
import { MicroLessonReviewItem } from '../../../../models/micro-lesson.model';

describe('MicroLessonTableComponent', () => {
  let component: MicroLessonTableComponent;
  let fixture: ComponentFixture<MicroLessonTableComponent>;

  const mockLesson: MicroLessonReviewItem = {
    id: 'lesson-1',
    subtopicName: 'Moment of Inertia',
    topicName: 'Rotational Motion',
    subjectName: 'Physics',
    targetExam: 'JEE_MAIN',
    status: 'AI_GENERATED',
    contentVersion: 1,
    content: {
      coreConcept: 'Test concept',
      trapAlert: 'Test trap',
      formulaCheatSheet: '$$I = m r^2$$',
    },
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MicroLessonTableComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(MicroLessonTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should render empty state when no lessons', () => {
    fixture.componentRef.setInput('lessons', []);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No micro-lessons match the current filter');
  });

  it('should render lessons and emit lessonSelect on row click', () => {
    const selectSpy = jest.fn();
    component.lessonSelect.subscribe(selectSpy);

    fixture.componentRef.setInput('lessons', [mockLesson]);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Moment of Inertia');

    const row = compiled.querySelector('tbody tr') as HTMLTableRowElement;
    row.click();

    expect(selectSpy).toHaveBeenCalledWith(mockLesson);
  });

  it('should emit pagination controls', () => {
    const firstSpy = jest.fn();
    const prevSpy = jest.fn();
    const nextSpy = jest.fn();
    const lastSpy = jest.fn();

    component.firstPage.subscribe(firstSpy);
    component.prevPage.subscribe(prevSpy);
    component.nextPage.subscribe(nextSpy);
    component.lastPage.subscribe(lastSpy);

    fixture.componentRef.setInput('lessons', [mockLesson]);
    fixture.componentRef.setInput('totalElements', 50);
    fixture.componentRef.setInput('totalPages', 5);
    fixture.componentRef.setInput('pageIndex', 2);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Page 3 of 5');

    const buttons = compiled.querySelectorAll('button[type="button"]');
    const firstBtn = Array.from(buttons).find((b) => b.getAttribute('title') === 'First Page') as HTMLButtonElement;
    const prevBtn = Array.from(buttons).find((b) => b.getAttribute('title') === 'Previous Page') as HTMLButtonElement;
    const nextBtn = Array.from(buttons).find((b) => b.getAttribute('title') === 'Next Page') as HTMLButtonElement;
    const lastBtn = Array.from(buttons).find((b) => b.getAttribute('title') === 'Last Page') as HTMLButtonElement;

    firstBtn?.click();
    expect(firstSpy).toHaveBeenCalled();

    prevBtn?.click();
    expect(prevSpy).toHaveBeenCalled();

    nextBtn?.click();
    expect(nextSpy).toHaveBeenCalled();

    lastBtn?.click();
    expect(lastSpy).toHaveBeenCalled();
  });
});
