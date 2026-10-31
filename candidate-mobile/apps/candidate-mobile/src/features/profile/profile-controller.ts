/**
 * Candidate Mobile Profile Controller
 * Implements Personal, Contact, Address with State->City cascade,
 * Education CRUD with rigorous validation, DigiLocker KYC, and Category/PwD preferences.
 */

import {
  CandidateProfileData,
  EducationRecord,
  AddressDetails,
  IdentityDocumentRecord,
  PwdAccommodationDetails,
  ProfileCategory,
  ProfileGender,
  ProfileIdentityDocType,
  INDIAN_STATES_AND_CITIES,
  DEFAULT_QUALIFICATIONS,
  DigiLockerClaim,
} from './profile-models';

export {
  CandidateProfileData,
  EducationRecord,
  AddressDetails,
  IdentityDocumentRecord,
  PwdAccommodationDetails,
  ProfileCategory,
  ProfileGender,
  ProfileIdentityDocType,
  INDIAN_STATES_AND_CITIES,
  DEFAULT_QUALIFICATIONS,
  DigiLockerClaim,
};

export interface ProfileState {
  profile: CandidateProfileData;
  availableCities: string[];
  isEditingPersonal: boolean;
  isEditingAddress: boolean;
  editingEducationId: string | null;
  isAddingEducation: boolean;
  validationErrors: Record<string, string>;
  statusMessage: string | null;
  loading: boolean;
}

export const DEFAULT_CANDIDATE_PROFILE: CandidateProfileData = {
  candidateId: 'CND-884210',
  fullName: 'Aryan Sharma',
  dateOfBirth: '2001-08-15',
  gender: 'MALE',
  nationality: 'Indian',
  category: 'GENERAL',
  reservationCategory: 'General (UR)',
  mobile: '9876543210',
  email: 'aryan.sharma@example.gov.in',
  mobileVerified: true,
  emailVerified: true,
  preferredLanguage: 'English',
  address: {
    addressLine: 'Flat 402, Block C, Pragati Vihar',
    state: 'Delhi (NCT)',
    district: 'New Delhi',
    city: 'New Delhi',
    pinCode: '110003',
  },
  identity: {
    docType: 'AADHAAR',
    docNumber: 'XXXXXXXX4920',
    aadhaarLastFour: '4920',
    verified: true,
    verifiedAt: '2026-01-10T11:20:00Z',
    digiLockerLinked: true,
    photoUploaded: true,
    photoUrl: 'https://cdn.nag.gov.in/candidates/CND-884210/photo.jpg',
    signatureUploaded: true,
    signatureUrl: 'https://cdn.nag.gov.in/candidates/CND-884210/sig.jpg',
    biometricConsent: true,
  },
  pwdDetails: {
    isPwd: false,
    requiresScribe: false,
    requiresCompensatoryTime: false,
  },
  education: [
    {
      id: 'edu-1',
      qualification: '10th Standard / Secondary (SSC)',
      courseName: 'All Subjects (CBSE)',
      boardOrUniversity: 'Central Board of Secondary Education (CBSE)',
      institutionName: 'Delhi Public School R.K. Puram',
      passingYear: 2017,
      scoreType: 'PERCENTAGE',
      percentageOrCgpa: '94.6',
      gradeOrDivision: 'Distinction',
      rollNumber: 'CBSE-10-849201',
      verifiedViaDigiLocker: true,
    },
    {
      id: 'edu-2',
      qualification: '12th Standard / Higher Secondary (HSC)',
      courseName: 'Science (Physics, Chemistry, Mathematics, CS)',
      boardOrUniversity: 'Central Board of Secondary Education (CBSE)',
      institutionName: 'Delhi Public School R.K. Puram',
      passingYear: 2019,
      scoreType: 'PERCENTAGE',
      percentageOrCgpa: '96.2',
      gradeOrDivision: 'First Class with Distinction',
      rollNumber: 'CBSE-12-910482',
      verifiedViaDigiLocker: true,
    },
    {
      id: 'edu-3',
      qualification: 'Bachelor Degree (B.Tech / B.E / B.Sc / B.Com / B.A / BBA)',
      courseName: 'B.Tech in Computer Science and Engineering',
      boardOrUniversity: 'Indian Institute of Technology Delhi',
      institutionName: 'IIT Delhi Campus',
      passingYear: 2023,
      scoreType: 'CGPA',
      percentageOrCgpa: '9.34',
      gradeOrDivision: 'First Class with Honours',
      specialization: 'Artificial Intelligence & Systems',
      rollNumber: '2019CS10492',
      verifiedViaDigiLocker: true,
    },
  ],
  digiLockerClaims: [
    {
      id: 'dl-claim-1',
      docType: 'AADHAAR',
      docName: 'Aadhaar Card (UIDAI Verified)',
      issuerName: 'Unique Identification Authority of India',
      docNumber: 'XXXXXXXX4920',
      issuedDate: '2018-05-12',
      verifiedAt: '2026-01-10T11:20:00Z',
      status: 'VERIFIED',
      hashDigest: '0x88fca91029baec4102948ca920184bba',
    },
    {
      id: 'dl-claim-2',
      docType: 'CLASS_X_MARKSHEET',
      docName: 'Class X Secondary Examination Certificate',
      issuerName: 'Central Board of Secondary Education',
      docNumber: 'CBSE-10-849201',
      issuedDate: '2017-06-03',
      verifiedAt: '2026-01-10T11:21:00Z',
      status: 'VERIFIED',
    },
  ],
  profileCompletionPercentage: 100,
};

export class ProfileController {
  private state: ProfileState;
  private readonly listeners = new Set<(state: ProfileState) => void>();

  constructor(initialProfile: CandidateProfileData = DEFAULT_CANDIDATE_PROFILE) {
    const profile = { ...initialProfile, education: [...initialProfile.education] };
    const stateMapping = INDIAN_STATES_AND_CITIES.find(
      (m) => m.state.toLowerCase() === (profile.address?.state || '').toLowerCase()
    );
    const availableCities = stateMapping ? [...stateMapping.districts] : [];

    const completion = this.calculateCompletionPercentage(profile);
    profile.profileCompletionPercentage = completion;

    this.state = {
      profile,
      availableCities,
      isEditingPersonal: false,
      isEditingAddress: false,
      editingEducationId: null,
      isAddingEducation: false,
      validationErrors: {},
      statusMessage: null,
      loading: false,
    };
  }

  public getState(): ProfileState {
    return {
      ...this.state,
      profile: {
        ...this.state.profile,
        address: { ...this.state.profile.address },
        identity: { ...this.state.profile.identity },
        pwdDetails: { ...this.state.profile.pwdDetails },
        education: this.state.profile.education.map((e) => ({ ...e })),
      },
    };
  }

  public subscribe(listener: (state: ProfileState) => void): () => void {
    this.listeners.add(listener);
    listener(this.getState());
    return () => {
      this.listeners.delete(listener);
    };
  }

  private notify(): void {
    const currentState = this.getState();
    for (const listener of this.listeners) {
      listener(currentState);
    }
  }

  /**
   * Calculates profile completion percentage across 5 core sections:
   * 1. Personal Details (20%)
   * 2. Contact & Address (20%)
   * 3. Educational Qualifications (25%)
   * 4. Identity & DigiLocker KYC (25%)
   * 5. Reservation Category & PwD (10%)
   */
  public calculateCompletionPercentage(profile?: CandidateProfileData): number {
    const p = profile || this.state.profile;
    let score = 0;

    // 1. Personal Details (20%)
    if (p.fullName && p.fullName.trim().length > 0) score += 5;
    if (p.dateOfBirth && p.dateOfBirth.length >= 8) score += 5;
    if (p.gender) score += 5;
    if (p.nationality) score += 5;

    // 2. Contact & Address (20%)
    if (p.mobile && p.mobileVerified) score += 5;
    if (p.email && p.emailVerified) score += 5;
    if (p.address?.addressLine && p.address?.addressLine.trim().length > 3) score += 4;
    if (p.address?.state && p.address?.city) score += 3;
    if (p.address?.pinCode && p.address.pinCode.length === 6) score += 3;

    // 3. Education Qualifications (25%)
    if (p.education && p.education.length > 0) {
      score += 15;
      if (p.education.length >= 2) score += 10;
    }

    // 4. Identity & KYC (25%)
    if (p.identity?.docNumber && p.identity?.docNumber.length > 3) score += 5;
    if (p.identity?.verified || p.identity?.digiLockerLinked) score += 10;
    if (p.identity?.photoUploaded) score += 5;
    if (p.identity?.signatureUploaded) score += 5;

    // 5. Category & PwD (10%)
    if (p.category) score += 5;
    if (p.pwdDetails !== undefined) score += 5;

    return Math.min(100, Math.round(score));
  }

  public updatePersonalDetails(
    details: Partial<{
      fullName: string;
      dateOfBirth: string;
      gender: ProfileGender;
      nationality: string;
      preferredLanguage: string;
    }>
  ): void {
    this.state.profile = {
      ...this.state.profile,
      ...details,
    };
    this.state.profile.profileCompletionPercentage = this.calculateCompletionPercentage();
    this.notify();
  }

  public updateContactDetails(mobile: string, email: string): void {
    const mobileChanged = mobile !== this.state.profile.mobile;
    const emailChanged = email !== this.state.profile.email;

    this.state.profile = {
      ...this.state.profile,
      mobile,
      email,
      mobileVerified: mobileChanged ? false : this.state.profile.mobileVerified,
      emailVerified: emailChanged ? false : this.state.profile.emailVerified,
    };
    this.state.profile.profileCompletionPercentage = this.calculateCompletionPercentage();
    this.notify();
  }

  public verifyMobileOtp(otp: string): boolean {
    if (otp && otp.trim() === '123456') {
      this.state.profile.mobileVerified = true;
      this.state.profile.profileCompletionPercentage = this.calculateCompletionPercentage();
      this.notify();
      return true;
    }
    return false;
  }

  public verifyEmailOtp(otp: string): boolean {
    if (otp && otp.trim() === '123456') {
      this.state.profile.emailVerified = true;
      this.state.profile.profileCompletionPercentage = this.calculateCompletionPercentage();
      this.notify();
      return true;
    }
    return false;
  }

  public onStateChange(selectedState: string): void {
    const mapping = INDIAN_STATES_AND_CITIES.find(
      (m) => m.state.toLowerCase() === selectedState.toLowerCase()
    );

    const availableCities = mapping ? [...mapping.districts] : [];
    const firstCity = availableCities.length > 0 ? availableCities[0] : '';

    this.state.availableCities = availableCities;
    this.state.profile.address = {
      ...this.state.profile.address,
      state: selectedState,
      city: firstCity,
      district: firstCity,
    };
    this.state.profile.profileCompletionPercentage = this.calculateCompletionPercentage();
    this.notify();
  }

  public updateAddress(address: Partial<AddressDetails>): void {
    this.state.profile.address = {
      ...this.state.profile.address,
      ...address,
    };
    this.state.profile.profileCompletionPercentage = this.calculateCompletionPercentage();
    this.notify();
  }

  /**
   * Validates educational record data with strict checking:
   * - Board/University must not be empty
   * - Passing year must be reasonable (1970 to next year)
   * - Percentage (0-100) or CGPA (0-10) must be within valid mathematical ranges
   */
  public validateEducationEntry(entry: Partial<EducationRecord>): {
    valid: boolean;
    errors: string[];
  } {
    const errors: string[] = [];

    if (!entry.boardOrUniversity || entry.boardOrUniversity.trim().length === 0) {
      errors.push('Board or University name is required.');
    }

    const currentYear = new Date().getFullYear();
    if (!entry.passingYear || entry.passingYear < 1970 || entry.passingYear > currentYear + 1) {
      errors.push(`Passing year must be between 1970 and ${currentYear + 1}.`);
    }

    const rawScore = entry.percentageOrCgpa != null ? String(entry.percentageOrCgpa).trim() : '';
    const numericScore = parseFloat(rawScore);

    if (!rawScore || isNaN(numericScore)) {
      errors.push('Percentage or CGPA score must be a valid number.');
    } else {
      if (entry.scoreType === 'CGPA') {
        if (numericScore < 0 || numericScore > 10) {
          errors.push('CGPA score must be between 0.0 and 10.0.');
        }
      } else {
        if (numericScore < 0 || numericScore > 100) {
          errors.push('Percentage score must be between 0% and 100%.');
        }
      }
    }

    return {
      valid: errors.length === 0,
      errors,
    };
  }

  public addEducation(
    entry: Omit<EducationRecord, 'id'>
  ): { success: boolean; error?: string; id?: string } {
    const validation = this.validateEducationEntry(entry);
    if (!validation.valid) {
      return { success: false, error: validation.errors.join(' ') };
    }

    const newId = `edu-${Date.now()}`;
    const newRecord: EducationRecord = {
      ...entry,
      id: newId,
    };

    this.state.profile.education = [...this.state.profile.education, newRecord];
    this.state.profile.profileCompletionPercentage = this.calculateCompletionPercentage();
    this.state.isAddingEducation = false;
    this.notify();

    return { success: true, id: newId };
  }

  public updateEducation(
    id: string,
    entry: Partial<EducationRecord>
  ): { success: boolean; error?: string } {
    const existing = this.state.profile.education.find((e) => e.id === id);
    if (!existing) {
      return { success: false, error: 'Education record not found.' };
    }

    const merged = { ...existing, ...entry };
    const validation = this.validateEducationEntry(merged);
    if (!validation.valid) {
      return { success: false, error: validation.errors.join(' ') };
    }

    this.state.profile.education = this.state.profile.education.map((e) =>
      e.id === id ? merged : e
    );
    this.state.profile.profileCompletionPercentage = this.calculateCompletionPercentage();
    this.state.editingEducationId = null;
    this.notify();

    return { success: true };
  }

  public deleteEducation(id: string): boolean {
    const initialLen = this.state.profile.education.length;
    this.state.profile.education = this.state.profile.education.filter((e) => e.id !== id);

    if (this.state.profile.education.length < initialLen) {
      this.state.profile.profileCompletionPercentage = this.calculateCompletionPercentage();
      this.notify();
      return true;
    }
    return false;
  }

  public linkDigiLocker(aadhaarOrDocNumber: string): boolean {
    if (!aadhaarOrDocNumber || aadhaarOrDocNumber.length < 4) {
      return false;
    }

    const last4 = aadhaarOrDocNumber.slice(-4);
    this.state.profile.identity = {
      ...this.state.profile.identity,
      docType: 'AADHAAR',
      docNumber: `XXXXXXXX${last4}`,
      aadhaarLastFour: last4,
      verified: true,
      verifiedAt: new Date().toISOString(),
      digiLockerLinked: true,
    };

    const newClaim: DigiLockerClaim = {
      id: `dl-claim-${Date.now()}`,
      docType: 'AADHAAR',
      docName: 'Aadhaar e-KYC Verified',
      issuerName: 'Unique Identification Authority of India',
      docNumber: `XXXXXXXX${last4}`,
      issuedDate: '2020-01-01',
      verifiedAt: new Date().toISOString(),
      status: 'VERIFIED',
      hashDigest: `SHA256:${last4}NAGKYC`,
    };

    this.state.profile.digiLockerClaims = [
      newClaim,
      ...(this.state.profile.digiLockerClaims || []),
    ];

    this.state.profile.profileCompletionPercentage = this.calculateCompletionPercentage();
    this.notify();
    return true;
  }

  public uploadDocument(type: 'PHOTO' | 'SIGNATURE' | 'ID_PROOF', fileDataUrl: string): void {
    if (type === 'PHOTO') {
      this.state.profile.identity.photoUploaded = true;
      this.state.profile.identity.photoUrl = fileDataUrl;
    } else if (type === 'SIGNATURE') {
      this.state.profile.identity.signatureUploaded = true;
      this.state.profile.identity.signatureUrl = fileDataUrl;
    } else if (type === 'ID_PROOF') {
      this.state.profile.identity.documentUrl = fileDataUrl;
    }

    this.state.profile.profileCompletionPercentage = this.calculateCompletionPercentage();
    this.notify();
  }

  public setBiometricConsent(consent: boolean): void {
    this.state.profile.identity.biometricConsent = consent;
    this.notify();
  }

  public setCategory(category: ProfileCategory): void {
    this.state.profile.category = category;
    this.state.profile.reservationCategory =
      category === 'GENERAL'
        ? 'General (UR)'
        : category === 'OBC_NCL'
        ? 'Other Backward Classes (Non-Creamy Layer)'
        : category === 'SC'
        ? 'Scheduled Caste'
        : category === 'ST'
        ? 'Scheduled Tribe'
        : 'Economically Weaker Section';

    this.state.profile.profileCompletionPercentage = this.calculateCompletionPercentage();
    this.notify();
  }

  public updatePwdPreferences(details: Partial<PwdAccommodationDetails>): void {
    this.state.profile.pwdDetails = {
      ...this.state.profile.pwdDetails,
      ...details,
    };
    this.state.profile.profileCompletionPercentage = this.calculateCompletionPercentage();
    this.notify();
  }

  public saveProfile(): boolean {
    this.state.statusMessage = 'Profile updated successfully.';
    this.notify();
    return true;
  }
}
