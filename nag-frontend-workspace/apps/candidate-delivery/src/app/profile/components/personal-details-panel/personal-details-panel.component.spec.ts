import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PersonalDetailsPanelComponent } from './personal-details-panel.component';
import { CandidateProfile } from '../../models';

describe('PersonalDetailsPanelComponent', () => {
  let component: PersonalDetailsPanelComponent;
  let fixture: ComponentFixture<PersonalDetailsPanelComponent>;

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
    preferredLanguage: 'hi',
    kycStatus: 'VERIFIED',
    digiLockerStatus: 'LINKED',
    education: [],
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PersonalDetailsPanelComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(PersonalDetailsPanelComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('profile', { ...mockProfile });
    fixture.detectChanges();
  });

  it('should create and render personal details', () => {
    expect(component).toBeTruthy();
    expect(component.profile().fullName).toBe('Aarav Sharma');
    expect(component.profile().gender).toBe('MALE');
    expect(component.profile().preferredLanguage).toBe('hi');
  });

  it('should expose supported languages including Indic options', () => {
    expect(component.supportedLanguages.length).toBeGreaterThanOrEqual(10);
    expect(component.supportedLanguages.some((l) => l.code === 'hi')).toBe(true);
    expect(component.supportedLanguages.some((l) => l.code === 'ta')).toBe(true);
  });

  it('should display validation error when fullName is empty or too short upon markTouched', () => {
    component.profile.update((p) => ({ ...p, fullName: '' }));
    component.markTouched('fullName');
    expect(component.fullNameError()).toBe('Full legal name is required.');

    component.profile.update((p) => ({ ...p, fullName: 'Al' }));
    expect(component.fullNameError()).toBe('Full legal name must be between 3 and 100 characters.');

    component.profile.update((p) => ({ ...p, fullName: 'Alice Wonderland' }));
    expect(component.fullNameError()).toBeNull();
  });

  it('should display validation error when identityDocNumber is empty upon markTouched', () => {
    component.profile.update((p) => ({ ...p, identityDocNumber: '' }));
    component.markTouched('identityDocNumber');
    expect(component.identityDocNumberError()).toBe('Identity document number is required.');

    component.profile.update((p) => ({ ...p, identityDocNumber: '1234 5678 9012' }));
    expect(component.identityDocNumberError()).toBeNull();
  });
});
