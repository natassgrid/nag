import { provideRouter } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MicroLessonReviewComponent } from './micro-lesson-review.component';
import { MicroLessonAdminService } from '../../services/micro-lesson-admin.service';
import {
  NotificationService,
  MathRendererComponent,
} from '@nag-frontend-workspace/shared-ui-components';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import {
  MicroLessonReviewItem,
  adaptToReviewItem,
  BatchGenerateResponse,
} from '../../models/micro-lesson.model';

describe('MicroLessonReviewComponent', () => {
  let component: MicroLessonReviewComponent;
  let fixture: ComponentFixture<MicroLessonReviewComponent>;
  let microLessonServiceMock: any;
  let notificationServiceMock: any;
  let snackBarMock: any;
  let dialogMock: any;

  const mockLessons: MicroLessonReviewItem[] = [
    {
      id: 'ml-1',
      subtopicId: 101,
      subtopicName: 'Bayes Theorem Application',
      topicName: 'Probability',
      subjectName: 'Mathematics',
      targetExam: 'JEE-ADVANCED',
      status: 'AI_GENERATED',
      contentVersion: 1,
      reviewerNotes: null,
      content: {
        coreConcept: 'Bayes Theorem relates $P(A|B)$ to $P(B|A)$.',
        trapAlert: 'Do not confuse false positive rate with posterior.',
        formulaCheatSheet: '$$P(A|B) = \\frac{P(B|A)P(A)}{P(B)}$$',
        stepByStepBreakdown: {
          problemStatement: 'Given test accuracy 99% and prevalence 0.1%...',
          steps: [
            {
              stepNumber: 1,
              title: 'Identify prior',
              explanation: 'Prior is 0.001',
              formula: 'P(D) = 0.001',
            },
          ],
        },
      },
    },
    {
      id: 'ml-2',
      subtopicId: 102,
      subtopicName: 'Eigenvalues & Diagonalization',
      topicName: 'Linear Algebra',
      subjectName: 'Mathematics',
      targetExam: 'GATE-CS',
      status: 'APPROVED',
      contentVersion: 2,
      reviewerNotes: 'Verified clean',
      content: {
        coreConcept: 'Eigenvalues satisfy $\\det(A - \\lambda I) = 0$.',
        trapAlert: 'Repeated eigenvalues might not yield distinct eigenvectors.',
        formulaCheatSheet: '$$Av = \\lambda v$$',
        stepByStepBreakdown: {
          problemStatement: 'Find eigenvalues of 2x2 matrix',
          steps: [],
        },
      },
    },
    {
      id: 'ml-3',
      subtopicId: 201,
      subtopicName: 'Moment of Inertia',
      topicName: 'Rotational Mechanics',
      subjectName: 'Physics',
      targetExam: 'JEE-MAIN',
      status: 'PUBLISHED',
      contentVersion: 1,
      reviewerNotes: 'Production ready',
      content: {
        coreConcept: 'Moment of Inertia $I = \\int r^2 dm$.',
        trapAlert: 'Remember parallel axis theorem shift.',
        formulaCheatSheet: '$$I = I_{cm} + Md^2$$',
        stepByStepBreakdown: {
          problemStatement: 'Calculate I for thin rod',
          steps: [],
        },
      },
    },
  ];

  beforeEach(async () => {
    microLessonServiceMock = {
      listForReview: jest.fn().mockReturnValue(
        of({
          content: mockLessons,
          totalElements: 3,
          totalPages: 1,
          number: 0,
          size: 20,
        })
      ),
      getStats: jest.fn().mockReturnValue(
        of({
          total: 3,
          aiGenerated: 1,
          inReview: 0,
          approved: 1,
          published: 1,
          rejected: 0,
        })
      ),
      getFilterOptions: jest.fn().mockReturnValue(
        of({
          subjects: ['Mathematics', 'Physics'],
          topics: ['Linear Algebra', 'Probability', 'Rotational Mechanics'],
          statuses: ['ALL', 'AI_GENERATED', 'IN_REVIEW', 'APPROVED', 'PUBLISHED', 'REJECTED'],
        })
      ),
      deleteMicroLesson: jest.fn().mockReturnValue(of(undefined)),
      bulkDelete: jest.fn().mockReturnValue(of({ deletedCount: 2 })),
      reviewMicroLesson: jest.fn(),
      triggerBatchGenerate: jest.fn(),
      generateSingle: jest.fn(),
      getActiveBatchJob: jest.fn().mockReturnValue(of(null)),
      getBatchJobStatus: jest.fn().mockReturnValue(of(null)),
      cancelBatchJob: jest.fn().mockReturnValue(of(null)),
    };

    notificationServiceMock = {
      confirm: jest.fn().mockResolvedValue(true),
    };

    snackBarMock = {
      open: jest.fn(),
    };

    dialogMock = {
      open: jest.fn().mockReturnValue({
        afterClosed: () => of(null),
      }),
    };

    await TestBed.configureTestingModule({
      imports: [
        MicroLessonReviewComponent,
        MathRendererComponent,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MicroLessonAdminService, useValue: microLessonServiceMock },
        { provide: NotificationService, useValue: notificationServiceMock },
        { provide: MatSnackBar, useValue: snackBarMock },
        { provide: MatDialog, useValue: dialogMock },
        provideRouter([]),
      ],
    })
    .overrideComponent(MicroLessonReviewComponent, {
      set: {
        providers: [
          { provide: MatDialog, useValue: dialogMock },
        ],
      },
    })
    .compileComponents();

    fixture = TestBed.createComponent(MicroLessonReviewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load data on init', () => {
    expect(component).toBeTruthy();
    expect(microLessonServiceMock.listForReview).toHaveBeenCalled();
    expect(microLessonServiceMock.getStats).toHaveBeenCalled();
    expect(microLessonServiceMock.getFilterOptions).toHaveBeenCalled();
    expect(microLessonServiceMock.getActiveBatchJob).toHaveBeenCalled();
    expect(component.lessons().length).toBe(3);
    expect(component.totalCount()).toBe(3);
    expect(component.aiGeneratedCount()).toBe(1);
    expect(component.approvedCount()).toBe(2);
  });

  it('should filter lessons by dynamic subjects and topics from database', () => {
    component.onSubjectChange('Mathematics');
    expect(component.selectedSubject()).toBe('Mathematics');
    expect(component.selectedTopic()).toBe('ALL');
    expect(microLessonServiceMock.getFilterOptions).toHaveBeenCalledWith('Mathematics');
    expect(microLessonServiceMock.listForReview).toHaveBeenCalledWith(
      expect.objectContaining({ subject: 'Mathematics' })
    );

    component.onTopicChange('Probability');
    expect(component.selectedTopic()).toBe('Probability');
    expect(microLessonServiceMock.listForReview).toHaveBeenCalledWith(
      expect.objectContaining({ topic: 'Probability' })
    );
  });

  it('should perform server-side pagination transitions', () => {
    microLessonServiceMock.listForReview.mockReturnValue(
      of({
        content: mockLessons,
        totalElements: 30,
        totalPages: 3,
        number: 0,
        size: 10,
      })
    );
    component.totalPages.set(3);
    component.pageIndex.set(0);

    component.nextPage();
    expect(component.pageIndex()).toBe(1);

    component.prevPage();
    expect(component.pageIndex()).toBe(0);

    component.lastPage();
    expect(component.pageIndex()).toBe(2);

    component.firstPage();
    expect(component.pageIndex()).toBe(0);

    component.onPageSizeChange(50);
    expect(component.pageSize()).toBe(50);
    expect(component.pageIndex()).toBe(0);
  });

  it('should prompt confirmation and delete single micro-lesson', async () => {
    const lessonToDelete = mockLessons[0];
    await component.deleteLesson(lessonToDelete);

    expect(notificationServiceMock.confirm).toHaveBeenCalledWith(
      expect.objectContaining({
        title: 'Delete Micro-Lesson?',
        type: 'danger',
      })
    );
    expect(microLessonServiceMock.deleteMicroLesson).toHaveBeenCalledWith('ml-1');
    expect(snackBarMock.open).toHaveBeenCalledWith(
      'Deleted micro-lesson: "Bayes Theorem Application"',
      'OK',
      expect.any(Object)
    );
  });

  it('should prompt confirmation and bulk delete selected micro-lessons', async () => {
    component.selectedIds.set(new Set(['ml-1', 'ml-2']));
    await component.bulkDeleteSelected();

    expect(notificationServiceMock.confirm).toHaveBeenCalledWith(
      expect.objectContaining({
        title: 'Delete 2 Micro-Lessons?',
        type: 'danger',
      })
    );
    expect(microLessonServiceMock.bulkDelete).toHaveBeenCalledWith(['ml-1', 'ml-2']);
    expect(component.selectedIds().size).toBe(0);
    expect(snackBarMock.open).toHaveBeenCalledWith(
      'Successfully deleted 2 micro-lessons.',
      'OK',
      expect.any(Object)
    );
  });

  it('should manage multi-selection and bulk submission', () => {
    expect(component.selectedIds().size).toBe(0);

    // Toggle select individual
    component.toggleSelect('ml-1', { stopPropagation: () => {} } as any);
    expect(component.selectedIds().has('ml-1')).toBe(true);
    expect(component.selectedIds().size).toBe(1);

    // Toggle select all
    component.toggleSelectAll(true);
    expect(component.selectedIds().size).toBe(3);
    expect(component.isAllSelected()).toBe(true);

    // Bulk submit APPROVE
    microLessonServiceMock.reviewMicroLesson.mockReturnValue(
      of({ ...mockLessons[0], status: 'APPROVED' })
    );
    component.bulkSubmit('APPROVE');
    expect(microLessonServiceMock.reviewMicroLesson).toHaveBeenCalledTimes(3);

    // Clear selection
    component.clearSelection();
    expect(component.selectedIds().size).toBe(0);
  });

  it('should support in-place content editing with KaTeX formulas', () => {
    component.selectLesson(mockLessons[0]);
    expect(component.isEditingContent()).toBe(false);

    component.startEditing(mockLessons[0]);
    expect(component.isEditingContent()).toBe(true);
    expect(component.editedCoreConcept()).toContain('Bayes Theorem');

    component.editedCoreConcept.set('Updated Core Concept with $P(A|B)$');
    const updatedResponse: MicroLessonReviewItem = {
      ...mockLessons[0],
      content: {
        ...mockLessons[0].content,
        coreConcept: 'Updated Core Concept with $P(A|B)$',
      },
    };
    microLessonServiceMock.reviewMicroLesson.mockReturnValue(of(updatedResponse));

    component.saveEditedContent();
    expect(microLessonServiceMock.reviewMicroLesson).toHaveBeenCalledWith(
      'ml-1',
      expect.objectContaining({
        action: 'EDIT',
        updatedContent: expect.objectContaining({
          coreConcept: 'Updated Core Concept with $P(A|B)$',
        }),
      })
    );
    expect(component.isEditingContent()).toBe(false);
  });

  it('should open generate dialog and reload lessons and stats', () => {
    const newItem: MicroLessonReviewItem = {
      id: 'ml-new',
      subtopicId: 999,
      subtopicName: 'Synthesized Subtopic',
      topicName: 'Custom Topic',
      subjectName: 'Custom Subject',
      targetExam: 'GATE-CS',
      status: 'AI_GENERATED',
      contentVersion: 1,
      reviewerNotes: null,
      content: {
        coreConcept: 'Concept',
        trapAlert: 'Trap',
        formulaCheatSheet: '$$F=ma$$',
        stepByStepBreakdown: { problemStatement: 'PS', steps: [] },
      },
    };

    dialogMock.open.mockReturnValue({
      afterClosed: () => of(newItem),
    });

    component.openGenerateDialog();
    expect(dialogMock.open).toHaveBeenCalled();
    expect(component.selectedLesson()?.id).toBe('ml-new');
    expect(microLessonServiceMock.listForReview).toHaveBeenCalled();
    expect(microLessonServiceMock.getStats).toHaveBeenCalled();
  });

  it('should handle raw backend response from generate dialog and not display undefined in snackbar', () => {
    const rawBackendItem: any = {
      id: '01a14085-5838-71ac-a705-9d025b34bba1',
      subjectId: 8,
      topicId: 80,
      subtopicId: 176,
      subject: 'Mathematics',
      topic: 'Arithmetic',
      subtopic: 'Percentage',
      targetExam: 'IBPS-PO-PRE-2027',
      coreConcept: 'Percentage bedrock principle.',
      trap: 'Sign conventions trap.',
      cheatSheet: ['$$\\Delta K + \\Delta U = W_{ext}$$'],
      stepByStepBreakdown: {
        questionStem: 'Evaluate percentage',
        steps: ['Step 1: Write down equation'],
        finalAnswer: '100%'
      },
      status: 'AI_GENERATED',
      contentVersion: 2,
    };

    dialogMock.open.mockReturnValue({
      afterClosed: () => of(rawBackendItem),
    });

    component.openGenerateDialog();
    expect(dialogMock.open).toHaveBeenCalled();
    expect(component.selectedLesson()?.subtopicName).toBe('Percentage');
    expect(component.selectedLesson()?.content.coreConcept).toBe('Percentage bedrock principle.');
    expect(snackBarMock.open).toHaveBeenCalledWith(
      'Synthesized micro-lesson: "Percentage"',
      'View',
      expect.objectContaining({ duration: 4000 })
    );
  });

  it('adaptToReviewItem should map backend fields to frontend model', () => {
    const raw = {
      id: 'abc',
      subject: 'Math',
      topic: 'Algebra',
      subtopic: 'Quadratics',
      coreConcept: 'Core text',
      trap: 'Trap text',
      cheatSheet: ['$f_1$', '$f_2$'],
      stepByStepBreakdown: {
        questionStem: 'Solve x^2 = 4',
        steps: ['Step 1: square root'],
        finalAnswer: 'x = \\pm 2'
      },
      status: 'AI_GENERATED',
      contentVersion: 1
    };
    const adapted = adaptToReviewItem(raw);
    expect(adapted.subtopicName).toBe('Quadratics');
    expect(adapted.topicName).toBe('Algebra');
    expect(adapted.subjectName).toBe('Math');
    expect(adapted.content.coreConcept).toBe('Core text');
    expect(adapted.content.trapAlert).toBe('Trap text');
    expect(adapted.content.formulaCheatSheet).toBe('$f_1$\n\n$f_2$');
    expect(adapted.content.stepByStepBreakdown?.problemStatement).toBe('Solve x^2 = 4');
    expect(adapted.content.stepByStepBreakdown?.steps.length).toBe(1);
    expect(adapted.content.stepByStepBreakdown?.finalAnswer).toBe('x = \\pm 2');
  });

  it('should open batch generate dialog and track active batch progress', () => {
    const mockBatchRes: BatchGenerateResponse = {
      batchId: 'batch-test-1',
      totalQueued: 10,
      totalGenerated: 2,
      totalFailed: 0,
      percentComplete: 20,
      currentSubtopic: 'Subtopic 3',
      status: 'IN_PROGRESS',
      message: 'Batch generation in progress...',
    };

    dialogMock.open.mockReturnValue({
      afterClosed: () => of(mockBatchRes),
    });

    component.openBatchGenerateDialog();
    expect(dialogMock.open).toHaveBeenCalled();
    expect(component.activeBatchJob()).toEqual(mockBatchRes);
  });

  it('should cancel active batch job when requested from progress banner', () => {
    const mockActiveJob: BatchGenerateResponse = {
      batchId: 'batch-active-99',
      totalQueued: 10,
      totalGenerated: 4,
      totalFailed: 0,
      percentComplete: 40,
      status: 'IN_PROGRESS',
      message: 'In progress',
    };
    component.activeBatchJob.set(mockActiveJob);

    const cancelledJob: BatchGenerateResponse = {
      ...mockActiveJob,
      status: 'CANCELLED',
      message: 'Batch generation cancelled.',
    };
    microLessonServiceMock.cancelBatchJob.mockReturnValue(of(cancelledJob));

    component.cancelActiveBatch();

    expect(microLessonServiceMock.cancelBatchJob).toHaveBeenCalledWith('batch-active-99');
    expect(component.activeBatchJob()?.status).toBe('CANCELLED');
    expect(snackBarMock.open).toHaveBeenCalledWith('Batch generation cancelled.', 'OK', expect.any(Object));
  });

  it('should dismiss batch banner when dismissed', () => {
    component.activeBatchJob.set({
      batchId: 'batch-1',
      totalQueued: 5,
      totalGenerated: 5,
      totalFailed: 0,
      percentComplete: 100,
      status: 'COMPLETED',
    });
    expect(component.activeBatchJob()).toBeTruthy();

    component.dismissBatchBanner();
    expect(component.activeBatchJob()).toBeNull();
  });
});
