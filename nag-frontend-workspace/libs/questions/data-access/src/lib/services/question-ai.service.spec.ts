import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { QuestionAiService } from './question-ai.service';
import { QuestionGenerationRequest } from '../models/ai-generation.model';

describe('QuestionAiService', () => {
  let service: QuestionAiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [QuestionAiService],
    });

    service = TestBed.inject(QuestionAiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should generate questions via AI endpoint', (done) => {
    const reqPayload: QuestionGenerationRequest = {
      subject: 'Physics',
      topic: 'Quantum Mechanics',
      difficulty: 'HARD',
      count: 5,
      type: 'SINGLE_CHOICE',
    };

    service.generateQuestions(reqPayload).subscribe((res) => {
      expect(res.questions.length).toBe(1);
      done();
    });

    const req = httpMock.expectOne('/api/v1/questions/generate');
    expect(req.request.method).toBe('POST');
    req.flush({
      data: {
        questions: [
          {
            content: 'What is Heisenberg Uncertainty Principle?',
            type: 'SINGLE_CHOICE',
            difficulty: 'HARD',
            options: [],
            explanation: 'dx * dp >= hbar / 2',
          },
        ],
        modelUsed: 'gemini-1.5-pro',
      },
    });
  });

  it('should submit and track batch generation job', (done) => {
    service.submitBatchJob({ prompt: 'Generate 100 questions', totalCount: 100 } as any).subscribe((job) => {
      expect(job.id).toBe('job-123');
      expect(job.status).toBe('PENDING');
      done();
    });

    const req = httpMock.expectOne('/api/v1/questions/batch');
    expect(req.request.method).toBe('POST');
    req.flush({ data: { id: 'job-123', status: 'PENDING', totalQuestions: 100, completedQuestions: 0 } });
  });
});
