import { UserAccountStatus } from '@nag-frontend-workspace/shared-data-access-auth';

export interface AdminUserProfile {
  id: string;
  username: string;
  email: string;
  fullName: string;
  phoneNumber?: string;
  specialization?: string;
  department?: string;
  designation?: string;
  avatarUrl?: string;
  timezone?: string;
  dateFormat?: string;
  timeFormat?: string;
  preferredLanguage?: string;
  themePreference?: string;
  employeeId?: string;
  roles: string[];
  status: UserAccountStatus;
  twoFactorEnabled: boolean;
  twoFactorMethod?: 'TOTP' | 'SMS' | 'WEBAUTHN';
  lastLoginAt?: string;
  createdAt: string;
  updatedAt?: string;
  tenantId?: string;
}

export interface UpdateProfilePayload {
  fullName?: string;
  phoneNumber?: string;
  specialization?: string;
  department?: string;
  designation?: string;
  avatarUrl?: string;
  timezone?: string;
  dateFormat?: string;
  timeFormat?: string;
  preferredLanguage?: string;
  themePreference?: string;
}

export interface ChangePasswordPayload {
  currentPassword: string;
  newPassword: string;
  confirmPassword?: string;
}

export interface TotpSetupResult {
  secret?: string;
  secretKey?: string;
  otpauthUri: string;
  qrCodeUrl?: string;
  backupCodes?: string[];
  issuer?: string;
  username?: string;
}

export interface ActiveSessionInfo {
  id: string;
  ipAddress: string;
  device: string;
  browser: string;
  os: string;
  lastActive: string;
  isCurrent: boolean;
  expiresAt?: string;
}

export interface PermissionItem {
  code: string;
  name: string;
  category: 'QUESTIONS' | 'EXAMINATIONS' | 'DELIVERY' | 'EVALUATION' | 'IDENTITY' | 'AUDIT' | 'SECURITY';
  description: string;
  granted: boolean;
}

export interface RoleDetail {
  code: string;
  name: string;
  badgeTone: 'purple' | 'indigo' | 'blue' | 'amber' | 'emerald' | 'slate';
  description: string;
  systemRole: boolean;
}

export interface PersonalAccessToken {
  id: string;
  name: string;
  tokenPrefix: string;
  scopes: string[];
  ipWhitelist?: string;
  expiresAt?: string;
  lastUsedAt?: string;
  revoked: boolean;
  createdAt: string;
  updatedAt?: string;
}

export interface CreateTokenPayload {
  name: string;
  scopes: string[];
  expiresInDays?: number;
  ipWhitelist?: string;
}

export interface CreatedTokenResult {
  token: PersonalAccessToken;
  rawSecret: string;
}

export interface AdminActivityLog {
  id: string;
  timestamp: string;
  action: string;
  category: string;
  details: string;
  ipAddress: string;
  status: string;
  resourceId?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
