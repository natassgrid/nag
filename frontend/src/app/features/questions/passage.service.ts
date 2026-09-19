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
import { QuestionResponse, QuestionOptionDto } from './question.service';

export interface SubQuestionRequest {
  id?: string;
  passageOrderIndex?: number;
  content: string;
  difficulty: string;
  cognitiveLevel: string;
  questionType: string;
  options: QuestionOptionDto[];
  answerKey?: string;
  explanation?: string;
}

export interface PassageRequest {
  subjectId?: number;
  topicId?: number;
  subtopicId?: number;
  subject: string;
  topic: string;
  subtopic?: string;
  chapter?: string;
  title?: string;
  content: string;
  hasImages?: boolean;
  subQuestions: SubQuestionRequest[];
}

export interface PassageResponse {
  id: string;
  subjectId: number;
  topicId: number;
  subtopicId?: number;
  subject?: string;
  topic?: string;
  subtopic?: string;
  chapter?: string;
  title?: string;
  content: string;
  hasImages?: boolean;
  state: string;
  authorId?: string;
  subQuestions: QuestionResponse[];
  createdAt: string;
}

interface ApiResponse<T> {
  status: string;
  message: string;
  data: T;
  timestamp: string;
}

@Injectable({ providedIn: 'root' })
export class PassageService {
  private readonly baseUrl = '/api/v1/passages';

  constructor(private http: HttpClient) {}

  getPassages(filters?: {
    subject?: string;
    topic?: string;
    state?: string;
  }): Observable<PassageResponse[]> {
    let params = new HttpParams();
    if (filters?.subject) params = params.set('subject', filters.subject);
    if (filters?.topic) params = params.set('topic', filters.topic);
    if (filters?.state) params = params.set('state', filters.state);

    return this.http
      .get<ApiResponse<PassageResponse[]>>(this.baseUrl, { params })
      .pipe(map(res => res.data || []));
  }

  getPassage(id: string): Observable<PassageResponse> {
    return this.http
      .get<ApiResponse<PassageResponse>>(`${this.baseUrl}/${id}`)
      .pipe(map(res => res.data));
  }

  createPassage(data: PassageRequest): Observable<PassageResponse> {
    return this.http
      .post<ApiResponse<PassageResponse>>(this.baseUrl, data)
      .pipe(map(res => res.data));
  }

  updatePassage(id: string, data: PassageRequest): Observable<PassageResponse> {
    return this.http
      .put<ApiResponse<PassageResponse>>(`${this.baseUrl}/${id}`, data)
      .pipe(map(res => res.data));
  }

  submitForReview(id: string): Observable<PassageResponse> {
    return this.http
      .put<ApiResponse<PassageResponse>>(`${this.baseUrl}/${id}/submit`, {})
      .pipe(map(res => res.data));
  }

  approvePassage(id: string): Observable<PassageResponse> {
    return this.http
      .put<ApiResponse<PassageResponse>>(`${this.baseUrl}/${id}/approve`, {})
      .pipe(map(res => res.data));
  }

  rejectPassage(id: string, comments: string): Observable<PassageResponse> {
    return this.http
      .put<ApiResponse<PassageResponse>>(`${this.baseUrl}/${id}/reject`, { comments })
      .pipe(map(res => res.data));
  }
}
