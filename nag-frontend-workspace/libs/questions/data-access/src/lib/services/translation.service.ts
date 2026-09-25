import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import {
  IndicTranslationLanguage,
  INDIC_TRANSLATION_LANGUAGES,
  TranslationRequest,
  TranslationResponse,
  AutoTranslateResponse,
  BatchTranslationRequest,
  BatchTranslationJobResponse,
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
      .post<{ data?: AutoTranslateResponse } | AutoTranslateResponse>(
        `${this.baseUrl}/question/${questionId}/auto-translate/${languageCode}`,
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

  listBatchJobs(): Observable<BatchTranslationJobResponse[]> {
    return this.http
      .get<{ data?: BatchTranslationJobResponse[] } | BatchTranslationJobResponse[]>(`${this.baseUrl}/batch`)
      .pipe(map((res) => (Array.isArray(res) ? res : (res as any)?.data || [])));
  }

  cancelBatchJob(jobId: string): Observable<BatchTranslationJobResponse> {
    return this.http
      .post<{ data?: BatchTranslationJobResponse } | BatchTranslationJobResponse>(
        `${this.baseUrl}/batch/${jobId}/cancel`,
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
