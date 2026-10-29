import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import {
  AdminUserAccount,
  AdminCreateUserPayload,
  AdminUpdateUserPayload,
  RoleDefinition,
  CreateRolePayload,
  UpdateRolePayload,
  PermissionDefinition,
  AdminInvitationItem,
  AdminInvitePayload,
} from '../models/user-management.model';
import {
  DEFAULT_SYSTEM_ROLES,
  formatRoleDisplayName,
} from '../models/system-roles.defaults';

function mapPermissionCategory(moduleStr: string): PermissionDefinition['category'] {
  const mod = (moduleStr || '').toUpperCase();
  if (mod.includes('QUESTION')) return 'QUESTIONS';
  if (mod.includes('EXAM')) return 'EXAMINATIONS';
  if (mod.includes('DELIVERY')) return 'DELIVERY';
  if (mod.includes('EVALUAT')) return 'EVALUATION';
  if (mod.includes('AUDIT')) return 'AUDIT';
  if (mod.includes('SECURITY')) return 'SECURITY';
  return 'IDENTITY';
}

function mapBackendRole(r: any): RoleDefinition {
  const code = r.code || r.name || '';
  const rawName = r.name || r.code || '';
  const displayName = rawName && rawName !== code ? rawName : formatRoleDisplayName(code);
  return {
    id: r.id ? String(r.id) : `role-${code.toLowerCase()}`,
    name: code,
    displayName: displayName || code,
    description: r.description || '',
    systemRole: Boolean(r.systemRole || r.isSystemRole),
    permissions: Array.isArray(r.permissions)
      ? r.permissions.map((p: any) => (typeof p === 'string' ? p : p.code || p.name))
      : [],
    userCount: r.userCount ?? 0,
    createdAt: r.createdAt ? String(r.createdAt) : '',
    updatedAt: r.updatedAt ? String(r.updatedAt) : '',
  };
}

function mapBackendPermission(p: any): PermissionDefinition {
  return {
    id: p.id ? String(p.id) : '',
    name: p.code || p.name,
    displayName: p.name || p.code,
    category: mapPermissionCategory(p.module || p.category || ''),
    description: p.description || '',
  };
}

@Injectable({
  providedIn: 'root',
})
export class UserRoleService {
  private readonly http = inject(HttpClient);

  // ─── Users API ────────────────────────────────────────────────────────────

  getUsers(): Observable<AdminUserAccount[]> {
    return this.http.get<{ data: any[] } | any[]>('/api/v1/identity/users').pipe(
      map((res) => {
        const list = (res as any)?.data?.content ?? (res as any)?.data ?? res;
        if (Array.isArray(list)) {
          return list.map((u: any): AdminUserAccount => {
            const roleList = Array.isArray(u.roles)
              ? u.roles.map((r: any) => (typeof r === 'string' ? r : r.code || r.name))
              : Array.isArray(u.assignedRoles)
              ? u.assignedRoles
              : [];
            return {
              id: u.id ? String(u.id) : '',
              username: u.username || u.email,
              email: u.email,
              fullName: u.fullName || `${u.firstName || ''} ${u.lastName || ''}`.trim() || u.username,
              phoneNumber: u.phoneNumber,
              roles: roleList,
              status: u.status || (u.active === false ? 'DEACTIVATED' : 'ACTIVE'),
              twoFactorEnabled: Boolean(u.twoFactorEnabled ?? u.mfaEnabled),
              twoFactorMethod: u.twoFactorMethod || 'TOTP',
              lastLoginAt: u.lastLoginAt,
              createdAt: u.createdAt || '',
              updatedAt: u.updatedAt,
              tenantId: u.tenantId,
            };
          });
        }
        return [];
      }),
      catchError(() => of([]))
    );
  }

  createUser(payload: AdminCreateUserPayload): Observable<AdminUserAccount> {
    return this.http.post<{ data: any } | any>('/api/v1/identity/users', payload).pipe(
      map((res) => {
        const u = (res as any)?.data ?? res;
        const roleList = Array.isArray(u.roles)
          ? u.roles.map((r: any) => (typeof r === 'string' ? r : r.code || r.name))
          : Array.isArray(payload.roles)
          ? payload.roles
          : [];
        return {
          id: u.id ? String(u.id) : '',
          username: u.username || payload.email,
          email: u.email || payload.email,
          fullName: u.fullName || payload.fullName,
          phoneNumber: u.phoneNumber || payload.phoneNumber,
          roles: roleList,
          status: u.status || 'ACTIVE',
          twoFactorEnabled: Boolean(u.twoFactorEnabled ?? u.mfaEnabled),
          twoFactorMethod: u.twoFactorMethod || 'TOTP',
          lastLoginAt: u.lastLoginAt,
          createdAt: u.createdAt || new Date().toISOString(),
          updatedAt: u.updatedAt,
          tenantId: u.tenantId,
        };
      })
    );
  }

  updateUser(userId: string, payload: AdminUpdateUserPayload): Observable<AdminUserAccount> {
    return this.http.put<{ data: any } | any>(`/api/v1/identity/users/${userId}`, payload).pipe(
      map((res) => {
        const u = (res as any)?.data ?? res;
        const roleList = Array.isArray(u.roles)
          ? u.roles.map((r: any) => (typeof r === 'string' ? r : r.code || r.name))
          : Array.isArray(payload.roles)
          ? payload.roles
          : [];
        return {
          id: u.id ? String(u.id) : userId,
          username: u.username || '',
          email: u.email || '',
          fullName: u.fullName || payload.fullName || '',
          phoneNumber: u.phoneNumber || payload.phoneNumber,
          roles: roleList,
          status: (u.status || payload.status || 'ACTIVE'),
          twoFactorEnabled: Boolean(u.twoFactorEnabled ?? u.mfaEnabled ?? payload.twoFactorEnabled ?? false),
          twoFactorMethod: u.twoFactorMethod || 'TOTP',
          lastLoginAt: u.lastLoginAt,
          createdAt: u.createdAt || '',
          updatedAt: u.updatedAt,
          tenantId: u.tenantId,
        };
      })
    );
  }

  toggleUserStatus(userId: string, currentStatus = 'ACTIVE'): Observable<AdminUserAccount> {
    const newStatus = currentStatus === 'ACTIVE' ? 'DEACTIVATED' : 'ACTIVE';
    return this.updateUser(userId, { status: newStatus as any });
  }

  // ─── Roles API (Direct from backend /api/v1/identity/roles/definitions) ────

  getRoles(): Observable<RoleDefinition[]> {
    return this.http.get<{ data: any } | any>('/api/v1/identity/roles/definitions?page=0&size=100').pipe(
      map((res) => {
        const list = (res as any)?.data?.content ?? (res as any)?.data ?? res;
        if (Array.isArray(list) && list.length > 0) {
          return list.map(mapBackendRole);
        }
        return DEFAULT_SYSTEM_ROLES;
      }),
      catchError(() => of(DEFAULT_SYSTEM_ROLES))
    );
  }

  createRole(payload: CreateRolePayload): Observable<RoleDefinition> {
    const body = {
      name: payload.displayName || payload.name,
      code: payload.name.toUpperCase().replace(/\s+/g, '_'),
      description: payload.description,
    };
    return this.http.post<{ data: any } | any>('/api/v1/identity/roles/definitions', body).pipe(
      map((res) => {
        const r = (res as any)?.data ?? res;
        return mapBackendRole(r);
      })
    );
  }

  updateRole(roleId: string, payload: UpdateRolePayload): Observable<RoleDefinition> {
    const body: any = {};
    if (payload.displayName) body.name = payload.displayName;
    if (payload.description) body.description = payload.description;

    return this.http.put<{ data: any } | any>(`/api/v1/identity/roles/definitions/${roleId}`, body).pipe(
      map((res) => {
        const r = (res as any)?.data ?? res;
        return mapBackendRole(r);
      })
    );
  }

  deleteRole(roleId: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/identity/roles/definitions/${roleId}`);
  }

  // ─── Permissions API ───────────────────────────────────────────────────────

  getPermissions(): Observable<PermissionDefinition[]> {
    return this.http.get<{ data: any } | any>('/api/v1/identity/roles/permissions').pipe(
      map((res) => {
        const list = (res as any)?.data ?? res;
        if (Array.isArray(list)) {
          return list.map(mapBackendPermission);
        }
        return [];
      }),
      catchError(() => of([]))
    );
  }

  // ─── User Role Assignments ─────────────────────────────────────────────────

  assignRole(userId: string, roleCode: string): Observable<void> {
    return this.http.post<void>(`/api/v1/identity/roles/users/${userId}/assign`, { roleCode });
  }

  revokeRole(userId: string, roleCode: string): Observable<void> {
    return this.http.post<void>(`/api/v1/identity/roles/users/${userId}/revoke`, { roleCode });
  }

  // ─── Invitations API ───────────────────────────────────────────────────────

  getInvitations(): Observable<AdminInvitationItem[]> {
    return this.http.get<{ data: any[] } | any[]>('/api/v1/identity/invitations').pipe(
      map((res) => {
        const list = (res as any)?.data?.content ?? (res as any)?.data ?? res;
        if (Array.isArray(list)) {
          return list.map((i: any): AdminInvitationItem => ({
            id: i.id ? String(i.id) : '',
            email: i.email,
            fullName: i.fullName || i.name || i.email,
            assignedRoles: Array.isArray(i.roles)
              ? i.roles
              : Array.isArray(i.assignedRoles)
              ? i.assignedRoles
              : [],
            status: i.status || 'PENDING',
            expiresAt: i.expiresAt || '',
            createdAt: i.createdAt || '',
            invitationToken: i.invitationToken || i.token || '',
          }));
        }
        return [];
      }),
      catchError(() => of([]))
    );
  }

  sendInvitation(payload: AdminInvitePayload): Observable<AdminInvitationItem> {
    return this.http.post<{ data: any } | any>('/api/v1/identity/invitations', payload).pipe(
      map((res) => {
        const i = (res as any)?.data ?? res;
        return {
          id: i.id ? String(i.id) : '',
          email: i.email || payload.email,
          fullName: i.fullName || payload.fullName,
          assignedRoles: i.assignedRoles || payload.assignedRoles || [],
          status: i.status || 'PENDING',
          expiresAt: i.expiresAt || '',
          createdAt: i.createdAt || new Date().toISOString(),
          invitationToken: i.invitationToken || '',
        };
      })
    );
  }

  resendInvitation(invitationId: string): Observable<void> {
    return this.http.post<void>(`/api/v1/identity/invitations/${invitationId}/resend`, {});
  }

  revokeInvitation(invitationId: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/identity/invitations/${invitationId}`);
  }

  resetUserMfa(userId: string): Observable<void> {
    return this.http.post<void>(`/api/v1/identity/users/${userId}/mfa/reset`, {});
  }
}
