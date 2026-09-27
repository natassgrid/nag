import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';
import { ApiResponse, PagedResponse } from '../models/common.model';
import {
  PaperSummary,
  PaperDetail,
  PaperGenerationRequest,
  PaperGenerationResponse,
  PaperApprovalResponse,
  PaperTranslateRequest,
  PaperTranslateResponse,
  BlueprintTemplateRequest,
  BlueprintTemplateResponse,
  BlueprintFeasibilityRequest,
  BlueprintFeasibilityResponse,
  PaperListParams,
} from '../models/paper.model';

@Injectable({
  providedIn: 'root',
})
export class PaperService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/papers';
  private readonly templateBaseUrl = '/api/v1/papers/blueprint-templates';

  readonly papers = signal<PaperSummary[]>([]);
  readonly loading = signal<boolean>(false);

  getPapers(params?: PaperListParams): Observable<PagedResponse<PaperSummary>> {
    this.loading.set(true);
    let httpParams = new HttpParams();

    if (params) {
      if (params.page !== undefined) httpParams = httpParams.set('page', params.page.toString());
      if (params.size !== undefined) httpParams = httpParams.set('size', params.size.toString());
      if (params.sort) httpParams = httpParams.set('sort', params.sort);
      if (params.order) httpParams = httpParams.set('order', params.order);
      if (params.search) httpParams = httpParams.set('search', params.search.trim());
      if (params.status) httpParams = httpParams.set('status', params.status);
      if (params.examId) httpParams = httpParams.set('examId', params.examId);
      if (params.shiftId) httpParams = httpParams.set('shiftId', params.shiftId);
      if (params.isPractice !== undefined)
        httpParams = httpParams.set('isPractice', params.isPractice.toString());
    }

    return this.http.get<any>(this.baseUrl, { params: httpParams }).pipe(
      map((res) => {
        const payload = res?.data ?? res;
        const items = payload?.content || (Array.isArray(payload) ? payload : []);
        const normalized: PaperSummary[] = items.map((p: any) => ({
          ...p,
          paperId: p.paperId || p.id,
        }));
        return {
          content: normalized,
          totalElements: payload?.totalElements ?? normalized.length,
          totalPages: payload?.totalPages ?? 1,
          size: payload?.size ?? (params?.size || 10),
          number: payload?.number ?? (params?.page || 0),
        };
      }),
      tap({
        next: (paged) => {
          this.papers.set(paged.content || []);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      })
    );
  }

  getPaper(paperId: string): Observable<PaperDetail> {
    return this.http
      .get<ApiResponse<PaperDetail> | PaperDetail>(`${this.baseUrl}/${paperId}`)
      .pipe(map((res) => ((res as any)?.data ?? res) as PaperDetail));
  }

  generatePaper(request: PaperGenerationRequest): Observable<PaperGenerationResponse> {
    return this.http
      .post<ApiResponse<PaperGenerationResponse> | PaperGenerationResponse>(
        `${this.baseUrl}/generate`,
        request
      )
      .pipe(map((res) => ((res as any)?.data ?? res) as PaperGenerationResponse));
  }

  approvePaper(paperId: string): Observable<PaperApprovalResponse> {
    return this.http
      .post<ApiResponse<PaperApprovalResponse> | PaperApprovalResponse>(
        `${this.baseUrl}/${paperId}/approve`,
        {}
      )
      .pipe(map((res) => ((res as any)?.data ?? res) as PaperApprovalResponse));
  }

  publishPaper(paperId: string): Observable<PaperApprovalResponse> {
    return this.http
      .post<ApiResponse<PaperApprovalResponse> | PaperApprovalResponse>(
        `${this.baseUrl}/${paperId}/publish`,
        {}
      )
      .pipe(map((res) => ((res as any)?.data ?? res) as PaperApprovalResponse));
  }

  startTranslation(
    paperId: string,
    req: PaperTranslateRequest
  ): Observable<PaperTranslateResponse> {
    return this.http
      .post<ApiResponse<PaperTranslateResponse> | PaperTranslateResponse>(
        `${this.baseUrl}/${paperId}/translate`,
        req
      )
      .pipe(map((res) => ((res as any)?.data ?? res) as PaperTranslateResponse));
  }

  getTranslationStatus(paperId: string): Observable<PaperTranslateResponse> {
    return this.http
      .get<ApiResponse<PaperTranslateResponse> | PaperTranslateResponse>(
        `${this.baseUrl}/${paperId}/translation-status`
      )
      .pipe(map((res) => ((res as any)?.data ?? res) as PaperTranslateResponse));
  }

  getTranslationJob(jobId: string): Observable<PaperTranslateResponse> {
    return this.http
      .get<ApiResponse<PaperTranslateResponse> | PaperTranslateResponse>(
        `${this.baseUrl}/translations/${jobId}`
      )
      .pipe(map((res) => ((res as any)?.data ?? res) as PaperTranslateResponse));
  }

  // --- Blueprint Templates ---

  createTemplate(req: BlueprintTemplateRequest): Observable<BlueprintTemplateResponse> {
    return this.http
      .post<ApiResponse<BlueprintTemplateResponse> | BlueprintTemplateResponse>(
        this.templateBaseUrl,
        req
      )
      .pipe(map((res) => ((res as any)?.data ?? res) as BlueprintTemplateResponse));
  }

  listTemplates(examId?: string): Observable<BlueprintTemplateResponse[]> {
    let params = new HttpParams();
    if (examId) {
      params = params.set('examId', examId);
    }
    return this.http
      .get<ApiResponse<BlueprintTemplateResponse[]> | BlueprintTemplateResponse[]>(
        this.templateBaseUrl,
        { params }
      )
      .pipe(
        map((res) => {
          const payload = (res as any)?.data ?? res;
          return (Array.isArray(payload) ? payload : []) as BlueprintTemplateResponse[];
        })
      );
  }

  getTemplate(templateId: string): Observable<BlueprintTemplateResponse> {
    return this.http
      .get<ApiResponse<BlueprintTemplateResponse> | BlueprintTemplateResponse>(
        `${this.templateBaseUrl}/${templateId}`
      )
      .pipe(map((res) => ((res as any)?.data ?? res) as BlueprintTemplateResponse));
  }

  updateTemplate(
    templateId: string,
    req: BlueprintTemplateRequest
  ): Observable<BlueprintTemplateResponse> {
    return this.http
      .put<ApiResponse<BlueprintTemplateResponse> | BlueprintTemplateResponse>(
        `${this.templateBaseUrl}/${templateId}`,
        req
      )
      .pipe(map((res) => ((res as any)?.data ?? res) as BlueprintTemplateResponse));
  }

  deleteTemplate(templateId: string): Observable<void> {
    return this.http.delete<void>(`${this.templateBaseUrl}/${templateId}`);
  }

  checkFeasibility(req: BlueprintFeasibilityRequest): Observable<BlueprintFeasibilityResponse> {
    const payload = {
      examId: req.examId,
      shiftId: req.shiftId,
      isPractice: req.isPractice,
      blueprintRules: req.blueprintRules || req.rules || [],
      notifyAdminOnDeficit: req.notifyAdminOnDeficit ?? true,
    };
    return this.http
      .post<ApiResponse<BlueprintFeasibilityResponse> | BlueprintFeasibilityResponse>(
        `${this.baseUrl}/blueprints/check-sufficiency`,
        payload
      )
      .pipe(map((res) => ((res as any)?.data ?? res) as BlueprintFeasibilityResponse));
  }

  checkTemplateSufficiency(templateId: string, notifyAdmin = false): Observable<any> {
    return this.http.post<any>(
      `${this.templateBaseUrl}/${templateId}/check-sufficiency`,
      {},
      { params: new HttpParams().set('notifyAdmin', notifyAdmin.toString()) }
    );
  }

  checkBlueprintSufficiency(request: any): Observable<any> {
    const payload = {
      examId: request.examId,
      shiftId: request.shiftId,
      isPractice: request.isPractice,
      blueprintRules: request.blueprintRules || request.rules || [],
      notifyAdminOnDeficit: request.notifyAdminOnDeficit ?? true,
    };
    return this.http.post<any>(`${this.baseUrl}/blueprints/check-sufficiency`, payload);
  }
}
