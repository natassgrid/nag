import {
  buildDockerApiUrl,
  DOCKER_API_PATHS,
  DOCKER_DEV_ORIGINS,
  NagCandidateApiClient,
  resolveDockerDevApiConfig,
} from '@nag/candidate-core';
import { DockerApiService } from '../services/docker-api.service';

describe('Docker-Based API Paths & Dev Configuration', () => {
  it('resolves default localhost-gateway Docker configuration (:9000)', () => {
    const cfg = resolveDockerDevApiConfig('localhost-gateway', {});
    expect(cfg.baseUrl).toBe('http://localhost:9000');
    expect(cfg.nginxProxyUrl).toBe('http://localhost:4300');
    expect(cfg.dockerInternalUrl).toBe('http://api-gateway:9000');
    expect(cfg.mockServerUrl).toBe('http://localhost:8099');
    expect(cfg.ocrServiceUrl).toBe('http://localhost:5005');
    expect(cfg.tenantId).toBe('default');
  });

  it('resolves Android Emulator loopback (10.0.2.2:9000 and 10.0.2.2:4300)', () => {
    const gatewayCfg = resolveDockerDevApiConfig('android-emulator', {});
    expect(gatewayCfg.baseUrl).toBe('http://10.0.2.2:9000');

    const nginxCfg = resolveDockerDevApiConfig('android-emulator-nginx', {});
    expect(nginxCfg.baseUrl).toBe('http://10.0.2.2:4300');
  });

  it('resolves Docker internal network alias (http://api-gateway:9000)', () => {
    const internalCfg = resolveDockerDevApiConfig('docker-internal', {});
    expect(internalCfg.baseUrl).toBe('http://api-gateway:9000');
  });

  it('maps canonical Docker API paths matching candidate-delivery & nginx.conf', () => {
    expect(DOCKER_API_PATHS.IDENTITY.REGISTER).toBe('/api/v1/identity/register');
    expect(DOCKER_API_PATHS.IDENTITY.AUTH_TOKEN).toBe('/api/v1/identity/auth/token');
    expect(DOCKER_API_PATHS.IDENTITY.AUTH_TOKEN_REFRESH).toBe(
      '/api/v1/identity/auth/token/refresh'
    );
    expect(DOCKER_API_PATHS.CANDIDATES.BY_ID('cand-001')).toBe(
      '/api/v1/candidates/cand-001'
    );
    expect(DOCKER_API_PATHS.EXAMINATIONS.PUBLIC).toBe('/api/v1/examinations/public');
    expect(DOCKER_API_PATHS.EXAMINATIONS.MY_EXAMS).toBe('/api/v1/examinations/my-exams');
    expect(DOCKER_API_PATHS.EXAMINATIONS.PUBLIC_CENTRES).toBe(
      '/api/v1/examinations/centres/public'
    );
    expect(DOCKER_API_PATHS.EXAMINATIONS.APPLY('exam-2')).toBe(
      '/api/v1/examinations/exam-2/apply'
    );
    expect(DOCKER_API_PATHS.EXAMINATIONS.ADMIT_CARD('exam-2')).toBe(
      '/api/v1/examinations/exam-2/admit-card'
    );
    expect(DOCKER_API_PATHS.PAPERS.PUBLIC_PRACTICE('exam-1')).toBe(
      '/api/v1/papers/public/practice?examId=exam-1'
    );
    expect(DOCKER_API_PATHS.DELIVERY.START_SESSION).toBe('/api/v1/sessions/start');
    expect(DOCKER_API_PATHS.DELIVERY.PAPER_QUESTIONS('paper-101', 'hi')).toBe(
      '/api/v1/sessions/paper/paper-101/questions?lang=hi'
    );
    expect(DOCKER_API_PATHS.PRACTICE.SETS).toBe('/api/practice/sets');
    expect(DOCKER_API_PATHS.PRACTICE.SESSION_RESPONSE('sess-1')).toBe(
      '/api/practice/sessions/sess-1/response'
    );
    expect(DOCKER_API_PATHS.LEARN.LEARNER_PROFILE).toBe('/api/v1/learner/profile');
    expect(DOCKER_API_PATHS.LEARN.DLP_CURRENT).toBe('/api/learning-path/current');
    expect(DOCKER_API_PATHS.LEARN.RECOMMENDATIONS_LATEST).toBe(
      '/api/recommendations/latest'
    );
    expect(DOCKER_API_PATHS.LEARN.MICROLESSON_BY_LANGUAGE('lesson-501', 'ta')).toBe(
      '/api/v1/microlessons/lesson-501/language/ta'
    );
    expect(DOCKER_API_PATHS.LEARN.EXPLANATIONS_GENERATE).toBe(
      '/api/v1/explanations/generate'
    );
    expect(DOCKER_API_PATHS.RESULTS.MY_RESULTS).toBe('/api/v1/results/my-results');
    expect(DOCKER_API_PATHS.RESULTS.DIGILOCKER_PUSH('res-1')).toBe(
      '/api/v1/results/res-1/digilocker/push'
    );
  });

  it('switches Docker target modes dynamically in DockerApiService and checks health', async () => {
    const calledUrls: string[] = [];
    const mockFetch = (async (url: string) => {
      calledUrls.push(url);
      return {
        ok: true,
        status: 200,
        statusText: 'OK',
        json: async () => ({ status: 'UP' }),
        text: async () => 'OK',
      } as unknown as Response;
    }) as unknown as typeof fetch;

    const service = new DockerApiService('localhost-gateway', { fetchFn: mockFetch });
    expect(service.getActiveBaseUrl()).toBe(DOCKER_DEV_ORIGINS.localhostGatewayUrl);

    const health1 = await service.checkDockerHealth();
    expect(health1.healthy).toBe(true);
    expect(health1.checkedEndpoint).toBe('http://localhost:9000/actuator/health');

    service.setTargetMode('android-emulator');
    expect(service.getActiveBaseUrl()).toBe('http://10.0.2.2:9000');
    expect(service.resolveEndpointUrl(DOCKER_API_PATHS.EXAMINATIONS.MY_EXAMS)).toBe(
      'http://10.0.2.2:9000/api/v1/examinations/my-exams'
    );

    service.setTargetMode('localhost-nginx');
    const health2 = await service.checkDockerHealth();
    expect(health2.healthy).toBe(true);
    expect(health2.checkedEndpoint).toBe('http://localhost:4300/health');

    expect(buildDockerApiUrl('/api/v1/results/my-results', 'docker-internal')).toBe(
      'http://api-gateway:9000/api/v1/results/my-results'
    );
    expect(calledUrls).toContain('http://localhost:9000/actuator/health');
    expect(calledUrls).toContain('http://localhost:4300/health');
  });

  it('executes Docker-routed domain calls via NagCandidateApiClient', async () => {
    const requests: { url: string; method?: string }[] = [];
    const mockFetch = (async (url: string, init?: RequestInit) => {
      requests.push({ url, method: init?.method });
      return {
        ok: true,
        status: 200,
        statusText: 'OK',
        json: async () => [],
        text: async () => '[]',
      } as unknown as Response;
    }) as unknown as typeof fetch;

    const client = new NagCandidateApiClient({
      dockerMode: 'android-emulator',
      fetchFn: mockFetch,
    });

    await client.getMyEnrolledExams();
    await client.getPublicPracticePapers('exam-101');
    await client.getPracticeSets();
    await client.getCurrentLearningPath();
    await client.getLatestRecommendations();
    await client.getMicroLessonByLanguage('501', 'hi');
    await client.getMyResults();

    expect(requests).toEqual([
      { url: 'http://10.0.2.2:9000/api/v1/examinations/my-exams', method: 'GET' },
      {
        url: 'http://10.0.2.2:9000/api/v1/papers/public/practice?examId=exam-101',
        method: 'GET',
      },
      { url: 'http://10.0.2.2:9000/api/practice/sets', method: 'GET' },
      { url: 'http://10.0.2.2:9000/api/learning-path/current', method: 'GET' },
      { url: 'http://10.0.2.2:9000/api/recommendations/latest', method: 'GET' },
      { url: 'http://10.0.2.2:9000/api/v1/microlessons/501/language/hi', method: 'GET' },
      { url: 'http://10.0.2.2:9000/api/v1/results/my-results', method: 'GET' },
    ]);
  });

  it('retries requestDocker against fallback origins on network failure in browser/WebView', async () => {
    const attemptedUrls: string[] = [];
    const mockFetch = (async (url: string) => {
      attemptedUrls.push(url);
      if (url.startsWith('http://10.0.2.2:9000')) {
        throw new TypeError('Failed to fetch (connection refused)');
      }
      if (url.startsWith('http://localhost:9000')) {
        return {
          ok: true,
          status: 200,
          statusText: 'OK',
          json: async () => ({ status: 'success', server: 'host-gateway' }),
          text: async () => 'OK',
        } as unknown as Response;
      }
      throw new TypeError('Failed to fetch');
    }) as unknown as typeof fetch;

    // Simulate browser environment
    (global as any).window = {};

    const service = new DockerApiService('android-emulator', { fetchFn: mockFetch });
    expect(service.getActiveBaseUrl()).toBe('http://10.0.2.2:9000');

    const result = await service.requestDocker<{ status: string }>('/api/v1/health');
    expect(result.status).toBe('success');
    expect(service.getActiveBaseUrl()).toBe('http://localhost:9000');
    expect(attemptedUrls).toContain('http://10.0.2.2:9000/api/v1/health');
    expect(attemptedUrls).toContain('http://localhost:9000/api/v1/health');

    delete (global as any).window;
  });
});
