export interface SystemSettingsState {
  // Security & Zero-Trust MFA
  authMfaEnforced: boolean;
  authMfaAdminPolicy: 'DISABLED' | 'OPTIONAL' | 'ENFORCED';
  authMfaCandidatePolicy: 'DISABLED' | 'OPTIONAL' | 'ENFORCED';
  authMfaAllowedMethods: string;
  authSessionTimeoutMinutes: number;
  authMaxLoginAttempts: number;
  authLockoutDurationMinutes: number;
  authPasswordExpiryDays: number;
  authPasswordMinLength: number;
  vaultTransitKeyAutoRotate: boolean;

  // Adaptive Risk Assessment & Threat Intelligence (Risk Signal 3 — Issue #115)
  authRiskIpEnabled: boolean;
  authRiskIpCountryChangeEnabled: boolean;
  authRiskIpVpnTorEnabled: boolean;
  authRiskIpTtlDays: number;

  // Exam Delivery & Kiosk Lockdown
  deliveryTamperDetectionEnabled: boolean;
  deliveryKioskModeEnforced: boolean;
  deliveryTelemetryHeartbeatSeconds: number;
  deliveryAutosaveIntervalSeconds: number;
  deliveryMaxDisconnectGraceSeconds: number;
  deliveryRetestAuthorizationRequired: boolean;

  // DPI & National Scale Infrastructure
  dpiDigilockerVerificationEnabled: boolean;
  dpiFaceVerificationThreshold: number;
  aiLiteLlmGatewayUrl: string;
  aiIndicTrans2Workers: number;

  // Question Bank & Evaluation
  questionDualReviewRequired: boolean;
  questionAiGenerationEnabled: boolean;
  evaluationAutoGradeInstant: boolean;
  evaluationAnonymizeCandidateSheets: boolean;

  // Maintenance & Alerts
  alertCriticalErrorWebhook: string;
  alertEmailRecipients: string;
  platformMaintenanceMode: boolean;
  platformBannerMessage: string;
}

export const DEFAULT_SYSTEM_SETTINGS: SystemSettingsState = {
  authMfaEnforced: false,
  authMfaAdminPolicy: 'OPTIONAL',
  authMfaCandidatePolicy: 'OPTIONAL',
  authMfaAllowedMethods: 'TOTP,EMAIL_OTP,RECOVERY_CODES',
  authSessionTimeoutMinutes: 30,
  authMaxLoginAttempts: 5,
  authLockoutDurationMinutes: 15,
  authPasswordExpiryDays: 90,
  authPasswordMinLength: 12,
  vaultTransitKeyAutoRotate: true,

  authRiskIpEnabled: true,
  authRiskIpCountryChangeEnabled: true,
  authRiskIpVpnTorEnabled: true,
  authRiskIpTtlDays: 30,

  deliveryTamperDetectionEnabled: true,
  deliveryKioskModeEnforced: true,
  deliveryTelemetryHeartbeatSeconds: 10,
  deliveryAutosaveIntervalSeconds: 15,
  deliveryMaxDisconnectGraceSeconds: 180,
  deliveryRetestAuthorizationRequired: true,

  dpiDigilockerVerificationEnabled: true,
  dpiFaceVerificationThreshold: 85,
  aiLiteLlmGatewayUrl: 'http://nag-ai-gateway:8000',
  aiIndicTrans2Workers: 4,

  questionDualReviewRequired: true,
  questionAiGenerationEnabled: true,
  evaluationAutoGradeInstant: true,
  evaluationAnonymizeCandidateSheets: true,

  alertCriticalErrorWebhook: 'https://ops.nag.gov.in/webhooks/security-alerts',
  alertEmailRecipients: 'sec-ops@nag.gov.in, admin@nag.gov.in',
  platformMaintenanceMode: false,
  platformBannerMessage: '',
};
