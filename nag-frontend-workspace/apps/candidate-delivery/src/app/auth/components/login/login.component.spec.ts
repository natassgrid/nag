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
    expect(mockAuthService.login).not.toHaveBeenCalled();
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
});
