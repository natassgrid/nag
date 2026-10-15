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
    };

    subjectTopicServiceMock = {
      getHierarchy: jest.fn().mockReturnValue(of(mockHierarchy)),
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

  it('should create and load taxonomy on init', () => {
    expect(component).toBeTruthy();
    expect(subjectTopicServiceMock.getHierarchy).toHaveBeenCalled();
    expect(component.hierarchy().length).toBe(1);
    expect(component.targetExam()).toBe('JEE_MAIN');
    expect(component.maxCount()).toBe(10);
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

  it('should call triggerBatchGenerate with configured payload and close on success', () => {
    const mockResponse: BatchGenerateResponse = {
      batchId: 'batch-123',
      totalQueued: 5,
      status: 'QUEUED',
      message: 'Queued 5 micro-lessons for batch generation',
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
    expect(dialogRefMock.close).toHaveBeenCalledWith(mockResponse);
    expect(component.isGenerating()).toBe(false);
  });

  it('should set error message on batch generation failure', () => {
    microLessonServiceMock.triggerBatchGenerate.mockReturnValue(
      throwError(() => ({ error: { message: 'Quota exceeded for batch generation' } }))
    );

    component.onStartBatch();

    expect(component.errorMessage()).toBe('Quota exceeded for batch generation');
    expect(component.isGenerating()).toBe(false);
    expect(dialogRefMock.close).not.toHaveBeenCalled();
  });

  it('should close dialog with null on cancel', () => {
    component.onCancel();
    expect(dialogRefMock.close).toHaveBeenCalledWith(null);
  });
});
