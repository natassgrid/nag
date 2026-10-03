import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { QuestionBankService } from './question-bank.service';
import { DifficultyLevel, QuestionStatus } from '../models/question.model';

describe('QuestionBankService', () => {
  let service: QuestionBankService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [QuestionBankService],
    });

    service = TestBed.inject(QuestionBankService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should initialize with default signals', () => {
    expect(service.questions()).toEqual([]);
    expect(service.total()).toBe(0);
    expect(service.loading()).toBe(false);
  });

  it('should load questions with filter parameters and map items', (done) => {
    const mockBackendResponse = {
      data: {
        content: [
          {
            id: 'q-1',
            text: 'What is Newton second law?',
            type: 'SINGLE_CHOICE',
            difficulty: 'MEDIUM',
            status: 'APPROVED',
            marks: 4,
            negativeMarks: 1,
            options: [
              { id: 'opt-1', text: 'F = ma', isCorrect: true },
              { id: 'opt-2', text: 'E = mc^2', isCorrect: false },
            ],
          },
        ],
        totalElements: 1,
        totalPages: 1,
      },
    };

    service
      .loadQuestions({ search: 'Newton', difficulty: 'MEDIUM' as DifficultyLevel, page: 0, size: 10 })
      .subscribe((res) => {
        expect(res.content.length).toBe(1);
        expect(res.content[0].id).toBe('q-1');
        expect(service.questions().length).toBe(1);
        expect(service.total()).toBe(1);
        expect(service.loading()).toBe(false);
        done();
      });

    const req = httpMock.expectOne(
      (r) =>
        r.url === '/api/v1/questions' &&
        r.params.get('search') === 'Newton' &&
        r.params.get('difficulty') === 'MEDIUM'
    );
    expect(req.request.method).toBe('GET');
    req.flush(mockBackendResponse);
  });

  it('should load question by id', (done) => {
    service.getQuestionById('q-99').subscribe((q) => {
      expect(q.id).toBe('q-99');
      expect(service.selectedQuestion()?.id).toBe('q-99');
      done();
    });

    const req = httpMock.expectOne('/api/v1/questions/q-99');
    expect(req.request.method).toBe('GET');
    req.flush({ data: { id: 'q-99', text: 'Calculus derivative of sin(x)' } });
  });

  it('should delete question and remove from state', (done) => {
    service.questions.set([
      { id: 'q-1', content: 'Q1', type: 'SINGLE_CHOICE' as any, difficulty: 'EASY' as any, status: 'DRAFT' as any, subjectId: 's1', topicId: 't1', options: [], marks: 1, negativeMarks: 0, tags: [], authorId: 'a1', createdAt: '', updatedAt: '', version: 1 },
      { id: 'q-2', content: 'Q2', type: 'SINGLE_CHOICE' as any, difficulty: 'EASY' as any, status: 'DRAFT' as any, subjectId: 's1', topicId: 't1', options: [], marks: 1, negativeMarks: 0, tags: [], authorId: 'a1', createdAt: '', updatedAt: '', version: 1 },
    ]);
    service.total.set(2);

    service.deleteQuestion('q-1').subscribe(() => {
      expect(service.questions().length).toBe(1);
      expect(service.questions()[0].id).toBe('q-2');
      expect(service.total()).toBe(1);
      done();
    });

    const req = httpMock.expectOne('/api/v1/questions/q-1');
    expect(req.request.method).toBe('DELETE');
    req.flush({});
  });
});
