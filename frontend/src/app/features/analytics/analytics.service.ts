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

export interface ScoreDistributionEntry {
  range: string;
  count: number;
}

export interface SectionAverage {
  sectionName: string;
  average: number;
}

export interface ExamAnalytics {
  examId: string;
  totalRegistered: number;
  totalAppeared: number;
  scoreDistribution: ScoreDistributionEntry[];
  sectionAverages: SectionAverage[];
  top10PercentileThreshold: number;
  bottom10PercentileThreshold: number;
}

interface ApiResponse<T> {
  data: T;
  message?: string;
  status?: string;
}

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private readonly baseUrl = '/api/v1/analytics';

  constructor(private http: HttpClient) {}

  getExamAnalytics(examId: string): Observable<ExamAnalytics> {
    return this.http
      .get<ApiResponse<ExamAnalytics>>(`${this.baseUrl}/exams/${examId}`)
      .pipe(map(res => res.data));
  }
}
