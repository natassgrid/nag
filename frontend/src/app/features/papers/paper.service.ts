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
import { Observable, map } from 'rxjs';
import { PaginatedResponse } from '../../shared/components/paginated-table/pagination.model';

// ── Domain models ────────────────────────────────────────────

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
  examName?: string;
  shiftId: string;
  shiftName?: string;
  status: 'DRAFT' | 'APPROVED' | 'ENCRYPTED';
  difficultyScore: number;
  encryptionKeyId?: string;
  createdAt?: string;
}

export interface QuestionDetail {
  questionId: string;
  subject: string;
  topic: string;
  difficulty: string;
  cognitiveLevel: string;
  usageCount: number;
  lastUsedAt?: string;
  content?: string;
}

export interface PaperDetail {
  id: string;
  examId: string;
  examName?: string;
  shiftId: string;
  shiftName?: string;
  status: 'DRAFT' | 'APPROVED' | 'ENCRYPTED';
  paperDefinitionJson?: string;
  difficultyScore: number;
  topicDistributionJson?: string;
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

// ── Blueprint template models ───────────────────────────────

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

// ── Service ──────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class PaperService {
  private readonly baseUrl = '/api/v1/papers';
  private readonly templateUrl = `${this.baseUrl}/blueprint-templates`;

  constructor(private http: HttpClient) {}

  // ── Paper generation ────────────────────────────────────────

  generatePaper(request: PaperGenerationRequest): Observable<PaperGenerationResponse> {
    return this.http.post<PaperGenerationResponse>(`${this.baseUrl}/generate`, request);
  }

  getPapers(
    page: number,
    size: number,
    examId?: string,
    status?: string
  ): Observable<PaginatedResponse<PaperSummary>> {
    let params = new HttpParams()
      .set('page', String(page))
      .set('size', String(size));

    if (examId) params = params.set('examId', examId);
    if (status) params = params.set('status', status);

    return this.http.get<any>(this.baseUrl, { params }).pipe(
      map(res => {
        const payload = res?.data ?? res;
        if (Array.isArray(payload)) {
          return { content: payload, totalElements: payload.length, totalPages: 1, size: payload.length, number: 0 };
        }
        if (payload && Array.isArray(payload.content)) {
          return {
            content: payload.content,
            totalElements: payload.totalElements ?? payload.content.length,
            totalPages: payload.totalPages ?? 1,
            size: payload.size ?? payload.content.length,
            number: payload.number ?? 0
          };
        }
        return { content: [], totalElements: 0, totalPages: 0, size, number: page };
      })
    );
  }

  getPaper(paperId: string): Observable<PaperDetail> {
    return this.http.get<any>(`${this.baseUrl}/${paperId}`).pipe(
      map(res => {
        const data = res?.data ?? res;
        // Parse topicDistributionJson if topicDistribution map is not already populated
        if (!data.topicDistribution && data.topicDistributionJson) {
          try {
            data.topicDistribution = JSON.parse(data.topicDistributionJson);
          } catch {
            data.topicDistribution = {};
          }
        }
        if (!data.totalQuestions && data.paperDefinitionJson) {
          try {
            const def = JSON.parse(data.paperDefinitionJson);
            if (Array.isArray(def?.questionIds)) {
              data.totalQuestions = def.questionIds.length;
            }
          } catch {}
        }
        return data as PaperDetail;
      })
    );
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

  // ── Blueprint templates ────────────────────────────────────

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
