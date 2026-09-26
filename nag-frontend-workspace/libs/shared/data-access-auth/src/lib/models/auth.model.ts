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
}

export interface TotpSetupData {
  secretKey: string;
  qrCodeUrl: string;
}

export interface ValidateInviteData {
  invitationId: string;
  email: string;
  fullName: string;
  assignedRoles: string[];
}
