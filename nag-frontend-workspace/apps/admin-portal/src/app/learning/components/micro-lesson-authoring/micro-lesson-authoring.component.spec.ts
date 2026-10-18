import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { SubjectTopicService } from '@nag-frontend-workspace/questions-data-access';
import { MicroLessonAdminService } from '../../services/micro-lesson-admin.service';
import { MicroLessonAuthoringComponent } from './micro-lesson-authoring.component';
import { adaptToReviewItem } from '../../models/micro-lesson.model';

describe('MicroLessonAuthoringComponent', () => {
  let component: MicroLessonAuthoringComponent;
  let fixture: ComponentFixture<MicroLessonAuthoringComponent>;
  let microLessonServiceMock: any;
  let subjectTopicServiceMock: any;
  let routeQueryParamMap: Map<string, string>;

  const mockSubjects = [
    { id: 10, name: 'Physics' },
    { id: 20, name: 'Chemistry' },
  ];
  const mockTopics = [{ id: 100, name: 'Mechanics', subjectId: 10 }];
  const mockSubtopics = [{ id: 1000, name: 'Work, Energy & Power', topicId: 100 }];

  const mockExistingLesson = adaptToReviewItem({
    id: 'lesson-edit-1',
    subjectId: 10,
    topicId: 100,
    subtopicId: 1000,
    subject: 'Physics',
    topic: 'Mechanics',
    subtopic: 'Work, Energy & Power',
    targetExam: 'JEE_ADVANCED',
    status: 'APPROVED',
    contentVersion: 2,
    coreConcept: 'Conservation of mechanical energy $$E = K + U$$.',
    trap: 'Do not apply $$\\Delta K + \\Delta U = 0$$ when friction is present.',
    cheatSheet: ['Mechanical Energy: $$E = K + U$$'],
    stepByStepBreakdown: {
      questionStem: 'A 2 kg mass falls from 10 m.',
      steps: ['Step 1: Equate potential energy to kinetic energy $$mgh = \\frac{1}{2}mv^2$$'],
      finalAnswer: '$$v = 14\\text{ m/s}$$',
    },
    reviewerNotes: 'Faculty verified',
  });

  function createComponent(editId?: string) {
    routeQueryParamMap = new Map<string, string>();
    if (editId) {
      routeQueryParamMap.set('edit', editId);
    }

    TestBed.overrideProvider(ActivatedRoute, {
      useValue: {
        snapshot: {
          queryParamMap: {
            get: (key: string) => routeQueryParamMap.get(key) ?? null,
          },
        },
      },
    });

    fixture = TestBed.createComponent(MicroLessonAuthoringComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(async () => {
    microLessonServiceMock = {
      getMicroLessonById: jest.fn().mockReturnValue(of(mockExistingLesson)),
      createMicroLesson: jest.fn().mockReturnValue(
        of(
          adaptToReviewItem({
            id: 'new-lesson-1',
            subject: 'Physics',
            topic: 'Mechanics',
            subtopic: 'Work, Energy & Power',
            targetExam: 'JEE_MAIN',
            status: 'APPROVED',
            coreConcept: 'Created concept',
          })
        )
      ),
      updateMicroLesson: jest.fn().mockReturnValue(
        of({
          ...mockExistingLesson,
          subtopicName: 'Work, Energy & Power (Updated)',
        })
      ),
    };

    subjectTopicServiceMock = {
      getSubjects: jest.fn().mockReturnValue(of(mockSubjects)),
      getTopics: jest.fn().mockReturnValue(of(mockTopics)),
      getSubtopics: jest.fn().mockReturnValue(of(mockSubtopics)),
      getExaminations: jest.fn().mockReturnValue(
        of([
          { code: 'JEE_MAIN', name: 'JEE Main', category: 'ENGINEERING' },
          { code: 'JEE_ADVANCED', name: 'JEE Advanced', category: 'ENGINEERING' },
        ])
      ),
      createSubject: jest.fn().mockReturnValue(of({ id: 30, name: 'Mathematics' })),
      createTopic: jest.fn().mockReturnValue(of({ id: 300, name: 'Calculus', subjectId: 30 })),
    };

    await TestBed.configureTestingModule({
      imports: [MicroLessonAuthoringComponent, NoopAnimationsModule],
      providers: [
        { provide: MicroLessonAdminService, useValue: microLessonServiceMock },
        { provide: SubjectTopicService, useValue: subjectTopicServiceMock },
        provideRouter([]),
      ],
    }).compileComponents();
  });

  it('should create in manual authoring mode and load taxonomy hierarchy', () => {
    createComponent();
    expect(component).toBeTruthy();
    expect(component.editingLessonId()).toBeNull();
    expect(subjectTopicServiceMock.getSubjects).toHaveBeenCalled();
    expect(subjectTopicServiceMock.getTopics).toHaveBeenCalledWith(10);
    expect(subjectTopicServiceMock.getSubtopics).toHaveBeenCalledWith(10, 100);
    expect(component.selectedSubjectId()).toBe(10);
    expect(component.selectedTopicId()).toBe(100);
    expect(component.selectedSubtopicId()).toBe(1000);
    expect(component.resolvedSubtopicName()).toBe('Work, Energy & Power');
  });

  it('should save a new micro-lesson via createMicroLesson in create mode', () => {
    createComponent();
    component.coreConcept.set('Impulse-Momentum Theorem: $$\\vec{J} = \\Delta \\vec{p}$$');
    component.trapAlert.set({
      title: 'Vector Direction Trap',
      description: 'Momentum is a vector quantity; sign reversal on rebound doubles $$\\Delta p$$.',
      commonMistake: 'Writing $$\\Delta p = mv - mv = 0$$ on elastic rebound.',
      correctApproach: 'Use $$\\Delta p_x = m(v_f - (-v_i)) = 2mv$$.',
    });

    component.saveLesson();

    expect(microLessonServiceMock.createMicroLesson).toHaveBeenCalledWith(
      expect.objectContaining({
        subjectId: 10,
        subject: 'Physics',
        topicId: 100,
        topic: 'Mechanics',
        subtopicId: 1000,
        subtopic: 'Work, Energy & Power',
        targetExam: 'JEE_MAIN',
        status: 'APPROVED',
        coreConcept: 'Impulse-Momentum Theorem: $$\\vec{J} = \\Delta \\vec{p}$$',
      })
    );
    expect(component.feedback()?.type).toBe('success');
  });

  it('should load existing micro-lesson and update via updateMicroLesson in edit mode', () => {
    createComponent('lesson-edit-1');

    expect(component.editingLessonId()).toBe('lesson-edit-1');
    expect(microLessonServiceMock.getMicroLessonById).toHaveBeenCalledWith('lesson-edit-1');
    expect(component.targetExam()).toBe('JEE_ADVANCED');
    expect(component.coreConcept()).toContain('Conservation of mechanical energy');
    expect(component.problemStatement()).toBe('A 2 kg mass falls from 10 m.');

    component.coreConcept.set('Updated conservation of mechanical energy $$E = K + U$$');
    component.saveLesson();

    expect(microLessonServiceMock.updateMicroLesson).toHaveBeenCalledWith(
      'lesson-edit-1',
      expect.objectContaining({
        coreConcept: 'Updated conservation of mechanical energy $$E = K + U$$',
        targetExam: 'JEE_ADVANCED',
      })
    );
    expect(component.feedback()?.type).toBe('success');
  });

  it('should display validation error when coreConcept is empty', () => {
    createComponent();
    component.coreConcept.set('   ');
    component.saveLesson();

    expect(microLessonServiceMock.createMicroLesson).not.toHaveBeenCalled();
    expect(component.feedback()?.type).toBe('error');
    expect(component.feedback()?.message).toContain('Core Concept');
  });

  it('should handle API error gracefully on save', () => {
    createComponent();
    microLessonServiceMock.createMicroLesson.mockReturnValue(
      throwError(() => ({ error: { message: 'KaTeX validation failed: unbalanced $$' } }))
    );

    component.saveLesson();
    expect(component.saving()).toBe(false);
    expect(component.feedback()?.type).toBe('error');
    expect(component.feedback()?.message).toContain('KaTeX validation failed');
  });

  it('should reset form state when resetForm is called', () => {
    createComponent();
    component.resetForm();
    expect(component.coreConcept()).toBe('');
    expect(component.problemStatement()).toBe('');
    expect(component.finalAnswer()).toBe('');
    expect(component.formulaEntries().length).toBe(1);
    expect(component.steps().length).toBe(1);
  });

  it('should apply imported micro-lesson to UI without saving to backend', () => {
    createComponent();

    const imported = {
      targetExam: 'NEET',
      status: 'APPROVED',
      subtopicCustomName: 'Bernoulli Principle',
      coreConcept: 'Bernoulli principle relates pressure and fluid speed.',
      trapAlert: {
        title: 'Viscosity Pitfall',
        description: 'Do not apply to viscous fluids.',
        commonMistake: 'Ignoring viscous dissipation.',
        correctApproach: 'Verify Reynolds number.',
      },
      formulaEntries: [
        { name: 'Bernoulli Eq', latex: '$$P + \\frac{1}{2}\\rho v^2 + \\rho gh = C$$', description: 'Constant along streamline', variables: [] }
      ],
      problemStatement: 'Water flows through a constricted pipe.',
      steps: [
        { stepNumber: 1, title: 'Apply continuity', explanation: '$$A_1 v_1 = A_2 v_2$$', formula: '', tip: '' }
      ],
      finalAnswer: '$$P_2 = 1.2\\text{ atm}$$',
      keyTakeaways: ['Streamline flow only'],
      quickCheckItems: [],
      reviewerNotes: 'Imported from Claude 3.5',
    };

    component.applyImportedLesson(imported);

    expect(component.targetExam()).toBe('NEET');
    expect(component.coreConcept()).toBe(imported.coreConcept);
    expect(component.trapAlert().title).toBe('Viscosity Pitfall');
    expect(component.formulaEntries().length).toBe(1);
    expect(component.steps().length).toBe(1);
    expect(component.finalAnswer()).toBe('$$P_2 = 1.2\\text{ atm}$$');
    expect(component.customSubtopicName()).toBe('Bernoulli Principle');
    expect(microLessonServiceMock.createMicroLesson).not.toHaveBeenCalled();
    expect(component.feedback()?.type).toBe('success');
    expect(component.feedback()?.message).toContain('loaded into studio');
  });

  it('should navigate back to moderation desk', () => {
    createComponent();
    const router = TestBed.inject(Router);
    const navSpy = jest.spyOn(router, 'navigate').mockResolvedValue(true);

    component.backToModerationDesk();
    expect(navSpy).toHaveBeenCalledWith(['/learning/micro-lessons']);
  });

  it('should require Part 3 formula cheat sheet for Physics/Math/Numerical topics and block save when empty', () => {
    createComponent();
    expect(component.isFormulaRequired()).toBe(true);
    expect(component.formulaPolicy().mode).toBe('MANDATORY');

    component.formulaEntries.set([{ name: 'Formula 1', latex: '   ', description: '', variables: [] }]);
    component.saveLesson();

    expect(microLessonServiceMock.createMicroLesson).not.toHaveBeenCalled();
    expect(component.feedback()?.type).toBe('error');
    expect(component.feedback()?.message).toContain('Part 3 (KaTeX Formula Cheat Sheet) requires at least 1 formula');
  });

  it('should make Part 3 formula cheat sheet optional for non-Math/Science subjects like English or Indian Polity', () => {
    subjectTopicServiceMock.getSubjects.mockReturnValue(
      of([
        { id: 10, name: 'Physics' },
        { id: 40, name: 'English' },
      ])
    );
    subjectTopicServiceMock.getTopics.mockImplementation((subjId: number) =>
      subjId === 40
        ? of([{ id: 400, name: 'Grammar', subjectId: 40 }])
        : of(mockTopics)
    );
    subjectTopicServiceMock.getSubtopics.mockImplementation((subjId: number) =>
      subjId === 40
        ? of([{ id: 4000, name: 'Subject-Verb Agreement', topicId: 400 }])
        : of(mockSubtopics)
    );

    createComponent();
    component.onSubjectChange(40);

    expect(component.resolvedSubjectName()).toBe('English');
    expect(component.resolvedTopicName()).toBe('Grammar');
    expect(component.isFormulaRequired()).toBe(false);
    expect(component.formulaPolicy().mode).toBe('OPTIONAL_NOT_APPLICABLE');
    expect(component.formulaEntries().length).toBe(0);

    component.coreConcept.set('When two subjects are joined by "along with", the verb agrees with the first subject.');
    component.saveLesson();

    expect(microLessonServiceMock.createMicroLesson).toHaveBeenCalledWith(
      expect.objectContaining({
        subject: 'English',
        topic: 'Grammar',
        cheatSheet: [],
        formulaCheatSheet: [],
      })
    );
    expect(component.feedback()?.type).toBe('success');
  });
});
