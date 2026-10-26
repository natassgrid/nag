import { TestBed } from '@angular/core/testing';
import { ExternalPromptService, QuestionPromptConfig } from './external-prompt.service';

describe('ExternalPromptService', () => {
  let service: ExternalPromptService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ExternalPromptService],
    });
    service = TestBed.inject(ExternalPromptService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('generateQuestionPrompt', () => {
    it('should generate a detailed question prompt with schema and guidelines', () => {
      const config: QuestionPromptConfig = {
        subject: 'Physics',
        topic: 'Rotational Motion',
        subtopic: 'Moment of Inertia',
        targetExam: 'JEE_MAIN',
        questionType: 'SINGLE_MCQ',
        difficulty: 'HARD',
        cognitiveLevel: 'APPLY',
        marks: 4,
        negativeMarks: 1,
        additionalInstructions: 'Focus on composite bodies with parallel axis theorem.',
      };

      const prompt = service.generateQuestionPrompt(config);

      expect(prompt).toContain('Physics');
      expect(prompt).toContain('Rotational Motion');
      expect(prompt).toContain('Moment of Inertia');
      expect(prompt).toContain('JEE_MAIN');
      expect(prompt).toContain('SINGLE_MCQ');
      expect(prompt).toContain('HARD');
      expect(prompt).toContain('APPLY');
      expect(prompt).toContain('$$...$$');
      expect(prompt).toContain('composite bodies with parallel axis theorem');
      expect(prompt).toContain('Expected JSON Output Schema');
      expect(prompt).toContain('```json');
      expect(prompt).toContain('enclosed inside markdown code fences');
      expect(prompt).toContain('"questionType": "SINGLE_MCQ"');
    });

    it('should fallback to default values when optional fields are empty', () => {
      const prompt = service.generateQuestionPrompt({
        questionType: 'SINGLE_MCQ',
        difficulty: 'MEDIUM',
        cognitiveLevel: 'UNDERSTAND',
      });

      expect(prompt).toContain('General Science / Mathematics');
      expect(prompt).toContain('Core Topic');
      expect(prompt).toContain('Key Sub-topic');
      expect(prompt).toContain('Positive Marks: 4');
      expect(prompt).toContain('Negative Marks: 1');
    });
  });

  describe('validateAndParseQuestionJson', () => {
    it('should return error if input is empty', () => {
      const res = service.validateAndParseQuestionJson('');
      expect(res.valid).toBe(false);
      expect(res.errors[0]).toContain('empty');
    });

    it('should return error if JSON syntax is invalid', () => {
      const res = service.validateAndParseQuestionJson('{ invalid json');
      expect(res.valid).toBe(false);
      expect(res.errors[0]).toContain('Invalid JSON');
    });

    it('should successfully parse valid SINGLE_MCQ question wrapped in markdown fences', () => {
      const jsonObj = {
        content: 'A body of mass $$m = 2\\text{ kg}$$ is rotated at angular velocity $$\\omega = 5\\text{ rad/s}$$. What is its kinetic energy?',
        questionType: 'SINGLE_MCQ',
        difficulty: 'MEDIUM',
        cognitiveLevel: 'APPLY',
        marks: 4,
        negativeMarks: 1,
        options: [
          { id: 'A', text: '$$25\\text{ J}$$', isCorrect: true },
          { id: 'B', text: '$$50\\text{ J}$$', isCorrect: false },
          { id: 'C', text: '$$75\\text{ J}$$', isCorrect: false },
          { id: 'D', text: '$$100\\text{ J}$$', isCorrect: false },
        ],
        answerKey: 'A',
        explanation: 'Rotational kinetic energy is $$K = \\frac{1}{2}I\\omega^2$$.',
      };
      const raw = '```json\n' + JSON.stringify(jsonObj, null, 2) + '\n```';

      const res = service.validateAndParseQuestionJson(raw);
      expect(res.valid).toBe(true);
      expect(res.errors.length).toBe(0);
      expect(res.data).toBeDefined();
      expect(res.data?.content).toContain('mass $$m = 2\\text{ kg}$$');
      expect(res.data?.type).toBe('MULTIPLE_CHOICE');
      expect(res.data?.options.length).toBe(4);
      expect(res.data?.answerKey).toBe('A');
      expect(res.data?.options.find((o) => o.id === 'A')?.isCorrect).toBe(true);
    });

    it('should match answerKey to options if isCorrect is omitted in option objects', () => {
      const raw = JSON.stringify({
        content: 'Solve for $$x$$ in $$2x + 4 = 10$$',
        type: 'SINGLE_MCQ',
        options: [
          { id: 'A', text: '1' },
          { id: 'B', text: '3' },
          { id: 'C', text: '5' },
          { id: 'D', text: '7' },
        ],
        answerKey: 'B',
      });

      const res = service.validateAndParseQuestionJson(raw);
      expect(res.valid).toBe(true);
      const bOpt = res.data?.options.find((o) => o.id === 'B');
      expect(bOpt?.isCorrect).toBe(true);
      const aOpt = res.data?.options.find((o) => o.id === 'A');
      expect(aOpt?.isCorrect).toBe(false);
    });

    it('should validate MULTI_MCQ questions with multiple correct options', () => {
      const raw = JSON.stringify({
        content: 'Which of the following are vector quantities?',
        type: 'MULTI_MCQ',
        options: [
          { id: 'A', text: 'Velocity', isCorrect: true },
          { id: 'B', text: 'Speed', isCorrect: false },
          { id: 'C', text: 'Acceleration', isCorrect: true },
          { id: 'D', text: 'Temperature', isCorrect: false },
        ],
        answerKey: 'A,C',
      });

      const res = service.validateAndParseQuestionJson(raw);
      expect(res.valid).toBe(true);
      expect(res.data?.type).toBe('MULTIPLE_SELECT');
      expect(res.data?.answerKey).toBe('A,C');
    });

    it('should reject SINGLE_MCQ with multiple correct options', () => {
      const raw = JSON.stringify({
        content: 'Question statement',
        type: 'SINGLE_MCQ',
        options: [
          { id: 'A', text: 'A', isCorrect: true },
          { id: 'B', text: 'B', isCorrect: true },
        ],
      });

      const res = service.validateAndParseQuestionJson(raw);
      expect(res.valid).toBe(false);
      expect(res.errors[0]).toContain('requires exactly 1 correct option');
    });

    it('should reject missing content', () => {
      const raw = JSON.stringify({
        type: 'SINGLE_MCQ',
        options: [{ id: 'A', text: 'A', isCorrect: true }, { id: 'B', text: 'B', isCorrect: false }],
      });

      const res = service.validateAndParseQuestionJson(raw);
      expect(res.valid).toBe(false);
      expect(res.errors[0]).toContain('content');
    });

    it('should handle single question inside a 1-element array', () => {
      const raw = JSON.stringify([
        {
          content: 'Array wrapped question',
          type: 'NUMERICAL',
          answerKey: '42',
        },
      ]);

      const res = service.validateAndParseQuestionJson(raw);
      expect(res.valid).toBe(true);
      expect(res.data?.type).toBe('NUMERICAL');
      expect(res.data?.content).toBe('Array wrapped question');
    });
  });
});
