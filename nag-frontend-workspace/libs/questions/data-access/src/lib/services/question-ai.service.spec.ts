import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { QuestionAiService } from './question-ai.service';
import {
  QuestionGenerationRequest,
  QuestionGenerationResponse,
  BatchGenerationRequest,
  BatchGenerationJob,
} from '../models/ai-generation.model';

describe('QuestionAiService', () => {
  let service: QuestionAiService;
  let httpMock: HttpTestingController;

  const baseUrl = '/api/v1/questions';

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

  // ---------------------------------------------------------------------------
  // generateQuestions
  // ---------------------------------------------------------------------------

  describe('generateQuestions()', () => {
    const minimalReq: QuestionGenerationRequest = {
      subject: 'Physics',
      topic: 'Quantum Mechanics',
      difficulty: 'HARD',
      cognitiveLevel: 'ANALYZE',
      questionType: 'SINGLE_MCQ',
      count: 3,
    };

    it('should POST to /generate and unwrap ApiResponse<QuestionGenerationResponse>', (done) => {
      const mockResponse: QuestionGenerationResponse = {
        questions: [{ content: 'Q1', difficulty: 'HARD', cognitiveLevel: 'ANALYZE', questionType: 'SINGLE_MCQ' }],
        modelUsed: 'qwen2.5-1.5b',
        totalGenerated: 1,
        totalValid: 1,
        totalDuplicates: 0,
      };

      service.generateQuestions(minimalReq).subscribe((res) => {
        expect(res.questions.length).toBe(1);
        expect(res.modelUsed).toBe('qwen2.5-1.5b');
        done();
      });

      const req = httpMock.expectOne(`${baseUrl}/generate`);
      expect(req.request.method).toBe('POST');
      req.flush({ status: 'success', data: mockResponse });
    });

    it('should forward subjectId and topicId in the request body', (done) => {
      const reqWithIds: QuestionGenerationRequest = {
        ...minimalReq,
        subjectId: 10,
        topicId: 42,
        subtopicId: 7,
      };

      service.generateQuestions(reqWithIds).subscribe(() => done());

      const req = httpMock.expectOne(`${baseUrl}/generate`);
      const body = req.request.body as QuestionGenerationRequest;
      expect(body.subjectId).toBe(10);
      expect(body.topicId).toBe(42);
      expect(body.subtopicId).toBe(7);
      req.flush({ status: 'success', data: { questions: [], modelUsed: 'test', totalGenerated: 0, totalValid: 0, totalDuplicates: 0 } });
    });

    it('should forward executionMode and generationQuality', (done) => {
      const reqWithMode: QuestionGenerationRequest = {
        ...minimalReq,
        executionMode: 'MULTI_AGENT',
        generationQuality: 'EXAM_READY',
        targetExam: 'JEE_ADV',
      };

      service.generateQuestions(reqWithMode).subscribe(() => done());

      const req = httpMock.expectOne(`${baseUrl}/generate`);
      const body = req.request.body as QuestionGenerationRequest;
      expect(body.executionMode).toBe('MULTI_AGENT');
      expect(body.generationQuality).toBe('EXAM_READY');
      expect(body.targetExam).toBe('JEE_ADV');
      req.flush({ status: 'success', data: { questions: [], modelUsed: 'test', totalGenerated: 0, totalValid: 0, totalDuplicates: 0 } });
    });

    it('should accept ASSERTION_REASON question type', (done) => {
      const reqAR: QuestionGenerationRequest = { ...minimalReq, questionType: 'ASSERTION_REASON' };
      service.generateQuestions(reqAR).subscribe(() => done());

      const req = httpMock.expectOne(`${baseUrl}/generate`);
      expect((req.request.body as QuestionGenerationRequest).questionType).toBe('ASSERTION_REASON');
      req.flush({ status: 'success', data: { questions: [], modelUsed: 'test', totalGenerated: 0, totalValid: 0, totalDuplicates: 0 } });
    });

    it('should use paragraphConfig (not paragraphSetConfig) for PARAGRAPH_SET', (done) => {
      const reqPS: QuestionGenerationRequest = {
        ...minimalReq,
        questionType: 'PARAGRAPH_SET',
        paragraphConfig: { passageWordLength: 300, subQuestionCount: 4 },
      };
      service.generateQuestions(reqPS).subscribe(() => done());

      const req = httpMock.expectOne(`${baseUrl}/generate`);
      const body = req.request.body as any;
      expect(body.paragraphConfig).toBeDefined();
      expect(body.paragraphConfig.passageWordLength).toBe(300);
      // paragraphSetConfig should NOT be present
      expect(body.paragraphSetConfig).toBeUndefined();
      req.flush({ status: 'success', data: { questions: [], modelUsed: 'test', totalGenerated: 0, totalValid: 0, totalDuplicates: 0 } });
    });
  });

  // ---------------------------------------------------------------------------
  // submitBatchJob
  // ---------------------------------------------------------------------------

  describe('submitBatchJob()', () => {
    it('should POST to /batch and unwrap ApiResponse<BatchGenerationJob>', (done) => {
      const batchReq: BatchGenerationRequest = {
        items: [
          {
            subjectId: 1,
            topicId: 5,
            subject: 'Mathematics',
            topic: 'Algebra',
            difficulty: 'MEDIUM',
            cognitiveLevel: 'APPLY',
            questionType: 'SINGLE_MCQ',
            count: 3,
          },
        ],
        avoidDuplicates: true,
      };

      service.submitBatchJob(batchReq).subscribe((job) => {
        expect(job.id).toBe('job-abc');
        expect(job.status).toBe('PENDING');
        done();
      });

      const req = httpMock.expectOne(`${baseUrl}/batch`);
      expect(req.request.method).toBe('POST');
      req.flush({ status: 'success', data: { id: 'job-abc', status: 'PENDING', totalRequested: 3, totalGenerated: 0, totalFailed: 0, totalDuplicates: 0 } });
    });

    it('should forward subjectId and topicId in batch items', (done) => {
      const batchReq: BatchGenerationRequest = {
        items: [
          {
            subjectId: 2,
            topicId: 8,
            subtopicId: 15,
            subject: 'Chemistry',
            topic: 'Organic',
            difficulty: 'HARD',
            cognitiveLevel: 'EVALUATE',
            questionType: 'MULTI_MCQ',
            count: 2,
          },
        ],
      };

      service.submitBatchJob(batchReq).subscribe(() => done());

      const req = httpMock.expectOne(`${baseUrl}/batch`);
      const body = req.request.body as BatchGenerationRequest;
      expect(body.items[0].subjectId).toBe(2);
      expect(body.items[0].topicId).toBe(8);
      expect(body.items[0].subtopicId).toBe(15);
      req.flush({ status: 'success', data: { id: 'job-xyz', status: 'PENDING', totalRequested: 2, totalGenerated: 0, totalFailed: 0, totalDuplicates: 0 } });
    });
  });

  // ---------------------------------------------------------------------------
  // getBatchJobStatus
  // ---------------------------------------------------------------------------

  describe('getBatchJobStatus()', () => {
    it('should GET /batch/{jobId} and unwrap the job', (done) => {
      service.getBatchJobStatus('job-123').subscribe((job) => {
        expect(job.status).toBe('PROCESSING');
        done();
      });

      const req = httpMock.expectOne(`${baseUrl}/batch/job-123`);
      expect(req.request.method).toBe('GET');
      req.flush({ status: 'success', data: { id: 'job-123', status: 'PROCESSING', totalRequested: 5, totalGenerated: 2, totalFailed: 0, totalDuplicates: 0 } });
    });
  });

  // ---------------------------------------------------------------------------
  // listBatchJobs
  // ---------------------------------------------------------------------------

  describe('listBatchJobs()', () => {
    it('should GET /batch with pagination params and return content + totalElements', (done) => {
      service.listBatchJobs(1, 10).subscribe((result) => {
        expect(result.content.length).toBe(1);
        expect(result.totalElements).toBe(5);
        done();
      });

      const req = httpMock.expectOne((r) => r.url === `${baseUrl}/batch` && r.params.get('page') === '1' && r.params.get('size') === '10');
      expect(req.request.method).toBe('GET');
      req.flush({
        status: 'success',
        data: { content: [{ id: 'job-1', status: 'COMPLETED', totalRequested: 3, totalGenerated: 3, totalFailed: 0, totalDuplicates: 0 }], totalElements: 5 },
      });
    });
  });

  // ---------------------------------------------------------------------------
  // cancelBatchJob
  // ---------------------------------------------------------------------------

  describe('cancelBatchJob()', () => {
    it('should POST to /batch/{jobId}/cancel and unwrap the job', (done) => {
      service.cancelBatchJob('job-999').subscribe((job) => {
        expect(job.status).toBe('CANCELLED');
        done();
      });

      const req = httpMock.expectOne(`${baseUrl}/batch/job-999/cancel`);
      expect(req.request.method).toBe('POST');
      req.flush({ status: 'success', data: { id: 'job-999', status: 'CANCELLED', totalRequested: 3, totalGenerated: 0, totalFailed: 0, totalDuplicates: 0 } });
    });
  });
});
