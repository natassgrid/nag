/**
 * Authentication and registration flow domain models for the candidate delivery app.
 */

export interface CandidateRegistrationForm {
  fullName: string;
  email: string;
  mobile: string;
  identityDocType?: 'AADHAAR' | 'PAN' | 'PASSPORT' | 'VOTER_ID' | 'DL' | string;
  identityDocNumber?: string;
  password: string;
  confirmPassword: string;
}

export interface CandidateRegistrationPayload {
  fullName: string;
  email: string;
  mobile: string;
  identityDocType?: string;
  identityDocNumber?: string;
  preferredLanguage?: string;
  password: string;
}

export interface OtpQueryParams {
  userId?: string;
  email?: string;
  mobile?: string;
  otp?: string;
  pending?: string;
}

export interface AuthBrandHeaderConfig {
  title: string;
  subtitle: string;
  description: string;
}
