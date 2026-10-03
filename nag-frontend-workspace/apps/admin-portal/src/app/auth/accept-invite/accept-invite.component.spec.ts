import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AcceptInviteComponent } from './accept-invite.component';
import { AuthService } from '@nag-frontend-workspace/shared-data-access-auth';

describe('Admin AcceptInviteComponent', () => {
  let component: AcceptInviteComponent;
  let fixture: ComponentFixture<AcceptInviteComponent>;
  let mockAuthService: {
    validateInvite: jest.Mock;
    acceptInvite: jest.Mock;
  };
  let mockRouter: {
    navigate: jest.Mock;
  };

  beforeEach(async () => {
    mockAuthService = {
      validateInvite: jest.fn().mockReturnValue(
        of({
          valid: true,
          email: 'newadmin@nag.gov.in',
          fullName: 'New Admin Officer',
          roles: ['ROLE_EXAM_CONTROLLER'],
          tenantId: 'default',
        })
      ),
      acceptInvite: jest.fn().mockReturnValue(
        of({
          accessToken: 'mock-access-token',
          userId: 'admin-002',
        })
      ),
    };

    mockRouter = {
      navigate: jest.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [AcceptInviteComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            queryParams: of({ token: 'valid-test-token-123' }),
          },
        },
        { provide: AuthService, useValue: mockAuthService },
        { provide: Router, useValue: mockRouter },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AcceptInviteComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should validate token and display invitee information on init', () => {
    expect(mockAuthService.validateInvite).toHaveBeenCalledWith('valid-test-token-123');
    expect(component.tokenValid()).toBe(true);
    expect(component.invitationData()?.email).toBe('newadmin@nag.gov.in');
  });

  it('should show validation error when invitation token is invalid or expired', () => {
    mockAuthService.validateInvite.mockReturnValue(
      throwError(() => ({ status: 400, error: { message: 'Token expired' } }))
    );
    component.validateInvitation('expired-token');

    expect(component.tokenValid()).toBe(false);
    expect(component.validationError()).toContain('Token expired');
  });

  it('should require minimum 8 character password before submission', () => {
    component.password = 'short';
    component.confirmPassword = 'short';
    component.totpCode = '123456';
    component.handleAcceptInvite();

    expect(component.errorMessage()).toContain('at least 8 characters');
    expect(mockAuthService.acceptInvite).not.toHaveBeenCalled();
  });

  it('should require matching passwords before submission', () => {
    component.password = 'Password123!';
    component.confirmPassword = 'DifferentPassword123!';
    component.totpCode = '123456';
    component.handleAcceptInvite();

    expect(component.errorMessage()).toContain('do not match');
    expect(mockAuthService.acceptInvite).not.toHaveBeenCalled();
  });

  it('should require 6-digit TOTP code before submission', () => {
    component.password = 'Password123!';
    component.confirmPassword = 'Password123!';
    component.totpCode = '12';
    component.handleAcceptInvite();

    expect(component.errorMessage()).toContain('6-digit');
    expect(mockAuthService.acceptInvite).not.toHaveBeenCalled();
  });

  it('should call acceptInvite with token, password, and TOTP secret/code on valid form submit', () => {
    component.password = 'Password123!';
    component.confirmPassword = 'Password123!';
    component.totpCode = '654321';
    component.handleAcceptInvite();

    expect(mockAuthService.acceptInvite).toHaveBeenCalledWith(
      expect.objectContaining({
        token: 'valid-test-token-123',
        password: 'Password123!',
        totpCode: '654321',
      })
    );
    expect(component.successMessage()).toContain('Account activated');
  });
});
