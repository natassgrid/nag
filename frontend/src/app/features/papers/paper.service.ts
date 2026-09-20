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
import { PaginatedResponse } from '../../shared/components/paginated-table/pagination.model';

// ── Interfaces ─────────────────────────────────────────────────────────────

export interface PaperSummary {
  paperId: string;
  name?: string;
  examId?: string;
  examName?: string;
  shiftId?: string;
  shiftName?: string;
  status: string;
  isPractice: boolean;
  difficultyScore: number;
  encryptionKeyId?: string;
  createdAt?: string;
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

export interface GapDetail {
  subject: string;
  topic: string;
  difficulty?: string;
  needed: number;
  available: number;
}

export interface RuleFeasibilityDetail {
  subject: string;
  topic: string;
  difficulty?: string;
  cognitiveLevel?: string;
  needed: number;
  available: number;
  surplus: number;
  deficit: number;
  status: 'SATISFIED' | 'DEFICIT';
}

export interface BlueprintFeasibilityRequest {
  examId?: string;
  shiftId?: string;
  blueprintRules: BlueprintRule[];
  notifyAdminOnDeficit?: boolean;
}

export interface BlueprintFeasibilityResponse {
  feasible: boolean;
  examId?: string;
  shiftId?: string;
  totalQuestionsNeeded: number;
  totalQuestionsAvailable: number;
  deficitRuleCount: number;
  ruleDetails: RuleFeasibilityDetail[];
  gaps: GapDetail[];
  notificationDispatched: boolean;
  summary: string;
  checkedAt: string;
}

// ── Service ────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class PaperService {
  private readonly baseUrl = '/api/v1/papers';
  private readonly templateBaseUrl = '/api/v1/papers/blueprint-templates';

  constructor(private http: HttpClient) {}

  getPapers(
    page: number = 0,
    size: number = 20,
    examId?: string,
    status?: string,
    search?: string,
    sort?: string,
    order?: string
  ): Observable<PaginatedResponse<PaperSummary>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (examId) {
      params = params.set('examId', examId);
    }
    if (status) {
      params = params.set('status', status);
    }
    if (search) {
      params = params.set('search', search);
    }
    if (sort) {
      params = params.set('sort', sort);
    }
    if (order) {
      params = params.set('order', order);
    }

    return this.http.get<any>(this.baseUrl, { params }).pipe(
      map((res) => ({
        content: (res.content ?? []).map((p: any) => ({
          paperId: p.paperId ?? p.id,
          name: p.name,
          examId: p.examId,
          examName: p.examName,
          shiftId: p.shiftId,
          shiftName: p.shiftName,
          status: p.status,
          isPractice: Boolean(p.isPractice),
          difficultyScore: p.difficultyScore ?? 0,
          encryptionKeyId: p.encryptionKeyId,
          createdAt: p.createdAt
        })),
        totalElements: res.totalElements ?? 0,
        totalPages: res.totalPages ?? 0,
        number: res.number ?? 0,
        size: res.size ?? size
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

  getTranslationStatus(paperId: string): Observable<PaperTranslateResponse> {
    return this.http.get<PaperTranslateResponse>(`${this.baseUrl}/${paperId}/translation-status`);
  }

  getTranslationJob(jobId: string): Observable<PaperTranslateResponse> {
    return this.http.get<PaperTranslateResponse>(`${this.baseUrl}/translations/${jobId}`);
  }

  // ── Blueprint Templates ──────────────────────────────────────────────────

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
}
