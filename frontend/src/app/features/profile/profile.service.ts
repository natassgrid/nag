import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

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

interface ApiResponse<T> {
  status: string;
  data: T;
}

@Injectable({ providedIn: 'root' })
export class ProfileService {
  constructor(private http: HttpClient) {}

  getProfile(userId: string): Observable<CandidateProfile> {
    return this.http.get<ApiResponse<CandidateProfile>>(`/api/v1/candidates/${userId}`)
      .pipe(map(res => res.data));
  }
}
