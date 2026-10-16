import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MicroLessonBulkToolbarComponent } from './micro-lesson-bulk-toolbar.component';

describe('MicroLessonBulkToolbarComponent', () => {
  let component: MicroLessonBulkToolbarComponent;
  let fixture: ComponentFixture<MicroLessonBulkToolbarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MicroLessonBulkToolbarComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(MicroLessonBulkToolbarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should not display anything when selectedCount is 0', () => {
    expect(fixture.nativeElement.textContent.trim()).toBe('');
  });

  it('should display selected count and emit actions', () => {
    const bulkSubmitSpy = jest.fn();
    const bulkDeleteSpy = jest.fn();
    const clearSpy = jest.fn();

    component.bulkSubmit.subscribe(bulkSubmitSpy);
    component.bulkDelete.subscribe(bulkDeleteSpy);
    component.clearSelection.subscribe(clearSpy);

    fixture.componentRef.setInput('selectedCount', 3);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('3 micro-lessons selected');

    const buttons = compiled.querySelectorAll('button');
    const approveBtn = Array.from(buttons).find((b) => b.textContent?.includes('Approve Selected'))!;
    const deleteBtn = Array.from(buttons).find((b) => b.textContent?.includes('Delete Selected'))!;
    const deselectBtn = Array.from(buttons).find((b) => b.textContent?.includes('Deselect All'))!;

    approveBtn.click();
    expect(bulkSubmitSpy).toHaveBeenCalledWith('APPROVE');

    deleteBtn.click();
    expect(bulkDeleteSpy).toHaveBeenCalled();

    deselectBtn.click();
    expect(clearSpy).toHaveBeenCalled();
  });
});
