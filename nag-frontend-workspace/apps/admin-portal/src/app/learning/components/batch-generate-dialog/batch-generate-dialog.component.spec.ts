import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BatchGenerateMicroLessonDialogComponent } from './batch-generate-dialog.component';
import { MicroLessonAdminService } from '../../services/micro-lesson-admin.service';
import { SubjectTopicService } from '@nag-frontend-workspace/questions-data-access';
import { MatDialogRef } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { BatchGenerateResponse } from '../../models/micro-lesson.model';

describe('BatchGenerateMicroLessonDialogComponent', () => {
  let component: BatchGenerateMicroLessonDialogComponent;
  let fixture: ComponentFixture<BatchGenerateMicroLessonDialogComponent>;
  let microLessonServiceMock: any;
  let subjectTopicServiceMock: any;
  let dialogRefMock: any;

  const mockHierarchy = [
    {
      id: 10,
      name: 'Physics',
      code: 'PHYS',
      topics: [
        {
          id: 101,
          name: 'Electrostatics',
          subtopics: [{ id: 1001, name: 'Gauss Law' }],
        },
      ],
    },
  ];

  beforeEach(async () => {
    microLessonServiceMock = {
      triggerBatchGenerate: jest.fn(),
      getActiveBatchJob: jest.fn().mockReturnValue(of(null)),
      getBatchJobStatus: jest.fn().mockReturnValue(of(null)),
      cancelBatchJob: jest.fn().mockReturnValue(of(null)),
    };

    const mockExams = [
      { id: '1', name: 'Joint Entrance Examination', code: 'JEE_MAIN', category: 'ENTRANCE' },
      { id: '2', name: 'National Eligibility Entrance Test', code: 'NEET', category: 'ENTRANCE' },
    ];

    subjectTopicServiceMock = {
      getHierarchy: jest.fn().mockReturnValue(of(mockHierarchy)),
      getExaminations: jest.fn().mockReturnValue(of(mockExams)),
    };

    dialogRefMock = {
      close: jest.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [BatchGenerateMicroLessonDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: MicroLessonAdminService, useValue: microLessonServiceMock },
        { provide: SubjectTopicService, useValue: subjectTopicServiceMock },
        { provide: MatDialogRef, useValue: dialogRefMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(BatchGenerateMicroLessonDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load taxonomy and examinations on init', () => {
    expect(component).toBeTruthy();
    expect(subjectTopicServiceMock.getHierarchy).toHaveBeenCalled();
    expect(subjectTopicServiceMock.getExaminations).toHaveBeenCalled();
    expect(component.hierarchy().length).toBe(1);
    expect(component.examOptions().length).toBe(2);
    expect(component.targetExam()).toBe('JEE_MAIN');
    expect(component.maxCount()).toBe(10);
    expect(component.isMonitoring()).toBe(false);
  });

  it('should allow custom examination code entry', () => {
    component.onExamChange('__CUSTOM__');
    expect(component.isCustomExam()).toBe(true);
    expect(component.targetExam()).toBe('');

    component.targetExam.set('CUSTOM_EXAM_99');
    expect(component.targetExam()).toBe('CUSTOM_EXAM_99');
  });

  it('should update topic scope when subject changes', () => {
    component.onSubjectChange('10');
    expect(component.selectedSubjectId()).toBe(10);
    expect(component.availableTopics().length).toBe(1);

    component.onTopicChange('101');
    expect(component.selectedTopicId()).toBe(101);

    component.onSubjectChange('ALL');
    expect(component.selectedSubjectId()).toBeNull();
    expect(component.selectedTopicId()).toBeNull();
    expect(component.availableTopics().length).toBe(0);
  });

  it('should call triggerBatchGenerate with configured payload and transition to live monitoring mode', () => {
    const mockResponse: BatchGenerateResponse = {
      batchId: 'batch-123',
      totalQueued: 5,
      totalGenerated: 0,
      totalFailed: 0,
      percentComplete: 0,
      status: 'IN_PROGRESS',
      message: 'Batch job queued with 5 subtopics',
    };
    microLessonServiceMock.triggerBatchGenerate.mockReturnValue(of(mockResponse));

    component.selectedSubjectId.set(10);
    component.selectedTopicId.set(101);
    component.targetExam.set('NEET');
    component.maxCount.set(5);
    component.forceRegenerate.set(true);

    component.onStartBatch();

    expect(microLessonServiceMock.triggerBatchGenerate).toHaveBeenCalledWith({
      subjectId: 10,
      topicId: 101,
      targetExam: 'NEET',
      maxCount: 5,
      forceRegenerate: true,
    });
    expect(component.isMonitoring()).toBe(true);
    expect(component.currentJob()?.batchId).toBe('batch-123');
    expect(component.isGenerating()).toBe(false);
  });

  it('should allow running in background and close dialog with current job', () => {
    const mockJob: BatchGenerateResponse = {
      batchId: 'batch-bg-1',
      totalQueued: 10,
      totalGenerated: 2,
      totalFailed: 0,
      percentComplete: 20,
      status: 'IN_PROGRESS',
    };
    component.currentJob.set(mockJob);
    component.isMonitoring.set(true);

    component.onRunInBackground();
    expect(dialogRefMock.close).toHaveBeenCalledWith(mockJob);
  });

  it('should allow cancelling ongoing batch job', () => {
    const mockJob: BatchGenerateResponse = {
      batchId: 'batch-cancel-1',
      totalQueued: 10,
      totalGenerated: 2,
      totalFailed: 0,
      status: 'IN_PROGRESS',
    };
    component.currentJob.set(mockJob);

    const cancelledJob: BatchGenerateResponse = {
      ...mockJob,
      status: 'CANCELLED',
      message: 'Batch job cancelled by user.',
    };
    microLessonServiceMock.cancelBatchJob.mockReturnValue(of(cancelledJob));

    component.onCancelJob();
    expect(microLessonServiceMock.cancelBatchJob).toHaveBeenCalledWith('batch-cancel-1');
    expect(component.currentJob()?.status).toBe('CANCELLED');
  });

  it('should set error message on batch generation failure', () => {
    microLessonServiceMock.triggerBatchGenerate.mockReturnValue(
      throwError(() => ({ error: { message: 'Quota exceeded for batch generation' } }))
    );

    component.onStartBatch();

    expect(component.errorMessage()).toBe('Quota exceeded for batch generation');
    expect(component.isGenerating()).toBe(false);
    expect(component.isMonitoring()).toBe(false);
  });

  it('should close dialog with null on cancel', () => {
    component.onCancel();
    expect(dialogRefMock.close).toHaveBeenCalledWith(null);
  });

  it('should close dialog on done when finished', () => {
    const mockJob: BatchGenerateResponse = {
      batchId: 'batch-done-1',
      totalQueued: 5,
      totalGenerated: 5,
      totalFailed: 0,
      percentComplete: 100,
      status: 'COMPLETED',
    };
    component.currentJob.set(mockJob);

    component.onDone();
    expect(dialogRefMock.close).toHaveBeenCalledWith(mockJob);
  });
});
