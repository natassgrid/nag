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
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

export interface UserAccountResponse {
  id: string;
  username: string;
  email: string;
  roles: string[];
  accountStatus: string;
  mfaEnabled: boolean;
  tenantId: string;
  createdAt: string;
  updatedAt: string;
}

export interface AdminInviteRequest {
  email: string;
  fullName: string;
  specialization?: string;
  roles: string[];
}

export interface AdminCreateUserRequest {
  email: string;
  fullName: string;
  password?: string;
  roles: string[];
}

export interface AdminUpdateUserRequest {
  accountStatus?: 'ACTIVE' | 'DEACTIVATED';
  mfaEnabled?: boolean;
}

export interface RoleAssignmentRequest {
  role: string;
  action: 'ASSIGN' | 'REVOKE';
}

export interface RoleAssignmentResponse {
  userId: string;
  roles: string[];
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

export interface AuditEventResponse {
  id: string;
  eventType: string;
  principal: string;
  ipAddress: string;
  action: string;
  resourceId?: string;
  severity: 'INFO' | 'WARN' | 'ERROR' | 'CRITICAL';
  status: 'SUCCESS' | 'FAILURE';
  details?: Record<string, any>;
  timestamp: string;
}

export interface SystemConfigItem {
  id: string;
  paramName: string;
  paramValue: string;
  tenantId: string;
  updatedBy?: string;
  updatedAtConfig: string;
  createdAt: string;
  updatedAt: string;
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
  private readonly auditUrl = '/api/v1/audit';
  private readonly configUrl = '/api/v1/admin/config';

  constructor(private http: HttpClient) {}

  getUsers(): Observable<UserAccountResponse[]> {
    return this.http.get<ApiResponse<UserAccountResponse[]>>(`${this.baseUrl}/users`).pipe(
      map(response => response.data)
    );
  }

  inviteUser(request: AdminInviteRequest): Observable<any> {
    return this.http.post<ApiResponse<any>>(`${this.baseUrl}/admin/invite`, request).pipe(
      map(response => response.data)
    );
  }

  inviteAdmin(request: AdminInviteRequest): Observable<any> {
    return this.http.post<ApiResponse<any>>(`${this.baseUrl}/admin/invite`, request).pipe(
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
    return this.http.post<ApiResponse<RoleAssignmentResponse>>(`${this.baseUrl}/roles/assignments/${userId}`, body).pipe(
      map(response => response.data)
    );
  }

  // ===================================================================
  // Role Definition CRUD
  // ===================================================================

  getRoleDefinitions(page: number, size: number, search: string, sort?: string, order?: string): Observable<PaginatedPage<RoleDefinitionResponse>> {
    const params: Record<string, string> = { page: page.toString(), size: size.toString(), search };
    if (sort) params['sort'] = sort;
    if (order) params['order'] = order;
    return this.http.get<ApiResponse<PaginatedPage<RoleDefinitionResponse>>>(
      `${this.baseUrl}/roles/definitions`,
      { params }
    ).pipe(map(response => response.data));
  }

  getRoleDefinition(roleId: string): Observable<RoleDefinitionResponse> {
    return this.http.get<ApiResponse<RoleDefinitionResponse>>(`${this.baseUrl}/roles/definitions/${roleId}`).pipe(map(response => response.data));
  }

  createRoleDefinition(request: CreateRoleRequest): Observable<RoleDefinitionResponse> {
    return this.http.post<ApiResponse<RoleDefinitionResponse>>(`${this.baseUrl}/roles/definitions`, request).pipe(map(response => response.data));
  }

  updateRoleDefinition(roleId: string, request: UpdateRoleRequest): Observable<RoleDefinitionResponse> {
    return this.http.put<ApiResponse<RoleDefinitionResponse>>(`${this.baseUrl}/roles/definitions/${roleId}`, request).pipe(map(response => response.data));
  }

  deleteRoleDefinition(roleId: string): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/roles/definitions/${roleId}`).pipe(
      map(() => undefined)
    );
  }

  getPermissions(page: number, size: number, search: string, sort?: string, order?: string): Observable<PaginatedPage<PermissionResponse>> {
    const params: Record<string, string> = { page: page.toString(), size: size.toString(), search };
    if (sort) params['sort'] = sort;
    if (order) params['order'] = order;
    return this.http.get<ApiResponse<PaginatedPage<PermissionResponse>>>(
      `${this.baseUrl}/roles/permissions`,
      { params }
    ).pipe(map(response => response.data));
  }

  getAllPermissions(): Observable<PermissionResponse[]> {
    return this.http.get<ApiResponse<PaginatedPage<PermissionResponse>>>(
      `${this.baseUrl}/roles/permissions`,
      { params: { page: '0', size: '200', search: '' } }
    ).pipe(map(response => response.data?.content || []));
  }

  // ===================================================================
  // System Configurations & Settings
  // ===================================================================

  getSystemConfigs(): Observable<SystemConfigItem[]> {
    return this.http.get<SystemConfigItem[]>(this.configUrl);
  }

  getSystemConfigMap(): Observable<Record<string, string>> {
    return this.http.get<Record<string, string>>(`${this.configUrl}/map`);
  }

  updateSystemConfig(paramName: string, paramValue: string): Observable<SystemConfigItem> {
    return this.http.put<SystemConfigItem>(`${this.configUrl}/${paramName}`, { paramValue });
  }

  // ===================================================================
  // Audit Logs
  // ===================================================================

  getAuditLogs(params: {
    eventType?: string;
    principal?: string;
    status?: string;
    from?: string;
    to?: string;
    page?: number;
    size?: number;
  }): Observable<PaginatedPage<AuditEventResponse>> {
    const queryParams: Record<string, string> = {};
    if (params.eventType) queryParams['eventType'] = params.eventType;
    if (params.principal) queryParams['principal'] = params.principal;
    if (params.status) queryParams['status'] = params.status;
    if (params.from) queryParams['from'] = params.from;
    if (params.to) queryParams['to'] = params.to;
    queryParams['page'] = (params.page ?? 0).toString();
    queryParams['size'] = (params.size ?? 20).toString();

    return this.http.get<ApiResponse<PaginatedPage<AuditEventResponse>>>(this.auditUrl, { params: queryParams }).pipe(
      map(response => response.data)
    );
  }
}
