/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { PaginatedResponse } from '../../shared/components/paginated-table';

export interface PaperSummary {
  id: string;
  name: string;
  examId?: string;
  examName?: string;
  shiftId?: string;
  shiftName?: string;
  status: string;
  isPractice?: boolean;
  totalMarks?: number;
  totalQuestions?: number;
  difficultyScore?: number;
  generatedBy?: string;
  approvedBy?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PaperGenerationRequest {
  examId: string;
  shiftId: string;
  paperName?: string;
  isPractice?: boolean;
  blueprintRules: BlueprintRule[];
}

export interface BlueprintRule {
  subject: string;
  topic: string;
  difficulty?: string;
  cognitiveLevel?: string;
  questionType?: string;
  targetCount: number;
  questionCount?: number;
}

export interface QuestionDetail {
  questionId: string;
  content: string;
  answerKey: string;
  subject: string;
  topic: string;
  difficulty: string;
  cognitiveLevel: string;
  orderIndex: number;
  marks: number;
  negativeMarks: number;
  explanation?: string;
  options?: any[];
  usageCount?: number;
  lastUsedAt?: string;
}

export interface PaperDetail {
  id?: string;
  paperId?: string;
  name?: string;
  examId?: string;
  examName?: string;
  shiftId?: string;
  shiftName?: string;
  status?: string;
  isPractice?: boolean;
  totalMarks?: number;
  difficultyScore?: number;
  encryptedPackageRef?: string;
  encryptionKeyId?: string;
  generatedBy?: string;
  createdAt?: string;
  updatedAt?: string;
  totalQuestions?: number;
  topicDistribution?: Record<string, number>;
  questions?: QuestionDetail[];
  paperDefinitionJson?: string;
}

export interface PaperGenerationResponse {
  paperId: string;
  name?: string;
  status: string;
  isPractice?: boolean;
  message: string;
}

export interface PaperApprovalResponse {
  paperId: string;
  name?: string;
  status: string;
  isPractice?: boolean;
  encryptionKeyId?: string;
  message: string;
}

export interface PaperTranslateRequest {
  targetLanguage?: string;
  sourceLanguage?: string;
  targetStatus?: string;
  overwriteExisting?: boolean;
  maxConcurrency?: number;
  throttleDelayMs?: number;
}

export interface PaperTranslateResponse {
  jobId: string;
  paperId: string;
  status: string;
  sourceLanguage: string;
  targetLanguage: string;
  targetStatus: string;
  overwriteExisting: boolean;
  totalQuestions: number;
  processedQuestions: number;
  successfulQuestions: number;
  failedQuestions: number;
  progressPercentage: number;
  errorMessage?: string;
  message?: string;
  createdAt?: string;
  completedAt?: string;
}

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
  examName?: string;
  rules: BlueprintRule[];
  totalQuestions?: number;
  createdBy?: string;
  createdAt?: string;
  updatedAt?: string;
  version?: number;
}

export interface RuleFeasibility {
  subject: string;
  topic: string;
  difficulty?: string;
  cognitiveLevel?: string;
  requested: number;
  available: number;
  sufficient: boolean;
}

export interface BlueprintFeasibilityRequest {
  rules: BlueprintRule[];
}

export interface BlueprintFeasibilityResponse {
  feasible: boolean;
  totalRequested: number;
  totalAvailable: number;
  rules: RuleFeasibility[];
  insufficientRules?: RuleFeasibility[];
  overallSufficiency?: number;
}

export interface PaperListParams {
  page?: number;
  size?: number;
  sort?: string;
  order?: string;
  search?: string;
  status?: string;
  examId?: string;
  shiftId?: string;
  isPractice?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class PaperService {
  private readonly baseUrl = '/api/v1/papers';
  private readonly templateBaseUrl = '/api/v1/papers/templates';

  constructor(private http: HttpClient) {}

  getPapers(params?: PaperListParams): Observable<PaginatedResponse<PaperSummary>> {
    let httpParams = new HttpParams();

    if (params) {
      if (params.page !== undefined) httpParams = httpParams.set('page', params.page.toString());
      if (params.size !== undefined) httpParams = httpParams.set('size', params.size.toString());
      if (params.sort) httpParams = httpParams.set('sort', params.sort);
      if (params.order) httpParams = httpParams.set('order', params.order);
      if (params.search) httpParams = httpParams.set('search', params.search);
      if (params.status) httpParams = httpParams.set('status', params.status);
      if (params.examId) httpParams = httpParams.set('examId', params.examId);
      if (params.shiftId) httpParams = httpParams.set('shiftId', params.shiftId);
      if (params.isPractice !== undefined) httpParams = httpParams.set('isPractice', params.isPractice.toString());
    }

    return this.http.get<any>(this.baseUrl, { params: httpParams }).pipe(
      map(res => ({
        content: res.data?.content || res.content || [],
        totalElements: res.data?.totalElements ?? res.totalElements ?? 0,
        totalPages: res.data?.totalPages ?? res.totalPages ?? 0,
        size: res.data?.size ?? res.size ?? (params?.size || 10),
        number: res.data?.number ?? res.number ?? (params?.page || 0)
      }))
    );
  }

  getPaper(paperId: string): Observable<PaperDetail> {
    return this.http.get<PaperDetail>(`${this.baseUrl}/${paperId}`);
  }

  generatePaper(request: PaperGenerationRequest): Observable<PaperGenerationResponse> {
    return this.http.post<PaperGenerationResponse>(`${this.baseUrl}/generate`, request);
  }

  approvePaper(paperId: string): Observable<PaperApprovalResponse> {
    return this.http.post<PaperApprovalResponse>(`${this.baseUrl}/${paperId}/approve`, {});
  }

  publishPaper(paperId: string): Observable<PaperApprovalResponse> {
    return this.http.post<PaperApprovalResponse>(`${this.baseUrl}/${paperId}/publish`, {});
  }

  startTranslation(paperId: string, req: PaperTranslateRequest): Observable<PaperTranslateResponse> {
    return this.http.post<PaperTranslateResponse>(`${this.baseUrl}/${paperId}/translate`, req);
  }

  translatePaper(paperId: string, req: PaperTranslateRequest): Observable<PaperTranslateResponse> {
    return this.startTranslation(paperId, req);
  }

  getTranslationStatus(paperId: string): Observable<PaperTranslateResponse> {
    return this.http.get<PaperTranslateResponse>(`${this.baseUrl}/${paperId}/translation-status`);
  }

  getPaperTranslationStatus(paperId: string, jobId?: string): Observable<PaperTranslateResponse> {
    return jobId ? this.getTranslationJob(jobId) : this.getTranslationStatus(paperId);
  }

  getTranslationJob(jobId: string): Observable<PaperTranslateResponse> {
    return this.http.get<PaperTranslateResponse>(`${this.baseUrl}/translations/${jobId}`);
  }

  // ── Blueprint Templates ──────────────────────────────────────────

  createTemplate(req: BlueprintTemplateRequest): Observable<BlueprintTemplateResponse> {
    return this.http.post<BlueprintTemplateResponse>(this.templateBaseUrl, req);
  }

  listTemplates(examId?: string): Observable<BlueprintTemplateResponse[]> {
    let params = new HttpParams();
    if (examId) {
      params = params.set('examId', examId);
    }
    return this.http.get<BlueprintTemplateResponse[]>(this.templateBaseUrl, { params });
  }

  getTemplates(examId?: string): Observable<BlueprintTemplateResponse[]> {
    return this.listTemplates(examId);
  }

  getTemplate(templateId: string): Observable<BlueprintTemplateResponse> {
    return this.http.get<BlueprintTemplateResponse>(`${this.templateBaseUrl}/${templateId}`);
  }

  updateTemplate(templateId: string, req: BlueprintTemplateRequest): Observable<BlueprintTemplateResponse> {
    return this.http.put<BlueprintTemplateResponse>(`${this.templateBaseUrl}/${templateId}`, req);
  }

  deleteTemplate(templateId: string): Observable<void> {
    return this.http.delete<void>(`${this.templateBaseUrl}/${templateId}`);
  }

  checkFeasibility(req: BlueprintFeasibilityRequest): Observable<BlueprintFeasibilityResponse> {
    return this.http.post<BlueprintFeasibilityResponse>(`${this.templateBaseUrl}/feasibility`, req);
  }

  checkTemplateSufficiency(templateId: string, notifyAdmin = false): Observable<any> {
    return this.http.post<any>(`${this.templateBaseUrl}/${templateId}/check-sufficiency`, { notifyAdmin });
  }

  checkBlueprintSufficiency(request: any): Observable<any> {
    return this.http.post<any>(`${this.templateBaseUrl}/check-sufficiency`, request);
  }
}
