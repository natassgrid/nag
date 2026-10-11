import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TranslationService } from './translation.service';
import { BatchTranslationJobResponse } from '../models/translation.model';

describe('TranslationService', () => {
  let service: TranslationService;
  let httpMock: HttpTestingController;

  const baseUrl = '/api/v1/translations';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [TranslationService],
    });
    service = TestBed.inject(TranslationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('listBatchJobs()', () => {
    it('should request paginated batch translation jobs with default page and size', () => {
      const mockPageResponse = {
        content: [
          {
            id: 'job-1',
            status: 'COMPLETED',
            sourceLanguage: 'en',
            targetLanguage: 'hi',
            totalQuestions: 10,
            processedQuestions: 10,
            successfulQuestions: 10,
            failedQuestions: 0,
            progressPercentage: 100,
          } as BatchTranslationJobResponse,
        ],
        totalElements: 1,
        totalPages: 1,
        number: 0,
        size: 20,
      };

      service.listBatchJobs().subscribe((res) => {
        expect(res.content.length).toBe(1);
        expect(res.totalElements).toBe(1);
        expect(res.content[0].id).toBe('job-1');
      });

      const req = httpMock.expectOne(`${baseUrl}/batch?page=0&size=20`);
      expect(req.request.method).toBe('GET');
      req.flush(mockPageResponse);
    });

    it('should request paginated batch jobs with custom page and size parameters', () => {
      service.listBatchJobs(2, 50).subscribe((res) => {
        expect(res.number).toBe(2);
        expect(res.size).toBe(50);
      });

      const req = httpMock.expectOne(`${baseUrl}/batch?page=2&size=50`);
      expect(req.request.method).toBe('GET');
      req.flush({
        content: [],
        totalElements: 0,
        totalPages: 0,
        number: 2,
        size: 50,
      });
    });

    it('should handle legacy array response gracefully', () => {
      const legacyArray = [
        {
          id: 'job-legacy',
          status: 'IN_PROGRESS',
          sourceLanguage: 'en',
          targetLanguage: 'ta',
        } as BatchTranslationJobResponse,
      ];

      service.listBatchJobs(0, 10).subscribe((res) => {
        expect(res.content.length).toBe(1);
        expect(res.totalElements).toBe(1);
        expect(res.content[0].id).toBe('job-legacy');
      });

      const req = httpMock.expectOne(`${baseUrl}/batch?page=0&size=10`);
      req.flush(legacyArray);
    });
  });
});
