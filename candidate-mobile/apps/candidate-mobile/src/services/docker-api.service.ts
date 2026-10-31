import {
  ApiClientError,
  DOCKER_API_PATHS,
  DOCKER_DEV_ORIGINS,
  DockerTargetMode,
  NagCandidateApiClient,
  ResolvedDockerApiConfig,
  resolveDockerDevApiConfig,
  SecureTokenStorage,
} from '@nag/candidate-core';

export interface DockerGatewayHealthStatus {
  healthy: boolean;
  mode: DockerTargetMode;
  baseUrl: string;
  checkedEndpoint: string;
  statusText: string;
  checkedAtIso: string;
}

/**
 * Docker-Based API Service for the NAG Candidate Mobile Dev App.
 * Routes all mobile API requests through the NAG Docker stack:
 * - `http://localhost:9000` (`exam-monolith-app` / `api-gateway` on host)
 * - `http://10.0.2.2:9000` (Android Emulator -> host Docker `:9000`)
 * - `http://localhost:4300` (`exam-candidate-delivery` Nginx proxy on host)
 * - `http://10.0.2.2:4300` (Android Emulator -> `exam-candidate-delivery` Nginx `:4300`)
 * - `http://api-gateway:9000` (Docker internal `exam-network` alias)
 */
export class DockerApiService {
  private readonly client: NagCandidateApiClient;
  private config: ResolvedDockerApiConfig;

  constructor(
    initialMode: DockerTargetMode = 'localhost-gateway',
    options?: {
      tokenStorage?: SecureTokenStorage;
      fetchFn?: typeof fetch;
      envOverrides?: Record<string, string | undefined>;
    }
  ) {
    this.config = resolveDockerDevApiConfig(initialMode, options?.envOverrides);
    this.client = new NagCandidateApiClient({
      baseUrl: this.config.baseUrl,
      dockerMode: this.config.mode,
      tenantId: this.config.tenantId,
      refreshEndpoint: DOCKER_API_PATHS.IDENTITY.AUTH_TOKEN_REFRESH,
      tokenStorage: options?.tokenStorage,
      fetchFn: options?.fetchFn,
    });
  }

  public getApiClient(): NagCandidateApiClient {
    return this.client;
  }

  public getConfig(): ResolvedDockerApiConfig {
    return { ...this.config };
  }

  public getActiveBaseUrl(): string {
    return this.client.getBaseUrl();
  }

  public getActiveMode(): DockerTargetMode {
    return this.config.mode;
  }

  public setTargetMode(
    mode: DockerTargetMode,
    envOverrides?: Record<string, string | undefined>
  ): ResolvedDockerApiConfig {
    this.config = resolveDockerDevApiConfig(mode, envOverrides);
    this.client.setDockerMode(mode);
    this.client.setBaseUrl(this.config.baseUrl);
    return this.getConfig();
  }

  /**
   * Builds a full URL for any canonical Docker API path.
   */
  public resolveEndpointUrl(path: string): string {
    const cleanPath = path.startsWith('/') ? path : `/${path}`;
    return `${this.client.getBaseUrl()}${cleanPath}`;
  }

  /**
   * Returns all canonical Docker-routed API paths used by the mobile app.
   */
  public getPaths(): typeof DOCKER_API_PATHS {
    return DOCKER_API_PATHS;
  }

  /**
   * Returns all Docker container origin URLs (`:9000`, `:4300`, `:8099`, `:5005`).
   */
  public getOrigins(): typeof DOCKER_DEV_ORIGINS {
    return DOCKER_DEV_ORIGINS;
  }

  /**
   * Checks connectivity against the active Docker container health endpoint
   * (`/actuator/health` for `:9000` gateway/monolith or `/health` for `:4300` Nginx proxy).
   */
  public async checkDockerHealth(): Promise<DockerGatewayHealthStatus> {
    const isNginxMode =
      this.config.mode === 'localhost-nginx' ||
      this.config.mode === 'android-emulator-nginx';
    const healthPath = isNginxMode
      ? DOCKER_API_PATHS.HEALTH.NGINX_HEALTH
      : DOCKER_API_PATHS.HEALTH.GATEWAY_ACTUATOR;

    try {
      await this.client.request<unknown>(healthPath, {
        method: 'GET',
        skipAuth: true,
      });
      return {
        healthy: true,
        mode: this.config.mode,
        baseUrl: this.client.getBaseUrl(),
        checkedEndpoint: this.resolveEndpointUrl(healthPath),
        statusText: 'UP',
        checkedAtIso: new Date().toISOString(),
      };
    } catch (err) {
      return {
        healthy: false,
        mode: this.config.mode,
        baseUrl: this.client.getBaseUrl(),
        checkedEndpoint: this.resolveEndpointUrl(healthPath),
        statusText: err instanceof Error ? err.message : 'UNREACHABLE',
        checkedAtIso: new Date().toISOString(),
      };
    }
  }

  /**
   * Performs an API request against the NAG Docker stack.
   * If the request encounters a network error in a browser/WebView runtime,
   * automatically attempts candidate fallback origins (10.0.2.2:9000, localhost:9000, 10.0.2.2:4300, localhost:4300)
   * and preserves whichever origin responded.
   */
  public async requestDocker<T>(
    path: string,
    options?: {
      method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
      body?: unknown;
      headers?: Record<string, string>;
      skipAuth?: boolean;
    }
  ): Promise<T> {
    try {
      return await this.client.request<T>(path, options);
    } catch (err) {
      const isNetworkError = !(err instanceof ApiClientError);
      const isBrowserOrWebView =
        typeof window !== 'undefined' || typeof navigator !== 'undefined';

      if (!isNetworkError || !isBrowserOrWebView) {
        throw err;
      }

      const currentBase = this.client.getBaseUrl();
      const fallbackOrigins = [
        'http://10.0.2.2:9000',
        'http://localhost:9000',
        'http://10.0.2.2:4300',
        'http://localhost:4300',
      ].filter((origin) => origin !== currentBase);

      for (const origin of fallbackOrigins) {
        try {
          this.client.setBaseUrl(origin);
          const result = await this.client.request<T>(path, options);
          this.config.baseUrl = origin;
          return result;
        } catch (retryErr) {
          if (retryErr instanceof ApiClientError) {
            this.config.baseUrl = origin;
            throw retryErr;
          }
        }
      }

      this.client.setBaseUrl(currentBase);
      throw err;
    }
  }
}

const defaultTargetMode: DockerTargetMode =
  typeof navigator !== 'undefined' && /Android/i.test(navigator.userAgent || '')
    ? 'android-emulator'
    : 'localhost-gateway';

export const defaultDockerApiService = new DockerApiService(defaultTargetMode);
