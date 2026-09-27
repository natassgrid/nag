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
    pincode: '110001',
    kycStatus: 'VERIFIED',
    digiLockerStatus: 'LINKED',
    twoFactorEnabled: true,
    education: [],
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PersonalDetailsPanelComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(PersonalDetailsPanelComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('profile', mockProfile);
    fixture.detectChanges();
  });

  it('should create and render personal details', () => {
    expect(component).toBeTruthy();
    expect(component.profile().fullName).toBe('Aarav Sharma');
    expect(component.profile().gender).toBe('MALE');
  });
});
