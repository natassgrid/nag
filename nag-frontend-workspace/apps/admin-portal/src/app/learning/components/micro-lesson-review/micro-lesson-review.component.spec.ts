import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MicroLessonReviewComponent } from './micro-lesson-review.component';
import { MicroLessonAdminService } from '../../services/micro-lesson-admin.service';
import { of } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { MicroLessonReviewItem } from '../../models/micro-lesson.model';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';

describe('MicroLessonReviewComponent', () => {
  let component: MicroLessonReviewComponent;
  let fixture: ComponentFixture<MicroLessonReviewComponent>;
  let microLessonServiceMock: any;
  let snackBarMock: any;
  let dialogMock: any;

  const mockLessons: MicroLessonReviewItem[] = [
    {
      id: 'ml-1',
      subtopicId: 101,
      subtopicName: 'Bayes Theorem & Conditional Probability',
      topicName: 'Probability',
      subjectName: 'Mathematics',
      targetExam: 'GATE-CS',
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
      reviewMicroLesson: jest.fn(),
      triggerBatchGenerate: jest.fn(),
      generateSingle: jest.fn(),
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
      imports: [MicroLessonReviewComponent, NoopAnimationsModule],
      providers: [
        { provide: MicroLessonAdminService, useValue: microLessonServiceMock },
        { provide: MatSnackBar, useValue: snackBarMock },
        { provide: MatDialog, useValue: dialogMock },
        { provide: ActivatedRoute, useValue: { snapshot: { params: {} } } },
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

  it('should create and populate computed metrics', () => {
    expect(component).toBeTruthy();
    expect(component.lessons().length).toBe(3);
    expect(component.totalCount()).toBe(3);
    expect(component.aiGeneratedCount()).toBe(1);
    expect(component.approvedCount()).toBe(2);
  });

  it('should filter lessons by cascading subject and topic', () => {
    expect(component.distinctSubjects()).toEqual(['ALL', 'Mathematics', 'Physics']);

    component.selectedSubject.set('Mathematics');
    expect(component.distinctTopics()).toEqual(['ALL', 'Linear Algebra', 'Probability']);
    expect(component.filteredLessons().length).toBe(2);

    component.selectedTopic.set('Probability');
    expect(component.filteredLessons().length).toBe(1);
    expect(component.filteredLessons()[0].id).toBe('ml-1');
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

  it('should open generate dialog and prepend created micro-lesson', () => {
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
    expect(component.lessons()[0].id).toBe('ml-new');
    expect(component.selectedLesson()?.id).toBe('ml-new');
  });

  it('should open batch generate dialog and display snackbar on queue success', () => {
    const mockBatchRes = {
      batchId: 'batch-test-1',
      totalQueued: 10,
      status: 'QUEUED',
      message: 'Queued 10 micro-lessons for batch generation.',
    };

    dialogMock.open.mockReturnValue({
      afterClosed: () => of(mockBatchRes),
    });

    component.openBatchGenerateDialog();
    expect(dialogMock.open).toHaveBeenCalled();
    expect(snackBarMock.open).toHaveBeenCalledWith(
      'Queued 10 micro-lessons for batch generation.',
      'OK',
      expect.objectContaining({ duration: 4000 })
    );
  });
});
