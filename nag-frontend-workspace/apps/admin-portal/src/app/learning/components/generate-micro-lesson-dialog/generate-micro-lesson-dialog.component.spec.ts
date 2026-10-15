import { ComponentFixture, TestBed } from '@angular/core/testing';
import { GenerateMicroLessonDialogComponent } from './generate-micro-lesson-dialog.component';
import { MicroLessonAdminService } from '../../services/micro-lesson-admin.service';
import { MatDialogRef } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MicroLessonReviewItem } from '../../models/micro-lesson.model';

describe('GenerateMicroLessonDialogComponent', () => {
  let component: GenerateMicroLessonDialogComponent;
  let fixture: ComponentFixture<GenerateMicroLessonDialogComponent>;
  let microLessonServiceMock: any;
  let dialogRefMock: any;

  beforeEach(async () => {
    microLessonServiceMock = {
      generateSingle: jest.fn(),
    };

    dialogRefMock = {
      close: jest.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [GenerateMicroLessonDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: MicroLessonAdminService, useValue: microLessonServiceMock },
        { provide: MatDialogRef, useValue: dialogRefMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(GenerateMicroLessonDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and initialize default fields', () => {
    expect(component).toBeTruthy();
    expect(component.subject()).toBe('Physics');
    expect(component.targetExam()).toBe('JEE_MAIN');
    expect(component.mode()).toBe('SINGLE');
  });

  it('should validate subtopic name before triggering synthesis', () => {
    component.subtopic.set('');
    component.onGenerate();

    expect(component.errorMessage()).toBe('Subtopic name is required.');
    expect(microLessonServiceMock.generateSingle).not.toHaveBeenCalled();
  });

  it('should call generateSingle and close dialog on success', () => {
    const mockCreated: MicroLessonReviewItem = {
      id: 'ml-created',
      subtopicId: 1001,
      subtopicName: 'Projectile Motion',
      topicName: 'Kinematics',
      subjectName: 'Physics',
      targetExam: 'JEE_MAIN',
      status: 'AI_GENERATED',
      contentVersion: 1,
      reviewerNotes: null,
      content: {
        coreConcept: 'Trajectory equation',
        trapAlert: 'Air resistance is neglected',
        formulaCheatSheet: '$$y = x\\tan\\theta - \\frac{gx^2}{2u^2\\cos^2\\theta}$$',
        stepByStepBreakdown: { problemStatement: 'Find range', steps: [] },
      },
    };

    microLessonServiceMock.generateSingle.mockReturnValue(of(mockCreated));

    component.subtopic.set('Projectile Motion');
    component.topic.set('Kinematics');
    component.subject.set('Physics');
    component.referenceText.set('Focus on horizontal range formula.');
    component.onGenerate();

    expect(microLessonServiceMock.generateSingle).toHaveBeenCalledWith({
      subject: 'Physics',
      topic: 'Kinematics',
      subtopic: 'Projectile Motion',
      targetExam: 'JEE_MAIN',
      referenceText: 'Focus on horizontal range formula.',
    });
    expect(dialogRefMock.close).toHaveBeenCalledWith(mockCreated);
    expect(component.isGenerating()).toBe(false);
  });

  it('should display error message on generation failure', () => {
    microLessonServiceMock.generateSingle.mockReturnValue(
      throwError(() => ({ error: { message: 'LLM service timeout' } }))
    );

    component.subtopic.set('Rotational Inertia');
    component.onGenerate();

    expect(component.errorMessage()).toBe('LLM service timeout');
    expect(component.isGenerating()).toBe(false);
    expect(dialogRefMock.close).not.toHaveBeenCalled();
  });

  it('should close dialog with null on cancel', () => {
    component.onCancel();
    expect(dialogRefMock.close).toHaveBeenCalledWith(null);
  });
});
