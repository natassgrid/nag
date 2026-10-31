/**
 * Docker-Based API Configuration & Canonical Endpoint Paths for NAG Candidate Mobile
 *
 * Aligns 1:1 with:
 * - `infrastructure/docker-compose/docker-compose.monolith.yml` (`monolith-app` / `api-gateway` on :9000)
 * - `nag-frontend-workspace/nginx.conf` (`http://api-gateway:9000` & `:4300` proxy)
 * - `nag-frontend-workspace/apps/candidate-delivery/proxy.conf.json` (`http://localhost:9000`)
 */

export type DockerTargetMode =
  | 'localhost-gateway'
  | 'android-emulator'
  | 'localhost-nginx'
  | 'android-emulator-nginx'
  | 'docker-internal';

export interface DockerDevEndpointOrigins {
  /** Host machine direct access to `exam-monolith-app` / `api-gateway` container (:9000) */
  localhostGatewayUrl: string;
  /** Android Emulator loopback (10.0.2.2) to host Docker `api-gateway` / `monolith-app` (:9000) */
  androidEmulatorGatewayUrl: string;
  /** Host machine access to `exam-candidate-delivery` Nginx container proxy (:4300) */
  localhostNginxProxyUrl: string;
  /** Android Emulator loopback (10.0.2.2) to `exam-candidate-delivery` Nginx proxy (:4300) */
  androidEmulatorNginxProxyUrl: string;
  /** Internal Docker bridge network (`exam-network`) service alias (`http://api-gateway:9000`) */
  dockerInternalGatewayUrl: string;
  /** Docker mock server for DigiLocker, Aadhaar e-KYC, and MSG91 OTP (:8099) */
  mockServerUrl: string;
  /** Docker local Tesseract OCR service (:5005) */
  ocrServiceUrl: string;
}

export const DOCKER_DEV_ORIGINS: DockerDevEndpointOrigins = {
  localhostGatewayUrl: 'http://localhost:9000',
  androidEmulatorGatewayUrl: 'http://10.0.2.2:9000',
  localhostNginxProxyUrl: 'http://localhost:4300',
  androidEmulatorNginxProxyUrl: 'http://10.0.2.2:4300',
  dockerInternalGatewayUrl: 'http://api-gateway:9000',
  mockServerUrl: 'http://localhost:8099',
  ocrServiceUrl: 'http://localhost:5005',
};

/**
 * Canonical Docker-routed API paths matching `candidate-delivery` & `api-gateway:9000`.
 */
export const DOCKER_API_PATHS = {
  HEALTH: {
    GATEWAY_ACTUATOR: '/actuator/health',
    NGINX_HEALTH: '/health',
    OCR_HEALTH: '/health',
  },
  IDENTITY: {
    BASE: '/api/v1/identity',
    REGISTER: '/api/v1/identity/register',
    AUTH_TOKEN: '/api/v1/identity/auth/token',
    AUTH_TOKEN_REFRESH: '/api/v1/identity/auth/token/refresh',
    LEGACY_REFRESH: '/api/v1/identity/refresh',
    AUTH_LOGOUT: '/api/v1/identity/auth/logout',
    CHANGE_PASSWORD: '/api/v1/identity/auth/change-password',
    OTP_SEND: '/api/v1/identity/otp/send',
    OTP_VERIFY: '/api/v1/identity/otp/verify',
    TOTP_VERIFY: '/api/v1/identity/totp/verify',
    VERIFY_EMAIL: '/api/v1/identity/verify/email',
    VERIFY_MOBILE: '/api/v1/identity/verify/mobile',
    RESEND_EMAIL_OTP: '/api/v1/identity/resend/email-otp',
    RESEND_SMS_OTP: '/api/v1/identity/resend/sms-otp',
    VERIFICATION_STATUS: (userId: string) =>
      `/api/v1/identity/verification-status?userId=${encodeURIComponent(userId)}`,
    TOTP_SETUP: (username?: string) =>
      username
        ? `/api/v1/identity/auth/2fa/setup?username=${encodeURIComponent(username)}`
        : '/api/v1/identity/auth/2fa/setup',
    TOTP_STATUS: (userId?: string) =>
      userId
        ? `/api/v1/identity/auth/2fa/status?userId=${encodeURIComponent(userId)}`
        : '/api/v1/identity/auth/2fa/status',
    TOTP_VERIFY_SETUP: '/api/v1/identity/auth/2fa/verify-setup',
    TOTP_DISABLE: (userId?: string) =>
      userId
        ? `/api/v1/identity/auth/2fa/disable?userId=${encodeURIComponent(userId)}`
        : '/api/v1/identity/auth/2fa/disable',
    USERS_ME: '/api/v1/identity/users/me',
  },
  CANDIDATES: {
    BASE: '/api/v1/candidates',
    BY_ID: (candidateId: string) => `/api/v1/candidates/${encodeURIComponent(candidateId)}`,
  },
  EXAMINATIONS: {
    BASE: '/api/v1/examinations',
    PUBLIC: '/api/v1/examinations/public',
    MY_EXAMS: '/api/v1/examinations/my-exams',
    PUBLIC_CENTRES: '/api/v1/examinations/centres/public',
    APPLY: (examId: string) => `/api/v1/examinations/${encodeURIComponent(examId)}/apply`,
    ADMIT_CARD: (examId: string) =>
      `/api/v1/examinations/${encodeURIComponent(examId)}/admit-card`,
    APPLICATION_ADMIT_CARD: (applicationId: string) =>
      `/api/v1/examinations/applications/${encodeURIComponent(applicationId)}/admit-card`,
  },
  PAPERS: {
    BASE: '/api/v1/papers',
    PUBLIC_PRACTICE: (examId?: string) =>
      examId
        ? `/api/v1/papers/public/practice?examId=${encodeURIComponent(examId)}`
        : '/api/v1/papers/public/practice',
  },
  DELIVERY: {
    START_SESSION: '/api/v1/sessions/start',
    PAPER_QUESTIONS: (paperId: string, lang = 'en') =>
      `/api/v1/sessions/paper/${encodeURIComponent(paperId)}/questions?lang=${encodeURIComponent(lang)}`,
    SAVE_RESPONSES: '/api/v1/responses/save',
    SUBMIT_RESPONSES: '/api/v1/responses/submit',
  },
  PRACTICE: {
    BASE: '/api/practice',
    SETS: '/api/practice/sets',
    SET_BY_ID: (setId: string) => `/api/practice/sets/${encodeURIComponent(setId)}`,
    SESSIONS: '/api/practice/sessions',
    SESSION_BY_ID: (sessionId: string) =>
      `/api/practice/sessions/${encodeURIComponent(sessionId)}`,
    SESSION_QUESTIONS: (sessionId: string, lang?: string) =>
      lang
        ? `/api/practice/sessions/${encodeURIComponent(sessionId)}/questions?lang=${encodeURIComponent(lang)}`
        : `/api/practice/sessions/${encodeURIComponent(sessionId)}/questions`,
    SESSION_RESPONSE: (sessionId: string) =>
      `/api/practice/sessions/${encodeURIComponent(sessionId)}/response`,
    SESSION_SUBMIT: (sessionId: string) =>
      `/api/practice/sessions/${encodeURIComponent(sessionId)}/submit`,
    SESSION_RESULT: (sessionId: string, lang?: string) =>
      lang
        ? `/api/practice/sessions/${encodeURIComponent(sessionId)}/result?lang=${encodeURIComponent(lang)}`
        : `/api/practice/sessions/${encodeURIComponent(sessionId)}/result`,
    HISTORY: (page = 0, size = 10) => `/api/practice/history?page=${page}&size=${size}`,
  },
  LEARN: {
    LEARNER_PROFILE: '/api/v1/learner/profile',
    LEARNER_EXAMS: '/api/v1/learner/exams',
    ENROLLMENTS: '/api/v1/learner/enrollments',
    ACTIVE_ENROLLMENT: '/api/v1/learner/enrollments/active',
    DIAGNOSTIC_START: (enrollmentId: string) =>
      `/api/v1/learner/enrollments/${encodeURIComponent(enrollmentId)}/diagnostic`,
    DIAGNOSTIC_SUBMIT: (enrollmentId: string) =>
      `/api/v1/learner/enrollments/${encodeURIComponent(enrollmentId)}/diagnostic/submit`,
    NEXT_ACTIVITY: (enrollmentId: string) =>
      `/api/v1/learning/enrollments/${encodeURIComponent(enrollmentId)}/next-activity`,
    COMPLETE_ACTIVITY: (enrollmentId: string, activityId: string) =>
      `/api/v1/learning/enrollments/${encodeURIComponent(enrollmentId)}/activities/${encodeURIComponent(activityId)}/complete`,
    PROGRESS: (enrollmentId: string) =>
      `/api/v1/learning/enrollments/${encodeURIComponent(enrollmentId)}/progress`,
    DLP_CURRENT: '/api/learning-path/current',
    DLP_SUMMARY: '/api/learning-path/summary',
    DLP_RECALCULATE: '/api/learning-path/recalculate',
    DLP_COMPLETE_MILESTONE: (milestoneId: string) =>
      `/api/learning-path/milestones/${encodeURIComponent(milestoneId)}/complete`,
    DLP_DIAGNOSTIC_START: (examTarget?: string) =>
      examTarget
        ? `/api/learning-path/diagnostic-test/start?examTarget=${encodeURIComponent(examTarget)}`
        : '/api/learning-path/diagnostic-test/start',
    DLP_ADAPTIVE_QUIZ_START: '/api/learning-path/adaptive-quiz/start',
    DLP_ADAPTIVE_QUIZ_SUBMIT: '/api/learning-path/adaptive-quiz/submit',
    DLP_GATEKEEPER_START: '/api/learning-path/gatekeeper/start',
    DLP_GATEKEEPER_SUBMIT: '/api/learning-path/gatekeeper/submit',
    DLP_GATEKEEPER_STATUS: '/api/learning-path/gatekeeper/status',
    RECOMMENDATIONS_GENERATE: (examTarget = 'GENERAL_COMPETITIVE') =>
      `/api/recommendations/generate?examTarget=${encodeURIComponent(examTarget)}`,
    RECOMMENDATIONS_LATEST: '/api/recommendations/latest',
    RECOMMENDATIONS_HISTORY: (page = 0, size = 10) =>
      `/api/recommendations/history?page=${page}&size=${size}`,
    RECOMMENDATIONS_DISMISS: (id: string) =>
      `/api/recommendations/${encodeURIComponent(id)}/dismiss`,
    RECOMMENDATIONS_LEARNER_PROFILE: '/api/recommendations/learner-profile',
    RECOMMENDATIONS_STUDY_PLAN_DAY: (dayNumber: number, completed: boolean) =>
      `/api/recommendations/study-plan/days/${dayNumber}?completed=${completed}`,
    MICROLESSON_BY_LANGUAGE: (lessonId: string | number, lang: string) =>
      `/api/v1/microlessons/${encodeURIComponent(String(lessonId))}/language/${encodeURIComponent(lang)}`,
    EXPLANATIONS_GENERATE: '/api/v1/explanations/generate',
  },
  RESULTS: {
    BASE: '/api/v1/results',
    MY_RESULTS: '/api/v1/results/my-results',
    BY_CANDIDATE: (userId: string) =>
      `/api/v1/results/candidate/${encodeURIComponent(userId)}`,
    DIGILOCKER_PUSH: (resultId: string) =>
      `/api/v1/results/${encodeURIComponent(resultId)}/digilocker/push`,
    SCORECARD_DOWNLOAD: (resultId: string) =>
      `/api/v1/results/${encodeURIComponent(resultId)}/scorecard/download`,
  },
  GEO: {
    BASE: '/api/v1/geo',
  },
  MOCK_DPI: {
    MSG91_OTP: '/api/v5/otp',
    DIGILOCKER_VERIFY: '/digilocker/v1/verify',
    DIGILOCKER_PUSH: '/digilocker/v1/credential/push',
    AADHAAR_VERIFY: '/aadhaar',
  },
} as const;

export interface ResolvedDockerApiConfig {
  mode: DockerTargetMode;
  baseUrl: string;
  nginxProxyUrl: string;
  dockerInternalUrl: string;
  mockServerUrl: string;
  ocrServiceUrl: string;
  tenantId: string;
}

/**
 * Resolves the active Docker API configuration for development or container execution.
 */
export function resolveDockerDevApiConfig(
  mode: DockerTargetMode = 'localhost-gateway',
  envOverrides?: Record<string, string | undefined>
): ResolvedDockerApiConfig {
  const env =
    envOverrides ||
    (typeof process !== 'undefined' && process.env ? process.env : {});

  const explicitUrl = env.EXPO_PUBLIC_API_BASE_URL;
  let baseUrl = DOCKER_DEV_ORIGINS.localhostGatewayUrl;

  switch (mode) {
    case 'android-emulator':
      baseUrl =
        env.EXPO_PUBLIC_ANDROID_EMULATOR_API_URL ||
        DOCKER_DEV_ORIGINS.androidEmulatorGatewayUrl;
      break;
    case 'localhost-nginx':
      baseUrl =
        env.EXPO_PUBLIC_DOCKER_NGINX_PROXY_URL ||
        DOCKER_DEV_ORIGINS.localhostNginxProxyUrl;
      break;
    case 'android-emulator-nginx':
      baseUrl = DOCKER_DEV_ORIGINS.androidEmulatorNginxProxyUrl;
      break;
    case 'docker-internal':
      baseUrl =
        env.EXPO_PUBLIC_DOCKER_INTERNAL_GATEWAY_URL ||
        DOCKER_DEV_ORIGINS.dockerInternalGatewayUrl;
      break;
    case 'localhost-gateway':
    default:
      baseUrl = explicitUrl || DOCKER_DEV_ORIGINS.localhostGatewayUrl;
      break;
  }

  return {
    mode,
    baseUrl: baseUrl.replace(/\/+$/, ''),
    nginxProxyUrl:
      env.EXPO_PUBLIC_DOCKER_NGINX_PROXY_URL ||
      DOCKER_DEV_ORIGINS.localhostNginxProxyUrl,
    dockerInternalUrl:
      env.EXPO_PUBLIC_DOCKER_INTERNAL_GATEWAY_URL ||
      DOCKER_DEV_ORIGINS.dockerInternalGatewayUrl,
    mockServerUrl:
      env.EXPO_PUBLIC_MOCK_SERVER_URL || DOCKER_DEV_ORIGINS.mockServerUrl,
    ocrServiceUrl:
      env.EXPO_PUBLIC_OCR_SERVICE_URL || DOCKER_DEV_ORIGINS.ocrServiceUrl,
    tenantId: env.EXPO_PUBLIC_TENANT_ID || 'default',
  };
}

/**
 * Builds a complete URL joining the active Docker base URL and a canonical API path.
 */
export function buildDockerApiUrl(
  path: string,
  mode: DockerTargetMode = 'localhost-gateway'
): string {
  const { baseUrl } = resolveDockerDevApiConfig(mode);
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  return `${baseUrl}${normalizedPath}`;
}
