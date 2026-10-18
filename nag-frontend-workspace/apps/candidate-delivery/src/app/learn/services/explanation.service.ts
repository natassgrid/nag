import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, tap } from 'rxjs';
import { ExplanationIr } from '../models/explanation-ir.model';

@Injectable({
  providedIn: 'root',
})
export class ExplanationService {
  private readonly http = inject(HttpClient);
  private readonly cache = new Map<string, ExplanationIr>();

  getExplanation(questionId: string): Observable<ExplanationIr> {
    if (this.cache.has(questionId)) {
      return of(this.cache.get(questionId)!);
    }
    return this.http
      .get<ExplanationIr>(`/api/practice/questions/${questionId}/explanation`)
      .pipe(
        tap((data) => {
          if (data) {
            this.cache.set(questionId, data);
          }
        })
      );
  }

  generateExplanation(payload: {
    question: {
      questionId: string;
      statement: string;
      domain?: string;
      authoritativeAnswer?: string;
    };
    policy?: {
      level?: string;
      showWhy?: boolean;
      showVerification?: boolean;
    };
  }): Observable<ExplanationIr> {
    const qId = payload?.question?.questionId;
    if (qId && this.cache.has(qId)) {
      return of(this.cache.get(qId)!);
    }
    return this.http
      .post<ExplanationIr>('/api/v1/explanations/generate', payload)
      .pipe(
        tap((data) => {
          if (data && qId) {
            this.cache.set(qId, data);
          }
        })
      );
  }

  setCachedExplanation(questionId: string, explanation: ExplanationIr): void {
    this.cache.set(questionId, explanation);
  }

  clearCache(): void {
    this.cache.clear();
  }
}
