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
import { Observable, map, of, catchError } from 'rxjs';

export interface SectionScore {
  sectionName: string;
  score: number;
  maxMarks: number;
}

export interface ResultResponse {
  id: string;
  candidateId: string;
  examId: string;
  totalScore: number;
  sectionScores: SectionScore[];
  overallRank: number;
  overallPercentile: number;
  scorecardPdfRef: string;
  createdAt: string;
}

interface ApiResponse<T> {
  data: T;
  message?: string;
  status?: string;
}

@Injectable({ providedIn: 'root' })
export class ResultService {
  private readonly baseUrl = '/api/v1/results';

  constructor(private http: HttpClient) {}

  getResults(candidateId: string): Observable<ResultResponse[]> {
    return this.http
      .get<ApiResponse<ResultResponse[]>>(`${this.baseUrl}?candidateId=${candidateId}`)
      .pipe(
        map(res => res?.data ?? []),
        catchError(() => of([] as ResultResponse[]))
      );
  }

  getResult(candidateId: string, examId: string): Observable<ResultResponse> {
    return this.http
      .get<ApiResponse<ResultResponse>>(`${this.baseUrl}/${candidateId}/${examId}`)
      .pipe(
        map(res => res?.data ?? ({} as ResultResponse)),
        catchError(() => of({} as ResultResponse))
      );
  }
}
