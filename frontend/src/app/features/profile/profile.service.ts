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

export interface CandidateProfile {
  id: string;
  name: string;
  email: string;
  mobile: string;
  identityDocType: string;
  identityDocNumber: string;
  verificationStatus: string;
  registrationDate: string;
}

export interface ProfileCreateUpdateRequest {
  name: string;
  email: string;
  mobile: string;
  identityDocType: string;
  identityDocNumber: string;
}

interface ApiResponse<T> {
  status: string;
  data: T;
}

@Injectable({ providedIn: 'root' })
export class ProfileService {
  constructor(private http: HttpClient) {}

  getProfile(userId: string): Observable<CandidateProfile | null> {
    return this.http.get<ApiResponse<CandidateProfile>>(`/api/v1/candidates/${userId}`)
      .pipe(
        map(res => res?.data ?? null),
        catchError(() => of(null))
      );
  }

  createProfile(data: ProfileCreateUpdateRequest): Observable<CandidateProfile> {
    return this.http.post<ApiResponse<CandidateProfile>>('/api/v1/candidates', data)
      .pipe(map(res => res.data));
  }

  updateProfile(userId: string, data: ProfileCreateUpdateRequest): Observable<CandidateProfile> {
    return this.http.put<ApiResponse<CandidateProfile>>(`/api/v1/candidates/${userId}`, data)
      .pipe(map(res => res.data));
  }
}
