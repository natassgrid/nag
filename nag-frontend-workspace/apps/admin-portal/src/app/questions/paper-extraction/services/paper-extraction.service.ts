import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import {
  QuestionPaperMetadata,
  ExtractedQuestion,
  PaperUploadRequest,
  BulkAnswerKeyRequest,
  PromoteQuestionsRequest,
  PromoteQuestionsResponse,
  UpdateExtractedQuestionRequest,
  Page,
} from '../models';

@Injectable({
  providedIn: 'root',
})
export class PaperExtractionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/questions/papers';

  /**
   * Initiates asynchronous question paper extraction for a PDF document pre-uploaded
   * to asset-service. Sends pure JSON payload with fileAssetId.
   */
  initiateExtraction(
    request: PaperUploadRequest
  ): Observable<QuestionPaperMetadata> {
    return this.http
      .post<QuestionPaperMetadata | { data: QuestionPaperMetadata }>(
        this.baseUrl,
        request
      )
      .pipe(map((res) => ((res as any)?.data ?? res) as QuestionPaperMetadata));
  }

  /**
   * Backward-compatible alias for initiateExtraction.
   */
  uploadPaper(
    request: PaperUploadRequest
  ): Observable<QuestionPaperMetadata> {
    return this.initiateExtraction(request);
  }

  listPapers(params?: {
    examinationName?: string;
    examYear?: number;
    department?: string;
    status?: string;
    page?: number;
    size?: number;
    sort?: string;
  }): Observable<Page<QuestionPaperMetadata>> {
    let httpParams = new HttpParams();
    if (params) {
      if (params.examinationName) {
        httpParams = httpParams.set('examinationName', params.examinationName);
      }
      if (params.examYear != null) {
        httpParams = httpParams.set('examYear', params.examYear.toString());
      }
      if (params.department) {
        httpParams = httpParams.set('department', params.department);
      }
      if (params.status) {
        httpParams = httpParams.set('status', params.status);
      }
      if (params.page != null) {
        httpParams = httpParams.set('page', params.page.toString());
      }
      if (params.size != null) {
        httpParams = httpParams.set('size', params.size.toString());
      }
      if (params.sort) {
        httpParams = httpParams.set('sort', params.sort);
      }
    }

    return this.http
      .get<Page<QuestionPaperMetadata> | { data: Page<QuestionPaperMetadata> }>(
        this.baseUrl,
        { params: httpParams }
      )
      .pipe(map((res) => ((res as any)?.data ?? res) as Page<QuestionPaperMetadata>));
  }

  getPaper(paperId: string): Observable<QuestionPaperMetadata> {
    return this.http
      .get<QuestionPaperMetadata | { data: QuestionPaperMetadata }>(
        `${this.baseUrl}/${paperId}`
      )
      .pipe(map((res) => ((res as any)?.data ?? res) as QuestionPaperMetadata));
  }

  cancelJob(paperId: string): Observable<QuestionPaperMetadata> {
    return this.http
      .post<QuestionPaperMetadata | { data: QuestionPaperMetadata }>(
        `${this.baseUrl}/${paperId}/cancel`,
        {}
      )
      .pipe(map((res) => ((res as any)?.data ?? res) as QuestionPaperMetadata));
  }

  retryJob(paperId: string): Observable<QuestionPaperMetadata> {
    return this.http
      .post<QuestionPaperMetadata | { data: QuestionPaperMetadata }>(
        `${this.baseUrl}/${paperId}/retry`,
        {}
      )
      .pipe(map((res) => ((res as any)?.data ?? res) as QuestionPaperMetadata));
  }

  applyAnswerKey(
    paperId: string,
    request: BulkAnswerKeyRequest
  ): Observable<QuestionPaperMetadata> {
    return this.http
      .post<QuestionPaperMetadata | { data: QuestionPaperMetadata }>(
        `${this.baseUrl}/${paperId}/answer-key`,
        request
      )
      .pipe(map((res) => ((res as any)?.data ?? res) as QuestionPaperMetadata));
  }

  listExtractedQuestions(
    paperId: string,
    params?: {
      status?: string;
      pageNumber?: number;
      page?: number;
      size?: number;
      sort?: string;
    }
  ): Observable<Page<ExtractedQuestion>> {
    let httpParams = new HttpParams();
    if (params) {
      if (params.status) {
        httpParams = httpParams.set('status', params.status);
      }
      if (params.pageNumber != null) {
        httpParams = httpParams.set('pageNumber', params.pageNumber.toString());
      }
      if (params.page != null) {
        httpParams = httpParams.set('page', params.page.toString());
      }
      if (params.size != null) {
        httpParams = httpParams.set('size', params.size.toString());
      }
      if (params.sort) {
        httpParams = httpParams.set('sort', params.sort);
      }
    }

    return this.http
      .get<Page<ExtractedQuestion> | { data: Page<ExtractedQuestion> }>(
        `${this.baseUrl}/${paperId}/questions`,
        { params: httpParams }
      )
      .pipe(map((res) => ((res as any)?.data ?? res) as Page<ExtractedQuestion>));
  }

  updateExtractedQuestion(
    paperId: string,
    questionId: string,
    req: UpdateExtractedQuestionRequest
  ): Observable<ExtractedQuestion> {
    return this.http
      .put<ExtractedQuestion | { data: ExtractedQuestion }>(
        `${this.baseUrl}/${paperId}/questions/${questionId}`,
        req
      )
      .pipe(map((res) => ((res as any)?.data ?? res) as ExtractedQuestion));
  }

  promoteQuestions(
    paperId: string,
    req?: PromoteQuestionsRequest
  ): Observable<PromoteQuestionsResponse> {
    return this.http
      .post<PromoteQuestionsResponse | { data: PromoteQuestionsResponse }>(
        `${this.baseUrl}/${paperId}/promote`,
        req ?? {}
      )
      .pipe(map((res) => ((res as any)?.data ?? res) as PromoteQuestionsResponse));
  }
}
