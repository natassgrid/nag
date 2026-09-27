import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { ApiResponse } from '../models/common.model';
import { GeoCountry, GeoState, GeoCity } from '../models/geolocation.model';

@Injectable({
  providedIn: 'root',
})
export class GeoLocationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/geo';

  getCountries(): Observable<GeoCountry[]> {
    return this.http
      .get<ApiResponse<GeoCountry[]> | GeoCountry[]>(`${this.baseUrl}/countries`)
      .pipe(
        map((res) => {
          const payload = (res as any)?.data ?? res;
          return (Array.isArray(payload) ? payload : []) as GeoCountry[];
        })
      );
  }

  getStates(countryId: number): Observable<GeoState[]> {
    return this.http
      .get<ApiResponse<GeoState[]> | GeoState[]>(
        `${this.baseUrl}/countries/${countryId}/states`
      )
      .pipe(
        map((res) => {
          const payload = (res as any)?.data ?? res;
          return (Array.isArray(payload) ? payload : []) as GeoState[];
        })
      );
  }

  getCities(stateId: number): Observable<GeoCity[]> {
    return this.http
      .get<ApiResponse<GeoCity[]> | GeoCity[]>(`${this.baseUrl}/states/${stateId}/cities`)
      .pipe(
        map((res) => {
          const payload = (res as any)?.data ?? res;
          return (Array.isArray(payload) ? payload : []) as GeoCity[];
        })
      );
  }
}
