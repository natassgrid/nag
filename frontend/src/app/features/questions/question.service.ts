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

export interface QuestionOptionDto {
  id: string;
  text: string;
  isCorrect: boolean;
  imageUrl?: string;
  imageAltText?: string;
}

export interface QuestionResponse {
  id: string;
  subjectId: number;
  topicId: number;
  subtopicId?: number;
  subject: string;
  topic: string;
  subtopic: string;
  chapter: string;
  difficulty: string;
  cognitiveLevel: string;
  questionType: string;
  content: string;
  answerKey: string;
  explanation?: string;
  hasImages?: boolean;
  state: string;
  authorId: string;
  createdAt: string;
  passageId?: string;
  passageOrderIndex?: number;
  options?: QuestionOptionDto[];
  translatedLanguages?: string[];
  translationStatusMap?: Record<string, string>;
  translationStatus?: string;
}

export interface CreateQuestionRequest {
  /** Numeric hierarchy ids — source of truth for the backend link. */
  subjectId?: number;
  topicId?: number;
  subtopicId?: number;
  /** Names kept for readability; backend resolves/denormalizes from the ids. */
  subject: string;
  topic: string;
  subtopic?: string;
  difficulty: string;
  cognitiveLevel: string;
  questionType: string;
  content: string;
  answerKey?: string;
  explanation?: string;
  hasImages?: boolean;
  passageId?: string;
  passageOrderIndex?: number;
  options?: QuestionOptionDto[];
  references?: string;
  chapter?: string;
}

export interface GenerateQuestionsRequest {
  subject: string;
  topic: string;
  subtopic?: string;
  difficulty: string;
  cognitiveLevel: string;
  questionType: string;
  count: number;
  autoSave: boolean;
}

export type QuestionGenerationRequest = GenerateQuestionsRequest;

export interface BatchGenerationRequest {
  items: BatchItem[];
  avoidDuplicates: boolean;
}

export interface BatchItem {
  subject: string;
  topic: string;
  subtopic?: string;
  difficulty: string;
  cognitiveLevel: string;
  questionType: string;
  count: number;
}

export interface BatchJobResponse {
  id: string;
  status: string;
  subject: string;
  topic: string;
  subtopic?: string;
  difficulty: string;
  cognitiveLevel: string;
  questionType: string;
  totalRequested: number;
  totalGenerated: number;
  totalFailed: number;
  totalDuplicates: number;
  modelUsed: string;
  initiatedBy: string;
  createdAt: string;
  startedAt?: string;
  completedAt?: string;
  errorMessage?: string;
  progress: number;
}

export interface GeneratedQuestion {
  content: string;
  answerKey: string;
  explanation: string;
  options?: QuestionOptionDto[];
  difficulty: string;
  cognitiveLevel: string;
  questionType: string;
  validation: { valid: boolean; errors: string[] };
  duplicate?: { similarQuestionId: string; similarity: number };
  savedQuestionId?: string;
}

export interface QuestionGenerationResponse {
  questions: GeneratedQuestion[];
  modelUsed: string;
  totalGenerated: number;
  totalValid: number;
  totalDuplicates: number;
}

export interface ImportResult {
  importId: string;
  status: string;
  totalQuestions: number;
  successfulCount: number;
  failedCount: number;
  duplicateCount: number;
  errorReportUrl?: string;
  errors?: { row: number; code: string; message: string }[];
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number; // current page (0-based)
}

interface ApiResponse<T> {
  status: string;
  message: string;
  data: T;
  timestamp: string;
}

@Injectable({ providedIn: 'root' })
export class QuestionService {
  private readonly baseUrl = '/api/v1/questions';

  constructor(private http: HttpClient) {}

  getQuestions(filters?: {
    subject?: string;
    subjectId?: number | string;
    topic?: string;
    topicId?: number | string;
    difficulty?: string;
    state?: string;
    targetLang?: string;
    translationStatus?: string;
    search?: string;
    page?: number;
    size?: number;
  }): Observable<PagedResponse<QuestionResponse>> {
    let params = new HttpParams();
    if (filters) {
      if (filters.subjectId !== undefined && filters.subjectId !== null && filters.subjectId !== '') {
        params = params.set('subjectId', String(filters.subjectId));
      } else if (filters.subject) {
        params = params.set('subject', filters.subject);
      }
      if (filters.topicId !== undefined && filters.topicId !== null && filters.topicId !== '') {
        params = params.set('topicId', String(filters.topicId));
      } else if (filters.topic) {
        params = params.set('topic', filters.topic);
      }
      if (filters.difficulty) params = params.set('difficulty', filters.difficulty);
      if (filters.state)      params = params.set('state', filters.state);
      if (filters.targetLang) params = params.set('targetLang', filters.targetLang);
      if (filters.translationStatus) params = params.set('translationStatus', filters.translationStatus);
      if (filters.search)     params = params.set('search', filters.search);
      params = params.set('page', String(filters.page ?? 0));
      params = params.set('size', String(filters.size ?? 20));
    }
    return this.http
      .get<ApiResponse<any>>(this.baseUrl, { params })
      .pipe(map(res => {
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
        return { content: [], totalElements: 0, totalPages: 0, size: 20, number: 0 };
      }));
  }

  createQuestion(data: CreateQuestionRequest): Observable<QuestionResponse> {
    return this.http
      .post<ApiResponse<QuestionResponse>>(this.baseUrl, data)
      .pipe(map(res => res.data));
  }

  updateQuestion(id: string, data: CreateQuestionRequest): Observable<QuestionResponse> {
    return this.http
      .put<ApiResponse<QuestionResponse>>(`${this.baseUrl}/${id}`, data)
      .pipe(map(res => res.data));
  }

  submitForReview(id: string): Observable<QuestionResponse> {
    return this.http
      .put<ApiResponse<QuestionResponse>>(`${this.baseUrl}/${id}/submit`, {})
      .pipe(map(res => res.data));
  }

  getQuestionsForReview(page = 0, size = 20, search?: string): Observable<PagedResponse<QuestionResponse>> {
    return this.getQuestions({ state: 'REVIEW', search, page, size });
  }

  approveQuestion(id: string): Observable<QuestionResponse> {
    return this.http
      .put<ApiResponse<QuestionResponse>>(`${this.baseUrl}/${id}/approve`, {})
      .pipe(map(res => res.data));
  }

  rejectQuestion(id: string, comments: string): Observable<QuestionResponse> {
    return this.http
      .put<ApiResponse<QuestionResponse>>(`${this.baseUrl}/${id}/reject`, { comments })
      .pipe(map(res => res.data));
  }

  /**
   * Exports questions matching the given filters as a compressed ZIP archive.
   * The archive contains batch files (100 questions each) plus a manifest.
   * Returns the raw Blob so the caller can trigger a browser download.
   */
  exportQuestions(filters?: {
    format?: 'json' | 'csv';
    subject?: string;
    subjectId?: number | string;
    topic?: string;
    topicId?: number | string;
    difficulty?: string;
    state?: string;
    search?: string;
  }): Observable<Blob> {
    let params = new HttpParams().set('format', filters?.format ?? 'json');
    if (filters?.subjectId !== undefined && filters?.subjectId !== null && filters?.subjectId !== '') {
      params = params.set('subjectId', String(filters?.subjectId));
    } else if (filters?.subject) {
      params = params.set('subject', filters.subject);
    }
    if (filters?.topicId !== undefined && filters?.topicId !== null && filters?.topicId !== '') {
      params = params.set('topicId', String(filters?.topicId));
    } else if (filters?.topic) {
      params = params.set('topic', filters.topic);
    }
    if (filters?.difficulty) params = params.set('difficulty', filters.difficulty);
    if (filters?.state)      params = params.set('state', filters.state);
    if (filters?.search)     params = params.set('search', filters.search);
    return this.http.get(`${this.baseUrl}/export`, { params, responseType: 'blob' });
  }

  /**
   * Imports questions from a ZIP archive of JSON/CSV batch files.
   */
  importQuestions(file: File, subjectId?: number): Observable<ImportResult> {
    const formData = new FormData();
    formData.append('file', file);
    let params = new HttpParams();
    if (subjectId !== undefined && subjectId !== null) {
      params = params.set('subjectId', String(subjectId));
    }
    return this.http
      .post<ApiResponse<ImportResult>>(`${this.baseUrl}/import`, formData, { params })
      .pipe(map(res => res.data));
  }

  generateQuestions(req: GenerateQuestionsRequest): Observable<QuestionGenerationResponse> {
    return this.http
      .post<ApiResponse<QuestionGenerationResponse>>('/api/v1/questions/ai-generate', req)
      .pipe(map(res => res.data));
  }

  generateBatch(req: BatchGenerationRequest): Observable<BatchJobResponse> {
    return this.http
      .post<ApiResponse<BatchJobResponse>>('/api/v1/questions/batch-generate', req)
      .pipe(map(res => res.data));
  }

  submitBatchJob(req: BatchGenerationRequest): Observable<BatchJobResponse> {
    return this.generateBatch(req);
  }

  getBatchJobStatus(jobId: string): Observable<BatchJobResponse> {
    return this.http
      .get<ApiResponse<BatchJobResponse>>(`/api/v1/questions/batch-generate/${jobId}`)
      .pipe(map(res => res.data));
  }

  cancelBatchJob(jobId: string): Observable<BatchJobResponse> {
    return this.http
      .post<ApiResponse<BatchJobResponse>>(`/api/v1/questions/batch-generate/${jobId}/cancel`, {})
      .pipe(map(res => res.data));
  }
}
