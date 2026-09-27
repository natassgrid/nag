import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DigiLockerPanelComponent } from './digilocker-panel.component';
import { CandidateProfile } from '../../models';

describe('DigiLockerPanelComponent', () => {
  let component: DigiLockerPanelComponent;
  let fixture: ComponentFixture<DigiLockerPanelComponent>;

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
      imports: [DigiLockerPanelComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(DigiLockerPanelComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('profile', mockProfile);
    fixture.detectChanges();
  });

  it('should render DigiLocker panel and open consent modal', () => {
    expect(component).toBeTruthy();
    component.openConnectFlow();
    expect(component.showConsentModal()).toBe(true);
    expect(component.otpStep()).toBe(false);
  });
});
