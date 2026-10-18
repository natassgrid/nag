import { ComponentFixture, TestBed } from '@angular/core/testing';
import { GenerateMicroLessonDialogComponent } from './generate-micro-lesson-dialog.component';
import { MicroLessonAdminService } from '../../services/micro-lesson-admin.service';
import { SubjectTopicService } from '@nag-frontend-workspace/questions-data-access';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MicroLessonReviewItem } from '../../models/micro-lesson.model';
import { BatchGenerateMicroLessonDialogComponent } from '../batch-generate-dialog/batch-generate-dialog.component';

describe('GenerateMicroLessonDialogComponent', () => {
  let component: GenerateMicroLessonDialogComponent;
  let fixture: ComponentFixture<GenerateMicroLessonDialogComponent>;
  let microLessonServiceMock: any;
  let subjectTopicServiceMock: any;
  let dialogRefMock: any;
  let dialogMock: any;

  const mockHierarchy = [
    {
      id: 1,
      name: 'Physics',
      code: 'PHYS',
      topics: [
        {
          id: 10,
          name: 'Mechanics',
          subtopics: [
            { id: 100, name: 'Rotational Dynamics' },
            { id: 101, name: 'Kinematics' },
          ],
        },
        {
          id: 11,
          name: 'Thermodynamics',
          subtopics: [
            { id: 110, name: 'Carnot Engine' },
          ],
        },
      ],
    },
    {
      id: 2,
      name: 'Mathematics',
      code: 'MATH',
      topics: [
        {
          id: 20,
          name: 'Calculus',
          subtopics: [
            { id: 200, name: 'Integration' },
          ],
        },
      ],
    },
  ];

  beforeEach(async () => {
    microLessonServiceMock = {
      generateSingle: jest.fn(),
    };

    const mockExams = [
      { id: '1', name: 'JEE Main & Advanced', code: 'JEE_MAIN', category: 'ENTRANCE' },
      { id: '2', name: 'NEET-UG', code: 'NEET', category: 'ENTRANCE' },
    ];

    subjectTopicServiceMock = {
      getHierarchy: jest.fn().mockReturnValue(of(mockHierarchy)),
      getExaminations: jest.fn().mockReturnValue(of(mockExams)),
    };

    dialogRefMock = {
      close: jest.fn(),
    };

    dialogMock = {
      open: jest.fn().mockReturnValue({
        afterClosed: () => of(null),
      }),
    };

    await TestBed.configureTestingModule({
      imports: [GenerateMicroLessonDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: MicroLessonAdminService, useValue: microLessonServiceMock },
        { provide: SubjectTopicService, useValue: subjectTopicServiceMock },
        { provide: MatDialogRef, useValue: dialogRefMock },
        { provide: MatDialog, useValue: dialogMock },
      ],
    })
    .overrideComponent(GenerateMicroLessonDialogComponent, {
      set: {
        providers: [
          { provide: MatDialog, useValue: dialogMock },
        ],
      },
    })
    .compileComponents();

    fixture = TestBed.createComponent(GenerateMicroLessonDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load taxonomy hierarchy and examinations on init', () => {
    expect(component).toBeTruthy();
    expect(subjectTopicServiceMock.getHierarchy).toHaveBeenCalled();
    expect(subjectTopicServiceMock.getExaminations).toHaveBeenCalled();
    expect(component.examOptions().length).toBe(2);
    expect(component.targetExam()).toBe('JEE_MAIN');
    expect(component.hierarchy().length).toBe(2);
    expect(component.selectedSubjectId()).toBe(1);
    expect(component.selectedSubjectName()).toBe('Physics');
    expect(component.selectedTopicId()).toBe(10);
    expect(component.selectedTopicName()).toBe('Mechanics');
    expect(component.selectedSubtopicId()).toBe(100);
    expect(component.selectedSubtopicName()).toBe('Rotational Dynamics');
  });

  it('should cascade available topics and subtopics when subject changes', () => {
    component.onSubjectChange('2');

    expect(component.selectedSubjectId()).toBe(2);
    expect(component.selectedSubjectName()).toBe('Mathematics');
    expect(component.availableTopics().length).toBe(1);
    expect(component.selectedTopicId()).toBe(20);
    expect(component.selectedTopicName()).toBe('Calculus');
    expect(component.selectedSubtopicId()).toBe(200);
    expect(component.selectedSubtopicName()).toBe('Integration');
  });

  it('should cascade available subtopics when topic changes', () => {
    component.onTopicChange('11');

    expect(component.selectedTopicId()).toBe(11);
    expect(component.selectedTopicName()).toBe('Thermodynamics');
    expect(component.availableSubtopics().length).toBe(1);
    expect(component.selectedSubtopicId()).toBe(110);
    expect(component.selectedSubtopicName()).toBe('Carnot Engine');
  });

  it('should switch to custom inputs when custom option is selected', () => {
    component.onSubjectChange('__CUSTOM__');
    expect(component.isCustomSubject()).toBe(true);
    expect(component.selectedSubjectId()).toBeNull();

    component.onTopicChange('__CUSTOM__');
    expect(component.isCustomTopic()).toBe(true);
    expect(component.selectedTopicId()).toBeNull();

    component.onSubtopicChange('__CUSTOM__');
    expect(component.isCustomSubtopic()).toBe(true);
    expect(component.selectedSubtopicId()).toBeNull();
  });

  it('should switch to batch dialog when switchToBatch is called', () => {
    component.switchToBatch();
    expect(dialogRefMock.close).toHaveBeenCalledWith(null);
    expect(dialogMock.open).toHaveBeenCalledWith(
      BatchGenerateMicroLessonDialogComponent,
      expect.objectContaining({ width: '560px' })
    );
  });

  it('should validate subtopic name before triggering synthesis', () => {
    component.selectedSubtopicName.set('');
    component.onGenerate();

    expect(component.errorMessage()).toBe('Subtopic name is required.');
    expect(microLessonServiceMock.generateSingle).not.toHaveBeenCalled();
  });

  it('should call generateSingle with IDs and close dialog on success', () => {
    const mockCreated: MicroLessonReviewItem = {
      id: 'ml-created',
      subtopicId: 100,
      subtopicName: 'Rotational Dynamics',
      topicName: 'Mechanics',
      subjectName: 'Physics',
      targetExam: 'JEE_MAIN',
      status: 'AI_GENERATED',
      contentVersion: 1,
      reviewerNotes: null,
      content: {
        coreConcept: 'Trajectory equation',
        trapAlert: 'Air resistance is neglected',
        formulaCheatSheet: '$$y = x\\tan\\theta$$',
        stepByStepBreakdown: { problemStatement: 'Find range', steps: [] },
      },
    };

    microLessonServiceMock.generateSingle.mockReturnValue(of(mockCreated));

    component.referenceText.set('Focus on torque and angular momentum.');
    component.onGenerate();

    expect(microLessonServiceMock.generateSingle).toHaveBeenCalledWith({
      subjectId: 1,
      subject: 'Physics',
      topicId: 10,
      topic: 'Mechanics',
      subtopicId: 100,
      subtopic: 'Rotational Dynamics',
      targetExam: 'JEE_MAIN',
      lessonType: 'CONCEPT',
      difficultyLevel: 'CORE',
      referenceText: 'Focus on torque and angular momentum.',
    });
    expect(dialogRefMock.close).toHaveBeenCalledWith(mockCreated);
    expect(component.isGenerating()).toBe(false);
  });

  it('should display error message on generation failure', () => {
    microLessonServiceMock.generateSingle.mockReturnValue(
      throwError(() => ({ error: { message: 'LLM service timeout' } }))
    );

    component.onGenerate();

    expect(component.errorMessage()).toBe('LLM service timeout');
    expect(component.isGenerating()).toBe(false);
    expect(dialogRefMock.close).not.toHaveBeenCalled();
  });

  it('should close dialog with null on cancel', () => {
    component.onCancel();
    expect(dialogRefMock.close).toHaveBeenCalledWith(null);
  });
  it('should support custom target examination entry', () => {
    component.onExamChange('__CUSTOM__');
    expect(component.isCustomExam()).toBe(true);
    expect(component.targetExam()).toBe('');

    component.targetExam.set('CUSTOM_ENTRANCE_TEST');
    expect(component.targetExam()).toBe('CUSTOM_ENTRANCE_TEST');
  });
});