import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MicroLessonAdminService } from './micro-lesson-admin.service';
import {
  MicroLessonTranslation,
  MicroLessonBatchTranslationRequest,
  MicroLessonBatchTranslationJob,
} from '../models/micro-lesson.model';

describe('MicroLessonAdminService', () => {
  let service: MicroLessonAdminService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        MicroLessonAdminService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(MicroLessonAdminService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('Micro-Lesson Translation APIs', () => {
    it('should trigger on-demand auto-translation via POST /api/v1/microlessons/:id/auto-translate/:lang', () => {
      const mockTranslation: MicroLessonTranslation = {
        id: 'trans-123',
        microlessonId: 'lesson-1',
        languageCode: 'hi',
        coreConcept: 'हिंदी में मुख्य अवधारणा: $$E = mc^2$$',
        trap: 'सामान्य जाल',
        cheatSheet: ['$$E = mc^2$$'],
        stepByStepBreakdown: { steps: [] },
        sourceVersion: 1,
        status: 'IN_REVIEW',
      };

      service.autoTranslateMicroLesson('lesson-1', 'hi').subscribe((res) => {
        expect(res).toEqual(mockTranslation);
        expect(res.languageCode).toBe('hi');
      });

      const req = httpMock.expectOne('/api/v1/microlessons/lesson-1/auto-translate/hi');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({});
      req.flush(mockTranslation);
    });

    it('should fetch all translations for a micro-lesson via GET /api/v1/microlessons/:id/translations', () => {
      const mockTranslations: MicroLessonTranslation[] = [
        {
          id: 'trans-hi',
          microlessonId: 'lesson-1',
          languageCode: 'hi',
          coreConcept: 'हिंदी कोर कॉन्सेप्ट',
          trap: 'ट्रैप चेतावनी',
          cheatSheet: ['$$F = ma$$'],
          stepByStepBreakdown: {},
          sourceVersion: 1,
          status: 'APPROVED',
        },
        {
          id: 'trans-ta',
          microlessonId: 'lesson-1',
          languageCode: 'ta',
          coreConcept: 'தமிழ் கருத்து',
          trap: 'எச்சரிக்கை',
          cheatSheet: ['$$F = ma$$'],
          stepByStepBreakdown: {},
          sourceVersion: 1,
          status: 'IN_REVIEW',
        },
      ];

      service.getTranslations('lesson-1').subscribe((list) => {
        expect(list.length).toBe(2);
        expect(list[0].languageCode).toBe('hi');
        expect(list[1].languageCode).toBe('ta');
      });

      const req = httpMock.expectOne('/api/v1/microlessons/lesson-1/translations');
      expect(req.request.method).toBe('GET');
      req.flush(mockTranslations);
    });

    it('should fetch localized micro-lesson and adapt to review item via GET /api/v1/microlessons/:id/language/:lang', () => {
      const rawLocalized = {
        id: 'lesson-1',
        subtopicName: 'Newton Laws',
        topicName: 'Mechanics',
        subjectName: 'Physics',
        targetExam: 'JEE_MAIN',
        status: 'PUBLISHED',
        contentVersion: 1,
        languageCode: 'hi',
        fallbackToEnglish: false,
        content: {
          coreConcept: 'न्यूटन के नियम',
          trapAlert: 'घर्षण भूलना',
          formulaCheatSheet: '$$F = ma$$',
        },
      };

      service.getLocalizedMicroLesson('lesson-1', 'hi').subscribe((adapted) => {
        expect(adapted.id).toBe('lesson-1');
        expect(adapted.languageCode).toBe('hi');
        expect(adapted.fallbackToEnglish).toBe(false);
        expect(adapted.content.coreConcept).toBe('न्यूटन के नियम');
      });

      const req = httpMock.expectOne('/api/v1/microlessons/lesson-1/language/hi');
      expect(req.request.method).toBe('GET');
      req.flush(rawLocalized);
    });

    it('should approve translation via POST /api/v1/microlessons/translations/:id/approve', () => {
      const mockResult: MicroLessonTranslation = {
        id: 'trans-1',
        microlessonId: 'lesson-1',
        languageCode: 'hi',
        coreConcept: 'धारणा',
        trap: 'जाल',
        cheatSheet: [],
        stepByStepBreakdown: {},
        sourceVersion: 1,
        status: 'APPROVED',
        reviewComments: 'Accurate terminology verified',
      };

      service.approveTranslation('trans-1', 'Accurate terminology verified').subscribe((res) => {
        expect(res.status).toBe('APPROVED');
        expect(res.reviewComments).toBe('Accurate terminology verified');
      });

      const req = httpMock.expectOne('/api/v1/microlessons/translations/trans-1/approve');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({
        comments: 'Accurate terminology verified',
        reviewComments: 'Accurate terminology verified',
      });
      req.flush(mockResult);
    });

    it('should reject translation via POST /api/v1/microlessons/translations/:id/reject', () => {
      const mockResult: MicroLessonTranslation = {
        id: 'trans-1',
        microlessonId: 'lesson-1',
        languageCode: 'hi',
        coreConcept: 'धारणा',
        trap: 'जाल',
        cheatSheet: [],
        stepByStepBreakdown: {},
        sourceVersion: 1,
        status: 'REJECTED',
        reviewComments: 'Mathematical symbols were translated into text',
      };

      service.rejectTranslation('trans-1', 'Mathematical symbols were translated into text').subscribe((res) => {
        expect(res.status).toBe('REJECTED');
      });

      const req = httpMock.expectOne('/api/v1/microlessons/translations/trans-1/reject');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({
        comments: 'Mathematical symbols were translated into text',
        reviewComments: 'Mathematical symbols were translated into text',
      });
      req.flush(mockResult);
    });

    it('should start batch translation job via POST /api/v1/microlessons/batch/auto-translate', () => {
      const batchReq: MicroLessonBatchTranslationRequest = {
        targetLanguage: 'hi',
        subject: 'Physics',
        targetExam: 'JEE_MAIN',
        overwriteExisting: false,
        batchSize: 20,
      };

      const mockJob: MicroLessonBatchTranslationJob = {
        id: 'job-100',
        status: 'PENDING',
        sourceLanguage: 'en',
        targetLanguage: 'hi',
        totalLessons: 50,
        processedLessons: 0,
        successfulLessons: 0,
        failedLessons: 0,
        progressPercentage: 0,
      };

      service.startBatchTranslation(batchReq).subscribe((job) => {
        expect(job.id).toBe('job-100');
        expect(job.status).toBe('PENDING');
      });

      const req = httpMock.expectOne('/api/v1/microlessons/batch/auto-translate');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(batchReq);
      req.flush(mockJob);
    });

    it('should get batch translation job status via GET /api/v1/microlessons/batch/:id', () => {
      const mockJob: MicroLessonBatchTranslationJob = {
        id: 'job-100',
        status: 'IN_PROGRESS',
        sourceLanguage: 'en',
        targetLanguage: 'hi',
        totalLessons: 50,
        processedLessons: 25,
        successfulLessons: 25,
        failedLessons: 0,
        progressPercentage: 50,
      };

      service.getBatchTranslationJobStatus('job-100').subscribe((job) => {
        expect(job.progressPercentage).toBe(50);
        expect(job.status).toBe('IN_PROGRESS');
      });

      const req = httpMock.expectOne('/api/v1/microlessons/batch/job-100');
      expect(req.request.method).toBe('GET');
      req.flush(mockJob);
    });

    it('should list batch translation jobs via GET /api/v1/microlessons/batch', () => {
      const mockPage = {
        content: [
          {
            id: 'job-1',
            status: 'COMPLETED' as const,
            sourceLanguage: 'en',
            targetLanguage: 'hi',
            totalLessons: 10,
            processedLessons: 10,
            successfulLessons: 10,
            failedLessons: 0,
            progressPercentage: 100,
          },
        ],
        totalElements: 1,
        totalPages: 1,
        number: 0,
        size: 20,
      };

      service.listBatchTranslationJobs(0, 20).subscribe((res) => {
        expect(res.totalElements).toBe(1);
        expect(res.content[0].targetLanguage).toBe('hi');
      });

      const req = httpMock.expectOne('/api/v1/microlessons/batch?page=0&size=20');
      expect(req.request.method).toBe('GET');
      req.flush(mockPage);
    });

    it('should cancel batch translation job via POST /api/v1/microlessons/batch/:id/cancel', () => {
      const mockCancelledJob: MicroLessonBatchTranslationJob = {
        id: 'job-100',
        status: 'CANCELLED',
        sourceLanguage: 'en',
        targetLanguage: 'hi',
        totalLessons: 50,
        processedLessons: 30,
        successfulLessons: 30,
        failedLessons: 0,
        progressPercentage: 60,
      };

      service.cancelBatchTranslationJob('job-100').subscribe((job) => {
        expect(job.status).toBe('CANCELLED');
      });

      const req = httpMock.expectOne('/api/v1/microlessons/batch/job-100/cancel');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({});
      req.flush(mockCancelledJob);
    });
  });
});
