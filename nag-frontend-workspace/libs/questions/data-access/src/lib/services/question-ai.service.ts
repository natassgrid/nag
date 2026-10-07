import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import {
  QuestionGenerationRequest,
  QuestionGenerationResponse,
  BatchGenerationRequest,
  BatchGenerationJob,
} from '../models/ai-generation.model';

/** Standard backend envelope shape (from shared-lib ApiResponse<T>). */
interface ApiResponse<T> {
  status: string;
  message?: string;
  data?: T;
}

@Injectable({
  providedIn: 'root',
})
export class QuestionAiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/questions';

  generateQuestions(request: QuestionGenerationRequest): Observable<QuestionGenerationResponse> {
    return this.http
      .post<ApiResponse<QuestionGenerationResponse>>(`${this.baseUrl}/generate`, request)
      .pipe(map((res) => res.data as QuestionGenerationResponse));
  }

  submitBatchJob(request: BatchGenerationRequest): Observable<BatchGenerationJob> {
    return this.http
      .post<ApiResponse<BatchGenerationJob>>(`${this.baseUrl}/batch`, request)
      .pipe(map((res) => res.data as BatchGenerationJob));
  }

  getBatchJobStatus(jobId: string): Observable<BatchGenerationJob> {
    return this.http
      .get<ApiResponse<BatchGenerationJob>>(`${this.baseUrl}/batch/${jobId}`)
      .pipe(map((res) => res.data as BatchGenerationJob));
  }

  listBatchJobs(
    page = 0,
    size = 20
  ): Observable<{ content: BatchGenerationJob[]; totalElements: number }> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http
      .get<ApiResponse<{ content: BatchGenerationJob[]; totalElements: number; totalPages: number; size: number; number: number }>>(
        `${this.baseUrl}/batch`,
        { params }
      )
      .pipe(
        map((res) => ({
          content: res.data?.content ?? [],
          totalElements: res.data?.totalElements ?? 0,
        }))
      );
  }

  cancelBatchJob(jobId: string): Observable<BatchGenerationJob> {
    return this.http
      .post<ApiResponse<BatchGenerationJob>>(`${this.baseUrl}/batch/${jobId}/cancel`, {})
      .pipe(map((res) => res.data as BatchGenerationJob));
  }

  backfillEmbeddings(): Observable<ApiResponse<unknown>> {
    return this.http.post<ApiResponse<unknown>>(`${this.baseUrl}/embeddings/backfill`, {});
  }
}
