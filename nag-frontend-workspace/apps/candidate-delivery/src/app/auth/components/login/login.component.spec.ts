import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { LoginComponent } from './login.component';
import { AuthService } from '@nag-frontend-workspace/shared-data-access-auth';
import { NotificationService } from '@nag-frontend-workspace/shared-ui-components';
import { AuthFlowService } from '../../services/auth-flow.service';

describe('Candidate LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let mockAuthService: {
    isAuthenticated: jest.Mock;
    login: jest.Mock;
  };
  let mockAuthFlowService: {
    navigateToDashboard: jest.Mock;
    navigateToVerifyOtp: jest.Mock;
    isUnverifiedAccountError: jest.Mock;
  };
  let mockNotificationService: {
    showSuccess: jest.Mock;
    showError: jest.Mock;
    success: jest.Mock;
    error: jest.Mock;
  };

  beforeEach(async () => {
    mockAuthService = {
      isAuthenticated: jest.fn().mockReturnValue(false),
      login: jest.fn().mockReturnValue(of({ token: 'test-token' })),
    };
    mockAuthFlowService = {
      navigateToDashboard: jest.fn(),
      navigateToVerifyOtp: jest.fn(),
      isUnverifiedAccountError: jest.fn().mockReturnValue(false),
    };
    mockNotificationService = {
      showSuccess: jest.fn(),
      showError: jest.fn(),
      success: jest.fn(),
      error: jest.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthService, useValue: mockAuthService },
        { provide: AuthFlowService, useValue: mockAuthFlowService },
        { provide: NotificationService, useValue: mockNotificationService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and display validation errors for empty fields', () => {
    component.username = '';
    component.password = '';
    component.handleLogin();

    expect(component.errorMessage()).toContain('Please enter both your email/mobile and password');
    expect(component.fieldErrors()['username']).toBe('Please enter your email or registered mobile.');
    expect(component.fieldErrors()['password']).toBe('Password is required.');
    expect(mockAuthService.login).not.toHaveBeenCalled();
  });

  it('should validate invalid numeric phone or invalid email format', () => {
    component.username = '12345';
    component.validateSingleField('username');
    expect(component.fieldErrors()['username']).toBe(
      'Mobile number must be a valid 10-digit Indian number starting with 6, 7, 8, or 9.'
    );

    component.username = 'test@';
    component.validateSingleField('username');
    expect(component.fieldErrors()['username']).toBe('Please enter a valid email address.');

    component.username = '9876543210';
    component.validateSingleField('username');
    expect(component.fieldErrors()['username']).toBeUndefined();
  });

  it('should validate otpCode when mfaStepRequired is true', () => {
    component.mfaStepRequired.set(true);
    component.otpCode = '12';
    component.handleLogin();

    expect(component.fieldErrors()['otpCode']).toBe('Please enter a valid 6-digit numeric OTP code.');
    expect(mockAuthService.login).not.toHaveBeenCalled();
  });

  it('should validate resetEmail on forgot password', () => {
    component.resetEmail = 'invalid';
    component.handleForgotPassword();

    expect(component.fieldErrors()['resetEmail']).toBe('Please enter a valid email address.');
  });

  it('should call authService.login and navigate to dashboard on success', () => {
    component.username = 'candidate@nag.gov.in';
    component.password = 'SecurePass!123';
    component.handleLogin();

    expect(mockAuthService.login).toHaveBeenCalledWith({
      username: 'candidate@nag.gov.in',
      password: 'SecurePass!123',
    });
    expect(mockAuthFlowService.navigateToDashboard).toHaveBeenCalled();
  });

  it('should switch to MFA step when server responds with MFA challenge', () => {
    mockAuthService.login.mockReturnValue(
      throwError(() => ({
        status: 403,
        error: { message: 'MFA challenge required' },
      }))
    );

    component.username = 'candidate@nag.gov.in';
    component.password = 'SecurePass!123';
    component.handleLogin();

    expect(component.mfaStepRequired()).toBe(true);
    expect(component.errorMessage()).toContain('Two-Factor Authentication required');
  });

  it('should parse backend HTTP error response and display field-level errors', () => {
    mockAuthService.login.mockReturnValue(
      throwError(() => ({
        status: 400,
        error: {
          fieldErrors: {
            username: 'Account does not exist with this mobile number',
          },
        },
      }))
    );

    component.username = '9876543210';
    component.password = 'Password@123';
    component.handleLogin();

    expect(component.errorMessage()).toBe('Account does not exist with this mobile number');
    expect(component.fieldErrors()['username']).toBe('Account does not exist with this mobile number');
  });
});
