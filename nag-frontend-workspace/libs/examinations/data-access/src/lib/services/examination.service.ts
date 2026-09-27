import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';
import { ApiResponse, PagedResponse } from '../models/common.model';
import {
  ExaminationResponse,
  CreateExamRequest,
} from '../models/examination.model';
import { LedgerProofResult } from '../models/paper.model';

@Injectable({
  providedIn: 'root',
})
export class ExaminationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/examinations';

  readonly exams = signal<ExaminationResponse[]>([]);
  readonly loading = signal<boolean>(false);
  readonly activeProof = signal<LedgerProofResult | null>(null);

  getExams(
    page = 0,
    size = 20,
    search?: string,
    sort?: string,
    order?: string
  ): Observable<ExaminationResponse[]> {
    this.loading.set(true);
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    if (search) params = params.set('search', search.trim());
    if (sort) params = params.set('sort', sort);
    if (order) params = params.set('order', order);

    return this.http
      .get<ApiResponse<any> | any>(this.baseUrl, { params })
      .pipe(
        map((res) => {
          const payload = res?.data ?? res;
          const items: ExaminationResponse[] = Array.isArray(payload?.content)
            ? payload.content
            : Array.isArray(payload)
            ? payload
            : [];
          return items;
        }),
        tap({
          next: (list) => {
            this.exams.set(list || []);
            this.loading.set(false);
          },
          error: () => this.loading.set(false),
        })
      );
  }

  getExamsPaged(
    page = 0,
    size = 20,
    search?: string,
    sort?: string,
    order?: string
  ): Observable<PagedResponse<ExaminationResponse>> {
    this.loading.set(true);
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    if (search) params = params.set('search', search.trim());
    if (sort) params = params.set('sort', sort);
    if (order) params = params.set('order', order);

    return this.http
      .get<ApiResponse<any> | any>(this.baseUrl, { params })
      .pipe(
        map((res) => {
          const payload = res?.data ?? res;
          if (Array.isArray(payload)) {
            return {
              content: payload,
              totalElements: payload.length,
              totalPages: 1,
              size: payload.length,
              number: 0,
            };
          }
          if (payload && Array.isArray(payload.content)) {
            return {
              content: payload.content,
              totalElements: payload.totalElements ?? payload.content.length,
              totalPages: payload.totalPages ?? 1,
              size: payload.size ?? payload.content.length,
              number: payload.number ?? 0,
            };
          }
          return { content: [], totalElements: 0, totalPages: 0, size: 20, number: 0 };
        }),
        tap({
          next: (paged) => {
            this.exams.set(paged.content || []);
            this.loading.set(false);
          },
          error: () => this.loading.set(false),
        })
      );
  }

  getExam(examId: string): Observable<ExaminationResponse> {
    return this.http
      .get<ApiResponse<ExaminationResponse> | ExaminationResponse>(`${this.baseUrl}/${examId}`)
      .pipe(map((res) => ((res as any)?.data ?? res) as ExaminationResponse));
  }

  createExam(req: CreateExamRequest): Observable<ExaminationResponse> {
    return this.http
      .post<ApiResponse<ExaminationResponse> | ExaminationResponse>(this.baseUrl, req)
      .pipe(
        map((res) => ((res as any)?.data ?? res) as ExaminationResponse),
        tap((created) => this.exams.update((list) => [created, ...(list || [])]))
      );
  }

  updateExam(examId: string, req: CreateExamRequest): Observable<ExaminationResponse> {
    return this.http
      .put<ApiResponse<ExaminationResponse> | ExaminationResponse>(`${this.baseUrl}/${examId}`, req)
      .pipe(
        map((res) => ((res as any)?.data ?? res) as ExaminationResponse),
        tap((updated) =>
          this.exams.update((list) =>
            (list || []).map((e) => (e.id === examId ? updated : e))
          )
        )
      );
  }

  publishExam(examId: string): Observable<ExaminationResponse> {
    return this.http
      .post<ApiResponse<ExaminationResponse> | ExaminationResponse>(
        `${this.baseUrl}/${examId}/publish`,
        {}
      )
      .pipe(
        map((res) => ((res as any)?.data ?? res) as ExaminationResponse),
        tap((updated) =>
          this.exams.update((list) =>
            (list || []).map((e) => (e.id === examId ? updated : e))
          )
        )
      );
  }
}
