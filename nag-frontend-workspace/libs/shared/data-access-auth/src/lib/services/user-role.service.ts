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
  UserManagementKpiStats,
} from '../models/user-management.model';

@Injectable({
  providedIn: 'root',
})
export class UserRoleService {
  private readonly http = inject(HttpClient);

  // Initial Mock Data for seamless offline/standalone admin experience
  private mockUsers: AdminUserAccount[] = [
    {
      id: 'u-101',
      username: 'amitabh.verma',
      email: 'exam_controller@nag.gov.in',
      fullName: 'Dr. Amitabh Verma',
      phoneNumber: '+91 98765 43210',
      roles: ['EXAM_CONTROLLER'],
      status: 'ACTIVE',
      twoFactorEnabled: true,
      twoFactorMethod: 'TOTP',
      lastLoginAt: '2026-09-26 07:45:10',
      createdAt: '2026-01-10 09:00:00',
    },
    {
      id: 'u-102',
      username: 'sunita.sharma',
      email: 'author_sharma@nag.gov.in',
      fullName: 'Prof. Sunita Sharma',
      phoneNumber: '+91 98111 22334',
      roles: ['QUESTION_AUTHOR'],
      status: 'ACTIVE',
      twoFactorEnabled: true,
      twoFactorMethod: 'TOTP',
      lastLoginAt: '2026-09-26 06:30:22',
      createdAt: '2026-02-14 11:20:00',
    },
    {
      id: 'u-103',
      username: 'ks.reddy',
      email: 'evaluator_reddy@nag.gov.in',
      fullName: 'Dr. K. S. Reddy',
      phoneNumber: '+91 94440 12345',
      roles: ['EVALUATOR'],
      status: 'ACTIVE',
      twoFactorEnabled: true,
      twoFactorMethod: 'SMS',
      lastLoginAt: '2026-09-25 18:15:00',
      createdAt: '2026-03-01 14:00:00',
    },
    {
      id: 'u-104',
      username: 'priya.sundaram',
      email: 'auditor_sundaram@nag.gov.in',
      fullName: 'Priya Sundaram',
      phoneNumber: '+91 97777 88899',
      roles: ['AUDITOR'],
      status: 'ACTIVE',
      twoFactorEnabled: true,
      twoFactorMethod: 'WEBAUTHN',
      lastLoginAt: '2026-09-24 10:11:45',
      createdAt: '2026-04-12 08:30:00',
    },
    {
      id: 'u-105',
      username: 'admin.root',
      email: 'admin_root@nag.gov.in',
      fullName: 'Chief Information Security Officer',
      phoneNumber: '+91 99999 00000',
      roles: ['SUPER_ADMIN', 'SECURITY_ADMIN'],
      status: 'ACTIVE',
      twoFactorEnabled: true,
      twoFactorMethod: 'WEBAUTHN',
      lastLoginAt: '2026-09-26 08:10:00',
      createdAt: '2026-01-01 00:00:00',
    },
  ];

  private mockRoles: RoleDefinition[] = [
    {
      id: 'r-1',
      name: 'SUPER_ADMIN',
      displayName: 'Super Administrator',
      description: 'Full unconstrained cryptographic authority over all clusters, nodes, and keys.',
      systemRole: true,
      permissions: ['ALL_PERMISSIONS'],
      userCount: 1,
      createdAt: '2026-01-01 00:00:00',
    },
    {
      id: 'r-2',
      name: 'EXAM_CONTROLLER',
      displayName: 'Examination Controller',
      description: 'Authority to schedule exams, approve blueprint matrices, and mint encrypted papers.',
      systemRole: true,
      permissions: [
        'EXAM_CREATE', 'EXAM_UPDATE', 'EXAM_SCHEDULE',
        'PAPER_ASSEMBLE', 'PAPER_APPROVE', 'PAPER_ENCRYPT', 'CENTRE_MANAGE'
      ],
      userCount: 4,
      createdAt: '2026-01-01 00:00:00',
    },
    {
      id: 'r-3',
      name: 'QUESTION_AUTHOR',
      displayName: 'Subject Matter Question Author',
      description: 'Create and submit taxonomy-tagged questions, LaTeX equations, and AI-prompted drafts.',
      systemRole: true,
      permissions: [
        'QUESTION_CREATE', 'QUESTION_EDIT_DRAFT', 'QUESTION_SUBMIT_REVIEW',
        'TAXONOMY_VIEW', 'AI_GENERATE_ASSESSMENT'
      ],
      userCount: 18,
      createdAt: '2026-01-01 00:00:00',
    },
    {
      id: 'r-4',
      name: 'EVALUATOR',
      displayName: 'Double-Blind Grading Evaluator',
      description: 'Perform confidential objective and subjective evaluation of candidate answer sheets.',
      systemRole: true,
      permissions: ['GRADING_EXECUTE', 'BLIND_REVIEW_VIEW', 'SCORE_SUBMIT'],
      userCount: 35,
      createdAt: '2026-01-01 00:00:00',
    },
    {
      id: 'r-5',
      name: 'AUDITOR',
      displayName: 'Compliance & Cryptographic Auditor',
      description: 'Inspect Merkle logs, immutable audit trails, and biometric tamper verification records.',
      systemRole: true,
      permissions: ['AUDIT_VIEW_LOGS', 'MERKLE_PROOF_VERIFY', 'REPORTS_VIEW'],
      userCount: 6,
      createdAt: '2026-01-01 00:00:00',
    },
  ];

  private mockPermissions: PermissionDefinition[] = [
    { id: 'p-1', name: 'QUESTION_CREATE', displayName: 'Create Questions', category: 'QUESTIONS', description: 'Author new assessment items and taxonomy items' },
    { id: 'p-2', name: 'QUESTION_EDIT_DRAFT', displayName: 'Edit Draft Questions', category: 'QUESTIONS', description: 'Modify draft stage questions and formulas' },
    { id: 'p-3', name: 'QUESTION_SUBMIT_REVIEW', displayName: 'Submit Question for Review', category: 'QUESTIONS', description: 'Push draft questions to reviewer pool' },
    { id: 'p-4', name: 'QUESTION_APPROVE', displayName: 'Approve & Bank Questions', category: 'QUESTIONS', description: 'Clear questions into sealed repository' },
    { id: 'p-5', name: 'AI_GENERATE_ASSESSMENT', displayName: 'AI Synthetic Question Gen', category: 'QUESTIONS', description: 'Invoke neural LiteLLM pipeline for question synthesis' },

    { id: 'p-6', name: 'EXAM_CREATE', displayName: 'Create Examinations', category: 'EXAMINATIONS', description: 'Define new examination programs and curricula' },
    { id: 'p-7', name: 'EXAM_SCHEDULE', displayName: 'Schedule Exam Shifts', category: 'EXAMINATIONS', description: 'Allocate dates, shifts, and regional windows' },
    { id: 'p-8', name: 'CENTRE_MANAGE', displayName: 'Manage Test Venues', category: 'EXAMINATIONS', description: 'Configure test centres, halls, and node capacity' },
    { id: 'p-9', name: 'PAPER_ASSEMBLE', displayName: 'Assemble Blueprint Paper', category: 'EXAMINATIONS', description: 'Run CSPRNG algorithmic paper assembly engine' },
    { id: 'p-10', name: 'PAPER_APPROVE', displayName: 'Approve & Seal Paper', category: 'EXAMINATIONS', description: 'Apply Controller cryptographic signature' },
    { id: 'p-11', name: 'PAPER_ENCRYPT', displayName: 'Encrypt Exam Package', category: 'EXAMINATIONS', description: 'Package AES-GCM-256 payload and key escrow' },

    { id: 'p-12', name: 'DELIVERY_MONITOR', displayName: 'Live Session Monitor', category: 'DELIVERY', description: 'Track real-time candidate delivery heartbeats' },
    { id: 'p-13', name: 'DELIVERY_KEY_DISTRIBUTE', displayName: 'Authorize Session Unlock', category: 'DELIVERY', description: 'Transmit OTP unlock token to exam nodes' },

    { id: 'p-14', name: 'GRADING_EXECUTE', displayName: 'Perform Grading', category: 'EVALUATION', description: 'Grade anonymized candidate submissions' },
    { id: 'p-15', name: 'SCORE_SUBMIT', displayName: 'Submit Candidate Marks', category: 'EVALUATION', description: 'Finalize double-blind grading score sheets' },

    { id: 'p-16', name: 'IDENTITY_USER_MANAGE', displayName: 'User & Authority Admin', category: 'IDENTITY', description: 'Invite, modify, and revoke authority accounts' },
    { id: 'p-17', name: 'IDENTITY_ROLE_MANAGE', displayName: 'RBAC Role Management', category: 'IDENTITY', description: 'Configure custom roles and permission maps' },

    { id: 'p-18', name: 'AUDIT_VIEW_LOGS', displayName: 'View Immutable Audit Trail', category: 'AUDIT', description: 'Access tamper-evident cryptographic log streams' },
    { id: 'p-19', name: 'MERKLE_PROOF_VERIFY', displayName: 'Verify Merkle Proofs', category: 'AUDIT', description: 'Validate batch block root signatures' },
  ];

  private mockInvitations: AdminInvitationItem[] = [
    {
      id: 'inv-901',
      email: 'dr.sharma.physics@nag.edu.in',
      fullName: 'Dr. Ramesh Sharma',
      assignedRoles: ['QUESTION_AUTHOR'],
      status: 'PENDING',
      expiresAt: '2026-09-30 23:59:59',
      createdAt: '2026-09-25 10:00:00',
      invitationToken: 'inv_tok_98234710293847',
    },
    {
      id: 'inv-902',
      email: 'controller.delhi@nag.gov.in',
      fullName: 'Anita Deshmukh',
      assignedRoles: ['EXAM_CONTROLLER'],
      status: 'PENDING',
      expiresAt: '2026-10-02 23:59:59',
      createdAt: '2026-09-26 05:30:00',
      invitationToken: 'inv_tok_77218391209384',
    },
  ];

  // Users API
  getUsers(): Observable<AdminUserAccount[]> {
    return this.http.get<{ data: AdminUserAccount[] } | AdminUserAccount[]>('/api/v1/identity/users').pipe(
      map((res) => {
        const list = (res as any)?.data ?? res;
        return Array.isArray(list) && list.length > 0 ? list : this.mockUsers;
      }),
      catchError(() => of([...this.mockUsers]))
    );
  }

  createUser(payload: AdminCreateUserPayload): Observable<AdminUserAccount> {
    return this.http.post<{ data: AdminUserAccount } | AdminUserAccount>('/api/v1/identity/users', payload).pipe(
      map((res) => ((res as any)?.data ?? res) as AdminUserAccount),
      catchError(() => {
        const newUser: AdminUserAccount = {
          id: `u-${Date.now()}`,
          username: payload.email.split('@')[0],
          email: payload.email,
          fullName: payload.fullName,
          phoneNumber: payload.phoneNumber,
          roles: payload.roles,
          status: 'ACTIVE',
          twoFactorEnabled: true,
          twoFactorMethod: 'TOTP',
          createdAt: new Date().toISOString(),
        };
        this.mockUsers.unshift(newUser);
        return of(newUser);
      })
    );
  }

  updateUser(userId: string, payload: AdminUpdateUserPayload): Observable<AdminUserAccount> {
    return this.http.put<{ data: AdminUserAccount } | AdminUserAccount>(`/api/v1/identity/users/${userId}`, payload).pipe(
      map((res) => ((res as any)?.data ?? res) as AdminUserAccount),
      catchError(() => {
        const index = this.mockUsers.findIndex((u) => u.id === userId);
        if (index !== -1) {
          this.mockUsers[index] = { ...this.mockUsers[index], ...payload };
          return of(this.mockUsers[index]);
        }
        throw new Error('User not found');
      })
    );
  }

  toggleUserStatus(userId: string): Observable<AdminUserAccount> {
    const target = this.mockUsers.find((u) => u.id === userId);
    if (!target) return of(this.mockUsers[0]);
    const newStatus = target.status === 'ACTIVE' ? 'REVOKED' : 'ACTIVE';
    return this.updateUser(userId, { status: newStatus });
  }

  // Roles API
  getRoles(): Observable<RoleDefinition[]> {
    return this.http.get<{ data: RoleDefinition[] } | RoleDefinition[]>('/api/v1/identity/roles/definitions').pipe(
      map((res) => {
        const list = (res as any)?.data?.content ?? (res as any)?.data ?? res;
        return Array.isArray(list) && list.length > 0 ? list : this.mockRoles;
      }),
      catchError(() => of([...this.mockRoles]))
    );
  }

  createRole(payload: CreateRolePayload): Observable<RoleDefinition> {
    return this.http.post<{ data: RoleDefinition } | RoleDefinition>('/api/v1/identity/roles/definitions', payload).pipe(
      map((res) => ((res as any)?.data ?? res) as RoleDefinition),
      catchError(() => {
        const newRole: RoleDefinition = {
          id: `r-${Date.now()}`,
          name: payload.name.toUpperCase().replace(/\s+/g, '_'),
          displayName: payload.displayName,
          description: payload.description,
          systemRole: false,
          permissions: payload.permissions,
          userCount: 0,
          createdAt: new Date().toISOString(),
        };
        this.mockRoles.push(newRole);
        return of(newRole);
      })
    );
  }

  updateRole(roleId: string, payload: UpdateRolePayload): Observable<RoleDefinition> {
    return this.http.put<{ data: RoleDefinition } | RoleDefinition>(`/api/v1/identity/roles/definitions/${roleId}`, payload).pipe(
      map((res) => ((res as any)?.data ?? res) as RoleDefinition),
      catchError(() => {
        const idx = this.mockRoles.findIndex((r) => r.id === roleId);
        if (idx !== -1) {
          this.mockRoles[idx] = { ...this.mockRoles[idx], ...payload };
          return of(this.mockRoles[idx]);
        }
        throw new Error('Role not found');
      })
    );
  }

  deleteRole(roleId: string): Observable<boolean> {
    return this.http.delete<void>(`/api/v1/identity/roles/definitions/${roleId}`).pipe(
      map(() => true),
      catchError(() => {
        this.mockRoles = this.mockRoles.filter((r) => r.id !== roleId);
        return of(true);
      })
    );
  }

  // Permissions API
  getPermissions(): Observable<PermissionDefinition[]> {
    return this.http.get<{ data: PermissionDefinition[] } | PermissionDefinition[]>('/api/v1/identity/roles/permissions').pipe(
      map((res) => {
        const list = (res as any)?.data ?? res;
        return Array.isArray(list) && list.length > 0 ? list : this.mockPermissions;
      }),
      catchError(() => of([...this.mockPermissions]))
    );
  }

  // Invitations API
  getInvitations(): Observable<AdminInvitationItem[]> {
    return this.http.get<{ data: AdminInvitationItem[] } | AdminInvitationItem[]>('/api/v1/identity/invitations').pipe(
      map((res) => {
        const list = (res as any)?.data ?? res;
        return Array.isArray(list) && list.length > 0 ? list : this.mockInvitations;
      }),
      catchError(() => of([...this.mockInvitations]))
    );
  }

  sendInvitation(payload: AdminInvitePayload): Observable<AdminInvitationItem> {
    return this.http.post<{ data: AdminInvitationItem } | AdminInvitationItem>('/api/v1/identity/invitations', payload).pipe(
      map((res) => ((res as any)?.data ?? res) as AdminInvitationItem),
      catchError(() => {
        const expiry = new Date();
        expiry.setDate(expiry.getDate() + (payload.expiryDays || 5));
        const newInv: AdminInvitationItem = {
          id: `inv-${Date.now()}`,
          email: payload.email,
          fullName: payload.fullName,
          assignedRoles: payload.assignedRoles,
          status: 'PENDING',
          expiresAt: expiry.toISOString(),
          createdAt: new Date().toISOString(),
          invitationToken: `inv_tok_${Math.random().toString(36).substring(2, 15)}`,
        };
        this.mockInvitations.unshift(newInv);
        return of(newInv);
      })
    );
  }

  revokeInvitation(invitationId: string): Observable<boolean> {
    return this.http.delete<void>(`/api/v1/identity/invitations/${invitationId}`).pipe(
      map(() => true),
      catchError(() => {
        const target = this.mockInvitations.find((i) => i.id === invitationId);
        if (target) target.status = 'REVOKED';
        return of(true);
      })
    );
  }
}
