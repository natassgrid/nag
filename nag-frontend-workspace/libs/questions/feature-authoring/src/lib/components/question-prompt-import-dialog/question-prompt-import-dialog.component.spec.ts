import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { of } from 'rxjs';
import {
  QuestionPromptImportDialogComponent,
  QuestionPromptImportDialogData,
} from './question-prompt-import-dialog.component';
import { ExternalPromptService } from '../../services/external-prompt.service';
import { SubjectHierarchy, SubjectTopicService } from '@nag-frontend-workspace/questions-data-access';

describe('QuestionPromptImportDialogComponent', () => {
  let component: QuestionPromptImportDialogComponent;
  let fixture: ComponentFixture<QuestionPromptImportDialogComponent>;
  let dialogRefMock: { close: jest.Mock };
  let promptService: ExternalPromptService;
  let subjectTopicServiceMock: any;

  const mockHierarchy: SubjectHierarchy[] = [
    {
      id: 1,
      name: 'Physics',
      topics: [
        {
          id: 10,
          name: 'Mechanics',
          subtopics: [
            { id: 100, name: 'Kinematics' },
            { id: 101, name: 'Dynamics' },
          ],
        },
      ],
    },
    {
      id: 2,
      name: 'Chemistry',
      topics: [
        {
          id: 20,
          name: 'Organic Chemistry',
          subtopics: [{ id: 200, name: 'Alkanes' }],
        },
      ],
    },
  ];

  const mockData: QuestionPromptImportDialogData = {
    subject: 'Physics',
    topic: 'Mechanics',
    subtopic: 'Kinematics',
    targetExam: 'JEE_MAIN',
    questionType: 'SINGLE_MCQ',
    difficulty: 'MEDIUM',
    cognitiveLevel: 'APPLY',
    marks: 4,
    negativeMarks: 1,
    activeTab: 'prompt',
  };

  beforeEach(async () => {
    dialogRefMock = { close: jest.fn() };
    subjectTopicServiceMock = {
      getHierarchy: jest.fn().mockReturnValue(of(mockHierarchy)),
      getExaminations: jest.fn().mockReturnValue(of([
        { id: '1', name: 'JEE Main', code: 'JEE_MAIN' },
        { id: '2', name: 'NEET-UG', code: 'NEET' },
      ])),
    };

    await TestBed.configureTestingModule({
      imports: [QuestionPromptImportDialogComponent],
      providers: [
        { provide: MatDialogRef, useValue: dialogRefMock },
        { provide: MAT_DIALOG_DATA, useValue: mockData },
        { provide: SubjectTopicService, useValue: subjectTopicServiceMock },
        ExternalPromptService,
      ],
    }).compileComponents();

    promptService = TestBed.inject(ExternalPromptService);
    fixture = TestBed.createComponent(QuestionPromptImportDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and dynamically load taxonomy hierarchy on init', () => {
    expect(component).toBeTruthy();
    expect(subjectTopicServiceMock.getHierarchy).toHaveBeenCalled();
    expect(component.hierarchy().length).toBe(2);
    expect(component.selectedSubjectId()).toBe(1);
    expect(component.selectedTopicId()).toBe(10);
    expect(component.selectedSubtopicId()).toBe(100);
    expect(component.resolvedSubjectName()).toBe('Physics');
    expect(component.resolvedTopicName()).toBe('Mechanics');
    expect(component.resolvedSubtopicName()).toBe('Kinematics');
  });

  it('should dynamically update cascading topics and subtopics when subject changes', () => {
    component.onSubjectSelect('2'); // Select Chemistry
    expect(component.selectedSubjectId()).toBe(2);
    expect(component.resolvedSubjectName()).toBe('Chemistry');
    expect(component.availableTopics().length).toBe(1);
    expect(component.selectedTopicId()).toBe(20);
    expect(component.resolvedTopicName()).toBe('Organic Chemistry');
    expect(component.availableSubtopics().length).toBe(1);
    expect(component.selectedSubtopicId()).toBe(200);
    expect(component.resolvedSubtopicName()).toBe('Alkanes');
  });

  it('should support switching to custom subject and topic inputs', () => {
    component.onSubjectSelect('__CUSTOM__');
    expect(component.isCustomSubject()).toBe(true);
    expect(component.selectedSubjectId()).toBeNull();

    component.customSubjectName.set('Astrophysics');
    component.customTopicName.set('Black Holes');
    component.customSubtopicName.set('Event Horizon');

    expect(component.resolvedSubjectName()).toBe('Astrophysics');
    expect(component.resolvedTopicName()).toBe('Black Holes');
    expect(component.resolvedSubtopicName()).toBe('Event Horizon');
  });

  it('should switch tabs', () => {
    component.setTab('import');
    expect(component.activeTab()).toBe('import');
    component.setTab('prompt');
    expect(component.activeTab()).toBe('prompt');
  });

  it('should generate real-time prompt computed from dynamic inputs', () => {
    const prompt = component.generatedPrompt();
    expect(prompt).toContain('Physics');
    expect(prompt).toContain('Mechanics');
    expect(prompt).toContain('Kinematics');
    expect(prompt).toContain('SINGLE_MCQ');
  });

  it('should copy prompt to clipboard', async () => {
    jest.spyOn(promptService, 'copyToClipboard').mockResolvedValue(true);
    await component.copyPrompt();
    expect(component.copied()).toBe(true);
  });

  it('should download prompt text file', () => {
    const spy = jest.spyOn(promptService, 'downloadFile').mockImplementation();
    component.downloadPrompt();
    expect(spy).toHaveBeenCalled();
  });

  it('should load sample JSON into import textarea', () => {
    component.loadSampleJson();
    expect(component.jsonInput()).toContain('questionType');
    expect(component.jsonInput()).toContain('content');
  });

  it('should validate and close dialog with imported question data when valid', () => {
    component.jsonInput.set(
      JSON.stringify({
        content: 'Valid test question?',
        questionType: 'SINGLE_MCQ',
        difficulty: 'MEDIUM',
        cognitiveLevel: 'APPLY',
        marks: 4,
        negativeMarks: 1,
        options: [
          { id: 'A', text: 'Option 1', isCorrect: true },
          { id: 'B', text: 'Option 2', isCorrect: false },
        ],
        answerKey: 'A',
      })
    );

    component.validateAndImport();
    expect(dialogRefMock.close).toHaveBeenCalledWith(
      expect.objectContaining({
        content: 'Valid test question?',
        type: 'MULTIPLE_CHOICE',
        subject: 'Physics',
      })
    );
  });

  it('should not close dialog and display errors when JSON is invalid', () => {
    component.jsonInput.set('{"invalid": true}');
    component.validateAndImport();
    expect(component.validationErrors().length).toBeGreaterThan(0);
    expect(dialogRefMock.close).not.toHaveBeenCalled();
  });

  it('should close on close()', () => {
    component.close();
    expect(dialogRefMock.close).toHaveBeenCalledWith(null);
  });
});
