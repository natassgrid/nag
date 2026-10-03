import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AdminLoginComponent } from './admin-login.component';
import { AuthService } from '@nag-frontend-workspace/shared-data-access-auth';

describe('AdminLoginComponent', () => {
  let component: AdminLoginComponent;
  let fixture: ComponentFixture<AdminLoginComponent>;
  let mockAuthService: {
    isAuthenticated: jest.Mock;
    login: jest.Mock;
  };
  let mockRouter: {
    navigate: jest.Mock;
  };

  beforeEach(async () => {
    mockAuthService = {
      isAuthenticated: jest.fn().mockReturnValue(false),
      login: jest.fn().mockReturnValue(of({ token: 'jwt-admin-token' })),
    };
    mockRouter = {
      navigate: jest.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [AdminLoginComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: mockAuthService },
        { provide: Router, useValue: mockRouter },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminLoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should authenticate officer and navigate to dashboard', () => {
    component.username = 'superadmin';
    component.password = 'AdminPass123!';
    component.handleLogin();

    expect(mockAuthService.login).toHaveBeenCalledWith({
      username: 'superadmin',
      password: 'AdminPass123!',
    });
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('should handle MFA challenge requirement', () => {
    mockAuthService.login.mockReturnValue(
      throwError(() => ({
        status: 403,
        error: { message: '2FA authentication code required' },
      }))
    );

    component.username = 'superadmin';
    component.password = 'AdminPass123!';
    component.handleLogin();

    expect(component.mfaStepRequired()).toBe(true);
    expect(component.errorMessage()).toContain('Two-Factor Authentication required');
  });
});
