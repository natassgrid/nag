import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';
import {
  DifficultyLevel,
  Question,
  QuestionOption,
  QuestionStatus,
} from '../models/question.model';
import {
  PassageRequest,
  PassageResponse,
  PagedPassagesResponse,
} from '../models/passage.model';

@Injectable({
  providedIn: 'root',
})
export class PassageService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/passages';

  readonly passages = signal<PassageResponse[]>([]);
  readonly total = signal<number>(0);
  readonly loading = signal<boolean>(false);

  listPassages(filters?: {
    subjectId?: number;
    state?: string;
    search?: string;
    page?: number;
    size?: number;
  }): Observable<PagedPassagesResponse> {
    this.loading.set(true);
    let params = new HttpParams()
      .set('page', (filters?.page ?? 0).toString())
      .set('size', (filters?.size ?? 20).toString());

    if (filters?.subjectId) params = params.set('subjectId', filters.subjectId.toString());
    if (filters?.state && filters.state !== 'ALL') params = params.set('state', filters.state);
    if (filters?.search && filters.search.trim()) params = params.set('search', filters.search.trim());

    return this.http
      .get<{ status?: string; data?: any }>(this.baseUrl, { params })
      .pipe(
        map((res) => {
          const page = res?.data || res;
          const rawItems = Array.isArray(page?.content) ? page.content : (Array.isArray(page) ? page : []);
          const items: PassageResponse[] = rawItems.map((raw: any) => this.mapToPassage(raw));
          return {
            content: items,
            totalElements: page?.totalElements ?? items.length,
            totalPages: page?.totalPages ?? 1,
            number: page?.number ?? 0,
            size: page?.size ?? 20,
          };
        }),
        tap({
          next: (res) => {
            this.passages.set(res.content);
            this.total.set(res.totalElements);
            this.loading.set(false);
          },
          error: () => this.loading.set(false),
        })
      );
  }

  getPassage(id: string): Observable<PassageResponse> {
    return this.http
      .get<{ status?: string; data?: any }>(`${this.baseUrl}/${id}`)
      .pipe(map((res) => this.mapToPassage(res?.data || res)));
  }

  createPassage(data: PassageRequest): Observable<PassageResponse> {
    return this.http
      .post<{ status?: string; data?: any }>(this.baseUrl, data)
      .pipe(
        map((res) => this.mapToPassage(res?.data || res)),
        tap((created) => {
          this.passages.update((list) => [created, ...list]);
          this.total.update((t) => t + 1);
        })
      );
  }

  updatePassage(id: string, data: PassageRequest): Observable<PassageResponse> {
    return this.http
      .put<{ status?: string; data?: any }>(`${this.baseUrl}/${id}`, data)
      .pipe(
        map((res) => this.mapToPassage(res?.data || res)),
        tap((updated) => {
          this.passages.update((list) =>
            list.map((item) => (item.id === id ? updated : item))
          );
        })
      );
  }

  deletePassage(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`).pipe(
      tap(() => {
        this.passages.update((list) => list.filter((p) => p.id !== id));
        this.total.update((t) => Math.max(0, t - 1));
      })
    );
  }

  submitForReview(id: string): Observable<PassageResponse> {
    return this.http
      .put<{ status?: string; data?: any }>(`${this.baseUrl}/${id}/submit`, {})
      .pipe(
        map((res) => this.mapToPassage(res?.data || res)),
        tap((updated) => {
          this.passages.update((list) =>
            list.map((item) => (item.id === id ? updated : item))
          );
        })
      );
  }

  approvePassage(id: string): Observable<PassageResponse> {
    return this.http
      .put<{ status?: string; data?: any }>(`${this.baseUrl}/${id}/approve`, {})
      .pipe(
        map((res) => this.mapToPassage(res?.data || res)),
        tap((updated) => {
          this.passages.update((list) =>
            list.map((item) => (item.id === id ? updated : item))
          );
        })
      );
  }

  rejectPassage(id: string, comments?: string): Observable<PassageResponse> {
    return this.http
      .put<{ status?: string; data?: any }>(`${this.baseUrl}/${id}/reject`, { comments })
      .pipe(
        map((res) => this.mapToPassage(res?.data || res)),
        tap((updated) => {
          this.passages.update((list) =>
            list.map((item) => (item.id === id ? updated : item))
          );
        })
      );
  }

  private mapToPassage(raw: any): PassageResponse {
    const rawSubs = raw?.subQuestions || [];
    const subQuestions: Question[] = rawSubs.map((sub: any, idx: number) => {
      let options: QuestionOption[] = [];
      if (Array.isArray(sub.options)) {
        options = sub.options.map((opt: any, oIdx: number) => ({
          id: opt.id || String.fromCharCode(65 + oIdx),
          text: opt.text || '',
          isCorrect: opt.isCorrect ?? (sub.answerKey === opt.id),
          imageUrl: opt.imageUrl,
          imageAltText: opt.imageAltText,
        }));
      }

      return {
        id: sub.id || `SQ-${idx + 1}`,
        content: sub.content || '',
        type: sub.questionType || sub.type || 'SINGLE_MCQ',
        difficulty: (sub.difficulty || 'MEDIUM') as DifficultyLevel,
        status: (sub.state || sub.status || raw.state || 'DRAFT') as QuestionStatus,
        subject: raw.subject || '',
        topic: raw.topic || '',
        subtopic: raw.subtopic || '',
        subjectId: raw.subjectId,
        topicId: raw.topicId,
        marks: sub.marks ?? 4,
        negativeMarks: sub.negativeMarks ?? 1,
        options,
        answerKey: sub.answerKey,
        explanation: sub.explanation,
        passageId: raw.id,
        passageOrderIndex: sub.passageOrderIndex ?? (idx + 1),
      };
    });

    return {
      id: raw.id,
      title: raw.title || '',
      content: raw.content || '',
      contentFormat: raw.contentFormat || 'MIXED',
      subjectId: raw.subjectId,
      topicId: raw.topicId,
      subject: raw.subject || '',
      topic: raw.topic || '',
      subtopic: raw.subtopic || '',
      hasImages: !!raw.hasImages,
      state: raw.state || 'DRAFT',
      authorId: raw.authorId,
      reviewerId: raw.reviewerId,
      subQuestions,
      subQuestionCount: raw.subQuestionCount ?? subQuestions.length,
      createdAt: raw.createdAt,
      updatedAt: raw.updatedAt,
    };
  }
}
