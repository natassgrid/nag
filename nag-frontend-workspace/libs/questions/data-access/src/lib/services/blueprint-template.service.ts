import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map, catchError } from 'rxjs';
import {
  BlueprintRequest,
  BlueprintResponse,
  BlueprintTemplateRequest,
  BlueprintTemplateResponse,
  BlueprintFeasibilityResponse,
} from '../models/blueprint.model';

@Injectable({
  providedIn: 'root',
})
export class BlueprintTemplateService {
  private readonly http = inject(HttpClient);
  private readonly primaryUrl = '/api/v1/papers/blueprints';
  private readonly legacyUrl = '/api/v1/papers/blueprint-templates';

  listBlueprints(examId?: string): Observable<BlueprintResponse[]> {
    let params = new HttpParams();
    if (examId) {
      params = params.set('examId', examId);
    }
    return this.http
      .get<{ data?: BlueprintResponse[] } | BlueprintResponse[]>(this.primaryUrl, { params })
      .pipe(
        map((res) => (Array.isArray(res) ? res : (res as any)?.data || [])),
        catchError(() =>
          this.http
            .get<{ data?: BlueprintResponse[] } | BlueprintResponse[]>(this.legacyUrl, { params })
            .pipe(map((res) => (Array.isArray(res) ? res : (res as any)?.data || [])))
        )
      );
  }

  listTemplates(examId?: string): Observable<BlueprintTemplateResponse[]> {
    return this.listBlueprints(examId);
  }

  getBlueprint(id: string): Observable<BlueprintResponse> {
    return this.http
      .get<{ data?: BlueprintResponse } | BlueprintResponse>(`${this.primaryUrl}/${id}`)
      .pipe(
        map((res) => ((res as any)?.data || res) as BlueprintResponse),
        catchError(() =>
          this.http
            .get<{ data?: BlueprintResponse } | BlueprintResponse>(`${this.legacyUrl}/${id}`)
            .pipe(map((res) => ((res as any)?.data || res) as BlueprintResponse))
        )
      );
  }

  getTemplate(id: string): Observable<BlueprintTemplateResponse> {
    return this.getBlueprint(id);
  }

  createBlueprint(req: BlueprintRequest): Observable<BlueprintResponse> {
    return this.http
      .post<{ data?: BlueprintResponse } | BlueprintResponse>(this.primaryUrl, req)
      .pipe(
        map((res) => ((res as any)?.data || res) as BlueprintResponse),
        catchError(() =>
          this.http
            .post<{ data?: BlueprintResponse } | BlueprintResponse>(this.legacyUrl, req)
            .pipe(map((res) => ((res as any)?.data || res) as BlueprintResponse))
        )
      );
  }

  createTemplate(req: BlueprintTemplateRequest): Observable<BlueprintTemplateResponse> {
    return this.createBlueprint(req);
  }

  updateBlueprint(id: string, req: BlueprintRequest): Observable<BlueprintResponse> {
    return this.http
      .put<{ data?: BlueprintResponse } | BlueprintResponse>(`${this.primaryUrl}/${id}`, req)
      .pipe(
        map((res) => ((res as any)?.data || res) as BlueprintResponse),
        catchError(() =>
          this.http
            .put<{ data?: BlueprintResponse } | BlueprintResponse>(`${this.legacyUrl}/${id}`, req)
            .pipe(map((res) => ((res as any)?.data || res) as BlueprintResponse))
        )
      );
  }

  updateTemplate(id: string, req: BlueprintTemplateRequest): Observable<BlueprintTemplateResponse> {
    return this.updateBlueprint(id, req);
  }

  deleteBlueprint(id: string): Observable<void> {
    return this.http.delete<void>(`${this.primaryUrl}/${id}`).pipe(
      catchError(() => this.http.delete<void>(`${this.legacyUrl}/${id}`))
    );
  }

  deleteTemplate(id: string): Observable<void> {
    return this.deleteBlueprint(id);
  }

  checkSufficiency(
    id: string,
    examId?: string,
    shifts = 1,
    notifyAdmin = false
  ): Observable<BlueprintFeasibilityResponse> {
    let params = new HttpParams()
      .set('notifyAdmin', notifyAdmin.toString())
      .set('shifts', shifts.toString());
    if (examId) {
      params = params.set('examId', examId);
    }

    return this.http
      .post<{ data?: BlueprintFeasibilityResponse } | BlueprintFeasibilityResponse>(
        `${this.primaryUrl}/${id}/check-sufficiency`,
        {},
        { params }
      )
      .pipe(
        map((res) => ((res as any)?.data || res) as BlueprintFeasibilityResponse),
        catchError(() =>
          this.http
            .post<{ data?: BlueprintFeasibilityResponse } | BlueprintFeasibilityResponse>(
              `${this.legacyUrl}/${id}/check-sufficiency`,
              {},
              { params }
            )
            .pipe(map((res) => ((res as any)?.data || res) as BlueprintFeasibilityResponse))
        )
      );
  }
}
