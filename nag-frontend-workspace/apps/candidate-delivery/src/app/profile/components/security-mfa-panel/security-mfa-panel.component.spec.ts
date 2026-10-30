import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { SecurityMfaPanelComponent } from './security-mfa-panel.component';
import { AuthService } from '@nag-frontend-workspace/shared-data-access-auth';

describe('SecurityMfaPanelComponent', () => {
  let component: SecurityMfaPanelComponent;
  let fixture: ComponentFixture<SecurityMfaPanelComponent>;
  let mockAuthService: {
    currentUser: any;
    getTotpStatus: jest.Mock;
    setupTotp: jest.Mock;
    verifyTotp: jest.Mock;
    verifyTotpSetup: jest.Mock;
    disableTotp: jest.Mock;
  };

  beforeEach(async () => {
    mockAuthService = {
      currentUser: signal({ userId: 'cand-001', username: 'aarav@nag.gov.in' }),
      getTotpStatus: jest.fn().mockReturnValue(of({ mfaEnabled: false })),
      setupTotp: jest.fn().mockReturnValue(
        of({
          secretKey: 'JBSWY3DPEHPK3PXP',
          qrCodeUri: 'otpauth://totp/NAG:test?secret=JBSWY3DPEHPK3PXP',
          backupCodes: ['CODE1', 'CODE2'],
        })
      ),
      verifyTotp: jest.fn().mockReturnValue(of({ success: true })),
      verifyTotpSetup: jest.fn().mockReturnValue(of({ success: true })),
      disableTotp: jest.fn().mockReturnValue(of({ success: true })),
    };

    await TestBed.configureTestingModule({
      imports: [SecurityMfaPanelComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: mockAuthService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SecurityMfaPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should refresh MFA status on init', () => {
    expect(mockAuthService.getTotpStatus).toHaveBeenCalledWith('cand-001');
    expect(component.isMfaActive()).toBe(false);
  });

  it('should start TOTP enrollment flow', () => {
    component.startEnrollment();
    expect(mockAuthService.setupTotp).toHaveBeenCalledWith('aarav@nag.gov.in');
    expect(component.setupStep()).toBe('enroll');
    expect(component.totpData()?.secretKey).toBe('JBSWY3DPEHPK3PXP');
  });

  it('should validate 6-digit numeric TOTP code in confirmTotpSetup', () => {
    component.startEnrollment();

    component.verifyCode.set('123');
    component.confirmTotpSetup();
    expect(component.errorMsg()).toBe('Please enter a valid 6-digit numeric authenticator OTP code.');
    expect(mockAuthService.verifyTotpSetup).not.toHaveBeenCalled();

    component.verifyCode.set('1234AB');
    component.confirmTotpSetup();
    expect(component.errorMsg()).toBe('Please enter a valid 6-digit numeric authenticator OTP code.');
    expect(mockAuthService.verifyTotpSetup).not.toHaveBeenCalled();
  });

  it('should parse backend HTTP error response on TOTP verification error', () => {
    mockAuthService.verifyTotpSetup.mockReturnValue(
      throwError(() => ({
        status: 400,
        error: { message: 'Invalid TOTP token' },
      }))
    );

    component.startEnrollment();
    component.verifyCode.set('123456');
    component.confirmTotpSetup();

    expect(component.errorMsg()).toBe('Invalid TOTP token');
    expect(component.isMfaActive()).toBe(false);
  });
});
