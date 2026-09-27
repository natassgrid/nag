import { ComponentFixture, TestBed } from '@angular/core/testing';
import { KycDocumentsPanelComponent } from './kyc-documents-panel.component';
import { CandidateProfile } from '../../models';

describe('KycDocumentsPanelComponent', () => {
  let component: KycDocumentsPanelComponent;
  let fixture: ComponentFixture<KycDocumentsPanelComponent>;

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
      imports: [KycDocumentsPanelComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(KycDocumentsPanelComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('profile', mockProfile);
    fixture.detectChanges();
  });

  it('should render KYC documents panel and handle webcam trigger', () => {
    expect(component).toBeTruthy();
    component.openWebcam();
    expect(component.showWebcamModal()).toBe(true);

    component.showWebcamModal.set(false);
    expect(component.showWebcamModal()).toBe(false);
  });
});
