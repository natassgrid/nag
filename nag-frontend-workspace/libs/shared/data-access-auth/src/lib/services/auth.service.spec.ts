import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { UserToken, AuthUser } from '../models/auth.model';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let routerSpy: { navigate: jest.Mock };

  beforeEach(() => {
    localStorage.clear();
    routerSpy = { navigate: jest.fn() };

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        AuthService,
        { provide: Router, useValue: routerSpy },
      ],
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should initialize unauthenticated when storage is empty', () => {
    expect(service.isAuthenticated()).toBe(false);
    expect(service.currentUser()).toBeNull();
    expect(service.userRoles()).toEqual([]);
    expect(service.userName()).toBe('Guest');
  });

  it('should set authenticated state when user is stored', () => {
    const user: AuthUser = {
      userId: 'usr-101',
      username: 'admin@gov.in',
      roles: ['SUPER_ADMIN'],
    };
    service.currentUser.set(user);
    service.isAuthenticated.set(true);

    expect(service.isAuthenticated()).toBe(true);
    expect(service.currentUser()?.userId).toBe('usr-101');
    expect(service.hasRole('SUPER_ADMIN')).toBe(true);
    expect(service.isSuperAdmin()).toBe(true);
    expect(service.userName()).toBe('admin@gov.in');
  });

  it('should check role helper functions correctly', () => {
    const user: AuthUser = {
      userId: 'usr-202',
      username: 'controller@gov.in',
      roles: ['EXAM_CONTROLLER', 'QUESTION_AUTHOR', 'CANDIDATE'],
    };
    service.currentUser.set(user);

    expect(service.hasRole('EXAM_CONTROLLER')).toBe(true);
    expect(service.hasAnyRole(['UNKNOWN', 'QUESTION_AUTHOR'])).toBe(true);
    expect(service.hasAnyRole(['UNKNOWN', 'OTHER'])).toBe(false);
    expect(service.isExamController()).toBe(true);
    expect(service.isQuestionAuthor()).toBe(true);
    expect(service.isCandidate()).toBe(true);
  });

  it('should handle tenant id getter and setter', () => {
    expect(service.getTenantId()).toBe('default');
    service.setTenantId('tenant-karnataka');
    expect(service.getTenantId()).toBe('tenant-karnataka');
  });

  it('should login and store tokens', (done) => {
    const mockToken: UserToken = {
      accessToken: 'access-jwt-123',
      refreshToken: 'refresh-jwt-123',
      userId: 'usr-1',
      roles: ['CANDIDATE'],
      tokenType: 'Bearer',
      expiresIn: 3600,
    };

    service.login({ username: 'candidate@test.com', password: 'password123' }).subscribe((res) => {
      expect(res.accessToken).toBe('access-jwt-123');
      expect(service.getToken()).toBe('access-jwt-123');
      expect(service.getRefreshToken()).toBe('refresh-jwt-123');
      expect(service.isAuthenticated()).toBe(true);
      done();
    });

    const req = httpMock.expectOne('/api/v1/identity/auth/login');
    expect(req.request.method).toBe('POST');
    req.flush({ data: mockToken });
  });

  it('should refresh tokens when refresh token is available', (done) => {
    localStorage.setItem('nag_refresh_token', 'old-refresh');

    const refreshed: UserToken = {
      accessToken: 'new-access-jwt',
      refreshToken: 'new-refresh-jwt',
      userId: 'usr-1',
      roles: ['CANDIDATE'],
      tokenType: 'Bearer',
      expiresIn: 3600,
    };

    service.refreshToken().subscribe((res) => {
      expect(res.accessToken).toBe('new-access-jwt');
      expect(service.getToken()).toBe('new-access-jwt');
      done();
    });

    const req = httpMock.expectOne('/api/v1/identity/auth/token/refresh');
    expect(req.request.method).toBe('POST');
    req.flush({ data: refreshed });
  });

  it('should throw error when refreshing without refresh token', (done) => {
    service.refreshToken().subscribe({
      error: (err) => {
        expect(err.message).toBe('No refresh token available');
        done();
      },
    });
  });

  it('should logout, clear tokens, and invoke logout endpoint', () => {
    localStorage.setItem('nag_access_token', 'token-to-invalidate');
    localStorage.setItem('nag_tenant_id', 'tenant-1');
    service.isAuthenticated.set(true);

    service.logout();

    expect(service.getToken()).toBeNull();
    expect(service.isAuthenticated()).toBe(false);

    const req = httpMock.expectOne('/api/v1/identity/auth/logout');
    expect(req.request.method).toBe('DELETE');
    expect(req.request.headers.get('Authorization')).toBe('Bearer token-to-invalidate');
    expect(req.request.headers.get('X-Tenant-Id')).toBe('tenant-1');
    req.flush({});
  });

  it('should verify email and mobile', (done) => {
    service.verifyEmail({ userId: 'u1', otp: '123456' }).subscribe((res) => {
      expect(res.emailVerified).toBe(true);
    });
    const req1 = httpMock.expectOne('/api/v1/identity/verify/email');
    req1.flush({ data: { userId: 'u1', emailVerified: true, mobileVerified: false, fullyVerified: false } });

    service.verifyMobile({ userId: 'u1', otp: '654321' }).subscribe((res) => {
      expect(res.mobileVerified).toBe(true);
      done();
    });
    const req2 = httpMock.expectOne('/api/v1/identity/verify/mobile');
    req2.flush({ data: { userId: 'u1', emailVerified: true, mobileVerified: true, fullyVerified: true } });
  });

  it('should get verification status and setup TOTP', (done) => {
    service.getVerificationStatus('u-123').subscribe((res) => {
      expect(res.fullyVerified).toBe(true);
    });
    const req1 = httpMock.expectOne('/api/v1/identity/verification-status?userId=u-123');
    req1.flush({ data: { userId: 'u-123', emailVerified: true, mobileVerified: true, fullyVerified: true } });

    service.setupTotp('admin@nag.gov.in').subscribe((res) => {
      expect(res.secret).toBe('TOTPSECRET123');
      done();
    });
    const req2 = httpMock.expectOne('/api/v1/identity/auth/2fa/setup?username=admin%40nag.gov.in');
    req2.flush({ data: { secret: 'TOTPSECRET123', qrCodeUrl: 'data:image/png;base64,123', manualEntryKey: 'TOTPSECRET123' } });
  });
});
