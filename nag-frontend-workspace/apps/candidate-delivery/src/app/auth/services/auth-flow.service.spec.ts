import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthFlowService } from './auth-flow.service';

describe('AuthFlowService', () => {
  let service: AuthFlowService;
  let httpMock: HttpTestingController;
  let mockRouter: { navigate: jest.Mock };

  beforeEach(() => {
    mockRouter = { navigate: jest.fn() };

    TestBed.configureTestingModule({
      providers: [
        AuthFlowService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: mockRouter },
      ],
    });

    service = TestBed.inject(AuthFlowService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should register candidate via POST request', (done) => {
    const payload = {
      fullName: 'Aarav Kumar',
      email: 'aarav@example.com',
      mobile: '9876543210',
      password: 'Password@123',
    };

    service.registerCandidate(payload).subscribe((res) => {
      expect(res).toBeDefined();
      done();
    });

    const req = httpMock.expectOne('/api/v1/identity/register');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush({ success: true });
  });

  it('should navigate to dashboard', () => {
    service.navigateToDashboard();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('should navigate to verify-otp with query params', () => {
    service.navigateToVerifyOtp({ userId: 'u-1', email: 'test@exam.com', pending: true });
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/verify-otp'], {
      queryParams: {
        userId: 'u-1',
        email: 'test@exam.com',
        mobile: undefined,
        pending: 'true',
      },
    });
  });

  it('should detect unverified account errors', () => {
    expect(service.isUnverifiedAccountError({ status: 403, error: { pendingVerification: true } })).toBe(true);
    expect(service.isUnverifiedAccountError({ status: 403, error: { title: 'Account Not Verified' } })).toBe(true);
    expect(service.isUnverifiedAccountError({ status: 403, error: { detail: 'Account is not yet verified' } })).toBe(true);
    expect(service.isUnverifiedAccountError({ status: 400, error: { message: 'invalid password' } })).toBe(false);
  });
});
