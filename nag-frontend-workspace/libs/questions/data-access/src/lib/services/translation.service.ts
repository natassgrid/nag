import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import {
  IndicTranslationLanguage,
  INDIC_TRANSLATION_LANGUAGES,
  TranslationRequest,
  TranslationResponse,
  AutoTranslateResponse,
  BatchTranslationRequest,
  BatchTranslationJobResponse,
  PagedBatchJobsResponse,
} from '../models/translation.model';

@Injectable({
  providedIn: 'root',
})
export class TranslationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/translations';

  getLanguage(code: string): IndicTranslationLanguage | undefined {
    return INDIC_TRANSLATION_LANGUAGES.find(
      (l) => l.code.toLowerCase() === (code || '').toLowerCase()
    );
  }

  listTranslationsForQuestion(questionId: string): Observable<TranslationResponse[]> {
    return this.http
      .get<{ data?: TranslationResponse[] } | TranslationResponse[]>(`${this.baseUrl}/question/${questionId}`)
      .pipe(map((res) => (Array.isArray(res) ? res : (res as any)?.data || [])));
  }

  getApprovedTranslation(questionId: string, lang: string): Observable<TranslationResponse> {
    return this.http
      .get<{ data?: TranslationResponse } | TranslationResponse>(
        `${this.baseUrl}/question/${questionId}/language/${lang}`
      )
      .pipe(map((res) => ((res as any).data || res) as TranslationResponse));
  }

  autoTranslateQuestion(questionId: string, languageCode: string): Observable<AutoTranslateResponse> {
    return this.http
      .post<{ data?: AutoTranslateResponse } | AutoTranslateResponse>
        (`${this.baseUrl}/question/${questionId}/auto-translate/${languageCode}`,
        {}
      )
      .pipe(map((res) => ((res as any).data || res) as AutoTranslateResponse));
  }

  startBatchTranslation(request?: BatchTranslationRequest): Observable<BatchTranslationJobResponse> {
    return this.http
      .post<{ data?: BatchTranslationJobResponse } | BatchTranslationJobResponse>(
        `${this.baseUrl}/batch/auto-translate`,
        request || { sourceLanguage: 'en', targetLanguage: 'hi' }
      )
      .pipe(map((res) => ((res as any).data || res) as BatchTranslationJobResponse));
  }

  getBatchJobStatus(jobId: string): Observable<BatchTranslationJobResponse> {
    return this.http
      .get<{ data?: BatchTranslationJobResponse } | BatchTranslationJobResponse>(
        `${this.baseUrl}/batch/${jobId}`
      )
      .pipe(map((res) => ((res as any).data || res) as BatchTranslationJobResponse));
  }

  listBatchJobs(
    page = 0,
    size = 20
  ): Observable<PagedBatchJobsResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http
      .get<any>(`${this.baseUrl}/batch`, { params })
      .pipe(
        map((res) => {
          if (Array.isArray(res)) {
            return {
              content: res,
              totalElements: res.length,
              totalPages: 1,
              number: page,
              size,
            };
          }
          const data = res?.data || res;
          if (Array.isArray(data)) {
            return {
              content: data,
              totalElements: data.length,
              totalPages: 1,
              number: page,
              size,
            };
          }
          return {
            content: data?.content || [],
            totalElements: data?.totalElements ?? (data?.content?.length || 0),
            totalPages: data?.totalPages ?? 1,
            number: data?.number ?? page,
            size: data?.size ?? size,
          };
        })
      );
  }

  cancelBatchJob(jobId: string): Observable<BatchTranslationJobResponse> {
    return this.http
      .post<{ data?: BatchTranslationJobResponse } | BatchTranslationJobResponse>(
        `${this.baseUrl}/batch/${jobId}/cancel`,
        {}
      )
      .pipe(map((res) => ((res as any).data || res) as BatchTranslationJobResponse));
  }

  resumeBatchJob(jobId: string): Observable<BatchTranslationJobResponse> {
    return this.http
      .post<{ data?: BatchTranslationJobResponse } | BatchTranslationJobResponse>(
        `${this.baseUrl}/batch/${jobId}/resume`,
        {}
      )
      .pipe(map((res) => ((res as any).data || res) as BatchTranslationJobResponse));
  }

  saveTranslation(request: TranslationRequest): Observable<TranslationResponse> {
    return this.http
      .post<{ data?: TranslationResponse } | TranslationResponse>(this.baseUrl, request)
      .pipe(map((res) => ((res as any).data || res) as TranslationResponse));
  }

  approveTranslation(translationId: string): Observable<TranslationResponse> {
    return this.http
      .post<{ data?: TranslationResponse } | TranslationResponse>(
        `${this.baseUrl}/${translationId}/approve`,
        {}
      )
      .pipe(map((res) => ((res as any).data || res) as TranslationResponse));
  }

  rejectTranslation(translationId: string, comments?: string): Observable<TranslationResponse> {
    return this.http
      .post<{ data?: TranslationResponse } | TranslationResponse>(
        `${this.baseUrl}/${translationId}/reject`,
        { comments: comments || 'Changes requested' }
      )
      .pipe(map((res) => ((res as any).data || res) as TranslationResponse));
  }
}
