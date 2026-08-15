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

export interface GeoCountry {
  id: number;
  name: string;
  iso2: string;
  iso3: string;
  phoneCode: string;
  capital: string;
  currency: string;
  region: string;
  subregion: string;
  active: boolean;
}

export interface GeoState {
  id: number;
  name: string;
  countryId: number;
  stateCode: string;
  type: string;
  active: boolean;
}

export interface GeoCity {
  id: number;
  name: string;
  stateId: number;
  countryId: number;
  latitude: number;
  longitude: number;
  active: boolean;
}

interface ApiResponse<T> {
  status: string;
  message: string;
  data: T;
  timestamp: string;
}

@Injectable({ providedIn: 'root' })
export class GeoLocationService {
  private readonly base = '/api/v1/geo';

  constructor(private http: HttpClient) {}

  getCountries(): Observable<GeoCountry[]> {
    return this.http.get<ApiResponse<GeoCountry[]>>(`${this.base}/countries`).pipe(map(r => r.data));
  }

  getStates(countryId: number): Observable<GeoState[]> {
    return this.http.get<ApiResponse<GeoState[]>>(`${this.base}/countries/${countryId}/states`).pipe(map(r => r.data));
  }

  getCities(stateId: number): Observable<GeoCity[]> {
    return this.http.get<ApiResponse<GeoCity[]>>(`${this.base}/states/${stateId}/cities`).pipe(map(r => r.data));
  }
}
