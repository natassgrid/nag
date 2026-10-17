import { ComponentFixture, TestBed } from '@angular/core/testing';
import { KycDocumentsPanelComponent } from './kyc-documents-panel.component';
import { CandidateProfile } from '../../models';

describe('KycDocumentsPanelComponent', () => {
  let component: KycDocumentsPanelComponent;
  let fixture: ComponentFixture<KycDocumentsPanelComponent>;

  const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

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
      imports: [KycDocumentsPanelComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(KycDocumentsPanelComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('profile', { ...mockProfile });
    fixture.detectChanges();
  });

  it('should render KYC documents panel and handle webcam trigger', () => {
    expect(component).toBeTruthy();
    component.openWebcam();
    expect(component.showWebcamModal()).toBe(true);

    component.showWebcamModal.set(false);
    expect(component.showWebcamModal()).toBe(false);
  });

  it('should assign a valid RFC 4122 UUID to photoAssetId when photo is cropped', () => {
    component.cropperTarget.set('photo');
    component.onCroppedImage('data:image/png;base64,mockCroppedPhoto');

    const updated = component.profile();
    expect(updated.photoUrl).toBe('data:image/png;base64,mockCroppedPhoto');
    expect(updated.photoAssetId).toBeDefined();
    expect(updated.photoAssetId).toMatch(UUID_REGEX);
    expect(updated.photoAssetId).not.toContain('asset-photo-');
  });

  it('should assign a valid RFC 4122 UUID to signatureAssetId when signature is cropped', () => {
    component.cropperTarget.set('signature');
    component.onCroppedImage('data:image/png;base64,mockCroppedSignature');

    const updated = component.profile();
    expect(updated.signatureUrl).toBe('data:image/png;base64,mockCroppedSignature');
    expect(updated.signatureAssetId).toBeDefined();
    expect(updated.signatureAssetId).toMatch(UUID_REGEX);
    expect(updated.signatureAssetId).not.toContain('asset-sign-');
  });

  it('should clear document asset IDs on removeDocument', () => {
    component.cropperTarget.set('photo');
    component.onCroppedImage('data:image/png;base64,mockPhoto');
    expect(component.profile().photoAssetId).toBeDefined();

    component.removeDocument('photo');
    expect(component.profile().photoAssetId).toBeUndefined();
    expect(component.profile().photoUrl).toBeUndefined();

    component.removeDocument('signature');
    expect(component.profile().signatureAssetId).toBeUndefined();

    component.removeDocument('idProof');
    expect(component.profile().idProofAssetId).toBeUndefined();
  });
});
