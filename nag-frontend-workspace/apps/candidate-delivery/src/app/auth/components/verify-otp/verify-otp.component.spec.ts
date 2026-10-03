import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { VerifyOtpComponent } from './verify-otp.component';
import { AuthService } from '@nag-frontend-workspace/shared-data-access-auth';
import { AuthFlowService } from '../../services/auth-flow.service';

describe('Candidate VerifyOtpComponent', () => {
  let component: VerifyOtpComponent;
  let fixture: ComponentFixture<VerifyOtpComponent>;
  let mockAuthService: {
    verifyOtp: jest.Mock;
    resendEmailOtp: jest.Mock;
    resendSmsOtp: jest.Mock;
    getVerificationStatus: jest.Mock;
  };
  let mockAuthFlowService: {
    navigateToDashboard: jest.Mock;
  };

  beforeEach(async () => {
    mockAuthService = {
      verifyOtp: jest.fn().mockReturnValue(of({ success: true })),
      resendEmailOtp: jest.fn().mockReturnValue(of({ message: 'Sent' })),
      resendSmsOtp: jest.fn().mockReturnValue(of({ message: 'SMS OTP dispatched' })),
      getVerificationStatus: jest.fn().mockReturnValue(of({ remainingSmsQuota: 3, maskedMobile: '9876543210' })),
    };
    mockAuthFlowService = {
      navigateToDashboard: jest.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [VerifyOtpComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            queryParams: of({ email: 'candidate@nag.gov.in', userId: 'usr-1', mobile: '9876543210' }),
          },
        },
        { provide: AuthService, useValue: mockAuthService },
        { provide: AuthFlowService, useValue: mockAuthFlowService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(VerifyOtpComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    component.ngOnDestroy();
  });

  it('should initialize email, userId, and masked mobile phone from route queryParams', () => {
    expect(component.email()).toBe('candidate@nag.gov.in');
    expect(component.userId()).toBe('usr-1');
    expect(component.maskedPhone()).toBe('+91 ******3210');
  });

  it('should validate 6-digit OTP code before submit', () => {
    component.otpCode = '123';
    component.handleVerifyOtp();

    expect(component.errorMessage()).toContain('6-digit');
    expect(mockAuthService.verifyOtp).not.toHaveBeenCalled();
  });

  it('should call verifyOtp and navigate on valid 6-digit code', () => {
    component.otpCode = '123456';
    component.handleVerifyOtp();

    expect(mockAuthService.verifyOtp).toHaveBeenCalledWith({
      userId: 'usr-1',
      email: 'candidate@nag.gov.in',
      mobile: '9876543210',
      otp: '123456',
    });
    expect(mockAuthFlowService.navigateToDashboard).toHaveBeenCalled();
  });

  it('should switch between Email and Mobile tabs and decrement weekly SMS quota on SMS dispatch', () => {
    component.switchTab('mobile');
    expect(component.activeTab()).toBe('mobile');
    expect(component.smsQuotaRemaining()).toBe(3);

    component.resendCountdown.set(0);
    component.resendOtp();

    expect(mockAuthService.resendSmsOtp).toHaveBeenCalled();
    expect(component.smsQuotaRemaining()).toBe(2);
    expect(component.successMessage()).toContain('SMS OTP dispatched');
  });

  it('should handle HTTP 429 quota exhausted when sending SMS', () => {
    mockAuthService.resendSmsOtp.mockReturnValue(throwError(() => ({ status: 429, error: { message: 'Rate limit' } })));
    component.switchTab('mobile');
    component.resendCountdown.set(0);
    component.resendOtp();

    expect(component.smsQuotaRemaining()).toBe(0);
    expect(component.errorMessage()).toContain('Weekly SMS limit reached');
  });

  it('should clear input and error message when clearInput is triggered', () => {
    component.otpCode = '123456';
    component.errorMessage.set('Some error');
    component.clearInput();

    expect(component.otpCode).toBe('');
    expect(component.errorMessage()).toBeNull();
  });
});
