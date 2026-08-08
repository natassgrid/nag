import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { PaginatedResponse } from '../../shared/components/paginated-table/pagination.model';

// ── Domain models ────────────────────────────────────────────────────────────

export interface BlueprintRule {
  subject: string;
  topic: string;
  difficulty: 'EASY' | 'MEDIUM' | 'HARD' | '';
  cognitiveLevel: string;
  questionCount: number;
}

export interface PaperGenerationRequest {
  examId: string;
  shiftId: string;
  blueprintRules: BlueprintRule[];
}

export interface PaperSummary {
  paperId: string;
  examId: string;
  shiftId: string;
  status: 'DRAFT' | 'APPROVED' | 'ENCRYPTED';
  difficultyScore: number;
  encryptionKeyId?: string;
  createdAt?: string;
}

export interface PaperGenerationResponse {
  paperId: string;
  status: string;
  message: string;
}

export interface PaperApprovalResponse {
  paperId: string;
  status: string;
  encryptionKeyId: string;
  message: string;
}

export interface SchemaValidationResponse {
  valid: boolean;
  errors?: string[];
  message?: string;
}

// ── Blueprint template models ─────────────────────────────────────────────────

export interface BlueprintTemplateRequest {
  name: string;
  description?: string;
  examId?: string;
  rules: BlueprintRule[];
}

export interface BlueprintTemplateResponse {
  id: string;
  name: string;
  description?: string;
  examId?: string;
  rules: BlueprintRule[];
  createdBy?: string;
  createdAt?: string;
  updatedAt?: string;
  totalQuestions?: number;
}

// ── Service ──────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class PaperService {
  private readonly baseUrl = '/api/v1/papers';
  private readonly templateUrl = `${this.baseUrl}/blueprint-templates`;

  constructor(private http: HttpClient) {}

  // ── Paper generation ───────────────────────────────────────────────────────

  generatePaper(request: PaperGenerationRequest): Observable<PaperGenerationResponse> {
    return this.http.post<PaperGenerationResponse>(`${this.baseUrl}/generate`, request);
  }

  getPapers(
    page: number,
    size: number,
    examId?: string,
    status?: string
  ): Observable<PaginatedResponse<PaperSummary>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (examId) params = params.set('examId', examId);
    if (status) params = params.set('status', status);
    // Stub until the backend exposes a list endpoint
    return of({ content: [], totalElements: 0, totalPages: 0, size, number: page });
  }

  approvePaper(paperId: string): Observable<PaperApprovalResponse> {
    return this.http.post<PaperApprovalResponse>(`${this.baseUrl}/${paperId}/approve`, {});
  }

  validatePaper(json: string): Observable<SchemaValidationResponse> {
    return this.http.post<SchemaValidationResponse>(
      `${this.baseUrl}/validate`,
      json,
      { headers: { 'Content-Type': 'application/json' } }
    );
  }

  // ── Blueprint templates ────────────────────────────────────────────────────

  /** List all templates for the current tenant, optionally filtered by exam. */
  getTemplates(examId?: string): Observable<BlueprintTemplateResponse[]> {
    let params = new HttpParams();
    if (examId) params = params.set('examId', examId);
    return this.http.get<BlueprintTemplateResponse[]>(this.templateUrl, { params });
  }

  getTemplate(id: string): Observable<BlueprintTemplateResponse> {
    return this.http.get<BlueprintTemplateResponse>(`${this.templateUrl}/${id}`);
  }

  createTemplate(request: BlueprintTemplateRequest): Observable<BlueprintTemplateResponse> {
    return this.http.post<BlueprintTemplateResponse>(this.templateUrl, request);
  }

  updateTemplate(id: string, request: BlueprintTemplateRequest): Observable<BlueprintTemplateResponse> {
    return this.http.put<BlueprintTemplateResponse>(`${this.templateUrl}/${id}`, request);
  }

  deleteTemplate(id: string): Observable<void> {
    return this.http.delete<void>(`${this.templateUrl}/${id}`);
  }
}
