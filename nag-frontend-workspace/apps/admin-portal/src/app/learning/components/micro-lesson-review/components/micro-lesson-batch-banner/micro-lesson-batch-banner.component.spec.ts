import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MicroLessonBatchBannerComponent } from './micro-lesson-batch-banner.component';
import { BatchGenerateResponse } from '../../../../models/micro-lesson.model';

describe('MicroLessonBatchBannerComponent', () => {
  let component: MicroLessonBatchBannerComponent;
  let fixture: ComponentFixture<MicroLessonBatchBannerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MicroLessonBatchBannerComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(MicroLessonBatchBannerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should not display anything when batchJob is null', () => {
    expect(fixture.nativeElement.textContent.trim()).toBe('');
  });

  it('should render progress and emit cancel when batch is in progress', () => {
    const cancelSpy = jest.fn();
    component.cancel.subscribe(cancelSpy);

    const mockJob: BatchGenerateResponse = {
      batchId: 'batch-123',
      status: 'IN_PROGRESS',
      totalQueued: 10,
      totalGenerated: 4,
      totalFailed: 0,
      percentComplete: 40,
      currentSubtopic: 'Projectile Motion',
    };

    fixture.componentRef.setInput('batchJob', mockJob);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('AI Batch Generation Running');
    expect(compiled.textContent).toContain('Projectile Motion');
    expect(compiled.textContent).toContain('40%');

    const cancelBtn = compiled.querySelector('button:last-child') as HTMLButtonElement;
    cancelBtn.click();
    expect(cancelSpy).toHaveBeenCalled();
  });

  it('should render completed state and dismiss button', () => {
    const dismissSpy = jest.fn();
    component.dismiss.subscribe(dismissSpy);

    const mockJob: BatchGenerateResponse = {
      batchId: 'batch-123',
      status: 'COMPLETED',
      totalQueued: 5,
      totalGenerated: 5,
      totalFailed: 0,
      percentComplete: 100,
    };

    fixture.componentRef.setInput('batchJob', mockJob);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Batch Generation Complete');

    const dismissBtn = compiled.querySelector('button') as HTMLButtonElement;
    expect(dismissBtn.textContent).toContain('Dismiss');
    dismissBtn.click();
    expect(dismissSpy).toHaveBeenCalled();
  });
});
