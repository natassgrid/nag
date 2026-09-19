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
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

export interface Subject {
  id: number;
  name: string;
  code?: string;
  description?: string;
  topicCount?: number;
  questionCount?: number;
}

export interface Topic {
  id: number;
  subjectId: number;
  name: string;
  description?: string;
  subtopicCount?: number;
  questionCount?: number;
}

export interface Subtopic {
  id: number;
  topicId: number;
  name: string;
  description?: string;
  questionCount?: number;
}

export interface SubtopicNode {
  id: number;
  name: string;
  description?: string;
  questionCount?: number;
}

export interface TopicNode {
  id: number;
  name: string;
  description?: string;
  questionCount?: number;
  subtopics: SubtopicNode[];
}

export interface SubjectHierarchy {
  id: number;
  name: string;
  code?: string;
  description?: string;
  questionCount?: number;
  topics: TopicNode[];
}

interface ApiResponse<T> {
  status: string;
  message?: string;
  data: T;
}

@Injectable({
  providedIn: 'root'
})
export class SubjectTopicService {
  private readonly baseUrl = '/api/v1/subjects';

  constructor(private http: HttpClient) {}

  getSubjects(): Observable<Subject[]> {
    return this.http
      .get<ApiResponse<Subject[]>>(this.baseUrl)
      .pipe(map(res => res.data));
  }

  getSubject(id: number): Observable<Subject> {
    return this.http
      .get<ApiResponse<Subject>>(`${this.baseUrl}/${id}`)
      .pipe(map(res => res.data));
  }

  createSubject(data: { name: string; code?: string; description?: string }): Observable<Subject> {
    return this.http
      .post<ApiResponse<Subject>>(this.baseUrl, data)
      .pipe(map(res => res.data));
  }

  updateSubject(id: number, data: { name: string; code?: string; description?: string }): Observable<Subject> {
    return this.http
      .put<ApiResponse<Subject>>(`${this.baseUrl}/${id}`, data)
      .pipe(map(res => res.data));
  }

  deleteSubject(id: number): Observable<void> {
    return this.http
      .delete<ApiResponse<void>>(`${this.baseUrl}/${id}`)
      .pipe(map(() => void 0));
  }

  getTopics(subjectId: number): Observable<Topic[]> {
    return this.http
      .get<ApiResponse<Topic[]>>(`${this.baseUrl}/${subjectId}/topics`)
      .pipe(map(res => res.data));
  }

  createTopic(subjectId: number, data: { name: string; description?: string }): Observable<Topic> {
    return this.http
      .post<ApiResponse<Topic>>(`${this.baseUrl}/${subjectId}/topics`, data)
      .pipe(map(res => res.data));
  }

  getSubtopics(subjectId: number, topicId: number): Observable<Subtopic[]> {
    return this.http
      .get<ApiResponse<Subtopic[]>>(`${this.baseUrl}/${subjectId}/topics/${topicId}/subtopics`)
      .pipe(map(res => res.data));
  }

  createSubtopic(subjectId: number, topicId: number, data: { name: string; description?: string }): Observable<Subtopic> {
    return this.http
      .post<ApiResponse<Subtopic>>(`${this.baseUrl}/${subjectId}/topics/${topicId}/subtopics`, data)
      .pipe(map(res => res.data));
  }

  getHierarchy(): Observable<SubjectHierarchy[]> {
    return this.http
      .get<ApiResponse<SubjectHierarchy[]>>(`${this.baseUrl}/hierarchy`)
      .pipe(map(res => res.data));
  }
}
