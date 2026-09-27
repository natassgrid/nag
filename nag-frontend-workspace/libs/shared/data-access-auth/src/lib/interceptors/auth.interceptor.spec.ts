import { TestBed } from '@angular/core/testing';
import {
  HttpClient,
  provideHttpClient,
  withInterceptors,
} from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';

describe('authInterceptor', () => {
  let httpClient: HttpClient;
  let httpMock: HttpTestingController;
  let authServiceMock: {
    getToken: jest.Mock;
    getTenantId: jest.Mock;
    getRefreshToken: jest.Mock;
    clearTokens: jest.Mock;
    refreshToken: jest.Mock;
  };
  let routerMock: { navigate: jest.Mock };

  beforeEach(() => {
    authServiceMock = {
      getToken: jest.fn(),
      getTenantId: jest.fn().mockReturnValue('default-tenant'),
      getRefreshToken: jest.fn(),
      clearTokens: jest.fn(),
      refreshToken: jest.fn(),
    };
    routerMock = { navigate: jest.fn() };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authServiceMock },
        { provide: Router, useValue: routerMock },
      ],
    });

    httpClient = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should attach Authorization and X-Tenant-Id headers when token is present', () => {
    authServiceMock.getToken.mockReturnValue('jwt-token-xyz');
    authServiceMock.getTenantId.mockReturnValue('tenant-upsc');

    httpClient.get('/api/v1/examinations').subscribe();

    const req = httpMock.expectOne('/api/v1/examinations');
    expect(req.request.headers.get('Authorization')).toBe('Bearer jwt-token-xyz');
    expect(req.request.headers.get('X-Tenant-Id')).toBe('tenant-upsc');
    req.flush({});
  });

  it('should attach only X-Tenant-Id header when token is null', () => {
    authServiceMock.getToken.mockReturnValue(null);
    authServiceMock.getTenantId.mockReturnValue('tenant-upsc');

    httpClient.get('/api/v1/examinations/public').subscribe();

    const req = httpMock.expectOne('/api/v1/examinations/public');
    expect(req.request.headers.has('Authorization')).toBe(false);
    expect(req.request.headers.get('X-Tenant-Id')).toBe('tenant-upsc');
    req.flush({});
  });

  it('should refresh token and retry request on 401 when refresh token is available', () => {
    authServiceMock.getToken.mockReturnValue('expired-jwt');
    authServiceMock.getRefreshToken.mockReturnValue('valid-refresh');
    authServiceMock.refreshToken.mockReturnValue(
      of({ accessToken: 'new-valid-jwt', refreshToken: 'new-refresh' })
    );

    httpClient.get('/api/v1/examinations/my-exams').subscribe();

    const req1 = httpMock.expectOne('/api/v1/examinations/my-exams');
    req1.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(authServiceMock.refreshToken).toHaveBeenCalled();

    const retryReq = httpMock.expectOne('/api/v1/examinations/my-exams');
    expect(retryReq.request.headers.get('Authorization')).toBe('Bearer new-valid-jwt');
    retryReq.flush({ data: [] });
  });

  it('should clear tokens and redirect on 401 if refresh token fails', () => {
    authServiceMock.getToken.mockReturnValue('expired-jwt');
    authServiceMock.getRefreshToken.mockReturnValue('bad-refresh');
    authServiceMock.refreshToken.mockReturnValue(
      throwError(() => new Error('Refresh expired'))
    );

    httpClient.get('/api/v1/examinations/my-exams').subscribe({
      error: () => {
        expect(authServiceMock.clearTokens).toHaveBeenCalled();
      },
    });

    const req = httpMock.expectOne('/api/v1/examinations/my-exams');
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
  });
});
