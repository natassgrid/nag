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
import { Observable, map } from 'rxjs';

export interface ExaminationResponse {
  id: string;
  name: string;
  code?: string;
  conductingAuthority?: string;
  category?: string;
  examinationType?: string;
  academicYear?: string;
  examinationMode?: string;
  durationMinutes: number;
  totalMarks: number;
  negativeMarkingEnabled: boolean;
  negativeMarkingValue: number;
  navigationPolicy: string;
  calculatorPolicy: string;
  reviewFlagEnabled: boolean;
  sections: Section[];
  status: string;
  createdAt: string;
}

export interface Section {
  name: string;
  questionCount: number;
  marksPerQuestion: number;
}

export interface CreateExamRequest {
  name: string;
  code?: string;
  conductingAuthority?: string;
  category?: string;
  examinationType?: string;
  academicYear?: string;
  examinationMode?: string;
  durationMinutes: number;
  totalMarks: number;
  negativeMarkingEnabled: boolean;
  negativeMarkingValue: number;
  navigationPolicy: string;
  calculatorPolicy: string;
  reviewFlagEnabled: boolean;
  sections: Section[];
}

interface ApiResponse<T> {
  status: string;
  message: string;
  data: T;
  timestamp: string;
}

@Injectable({ providedIn: 'root' })
export class ExamManagementService {
  private readonly baseUrl = '/api/v1/examinations';

  constructor(private http: HttpClient) {}

  getExams(page = 0, size = 20, search?: string): Observable<ExaminationResponse[]> {
    let params = `?page=${page}&size=${size}`;
    if (search) params += `&search=${encodeURIComponent(search)}`;
    return this.http
      .get<ApiResponse<any>>(`${this.baseUrl}${params}`)
      .pipe(map(res => res?.data?.content ?? res?.data ?? []));
  }

  createExam(data: CreateExamRequest): Observable<ExaminationResponse> {
    return this.http
      .post<ApiResponse<ExaminationResponse>>(this.baseUrl, data)
      .pipe(map(res => res.data));
  }

  updateExam(id: string, data: CreateExamRequest): Observable<ExaminationResponse> {
    return this.http
      .put<ApiResponse<ExaminationResponse>>(`${this.baseUrl}/${id}`, data)
      .pipe(map(res => res.data));
  }

  publishExam(id: string): Observable<ExaminationResponse> {
    return this.http
      .put<ApiResponse<ExaminationResponse>>(`${this.baseUrl}/${id}/publish`, {})
      .pipe(map(res => res.data));
  }

  getExam(id: string): Observable<ExaminationResponse> {
    return this.http
      .get<ApiResponse<ExaminationResponse>>(`${this.baseUrl}/${id}`)
      .pipe(map(res => res?.data as ExaminationResponse));
  }
}
