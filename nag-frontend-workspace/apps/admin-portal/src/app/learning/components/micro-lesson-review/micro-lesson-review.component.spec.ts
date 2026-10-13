import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MicroLessonReviewComponent } from './micro-lesson-review.component';
import { MicroLessonAdminService } from '../../services/micro-lesson-admin.service';
import { of } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MicroLessonReviewItem } from '../../models/micro-lesson.model';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('MicroLessonReviewComponent', () => {
  let component: MicroLessonReviewComponent;
  let fixture: ComponentFixture<MicroLessonReviewComponent>;
  let microLessonServiceMock: any;
  let snackBarMock: any;

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
  ];

  beforeEach(async () => {
    microLessonServiceMock = {
      listForReview: jest.fn().mockReturnValue(
        of({
          content: mockLessons,
          totalElements: 2,
          totalPages: 1,
          number: 0,
          size: 20,
        })
      ),
      reviewMicroLesson: jest.fn(),
      triggerBatchGenerate: jest.fn(),
    };

    snackBarMock = {
      open: jest.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [MicroLessonReviewComponent, NoopAnimationsModule],
      providers: [
        { provide: MicroLessonAdminService, useValue: microLessonServiceMock },
        { provide: MatSnackBar, useValue: snackBarMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MicroLessonReviewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(component.lessons().length).toBe(2);
    expect(component.totalCount()).toBe(2);
    expect(component.aiGeneratedCount()).toBe(1);
    expect(component.approvedCount()).toBe(1);
  });

  it('should filter lessons by status and search query', () => {
    component.statusFilter.set('APPROVED');
    expect(component.filteredLessons().length).toBe(1);
    expect(component.filteredLessons()[0].id).toBe('ml-2');

    component.statusFilter.set('ALL');
    component.searchQuery.set('Bayes');
    expect(component.filteredLessons().length).toBe(1);
    expect(component.filteredLessons()[0].id).toBe('ml-1');
  });

  it('should select a lesson and set reviewer notes', () => {
    component.selectLesson(mockLessons[0]);
    expect(component.selectedLesson()?.id).toBe('ml-1');
    expect(component.reviewerNotes()).toBe('');

    component.closePreview();
    expect(component.selectedLesson()).toBeNull();
  });

  it('should submit review action successfully', () => {
    const updatedItem: MicroLessonReviewItem = {
      ...mockLessons[0],
      status: 'APPROVED',
      reviewerNotes: 'Approved after review',
    };
    microLessonServiceMock.reviewMicroLesson.mockReturnValue(of(updatedItem));

    component.selectLesson(mockLessons[0]);
    component.reviewerNotes.set('Approved after review');
    component.submitReview('APPROVE');

    expect(microLessonServiceMock.reviewMicroLesson).toHaveBeenCalledWith(
      'ml-1',
      {
        action: 'APPROVE',
        reviewerNotes: 'Approved after review',
      }
    );
    expect(component.selectedLesson()?.status).toBe('APPROVED');
    expect(snackBarMock.open).toHaveBeenCalled();
  });

  it('should trigger batch generation', () => {
    microLessonServiceMock.triggerBatchGenerate.mockReturnValue(
      of({
        batchId: 'batch-1',
        totalQueued: 5,
        status: 'QUEUED',
        message: 'Queued 5 micro-lessons',
      })
    );

    component.triggerBatchGenerate();
    expect(microLessonServiceMock.triggerBatchGenerate).toHaveBeenCalled();
    expect(snackBarMock.open).toHaveBeenCalledWith(
      'Queued 5 micro-lessons',
      'OK',
      { duration: 4000 }
    );
  });
});
