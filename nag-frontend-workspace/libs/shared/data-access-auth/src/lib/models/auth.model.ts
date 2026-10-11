export interface UserToken {
  accessToken: string;
  refreshToken?: string;
  expiresIn?: number;
  tokenType?: string;
  roles?: string[];
  userId?: string;
}

export interface AuthUser {
  userId: string;
  username: string;
  roles: string[];
  preferredLanguage?: string;
  token?: string;
  email?: string;
}

export interface TotpSetupData {
  secret?: string;
  secretKey?: string;
  otpauthUri?: string;
  qrCodeUrl?: string;
  qrCode?: string;
  qrCodeDataUrl?: string;
  issuer?: string;
  username?: string;
  backupCodes?: string[];
}

export interface TotpVerifySetupRequest {
  userId?: string;
  secret: string;
  code: string;
  backupCodes?: string[];
}

export interface MfaPolicySettings {
  adminMfaPolicy: 'DISABLED' | 'OPTIONAL' | 'ENFORCED';
  candidateMfaPolicy: 'DISABLED' | 'OPTIONAL' | 'ENFORCED';
  allowedMethods: string[];
  globalMfaEnforced: boolean;
}

export interface ValidateInviteData {
  valid?: boolean;
  invitationId?: string;
  email: string;
  fullName: string;
  roles?: string[];
  assignedRoles?: string[];
  tenantId?: string;
  expiresAt?: string;
  message?: string;
}

export interface VerificationStatusData {
  userId: string;
  emailVerified: boolean;
  mobileVerified: boolean;
  accountStatus: string;
  smsRemainingThisWeek?: number;
  remainingSmsQuota?: number;
  nextSmsAvailableAt?: string;
  maskedMobile?: string;
  fullyVerified: boolean;
  mfaEnabled?: boolean;
}

export interface TotpStatusData {
  userId: string;
  mfaEnabled: boolean;
}
