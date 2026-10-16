import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MicroLessonDetailDrawerComponent } from './micro-lesson-detail-drawer.component';
import { MicroLessonReviewItem } from '../../../../models/micro-lesson.model';

describe('MicroLessonDetailDrawerComponent', () => {
  let component: MicroLessonDetailDrawerComponent;
  let fixture: ComponentFixture<MicroLessonDetailDrawerComponent>;

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
      stepByStepBreakdown: {
        problemStatement: 'Calculate I',
        steps: [{ stepNumber: 1, title: 'Step 1', explanation: 'Use formula' }],
        finalAnswer: '2 kg m^2',
      },
    },
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MicroLessonDetailDrawerComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(MicroLessonDetailDrawerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should not render anything when lesson is null', () => {
    expect(fixture.nativeElement.textContent.trim()).toBe('');
  });

  it('should render lesson details and trigger review submissions', () => {
    const reviewSpy = jest.fn();
    component.submitReview.subscribe(reviewSpy);

    fixture.componentRef.setInput('lesson', mockLesson);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Moment of Inertia');
    expect(compiled.textContent).toContain('1. Core Concept');
    expect(compiled.textContent).toContain('2. Trap Alert');
    expect(compiled.textContent).toContain('3. Formula Cheat Sheet');
    expect(compiled.textContent).toContain('4. Exemplar Breakdown');

    const buttons = compiled.querySelectorAll('button');
    const approveBtn = Array.from(buttons).find((b) => b.textContent?.includes('Approve'))!;
    const rejectBtn = Array.from(buttons).find((b) => b.textContent?.includes('Reject'))!;
    const publishBtn = Array.from(buttons).find((b) => b.textContent?.includes('Publish Live'))!;

    approveBtn.click();
    expect(reviewSpy).toHaveBeenCalledWith('APPROVE');

    rejectBtn.click();
    expect(reviewSpy).toHaveBeenCalledWith('REJECT');

    publishBtn.click();
    expect(reviewSpy).toHaveBeenCalledWith('PUBLISH');
  });

  it('should toggle edit mode and emit edit actions', () => {
    const startEditSpy = jest.fn();
    const saveEditSpy = jest.fn();

    component.startEdit.subscribe(startEditSpy);
    component.saveEdit.subscribe(saveEditSpy);

    fixture.componentRef.setInput('lesson', mockLesson);
    fixture.componentRef.setInput('isEditingContent', false);
    fixture.detectChanges();

    const editBtn = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
    editBtn.click();
    expect(startEditSpy).toHaveBeenCalledWith(mockLesson);

    fixture.componentRef.setInput('isEditingContent', true);
    fixture.detectChanges();

    const saveBtn = Array.from(fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>)
      .find((b) => b.textContent?.includes('Save Changes'))!;
    saveBtn.click();
    expect(saveEditSpy).toHaveBeenCalled();
  });
});
