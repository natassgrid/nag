import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ExplanationService } from './explanation.service';
import { ExplanationIr } from '../models/explanation-ir.model';

describe('ExplanationService', () => {
  let service: ExplanationService;
  let httpMock: HttpTestingController;

  const mockExplanation: ExplanationIr = {
    explanationId: 'exp-101',
    questionId: 'q-alg-1',
    status: 'APPROVED',
    steps: [
      {
        id: 's1',
        sequence: 1,
        type: 'GIVEN',
        after: '3x + 5 = 20',
      },
      {
        id: 's2',
        sequence: 2,
        type: 'CONCLUDE',
        after: 'x = 5',
      },
    ],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ExplanationService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(ExplanationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should fetch explanation from API and cache response', () => {
    service.getExplanation('q-alg-1').subscribe((res) => {
      expect(res).toEqual(mockExplanation);
      expect(res.explanationId).toBe('exp-101');
    });

    const req = httpMock.expectOne('/api/practice/questions/q-alg-1/explanation');
    expect(req.request.method).toBe('GET');
    req.flush(mockExplanation);

    // Second call should return cached without HTTP request
    service.getExplanation('q-alg-1').subscribe((res) => {
      expect(res).toEqual(mockExplanation);
    });
    httpMock.expectNone('/api/practice/questions/q-alg-1/explanation');
  });

  it('should generate explanation via POST and cache response', () => {
    const payload = {
      question: {
        questionId: 'q-alg-1',
        statement: 'Solve 3x + 5 = 20',
      },
    };

    service.generateExplanation(payload).subscribe((res) => {
      expect(res.status).toBe('APPROVED');
    });

    const req = httpMock.expectOne('/api/v1/explanations/generate');
    expect(req.request.method).toBe('POST');
    req.flush(mockExplanation);
  });
});
