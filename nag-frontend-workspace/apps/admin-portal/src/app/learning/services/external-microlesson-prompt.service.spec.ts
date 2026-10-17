import { TestBed } from '@angular/core/testing';
import {
  ExternalMicroLessonPromptService,
  MicroLessonPromptConfig,
} from './external-microlesson-prompt.service';

describe('ExternalMicroLessonPromptService', () => {
  let service: ExternalMicroLessonPromptService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ExternalMicroLessonPromptService],
    });
    service = TestBed.inject(ExternalMicroLessonPromptService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('generateMicroLessonPrompt', () => {
    it('should generate a complete pedagogical micro-lesson prompt', () => {
      const config: MicroLessonPromptConfig = {
        subject: 'Physics',
        topic: 'Thermodynamics',
        subtopic: 'Carnot Engine Efficiency',
        targetExam: 'JEE_ADVANCED',
        referenceText: 'Focus on reversible vs irreversible cycle calculation traps.',
      };

      const prompt = service.generateMicroLessonPrompt(config);

      expect(prompt).toContain('Physics');
      expect(prompt).toContain('Thermodynamics');
      expect(prompt).toContain('Carnot Engine Efficiency');
      expect(prompt).toContain('JEE_ADVANCED');
      expect(prompt).toContain('3 simple, lucid sentences');
      expect(prompt).toContain('The Trap (trapAlert)');
      expect(prompt).toContain('The Cheat Sheet (formulaEntries)');
      expect(prompt).toContain('Step-by-Step Breakdown');
      expect(prompt).toContain('reversible vs irreversible cycle calculation traps');
      expect(prompt).toContain('$$...$$');
      expect(prompt).toContain('Expected JSON Output Schema');
    });
  });

  describe('validateAndParseMicroLessonJson', () => {
    it('should reject empty input', () => {
      const res = service.validateAndParseMicroLessonJson('');
      expect(res.valid).toBe(false);
      expect(res.errors[0]).toContain('empty');
    });

    it('should reject invalid JSON syntax', () => {
      const res = service.validateAndParseMicroLessonJson('{ invalid json');
      expect(res.valid).toBe(false);
      expect(res.errors[0]).toContain('Invalid JSON');
    });

    it('should parse valid micro-lesson with structured fields', () => {
      const jsonObj = {
        targetExam: 'JEE_MAIN',
        subtopicCustomName: 'Work-Energy Theorem',
        coreConcept: 'The Work-Energy Theorem states that net work equals change in kinetic energy.',
        trapAlert: {
          title: 'Frame Trap',
          description: 'Neglecting pseudo force in accelerating frame.',
          commonMistake: 'Using Newton second law in accelerating frame.',
          correctApproach: 'Include pseudo force.',
        },
        formulaEntries: [
          {
            name: 'Work Energy',
            latex: '$$W = \\Delta K$$',
            description: 'Net work',
            variables: ['W: Work'],
          },
        ],
        stepByStepBreakdown: {
          problemStatement: 'A block is pushed with force $$F = 10\\text{ N}$$.',
          steps: [
            {
              stepNumber: 1,
              title: 'Find Work',
              explanation: '$$W = Fd$$',
              formula: '$$W = 50\\text{ J}$$',
              tip: 'Check direction',
            },
          ],
          finalAnswer: '$$v = 5\\text{ m/s}$$',
        },
        keyTakeaways: ['Always check frame of reference'],
        quickCheckItems: [
          {
            question: 'Is work a scalar?',
            options: ['Yes', 'No'],
            answer: 'Yes',
            explanation: 'Work is dot product of force and displacement.',
          },
        ],
      };

      const raw = '```json\n' + JSON.stringify(jsonObj, null, 2) + '\n```';
      const res = service.validateAndParseMicroLessonJson(raw);

      expect(res.valid).toBe(true);
      expect(res.errors.length).toBe(0);
      expect(res.data).toBeDefined();
      expect(res.data?.coreConcept).toContain('Work-Energy Theorem');
      expect(res.data?.trapAlert.title).toBe('Frame Trap');
      expect(res.data?.formulaEntries.length).toBe(1);
      expect(res.data?.steps.length).toBe(1);
      expect(res.data?.finalAnswer).toBe('$$v = 5\\text{ m/s}$$');
    });

    it('should tolerate legacy flat schema with string trap and cheatSheet array', () => {
      const jsonObj = {
        coreConcept: 'Core concept statement in three sentences.',
        trap: 'Common sign convention pitfall in thermodynamics.',
        cheatSheet: ['$$\\eta = 1 - \\frac{T_C}{T_H}$$', '$$W = Q_H - Q_C$$'],
        stepByStepBreakdown: {
          questionStem: 'Calculate the efficiency of a Carnot engine between 300K and 600K.',
          steps: [
            'Step 1: Convert temperatures to Kelvin if needed.',
            'Step 2: Apply formula $$\\eta = 1 - 300/600 = 0.5$$.',
          ],
          finalAnswer: '$$\\eta = 50\\%$$',
        },
      };

      const res = service.validateAndParseMicroLessonJson(JSON.stringify(jsonObj));
      expect(res.valid).toBe(true);
      expect(res.data?.trapAlert.description).toBe('Common sign convention pitfall in thermodynamics.');
      expect(res.data?.formulaEntries.length).toBe(2);
      expect(res.data?.steps.length).toBe(2);
      expect(res.data?.problemStatement).toContain('Calculate the efficiency');
      expect(res.data?.finalAnswer).toBe('$$\\eta = 50\\%$$');
    });

    it('should reject missing coreConcept', () => {
      const jsonObj = {
        trapAlert: { description: 'Trap description' },
        stepByStepBreakdown: { steps: [{ title: 'Step 1' }] },
      };

      const res = service.validateAndParseMicroLessonJson(JSON.stringify(jsonObj));
      expect(res.valid).toBe(false);
      expect(res.errors[0]).toContain('coreConcept');
    });
  });
});
