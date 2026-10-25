import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  MicroLessonReviewItem,
  ReviewMicroLessonRequest,
  BatchGenerateMicroLessonsRequest,
  BatchGenerateResponse,
  GenerateMicroLessonRequest,
  MicroLessonStats,
  MicroLessonFilterOptions,
  BulkDeleteResponse,
  CreateMicroLessonRequest,
  UpdateMicroLessonRequest,
  MicroLessonTranslation,
  MicroLessonBatchTranslationRequest,
  MicroLessonBatchTranslationJob,
  adaptToReviewItem,
} from '../models/micro-lesson.model';

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface MicroLessonQueryParams {
  status?: string;
  subjectId?: number;
  subject?: string;
  topic?: string;
  targetExam?: string;
  lessonType?: string;
  difficultyLevel?: string;
  query?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  direction?: string;
}

@Injectable({ providedIn: 'root' })
export class MicroLessonAdminService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/admin/micro-lessons';
  private readonly translationApiUrl = '/api/v1/microlessons';

  listForReview(
    paramsOrStatus?: MicroLessonQueryParams | string,
    subjectId?: number,
    page: number = 0,
    size: number = 20
  ): Observable<PageResponse<MicroLessonReviewItem>> {
    let params = new HttpParams();

    if (paramsOrStatus && typeof paramsOrStatus === 'object') {
      const q = paramsOrStatus;
      params = params
        .set('page', (q.page ?? 0).toString())
        .set('size', (q.size ?? 20).toString());

      if (q.status) {
        params = params.set('status', q.status);
      }
      if (q.subjectId != null) {
        params = params.set('subjectId', q.subjectId.toString());
      }
      if (q.subject && q.subject !== 'ALL') {
        params = params.set('subject', q.subject);
      }
      if (q.topic && q.topic !== 'ALL') {
        params = params.set('topic', q.topic);
      }
      if (q.targetExam && q.targetExam !== 'ALL') {
        params = params.set('targetExam', q.targetExam);
      }
      if (q.lessonType && q.lessonType !== 'ALL') {
        params = params.set('lessonType', q.lessonType);
      }
      if (q.difficultyLevel && q.difficultyLevel !== 'ALL') {
        params = params.set('difficultyLevel', q.difficultyLevel);
      }
      if (q.query && q.query.trim()) {
        params = params.set('query', q.query.trim());
      }
      if (q.sortBy) {
        params = params.set('sortBy', q.sortBy);
        const dir = (q.direction || 'desc').toLowerCase();
        params = params.set('sort', `${q.sortBy},${dir}`);
      }
      if (q.direction) {
        params = params.set('direction', q.direction);
      }
    } else {
      const status = paramsOrStatus;
      params = params
        .set('page', page.toString())
        .set('size', size.toString());

      if (status && status !== 'ALL') {
        params = params.set('status', status);
      }
      if (subjectId != null) {
        params = params.set('subjectId', subjectId.toString());
      }
    }

    return this.http
      .get<PageResponse<any>>(this.apiUrl, { params })
      .pipe(
        map((res) => ({
          ...res,
          content: (res.content || []).map(adaptToReviewItem),
        }))
      );
  }

  getMicroLessonById(id: string): Observable<MicroLessonReviewItem> {
    return this.http
      .get<any>(`${this.apiUrl}/${id}`)
      .pipe(map(adaptToReviewItem));
  }

  createMicroLesson(
    request: CreateMicroLessonRequest
  ): Observable<MicroLessonReviewItem> {
    return this.http
      .post<any>(this.apiUrl, request)
      .pipe(map(adaptToReviewItem));
  }

  updateMicroLesson(
    id: string,
    request: UpdateMicroLessonRequest
  ): Observable<MicroLessonReviewItem> {
    return this.http
      .put<any>(`${this.apiUrl}/${id}`, request)
      .pipe(map(adaptToReviewItem));
  }

  getStats(): Observable<MicroLessonStats> {
    return this.http.get<MicroLessonStats>(`${this.apiUrl}/stats`);
  }

  getFilterOptions(subject?: string): Observable<MicroLessonFilterOptions> {
    let params = new HttpParams();
    if (subject && subject !== 'ALL') {
      params = params.set('subject', subject);
    }
    return this.http.get<MicroLessonFilterOptions>(`${this.apiUrl}/filter-options`, { params });
  }

  deleteMicroLesson(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  bulkDelete(ids: string[]): Observable<BulkDeleteResponse> {
    return this.http.post<BulkDeleteResponse>(`${this.apiUrl}/bulk-delete`, { ids });
  }

  reviewMicroLesson(
    id: string,
    req: ReviewMicroLessonRequest
  ): Observable<MicroLessonReviewItem> {
    let targetStatus = req.status;
    const action = req.action ? req.action.toUpperCase() : undefined;
    if (action === 'APPROVE' || targetStatus === 'APPROVE') targetStatus = 'APPROVED';
    else if (action === 'REJECT' || targetStatus === 'REJECT') targetStatus = 'REJECTED';
    else if (action === 'PUBLISH' || targetStatus === 'PUBLISH') targetStatus = 'PUBLISHED';
    else if (action === 'EDIT' || targetStatus === 'EDIT') targetStatus = req.status || 'IN_REVIEW';
    else if (!targetStatus && action) targetStatus = action;

    const payload: any = {
      status: targetStatus,
      comments: req.reviewerNotes || req.comments,
      action: req.action,
      reviewerNotes: req.reviewerNotes,
    };
    if (req.updatedContent) {
      payload.coreConcept = req.updatedContent.coreConcept;
      payload.trap = req.updatedContent.trapAlert;
      payload.trapAlert = req.updatedContent.trapAlert;
      if (req.updatedContent.formulaCheatSheet) {
        payload.cheatSheet = req.updatedContent.formulaCheatSheet
          .split('\n\n')
          .map((s) => s.trim())
          .filter((s) => s.length > 0);
      }
      if (req.updatedContent.stepByStepBreakdown) {
        payload.stepByStepBreakdown = {
          questionStem: req.updatedContent.stepByStepBreakdown.problemStatement,
          steps: req.updatedContent.stepByStepBreakdown.steps.map(
            (s) => s.explanation || s.title
          ),
          finalAnswer: req.updatedContent.stepByStepBreakdown.finalAnswer,
        };
      }
    }
    return this.http
      .put<any>(`${this.apiUrl}/${id}/review`, payload)
      .pipe(map(adaptToReviewItem));
  }

  generateSingle(
    req: GenerateMicroLessonRequest
  ): Observable<MicroLessonReviewItem> {
    return this.http
      .post<any>(`${this.apiUrl}/generate`, req)
      .pipe(map(adaptToReviewItem));
  }

  triggerBatchGenerate(
    req?: BatchGenerateMicroLessonsRequest
  ): Observable<BatchGenerateResponse> {
    return this.http.post<BatchGenerateResponse>(
      `${this.apiUrl}/batch-generate`,
      req || {}
    );
  }

  getBatchJobStatus(batchId: string): Observable<BatchGenerateResponse> {
    return this.http.get<BatchGenerateResponse>(`${this.apiUrl}/batch-jobs/${batchId}`);
  }

  getActiveBatchJob(): Observable<BatchGenerateResponse | null> {
    return this.http.get<BatchGenerateResponse | null>(`${this.apiUrl}/batch-jobs/active`);
  }

  cancelBatchJob(batchId: string): Observable<BatchGenerateResponse> {
    return this.http.post<BatchGenerateResponse>(`${this.apiUrl}/batch-jobs/${batchId}/cancel`, {});
  }

  // --- Multilingual Translation & Localization APIs ---

  autoTranslateMicroLesson(id: string, lang: string): Observable<MicroLessonTranslation> {
    return this.http.post<MicroLessonTranslation>(
      `${this.translationApiUrl}/${id}/auto-translate/${lang}`,
      {}
    );
  }

  getTranslations(id: string): Observable<MicroLessonTranslation[]> {
    return this.http.get<MicroLessonTranslation[]>(
      `${this.translationApiUrl}/${id}/translations`
    );
  }

  getLocalizedMicroLesson(id: string, lang: string): Observable<MicroLessonReviewItem> {
    return this.http
      .get<any>(`${this.translationApiUrl}/${id}/language/${lang}`)
      .pipe(map(adaptToReviewItem));
  }

  approveTranslation(
    translationId: string,
    comments?: string
  ): Observable<MicroLessonTranslation> {
    return this.http.post<MicroLessonTranslation>(
      `${this.translationApiUrl}/translations/${translationId}/approve`,
      { comments, reviewComments: comments }
    );
  }

  rejectTranslation(
    translationId: string,
    comments: string
  ): Observable<MicroLessonTranslation> {
    return this.http.post<MicroLessonTranslation>(
      `${this.translationApiUrl}/translations/${translationId}/reject`,
      { comments, reviewComments: comments }
    );
  }

  startBatchTranslation(
    req: MicroLessonBatchTranslationRequest
  ): Observable<MicroLessonBatchTranslationJob> {
    return this.http.post<MicroLessonBatchTranslationJob>(
      `${this.translationApiUrl}/batch/auto-translate`,
      req
    );
  }

  getBatchTranslationJobStatus(
    jobId: string
  ): Observable<MicroLessonBatchTranslationJob> {
    return this.http.get<MicroLessonBatchTranslationJob>(
      `${this.translationApiUrl}/batch/${jobId}`
    );
  }

  listBatchTranslationJobs(
    page: number = 0,
    size: number = 20
  ): Observable<PageResponse<MicroLessonBatchTranslationJob>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<MicroLessonBatchTranslationJob>>(
      `${this.translationApiUrl}/batch`,
      { params }
    );
  }

  cancelBatchTranslationJob(
    jobId: string
  ): Observable<MicroLessonBatchTranslationJob> {
    return this.http.post<MicroLessonBatchTranslationJob>(
      `${this.translationApiUrl}/batch/${jobId}/cancel`,
      {}
    );
  }
}

