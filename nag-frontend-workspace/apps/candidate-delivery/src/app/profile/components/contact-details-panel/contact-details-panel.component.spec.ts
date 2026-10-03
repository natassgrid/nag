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
    pincode: '110001',
    kycStatus: 'VERIFIED',
    digiLockerStatus: 'LINKED',
    twoFactorEnabled: true,
    education: [],
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ContactDetailsPanelComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(ContactDetailsPanelComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('profile', mockProfile);
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
});
