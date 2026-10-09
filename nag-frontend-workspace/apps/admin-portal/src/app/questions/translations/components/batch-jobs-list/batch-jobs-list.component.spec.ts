import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BatchJobsListComponent } from './batch-jobs-list.component';
import { BatchTranslationJobResponse } from '@nag-frontend-workspace/questions-data-access';

describe('BatchJobsListComponent', () => {
  let component: BatchJobsListComponent;
  let fixture: ComponentFixture<BatchJobsListComponent>;

  const mockJobs: BatchTranslationJobResponse[] = [
    {
      id: 'job-1',
      status: 'IN_PROGRESS',
      sourceLanguage: 'en',
      targetLanguage: 'hi',
      targetStatus: 'PUBLISHED',
      totalQuestions: 10,
      processedQuestions: 5,
      successfulQuestions: 5,
      failedQuestions: 0,
      progressPercentage: 50,
      createdAt: '2026-10-09T10:00:00Z',
    },
    {
      id: 'job-2',
      status: 'COMPLETED',
      sourceLanguage: 'en',
      targetLanguage: 'ta',
      targetStatus: 'PUBLISHED',
      totalQuestions: 20,
      processedQuestions: 20,
      successfulQuestions: 20,
      failedQuestions: 0,
      progressPercentage: 100,
      createdAt: '2026-10-09T09:00:00Z',
    },
    {
      id: 'job-3',
      status: 'FAILED',
      sourceLanguage: 'en',
      targetLanguage: 'te',
      targetStatus: 'PUBLISHED',
      totalQuestions: 15,
      processedQuestions: 3,
      successfulQuestions: 2,
      failedQuestions: 1,
      progressPercentage: 20,
      createdAt: '2026-10-09T08:00:00Z',
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BatchJobsListComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(BatchJobsListComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('batchJobs', mockJobs);
    fixture.detectChanges();
  });

  it('should create BatchJobsListComponent', () => {
    expect(component).toBeTruthy();
  });

  it('should emit resumeJob event when Resume button is clicked on an IN_PROGRESS or FAILED job', () => {
    jest.spyOn(component.resumeJob, 'emit');

    const compiled = fixture.nativeElement as HTMLElement;
    const resumeButtons = Array.from(compiled.querySelectorAll('button')).filter((b) =>
      b.textContent?.includes('Resume')
    );

    expect(resumeButtons.length).toBeGreaterThanOrEqual(2);

    resumeButtons[0].click();
    expect(component.resumeJob.emit).toHaveBeenCalledWith('job-1');
  });

  it('should emit cancelJob event when Cancel Job button is clicked on an IN_PROGRESS job', () => {
    jest.spyOn(component.cancelJob, 'emit');

    const compiled = fixture.nativeElement as HTMLElement;
    const cancelButtons = Array.from(compiled.querySelectorAll('button')).filter((b) =>
      b.textContent?.includes('Cancel Job')
    );

    expect(cancelButtons.length).toBe(1);

    cancelButtons[0].click();
    expect(component.cancelJob.emit).toHaveBeenCalledWith('job-1');
  });

  it('should emit refreshJobs when Refresh button is clicked', () => {
    jest.spyOn(component.refreshJobs, 'emit');

    const compiled = fixture.nativeElement as HTMLElement;
    const refreshButton = Array.from(compiled.querySelectorAll('button')).find((b) =>
      b.textContent?.includes('Refresh Jobs')
    );

    expect(refreshButton).toBeTruthy();
    refreshButton?.click();
    expect(component.refreshJobs.emit).toHaveBeenCalled();
  });
});
