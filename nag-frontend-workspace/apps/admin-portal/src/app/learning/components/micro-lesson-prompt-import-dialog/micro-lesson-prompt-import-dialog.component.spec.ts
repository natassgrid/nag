import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { of } from 'rxjs';
import {
  MicroLessonPromptImportDialogComponent,
  MicroLessonPromptImportDialogData,
} from './micro-lesson-prompt-import-dialog.component';
import { ExternalMicroLessonPromptService } from '../../services/external-microlesson-prompt.service';
import { SubjectHierarchy, SubjectTopicService } from '@nag-frontend-workspace/questions-data-access';

describe('MicroLessonPromptImportDialogComponent', () => {
  let component: MicroLessonPromptImportDialogComponent;
  let fixture: ComponentFixture<MicroLessonPromptImportDialogComponent>;
  let dialogRefMock: { close: jest.Mock };
  let promptService: ExternalMicroLessonPromptService;
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
            { id: 100, name: 'Work-Energy Theorem' },
            { id: 101, name: 'Rotational Motion' },
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
          name: 'Thermodynamics',
          subtopics: [{ id: 200, name: 'Enthalpy and Entropy' }],
        },
      ],
    },
  ];

  const mockData: MicroLessonPromptImportDialogData = {
    subject: 'Physics',
    topic: 'Mechanics',
    subtopic: 'Work-Energy Theorem',
    targetExam: 'JEE_MAIN',
    referenceText: 'Test reference note',
    activeTab: 'prompt',
  };

  beforeEach(async () => {
    dialogRefMock = { close: jest.fn() };
    subjectTopicServiceMock = {
      getHierarchy: jest.fn().mockReturnValue(of(mockHierarchy)),
      getExaminations: jest.fn().mockReturnValue(of([
        { id: '1', name: 'JEE Main & Advanced', code: 'JEE_MAIN' },
        { id: '2', name: 'NEET-UG', code: 'NEET' },
      ])),
    };

    await TestBed.configureTestingModule({
      imports: [MicroLessonPromptImportDialogComponent],
      providers: [
        { provide: MatDialogRef, useValue: dialogRefMock },
        { provide: MAT_DIALOG_DATA, useValue: mockData },
        { provide: SubjectTopicService, useValue: subjectTopicServiceMock },
        ExternalMicroLessonPromptService,
      ],
    }).compileComponents();

    promptService = TestBed.inject(ExternalMicroLessonPromptService);
    fixture = TestBed.createComponent(MicroLessonPromptImportDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and initialize with dynamically loaded taxonomy data', () => {
    expect(component).toBeTruthy();
    expect(subjectTopicServiceMock.getHierarchy).toHaveBeenCalled();
    expect(component.hierarchy().length).toBe(2);
    expect(component.selectedSubjectId()).toBe(1);
    expect(component.selectedTopicId()).toBe(10);
    expect(component.selectedSubtopicId()).toBe(100);
    expect(component.resolvedSubjectName()).toBe('Physics');
    expect(component.resolvedTopicName()).toBe('Mechanics');
    expect(component.resolvedSubtopicName()).toBe('Work-Energy Theorem');
    expect(component.targetExam()).toBe('JEE_MAIN');
    expect(component.activeTab()).toBe('prompt');
  });

  it('should dynamically update cascading topics and subtopics when subject changes', () => {
    component.onSubjectSelect('2'); // Chemistry
    expect(component.selectedSubjectId()).toBe(2);
    expect(component.resolvedSubjectName()).toBe('Chemistry');
    expect(component.availableTopics().length).toBe(1);
    expect(component.selectedTopicId()).toBe(20);
    expect(component.resolvedTopicName()).toBe('Thermodynamics');
    expect(component.availableSubtopics().length).toBe(1);
    expect(component.selectedSubtopicId()).toBe(200);
    expect(component.resolvedSubtopicName()).toBe('Enthalpy and Entropy');
  });

  it('should toggle tabs', () => {
    component.setTab('import');
    expect(component.activeTab()).toBe('import');
    component.setTab('prompt');
    expect(component.activeTab()).toBe('prompt');
  });

  it('should generate real-time prompt with dynamic taxonomy names', () => {
    const prompt = component.generatedPrompt();
    expect(prompt).toContain('Work-Energy Theorem');
    expect(prompt).toContain('JEE_MAIN');
    expect(prompt).toContain('Physics');
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
    expect(component.jsonInput()).toContain('coreConcept');
    expect(component.jsonInput()).toContain('trapAlert');
  });

  it('should validate and close dialog with imported lesson data when valid', () => {
    component.jsonInput.set(
      JSON.stringify({
        coreConcept: 'Valid core concept.',
        trapAlert: { description: 'Trap description.' },
        formulaEntries: [{ name: 'F1', latex: '$$a=b$$' }],
        stepByStepBreakdown: {
          problemStatement: 'Problem statement',
          steps: [{ title: 'Step 1' }],
          finalAnswer: '$$10$$',
        },
      })
    );

    component.validateAndImport();
    expect(dialogRefMock.close).toHaveBeenCalledWith(
      expect.objectContaining({
        coreConcept: 'Valid core concept.',
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
