/**
 * NAG Candidate API Client
 * Features:
 * - Configured with Docker-based API paths (`DOCKER_API_PATHS` & `resolveDockerDevApiConfig`)
 * - Compatible with expo-secure-store token storage
 * - Automatic Bearer token & X-Tenant-Id header injection
 * - Concurrent-safe 401 token refresh lifecycle with request queue coalescing
 * - Offline response packet buffering with exponential backoff retry calculation
 * - Fully typed REST endpoints for Auth, Catalog, Applications, Profile, Practice, Learn, Results & CBT Delivery
 */

import {
  ApplyExamPayload,
  ApplicationReceipt,
  AuthTokens,
  CandidateProfile,
  CandidateRegistrationPayload,
  CatalogExam,
  CatalogFilterState,
  DigitalAdmitCard,
  ExamItem,
  ExamSubmissionReceipt,
  MobileOtpRequest,
  MobileOtpVerifyPayload,
  OfflineResponsePacket,
  PublicCentre,
  TotpVerifyPayload,
} from '../models';
import {
  DOCKER_API_PATHS,
  DockerTargetMode,
  resolveDockerDevApiConfig,
} from './docker-api-config';

export interface SecureTokenStorage {
  getItem(key: string): Promise<string | null>;
  setItem(key: string, value: string): Promise<void>;
  removeItem(key: string): Promise<void>;
}

export class InMemoryTokenStorage implements SecureTokenStorage {
  private storage: Map<string, string> = new Map();

  async getItem(key: string): Promise<string | null> {
    return this.storage.get(key) ?? null;
  }

  async setItem(key: string, value: string): Promise<void> {
    this.storage.set(key, value);
  }

  async removeItem(key: string): Promise<void> {
    this.storage.delete(key);
  }
}

export interface ApiClientConfig {
  baseUrl?: string;
  dockerMode?: DockerTargetMode;
  tenantId?: string;
  refreshEndpoint?: string;
  tokenStorage?: SecureTokenStorage;
  fetchFn?: typeof fetch;
  onUnauthorized?: () => void;
}

export interface RequestOptions {
  method?: string;
  headers?: Record<string, string>;
  body?: unknown;
  skipAuth?: boolean;
}

export class ApiClientError extends Error {
  constructor(
    public readonly status: number,
    public readonly statusText: string,
    public readonly responseBody?: unknown
  ) {
    super(`API Error ${status} (${statusText}): ${JSON.stringify(responseBody || '')}`);
    this.name = 'ApiClientError';
  }
}

export class NagCandidateApiClient {
  private baseUrl: string;
  private dockerMode: DockerTargetMode;
  private tenantId: string;
  private readonly refreshEndpoint: string;
  private readonly tokenStorage: SecureTokenStorage;
  private readonly fetchFn: typeof fetch;
  private readonly onUnauthorized?: () => void;

  private refreshPromise: Promise<AuthTokens> | null = null;
  private offlineQueue: OfflineResponsePacket[] = [];

  constructor(config: ApiClientConfig = {}) {
    const dockerConfig = resolveDockerDevApiConfig(
      config.dockerMode || 'localhost-gateway'
    );
    this.dockerMode = dockerConfig.mode;
    this.baseUrl = (config.baseUrl || dockerConfig.baseUrl).replace(/\/+$/, '');
    this.tenantId = config.tenantId || dockerConfig.tenantId || 'default';
    this.refreshEndpoint =
      config.refreshEndpoint || DOCKER_API_PATHS.IDENTITY.LEGACY_REFRESH;
    this.tokenStorage = config.tokenStorage || new InMemoryTokenStorage();
    this.fetchFn =
      config.fetchFn ||
      (typeof fetch !== 'undefined'
        ? fetch.bind(globalThis)
        : (undefined as unknown as typeof fetch));
    this.onUnauthorized = config.onUnauthorized;
  }

  // ============================================================================
  // Docker Environment & Base URL Configuration
  // ============================================================================

  public getBaseUrl(): string {
    return this.baseUrl;
  }

  public setBaseUrl(url: string): void {
    this.baseUrl = url.replace(/\/+$/, '');
  }

  public getDockerMode(): DockerTargetMode {
    return this.dockerMode;
  }

  public setDockerMode(mode: DockerTargetMode): string {
    const resolved = resolveDockerDevApiConfig(mode);
    this.dockerMode = resolved.mode;
    this.baseUrl = resolved.baseUrl;
    return this.baseUrl;
  }

  public getTenantId(): string {
    return this.tenantId;
  }

  public setTenantId(tenantId: string): void {
    this.tenantId = tenantId || 'default';
  }

  // ============================================================================
  // Token Management & 401 Refresh Lifecycle
  // ============================================================================

  public async setAuthTokens(tokens: AuthTokens): Promise<void> {
    await this.tokenStorage.setItem('nag_access_token', tokens.accessToken);
    await this.tokenStorage.setItem('nag_refresh_token', tokens.refreshToken);
  }

  public async setTokens(tokens: AuthTokens): Promise<void> {
    return this.setAuthTokens(tokens);
  }

  public async getAccessToken(): Promise<string | null> {
    return this.tokenStorage.getItem('nag_access_token');
  }

  public async getRefreshToken(): Promise<string | null> {
    return this.tokenStorage.getItem('nag_refresh_token');
  }

  public async clearAuthTokens(): Promise<void> {
    await this.tokenStorage.removeItem('nag_access_token');
    await this.tokenStorage.removeItem('nag_refresh_token');
  }

  /**
   * Concurrent-safe access token refresh.
   * If a refresh is already in-flight, returns the active promise to prevent duplicate calls.
   */
  public async refreshAccessToken(): Promise<AuthTokens> {
    if (this.refreshPromise) {
      return this.refreshPromise;
    }

    this.refreshPromise = this.executeTokenRefresh().finally(() => {
      this.refreshPromise = null;
    });

    return this.refreshPromise;
  }

  private async executeTokenRefresh(): Promise<AuthTokens> {
    const refreshToken = await this.getRefreshToken();
    if (!refreshToken) {
      await this.clearAuthTokens();
      this.onUnauthorized?.();
      throw new ApiClientError(401, 'Unauthorized', { message: 'No refresh token available' });
    }

    try {
      const response = await this.fetchFn(`${this.baseUrl}${this.refreshEndpoint}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Accept: 'application/json',
        },
        body: JSON.stringify({ refreshToken }),
      });

      if (!response.ok) {
        await this.clearAuthTokens();
        this.onUnauthorized?.();
        throw new ApiClientError(response.status, response.statusText, await response.text());
      }

      const raw = (await response.json()) as { data?: AuthTokens } & AuthTokens;
      const tokens: AuthTokens = raw.data || raw;
      await this.setAuthTokens(tokens);
      return tokens;
    } catch (err) {
      await this.clearAuthTokens();
      this.onUnauthorized?.();
      throw err;
    }
  }

  // ============================================================================
  // Core HTTP Request Handler with Automatic Auth & 401 Retry
  // ============================================================================

  public async request<T>(endpoint: string, options: RequestOptions = {}, isRetry = false): Promise<T> {
    const url = `${this.baseUrl}${endpoint.startsWith('/') ? endpoint : `/${endpoint}`}`;
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      ...(options.headers || {}),
    };

    if (!options.skipAuth) {
      const token = await this.getAccessToken();
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
    }

    const response = await this.fetchFn(url, {
      method: options.method || 'GET',
      headers,
      body: options.body ? JSON.stringify(options.body) : undefined,
    });

    // Handle 401 Unauthorized with single token refresh retry
    if (response.status === 401 && !isRetry && !options.skipAuth) {
      await this.refreshAccessToken();
      return await this.request<T>(endpoint, options, true);
    }

    if (!response.ok) {
      let errorData: unknown;
      try {
        errorData = await response.json();
      } catch {
        errorData = await response.text();
      }
      throw new ApiClientError(response.status, response.statusText, errorData);
    }

    return (await response.json()) as T;
  }

  // ============================================================================
  // Offline Response Packet Buffering & Backoff Calculation
  // ============================================================================

  public enqueueResponsePacket(packet: OfflineResponsePacket): void {
    const existingIndex = this.offlineQueue.findIndex(
      (p) => p.sessionId === packet.sessionId && p.revision === packet.revision
    );
    if (existingIndex >= 0) {
      this.offlineQueue[existingIndex] = packet;
    } else {
      this.offlineQueue.push(packet);
    }
  }

  public getOfflineQueue(): OfflineResponsePacket[] {
    return [...this.offlineQueue];
  }

  public clearOfflineQueue(): void {
    this.offlineQueue = [];
  }

  public calculateBackoffDelay(attempt: number, baseDelayMs = 1000, maxDelayMs = 30000): number {
    const exp = Math.pow(2, Math.max(0, attempt));
    return Math.min(maxDelayMs, Math.round(baseDelayMs * exp));
  }

  public async flushOfflineQueue(
    saveFn?: (packet: OfflineResponsePacket) => Promise<boolean>
  ): Promise<{ flushed: number; remaining: number }> {
    let flushed = 0;

    while (this.offlineQueue.length > 0) {
      const packet = this.offlineQueue[0];
      try {
        let success = false;
        if (saveFn) {
          success = await saveFn(packet);
        } else {
          await this.saveResponses(packet);
          success = true;
        }

        if (success) {
          this.offlineQueue.shift();
          flushed++;
        } else {
          break;
        }
      } catch {
        break;
      }
    }

    return { flushed, remaining: this.offlineQueue.length };
  }

  // ============================================================================
  // Typed Domain API Methods (Docker-Routed Paths)
  // ============================================================================

  // 1. Auth & Identity Endpoints (`/api/v1/identity/*`)
  public async register(payload: CandidateRegistrationPayload): Promise<CandidateProfile> {
    return this.request<CandidateProfile>(DOCKER_API_PATHS.IDENTITY.REGISTER, {
      method: 'POST',
      body: payload,
      skipAuth: true,
    });
  }

  public async loginWithCredentials(credentials: {
    username: string;
    password?: string;
    totpCode?: string;
  }): Promise<AuthTokens> {
    const res = await this.request<{ data?: AuthTokens } & AuthTokens>(
      DOCKER_API_PATHS.IDENTITY.AUTH_TOKEN,
      {
        method: 'POST',
        body: credentials,
        skipAuth: true,
      }
    );
    const tokens = res.data || res;
    await this.setAuthTokens(tokens);
    return tokens;
  }

  public async refreshDockerAuthToken(): Promise<AuthTokens> {
    const refreshToken = await this.getRefreshToken();
    const res = await this.request<{ data?: AuthTokens } & AuthTokens>(
      DOCKER_API_PATHS.IDENTITY.AUTH_TOKEN_REFRESH,
      {
        method: 'POST',
        body: { refreshToken },
        skipAuth: true,
      }
    );
    const tokens = res.data || res;
    await this.setAuthTokens(tokens);
    return tokens;
  }

  public async logoutFromGateway(): Promise<void> {
    try {
      await this.request<unknown>(DOCKER_API_PATHS.IDENTITY.AUTH_LOGOUT, {
        method: 'DELETE',
        headers: { 'X-Tenant-Id': this.tenantId },
      });
    } finally {
      await this.clearAuthTokens();
    }
  }

  public async sendOtp(payload: MobileOtpRequest): Promise<{ success: boolean; message: string }> {
    return this.request<{ success: boolean; message: string }>(DOCKER_API_PATHS.IDENTITY.OTP_SEND, {
      method: 'POST',
      body: payload,
      skipAuth: true,
    });
  }

  public async verifyOtp(payload: MobileOtpVerifyPayload): Promise<AuthTokens> {
    const tokens = await this.request<AuthTokens>(DOCKER_API_PATHS.IDENTITY.OTP_VERIFY, {
      method: 'POST',
      body: payload,
      skipAuth: true,
    });
    await this.setAuthTokens(tokens);
    return tokens;
  }

  public async verifyTotp(payload: TotpVerifyPayload): Promise<AuthTokens> {
    const tokens = await this.request<AuthTokens>(DOCKER_API_PATHS.IDENTITY.TOTP_VERIFY, {
      method: 'POST',
      body: payload,
      skipAuth: true,
    });
    await this.setAuthTokens(tokens);
    return tokens;
  }

  // 2. Candidate Profile Endpoints (`/api/v1/candidates/*`)
  public async getCandidateProfile(candidateId: string): Promise<unknown> {
    return this.request<unknown>(DOCKER_API_PATHS.CANDIDATES.BY_ID(candidateId), {
      method: 'GET',
    });
  }

  public async saveCandidateProfile(
    payload: Record<string, unknown>,
    candidateId?: string
  ): Promise<unknown> {
    if (candidateId) {
      return this.request<unknown>(DOCKER_API_PATHS.CANDIDATES.BY_ID(candidateId), {
        method: 'PUT',
        body: payload,
      });
    }
    return this.request<unknown>(DOCKER_API_PATHS.CANDIDATES.BASE, {
      method: 'POST',
      body: payload,
    });
  }

  public async updatePreferredLanguage(preferredLanguage: string): Promise<unknown> {
    return this.request<unknown>(DOCKER_API_PATHS.IDENTITY.USERS_ME, {
      method: 'PUT',
      body: { preferredLanguage },
    });
  }

  // 3. Catalog, Dashboard & Examination Application Endpoints (`/api/v1/examinations/*`)
  public async getPublicExams(filters?: CatalogFilterState): Promise<CatalogExam[]> {
    const query = new URLSearchParams();
    if (filters?.searchQuery) query.append('q', filters.searchQuery);
    if (filters?.conductingBody) query.append('body', filters.conductingBody);
    if (filters?.category) query.append('cat', filters.category);
    if (filters?.status) query.append('status', filters.status);
    if (filters?.eligibleOnly) query.append('eligibleOnly', 'true');

    const qs = query.toString();
    const endpoint = `${DOCKER_API_PATHS.EXAMINATIONS.PUBLIC}${qs ? `?${qs}` : ''}`;
    return this.request<CatalogExam[]>(endpoint, { method: 'GET', skipAuth: true });
  }

  public async getMyEnrolledExams(): Promise<unknown[]> {
    return this.request<unknown[]>(DOCKER_API_PATHS.EXAMINATIONS.MY_EXAMS, {
      method: 'GET',
    });
  }

  public async getPublicCentres(): Promise<PublicCentre[]> {
    return this.request<PublicCentre[]>(DOCKER_API_PATHS.EXAMINATIONS.PUBLIC_CENTRES, {
      method: 'GET',
      skipAuth: true,
    });
  }

  public async applyForExam(examId: string, payload: ApplyExamPayload): Promise<ApplicationReceipt> {
    return this.request<ApplicationReceipt>(DOCKER_API_PATHS.EXAMINATIONS.APPLY(examId), {
      method: 'POST',
      body: payload,
    });
  }

  public async getAdmitCard(examId: string): Promise<DigitalAdmitCard> {
    return this.request<DigitalAdmitCard>(DOCKER_API_PATHS.EXAMINATIONS.ADMIT_CARD(examId), {
      method: 'GET',
    });
  }

  public async getPublicPracticePapers(examId?: string): Promise<unknown[]> {
    return this.request<unknown[]>(DOCKER_API_PATHS.PAPERS.PUBLIC_PRACTICE(examId), {
      method: 'GET',
    });
  }

  // 4. CBT Session & Response Sync Endpoints (`/api/v1/sessions/*` & `/api/v1/responses/*`)
  public async startSession(
    examId: string,
    options?: { paperId?: string; terminateExisting?: boolean }
  ): Promise<{ sessionId: string; exam: CatalogExam; items: ExamItem[] }> {
    const body = options ? { examId, ...options } : { examId };
    return this.request<{ sessionId: string; exam: CatalogExam; items: ExamItem[] }>(
      DOCKER_API_PATHS.DELIVERY.START_SESSION,
      {
        method: 'POST',
        body,
      }
    );
  }

  public async getPaperQuestions(paperId: string, lang = 'en'): Promise<ExamItem[]> {
    return this.request<ExamItem[]>(
      DOCKER_API_PATHS.DELIVERY.PAPER_QUESTIONS(paperId, lang),
      {
        method: 'GET',
      }
    );
  }

  public async saveResponses(
    packet: OfflineResponsePacket
  ): Promise<{ acknowledgedRevision: number }> {
    return this.request<{ acknowledgedRevision: number }>(
      DOCKER_API_PATHS.DELIVERY.SAVE_RESPONSES,
      {
        method: 'POST',
        body: packet,
      }
    );
  }

  public async submitSession(
    receipt: ExamSubmissionReceipt
  ): Promise<{ confirmed: boolean; submissionId: string }> {
    return this.request<{ confirmed: boolean; submissionId: string }>(
      DOCKER_API_PATHS.DELIVERY.SUBMIT_RESPONSES,
      {
        method: 'POST',
        body: receipt,
      }
    );
  }

  // 5. Practice Hub Endpoints (`/api/practice/*`)
  public async getPracticeSets(): Promise<unknown[]> {
    return this.request<unknown[]>(DOCKER_API_PATHS.PRACTICE.SETS, { method: 'GET' });
  }

  public async startPracticeSession(payload: Record<string, unknown>): Promise<unknown> {
    return this.request<unknown>(DOCKER_API_PATHS.PRACTICE.SESSIONS, {
      method: 'POST',
      body: payload,
    });
  }

  public async savePracticeResponse(
    sessionId: string,
    payload: Record<string, unknown>
  ): Promise<void> {
    await this.request<void>(DOCKER_API_PATHS.PRACTICE.SESSION_RESPONSE(sessionId), {
      method: 'PUT',
      body: payload,
    });
  }

  public async submitPracticeSession(sessionId: string): Promise<unknown> {
    return this.request<unknown>(DOCKER_API_PATHS.PRACTICE.SESSION_SUBMIT(sessionId), {
      method: 'POST',
      body: {},
    });
  }

  public async getPracticeHistory(page = 0, size = 10): Promise<unknown> {
    return this.request<unknown>(DOCKER_API_PATHS.PRACTICE.HISTORY(page, size), {
      method: 'GET',
    });
  }

  // 6. Adaptive Learning, DLP Roadmap, Recommendations & Micro-Lessons
  public async getLearnerProfile(): Promise<unknown> {
    return this.request<unknown>(DOCKER_API_PATHS.LEARN.LEARNER_PROFILE, { method: 'GET' });
  }

  public async updateLearnerProfile(payload: Record<string, unknown>): Promise<unknown> {
    return this.request<unknown>(DOCKER_API_PATHS.LEARN.LEARNER_PROFILE, {
      method: 'PUT',
      body: payload,
    });
  }

  public async getActiveEnrollment(): Promise<unknown> {
    return this.request<unknown>(DOCKER_API_PATHS.LEARN.ACTIVE_ENROLLMENT, { method: 'GET' });
  }

  public async getCurrentLearningPath(): Promise<unknown> {
    return this.request<unknown>(DOCKER_API_PATHS.LEARN.DLP_CURRENT, { method: 'GET' });
  }

  public async getLatestRecommendations(): Promise<unknown> {
    return this.request<unknown>(DOCKER_API_PATHS.LEARN.RECOMMENDATIONS_LATEST, {
      method: 'GET',
    });
  }

  public async getMicroLessonByLanguage(
    lessonId: string | number,
    lang: string
  ): Promise<unknown> {
    return this.request<unknown>(
      DOCKER_API_PATHS.LEARN.MICROLESSON_BY_LANGUAGE(lessonId, lang),
      {
        method: 'GET',
      }
    );
  }

  public async generateExplanation(payload: Record<string, unknown>): Promise<unknown> {
    return this.request<unknown>(DOCKER_API_PATHS.LEARN.EXPLANATIONS_GENERATE, {
      method: 'POST',
      body: payload,
    });
  }

  // 7. Official Scorecards & Results (`/api/v1/results/*`)
  public async getMyResults(): Promise<unknown[]> {
    return this.request<unknown[]>(DOCKER_API_PATHS.RESULTS.MY_RESULTS, {
      method: 'GET',
    });
  }

  public async pushResultToDigiLocker(resultId: string): Promise<unknown> {
    return this.request<unknown>(DOCKER_API_PATHS.RESULTS.DIGILOCKER_PUSH(resultId), {
      method: 'POST',
      body: {},
    });
  }
}
