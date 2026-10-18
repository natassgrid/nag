import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MicroLessonFiltersComponent } from './micro-lesson-filters.component';

describe('MicroLessonFiltersComponent', () => {
  let component: MicroLessonFiltersComponent;
  let fixture: ComponentFixture<MicroLessonFiltersComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MicroLessonFiltersComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(MicroLessonFiltersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should emit searchChange on input change', () => {
    const searchSpy = jest.fn();
    component.searchChange.subscribe(searchSpy);

    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    input.value = 'Rotational';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(searchSpy).toHaveBeenCalledWith('Rotational');
  });

  it('should emit statusChange on status button click', () => {
    const statusSpy = jest.fn();
    component.statusChange.subscribe(statusSpy);

    fixture.componentRef.setInput('statusOptions', [
      { label: 'All', value: 'ALL' },
      { label: 'AI Generated', value: 'AI_GENERATED' },
    ]);
    fixture.detectChanges();

    const buttons = fixture.nativeElement.querySelectorAll('button[type="button"]');
    const aiGenBtn = Array.from(buttons).find((b: any) => b.textContent.includes('AI Generated')) as HTMLButtonElement;
    expect(aiGenBtn).toBeTruthy();
    aiGenBtn.click();

    expect(statusSpy).toHaveBeenCalledWith('AI_GENERATED');
  });
});
