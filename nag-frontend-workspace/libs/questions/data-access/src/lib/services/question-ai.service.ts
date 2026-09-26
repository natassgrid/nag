import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import {
  QuestionGenerationRequest,
  QuestionGenerationResponse,
  BatchGenerationRequest,
  BatchGenerationJob,
} from '../models/ai-generation.model';

@Injectable({
  providedIn: 'root',
})
export class QuestionAiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/questions';

  generateQuestions(request: QuestionGenerationRequest): Observable<QuestionGenerationResponse> {
    return this.http
      .post<{ status?: string; message?: string; data?: QuestionGenerationResponse } | QuestionGenerationResponse>(
        `${this.baseUrl}/generate`,
        request
      )
      .pipe(map((res) => ((res as any)?.data || res) as QuestionGenerationResponse));
  }

  submitBatchJob(request: BatchGenerationRequest): Observable<BatchGenerationJob> {
    return this.http
      .post<{ status?: string; message?: string; data?: BatchGenerationJob } | BatchGenerationJob>(
        `${this.baseUrl}/batch`,
        request
      )
      .pipe(map((res) => ((res as any)?.data || res) as BatchGenerationJob));
  }

  getBatchJobStatus(jobId: string): Observable<BatchGenerationJob> {
    return this.http
      .get<{ status?: string; message?: string; data?: BatchGenerationJob } | BatchGenerationJob>(
        `${this.baseUrl}/batch/${jobId}`
      )
      .pipe(map((res) => ((res as any)?.data || res) as BatchGenerationJob));
  }

  listBatchJobs(
    page = 0,
    size = 20
  ): Observable<{ content: BatchGenerationJob[]; totalElements: number }> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http
      .get<{ status?: string; message?: string; data?: any }>(`${this.baseUrl}/batch`, { params })
      .pipe(
        map((res) => {
          const p = res?.data || res;
          return {
            content: p?.content || [],
            totalElements: p?.totalElements || 0,
          };
        })
      );
  }

  cancelBatchJob(jobId: string): Observable<BatchGenerationJob> {
    return this.http
      .post<{ status?: string; message?: string; data?: BatchGenerationJob } | BatchGenerationJob>(
        `${this.baseUrl}/batch/${jobId}/cancel`,
        {}
      )
      .pipe(map((res) => ((res as any)?.data || res) as BatchGenerationJob));
  }

  backfillEmbeddings(): Observable<any> {
    return this.http.post(`${this.baseUrl}/embeddings/backfill`, {});
  }
}
