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
