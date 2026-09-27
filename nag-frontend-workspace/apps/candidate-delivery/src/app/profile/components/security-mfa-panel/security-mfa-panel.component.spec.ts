import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { of } from 'rxjs';
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
});
