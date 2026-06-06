import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

export interface UserAccountResponse {
  id: string;
  username: string;
  accountStatus: string;
  mfaEnabled: boolean;
  roles: string[];
  createdAt: string;
}

export interface RoleAssignmentRequest {
  role: string;
  action: 'ASSIGN' | 'REVOKE';
}

export interface RoleAssignmentResponse {
  userId: string;
  role: string;
  action: string;
  message: string;
}

interface ApiResponse<T> {
  status: string;
  data: T;
  message: string;
  timestamp: string;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly baseUrl = '/api/v1/identity';

  constructor(private http: HttpClient) {}

  getUsers(): Observable<UserAccountResponse[]> {
    return this.http.get<ApiResponse<UserAccountResponse[]>>(`${this.baseUrl}/users`).pipe(
      map(response => response.data)
    );
  }

  assignRole(userId: string, role: string, action: 'ASSIGN' | 'REVOKE'): Observable<RoleAssignmentResponse> {
    const body: RoleAssignmentRequest = { role, action };
    return this.http.post<ApiResponse<RoleAssignmentResponse>>(`${this.baseUrl}/roles/${userId}`, body).pipe(
      map(response => response.data)
    );
  }
}
