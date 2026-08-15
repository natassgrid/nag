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

export interface AdminCreateUserRequest {
  fullName: string;
  email: string;
  password: string;
  roles: string[];
}

export interface AdminUpdateUserRequest {
  fullName?: string;
  accountStatus?: string;
  mfaEnabled?: boolean;
}

export interface PermissionResponse {
  id: string;
  code: string;
  name: string;
  description: string;
  module: string;
}

export interface RoleDefinitionResponse {
  id: string;
  name: string;
  code: string;
  description: string;
  active: boolean;
  systemRole: boolean;
  permissions: PermissionResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateRoleRequest {
  name: string;
  code: string;
  description?: string;
  permissionIds?: string[];
}

export interface UpdateRoleRequest {
  name?: string;
  description?: string;
  active?: boolean;
  permissionIds?: string[];
}

export interface PaginatedPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
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

  createUser(request: AdminCreateUserRequest): Observable<UserAccountResponse> {
    return this.http.post<ApiResponse<UserAccountResponse>>(`${this.baseUrl}/users`, request).pipe(
      map(response => response.data)
    );
  }

  updateUser(userId: string, request: AdminUpdateUserRequest): Observable<UserAccountResponse> {
    return this.http.put<ApiResponse<UserAccountResponse>>(`${this.baseUrl}/users/${userId}`, request).pipe(
      map(response => response.data)
    );
  }

  deactivateUser(userId: string): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/users/${userId}`).pipe(
      map(() => undefined)
    );
  }

  assignRole(userId: string, role: string, action: 'ASSIGN' | 'REVOKE'): Observable<RoleAssignmentResponse> {
    const body: RoleAssignmentRequest = { role, action };
    return this.http.post<ApiResponse<RoleAssignmentResponse>>(`${this.baseUrl}/roles/${userId}`, body).pipe(
      map(response => response.data)
    );
  }

  // ===================================================================
  // Role Definition CRUD
  // ===================================================================

  getRoleDefinitions(page: number, size: number, search: string): Observable<PaginatedPage<RoleDefinitionResponse>> {
    return this.http.get<ApiResponse<PaginatedPage<RoleDefinitionResponse>>>(
      `${this.baseUrl}/roles/definitions`,
      { params: { page: page.toString(), size: size.toString(), search } }
    ).pipe(map(response => response.data));
  }

  getRoleDefinition(roleId: string): Observable<RoleDefinitionResponse> {
    return this.http.get<ApiResponse<RoleDefinitionResponse>>(
      `${this.baseUrl}/roles/definitions/${roleId}`
    ).pipe(map(response => response.data));
  }

  createRoleDefinition(request: CreateRoleRequest): Observable<RoleDefinitionResponse> {
    return this.http.post<ApiResponse<RoleDefinitionResponse>>(
      `${this.baseUrl}/roles/definitions`, request
    ).pipe(map(response => response.data));
  }

  updateRoleDefinition(roleId: string, request: UpdateRoleRequest): Observable<RoleDefinitionResponse> {
    return this.http.put<ApiResponse<RoleDefinitionResponse>>(
      `${this.baseUrl}/roles/definitions/${roleId}`, request
    ).pipe(map(response => response.data));
  }

  deleteRoleDefinition(roleId: string): Observable<void> {
    return this.http.delete<ApiResponse<void>>(
      `${this.baseUrl}/roles/definitions/${roleId}`
    ).pipe(map(() => undefined));
  }

  getPermissions(page: number, size: number, search: string): Observable<PaginatedPage<PermissionResponse>> {
    return this.http.get<ApiResponse<PaginatedPage<PermissionResponse>>>(
      `${this.baseUrl}/roles/permissions`,
      { params: { page: page.toString(), size: size.toString(), search } }
    ).pipe(map(response => response.data));
  }
}
