import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { of } from 'rxjs';
import { BatchTranslateMicroLessonDialogComponent } from './batch-translate-micro-lesson-dialog.component';
import { MicroLessonAdminService } from '../../services/micro-lesson-admin.service';
import { SubjectTopicService } from '@nag-frontend-workspace/questions-data-access';
import { MicroLessonBatchTranslationJob } from '../../models/micro-lesson.model';

describe('BatchTranslateMicroLessonDialogComponent', () => {
  let component: BatchTranslateMicroLessonDialogComponent;
  let fixture: ComponentFixture<BatchTranslateMicroLessonDialogComponent>;
  let mockDialogRef: { close: jest.Mock };
  let mockMicroLessonService: {
    startBatchTranslation: jest.Mock;
    getBatchTranslationJobStatus: jest.Mock;
    cancelBatchTranslationJob: jest.Mock;
  };
  let mockSubjectTopicService: {
    getHierarchy: jest.Mock;
  };

  const sampleJob: MicroLessonBatchTranslationJob = {
    id: 'job-123',
    status: 'IN_PROGRESS',
    sourceLanguage: 'en',
    targetLanguage: 'hi',
    totalLessons: 10,
    processedLessons: 5,
    successfulLessons: 5,
    failedLessons: 0,
    progressPercentage: 50,
  };

  beforeEach(async () => {
    mockDialogRef = { close: jest.fn() };
    mockMicroLessonService = {
      startBatchTranslation: jest.fn().mockReturnValue(of(sampleJob)),
      getBatchTranslationJobStatus: jest.fn().mockReturnValue(of(sampleJob)),
      cancelBatchTranslationJob: jest.fn().mockReturnValue(
        of({ ...sampleJob, status: 'CANCELLED' })
      ),
    };
    mockSubjectTopicService = {
      getHierarchy: jest.fn().mockReturnValue(
        of([
          {
            id: 1,
            name: 'Physics',
            topics: [{ id: 10, name: 'Kinematics', subtopics: [] }],
          },
        ])
      ),
    };

    await TestBed.configureTestingModule({
      imports: [BatchTranslateMicroLessonDialogComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: mockDialogRef },
        { provide: MicroLessonAdminService, useValue: mockMicroLessonService },
        { provide: SubjectTopicService, useValue: mockSubjectTopicService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(BatchTranslateMicroLessonDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load taxonomy on init', () => {
    expect(component).toBeTruthy();
    expect(mockSubjectTopicService.getHierarchy).toHaveBeenCalled();
    expect(component.hierarchy().length).toBe(1);
    expect(component.hierarchy()[0].name).toBe('Physics');
  });

  it('should update topics when subject changes', () => {
    component.onSubjectChange('Physics');
    expect(component.selectedSubject()).toBe('Physics');
    expect(component.availableTopics().length).toBe(1);
    expect(component.availableTopics()[0].name).toBe('Kinematics');
  });

  it('should trigger batch translation on startBatchTranslation', () => {
    component.targetLanguage.set('hi');
    component.selectedSubject.set('Physics');
    component.startBatchTranslation();

    expect(mockMicroLessonService.startBatchTranslation).toHaveBeenCalledWith(
      expect.objectContaining({
        targetLanguage: 'hi',
        subject: 'Physics',
      })
    );
    expect(component.isMonitoring()).toBe(true);
    expect(component.currentJob()?.id).toBe('job-123');
  });

  it('should cancel batch job when cancelJob is called', () => {
    component.currentJob.set(sampleJob);
    component.cancelJob();

    expect(mockMicroLessonService.cancelBatchTranslationJob).toHaveBeenCalledWith('job-123');
    expect(component.currentJob()?.status).toBe('CANCELLED');
  });

  it('should close dialog with job reference on onClose', () => {
    component.currentJob.set(sampleJob);
    component.onClose();
    expect(mockDialogRef.close).toHaveBeenCalledWith(sampleJob);
  });
});
