import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import {
  BlueprintTemplateRequest,
  BlueprintTemplateResponse,
  BlueprintFeasibilityResponse,
} from '../models/blueprint.model';

@Injectable({
  providedIn: 'root',
})
export class BlueprintTemplateService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/papers/blueprint-templates';

  listTemplates(examId?: string): Observable<BlueprintTemplateResponse[]> {
    let params = new HttpParams();
    if (examId) {
      params = params.set('examId', examId);
    }
    return this.http
      .get<{ data?: BlueprintTemplateResponse[] } | BlueprintTemplateResponse[]>(this.baseUrl, { params })
      .pipe(map((res) => (Array.isArray(res) ? res : (res as any)?.data || [])));
  }

  getTemplate(id: string): Observable<BlueprintTemplateResponse> {
    return this.http
      .get<{ data?: BlueprintTemplateResponse } | BlueprintTemplateResponse>(`${this.baseUrl}/${id}`)
      .pipe(map((res) => ((res as any).data || res) as BlueprintTemplateResponse));
  }

  createTemplate(req: BlueprintTemplateRequest): Observable<BlueprintTemplateResponse> {
    return this.http
      .post<{ data?: BlueprintTemplateResponse } | BlueprintTemplateResponse>(this.baseUrl, req)
      .pipe(map((res) => ((res as any).data || res) as BlueprintTemplateResponse));
  }

  updateTemplate(id: string, req: BlueprintTemplateRequest): Observable<BlueprintTemplateResponse> {
    return this.http
      .put<{ data?: BlueprintTemplateResponse } | BlueprintTemplateResponse>(`${this.baseUrl}/${id}`, req)
      .pipe(map((res) => ((res as any).data || res) as BlueprintTemplateResponse));
  }

  deleteTemplate(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  checkSufficiency(id: string, notifyAdmin = false): Observable<BlueprintFeasibilityResponse> {
    const params = new HttpParams().set('notifyAdmin', notifyAdmin.toString());
    return this.http
      .post<{ data?: BlueprintFeasibilityResponse } | BlueprintFeasibilityResponse>(
        `${this.baseUrl}/${id}/check-sufficiency`,
        {},
        { params }
      )
      .pipe(map((res) => ((res as any).data || res) as BlueprintFeasibilityResponse));
  }
}
