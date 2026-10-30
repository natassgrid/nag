import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ContactDetailsPanelComponent } from './contact-details-panel.component';
import { CandidateProfile } from '../../models';

describe('ContactDetailsPanelComponent', () => {
  let component: ContactDetailsPanelComponent;
  let fixture: ComponentFixture<ContactDetailsPanelComponent>;

  const mockProfile: CandidateProfile = {
    candidateId: 'cand-001',
    fullName: 'Aarav Sharma',
    dateOfBirth: '1998-05-15',
    gender: 'MALE',
    nationality: 'Indian',
    category: 'GENERAL',
    identityDocType: 'AADHAAR',
    identityDocNumber: 'XXXXXXXX1234',
    mobile: '9876543210',
    email: 'aarav@nag.gov.in',
    address: '123 Civil Lines',
    state: 'Delhi',
    city: 'New Delhi',
    pinCode: '110001',
    kycStatus: 'VERIFIED',
    digiLockerStatus: 'LINKED',
    education: [],
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ContactDetailsPanelComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(ContactDetailsPanelComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('profile', { ...mockProfile });
    fixture.detectChanges();
  });

  it('should render contact details and state list', () => {
    expect(component).toBeTruthy();
    expect(component.stateList.length).toBeGreaterThan(0);
    expect(component.profile().mobile).toBe('9876543210');
  });

  it('should update state and reset district on state change', () => {
    component.onStateChange('Maharashtra');
    expect(component.profile().state).toBe('Maharashtra');
  });

  it('should validate mobile number format and pinCode format', () => {
    component.profile.update((p) => ({ ...p, mobile: '12345' }));
    component.markTouched('mobile');
    expect(component.mobileErrorMsg()).toBe(
      'Mobile number must be a valid 10-digit Indian number starting with 6, 7, 8, or 9.'
    );

    component.profile.update((p) => ({ ...p, mobile: '9876543210' }));
    expect(component.mobileErrorMsg()).toBeNull();

    component.profile.update((p) => ({ ...p, pinCode: '012345' }));
    expect(component.pinCodeErrorMsg()).toBe('PIN code must be a valid 6-digit Indian postal code.');

    component.profile.update((p) => ({ ...p, pinCode: '110001' }));
    expect(component.pinCodeErrorMsg()).toBeNull();
  });

  it('should reject invalid mobile number when openMobileModal is invoked', () => {
    component.profile.update((p) => ({ ...p, mobile: '55555' }));
    component.openMobileModal();

    expect(component.isMobileModalOpen()).toBe(true);
    expect(component.mobileError()).toContain('valid 10-digit Indian mobile number');
  });
});
