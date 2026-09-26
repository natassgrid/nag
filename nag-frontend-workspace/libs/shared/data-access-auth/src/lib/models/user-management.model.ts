export type UserAccountStatus = 'ACTIVE' | 'REVOKED' | 'PENDING_VERIFICATION' | 'LOCKED';

export interface AdminUserAccount {
  id: string;
  username: string;
  email: string;
  fullName: string;
  phoneNumber?: string;
  roles: string[];
  status: UserAccountStatus;
  twoFactorEnabled: boolean;
  twoFactorMethod?: 'TOTP' | 'SMS' | 'WEBAUTHN';
  lastLoginAt?: string;
  createdAt: string;
  updatedAt?: string;
  tenantId?: string;
}

export interface AdminCreateUserPayload {
  email: string;
  fullName: string;
  phoneNumber?: string;
  roles: string[];
  password?: string;
  temporaryPassword?: boolean;
}

export interface AdminUpdateUserPayload {
  fullName?: string;
  phoneNumber?: string;
  roles?: string[];
  status?: UserAccountStatus;
  twoFactorEnabled?: boolean;
}

export interface PermissionDefinition {
  id: string;
  name: string;
  displayName: string;
  category: 'QUESTIONS' | 'EXAMINATIONS' | 'DELIVERY' | 'EVALUATION' | 'IDENTITY' | 'AUDIT' | 'SECURITY';
  description: string;
}

export interface RoleDefinition {
  id: string;
  name: string;
  displayName: string;
  description: string;
  systemRole: boolean;
  permissions: string[];
  userCount?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateRolePayload {
  name: string;
  displayName: string;
  description: string;
  permissions: string[];
}

export interface UpdateRolePayload {
  displayName?: string;
  description?: string;
  permissions?: string[];
}

export type InvitationStatus = 'PENDING' | 'ACCEPTED' | 'EXPIRED' | 'REVOKED';

export interface AdminInvitationItem {
  id: string;
  email: string;
  fullName: string;
  assignedRoles: string[];
  status: InvitationStatus;
  expiresAt: string;
  createdAt: string;
  invitationToken?: string;
}

export interface AdminInvitePayload {
  email: string;
  fullName: string;
  assignedRoles: string[];
  expiryDays?: number;
}

export interface UserManagementKpiStats {
  totalUsers: number;
  activeUsers: number;
  mfaEnforcedPercent: number;
  totalRoles: number;
  pendingInvitations: number;
}
