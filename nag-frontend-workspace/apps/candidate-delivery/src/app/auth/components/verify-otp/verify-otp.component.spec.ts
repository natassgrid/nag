import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { VerifyOtpComponent } from './verify-otp.component';
import { AuthService } from '@nag-frontend-workspace/shared-data-access-auth';
import { AuthFlowService } from '../../services/auth-flow.service';

describe('Candidate VerifyOtpComponent', () => {
  let component: VerifyOtpComponent;
  let fixture: ComponentFixture<VerifyOtpComponent>;
  let mockAuthService: {
    verifyOtp: jest.Mock;
    resendEmailOtp: jest.Mock;
  };
  let mockAuthFlowService: {
    navigateToDashboard: jest.Mock;
  };

  beforeEach(async () => {
    mockAuthService = {
      verifyOtp: jest.fn().mockReturnValue(of({ success: true })),
      resendEmailOtp: jest.fn().mockReturnValue(of({ message: 'Sent' })),
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
            queryParams: of({ email: 'candidate@nag.gov.in', userId: 'usr-1' }),
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

  it('should initialize email and userId from route queryParams', () => {
    expect(component.email()).toBe('candidate@nag.gov.in');
    expect(component.userId()).toBe('usr-1');
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
      mobile: undefined,
      otp: '123456',
    });
    expect(mockAuthFlowService.navigateToDashboard).toHaveBeenCalled();
  });
});
